/*******************************************************************************
 * Copyright (c) 2008, 2015 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IMemoryBlockManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.core.model.IMemoryBlockRetrievalExtension;
import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxyFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.debug.ui.memory.IMemoryRenderingContainer;
import org.eclipse.debug.ui.memory.IMemoryRenderingManager;
import org.eclipse.debug.ui.memory.IMemoryRenderingSite;
import org.eclipse.debug.ui.memory.IMemoryRenderingType;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tcf.internal.debug.model.ITCFConstants;
import org.eclipse.tcf.internal.debug.model.TCFLaunch;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IExpressions;
import org.eclipse.tcf.services.IMemory;
import org.eclipse.tcf.services.IMemory.MemoryError;
import org.eclipse.tcf.services.ISymbols;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A memory block allows the user interface to request a memory block data from the debugger when needed.
 * TCF memory block is based on TCF Memory service.
 */
class TCFMemoryBlock extends PlatformObject implements IMemoryBlockExtension, IModelProxyFactory {

    private static class MemData {

        final BigInteger addr;
        final MemoryByte[] data;
        final byte[] bytes;

        MemData(BigInteger addr, MemoryByte[] data) {
            int i = 0;
            this.addr = addr;
            this.data = data;
            this.bytes = new byte[data.length];
            for (MemoryByte b : data) bytes[i++] = b.getValue();
        }
    }

    private static class ModelProxy extends AbstractModelProxy implements Runnable {

        final TCFMemoryBlock mem_block;
        final Display display;

        ModelDelta delta;

        public ModelProxy(TCFMemoryBlock mem_block, Display display) {
            this.mem_block = mem_block;
            this.display = display;
        }

        @Override
        public void installed(Viewer viewer) {
            synchronized (mem_block.model_proxies) {
                if (isDisposed()) return;
                setInstalled(true);
                super.installed(viewer);
                mem_block.model_proxies.add(this);
            }
        }

        @Override
        public void dispose() {
            synchronized (mem_block.model_proxies) {
                if (isDisposed()) return;
                mem_block.model_proxies.remove(this);
                super.dispose();
            }
        }

        void onMemoryChanged(boolean suspended) {
            assert Protocol.isDispatchThread();
            int flags = IModelDelta.CONTENT;
            if (suspended) flags |= IModelDelta.STATE;
            if (delta != null) {
                delta.setFlags(delta.getFlags() | flags);
            }
            else {
                delta = new ModelDelta(mem_block, flags);
                Protocol.invokeLater(this);
            }
        }

        @Override
        public void run() {
            // Note: double posting is necessary to avoid deadlocks
            assert Protocol.isDispatchThread();
            final ModelDelta d = delta;
            delta = null;
            synchronized (Device.class) {
                if (!display.isDisposed()) {
                    display.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            fireModelChanged(d);
                        }
                    });
                }
            }
        }
    }

    private final TCFModel model;
    private final IMemoryBlockRetrieval block_retrieval;
    private final String ctx_id;
    private final String expression;
    private final long length;
    private final Set<Object> connections = new HashSet<Object>();
    private final TCFDataCache<IExpressions.Expression> remote_expression;
    private final TCFDataCache<IExpressions.Value> expression_value;
    private final LinkedList<ModelProxy> model_proxies = new LinkedList<ModelProxy>();

    private MemData mem_data; // current memory block data
    private MemData mem_prev; // previous data - before last suspend
    private MemData mem_last; // last retrieved memory block data

    private boolean disposed;

    TCFMemoryBlock(final TCFModel model, IMemoryBlockRetrieval block_retrieval, final String ctx_id, final String expression, long length) {
        assert Protocol.isDispatchThread();
        this.model = model;
        this.block_retrieval = block_retrieval;
        this.ctx_id = ctx_id;
        this.expression = expression;
        this.length = length;
        final TCFLaunch launch = model.getLaunch();
        final IChannel channel = launch.getChannel();
        remote_expression = new TCFDataCache<IExpressions.Expression>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                IExpressions exps = launch.getService(IExpressions.class);
                if (exps == null) {
                    set(null, new Exception("Expressions service not available"), null);
                    return true;
                }
                command = exps.create(ctx_id, null, expression, new IExpressions.DoneCreate() {
                    @Override
                    public void doneCreate(IToken token, Exception error, IExpressions.Expression context) {
                        TCFNode node = model.getNode(ctx_id);
                        if (node == null) {
                            if (context != null) {
                                IExpressions exps = channel.getRemoteService(IExpressions.class);
                                exps.dispose(context.getID(), new IExpressions.DoneDispose() {
                                    @Override
                                    public void doneDispose(IToken token, Exception error) {
                                        if (error == null) return;
                                        if (channel.getState() != IChannel.STATE_OPEN) return;
                                        Activator.log("Error disposing remote expression evaluator", error);
                                    }
                                });
                            }
                            return;
                        }
                        set(token, error, context);
                    }
                });
                return false;
            }
            @Override
            public void reset() {
                if (isValid() && getData() != null) {
                    if (channel.getState() == IChannel.STATE_OPEN) {
                        IExpressions exps = channel.getRemoteService(IExpressions.class);
                        exps.dispose(remote_expression.getData().getID(), new IExpressions.DoneDispose() {
                            @Override
                            public void doneDispose(IToken token, Exception error) {
                                if (error == null) return;
                                if (channel.getState() != IChannel.STATE_OPEN) return;
                                Activator.log("Error disposing remote expression evaluator", error);
                            }
                        });
                    }
                }
                super.reset();
            }
        };
        expression_value = new TCFDataCache<IExpressions.Value>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                if (!remote_expression.validate(this)) return false;
                final IExpressions.Expression ctx = remote_expression.getData();
                if (ctx == null) {
                    set(null, remote_expression.getError(), null);
                    return true;
                }
                IExpressions exps = launch.getService(IExpressions.class);
                command = exps.evaluate(ctx.getID(), new IExpressions.DoneEvaluate() {
                    @Override
                    public void doneEvaluate(IToken token, Exception error, IExpressions.Value value) {
                        set(token, error, value);
                    }
                });
                return false;
            }
        };
    }

    @Override
    public synchronized void connect(Object client) {
        connections.add(client);
    }

    @Override
    public synchronized void disconnect(Object client) {
        connections.remove(client);
    }

    @Override
    public synchronized Object[] getConnections() {
        return connections.toArray(new Object[connections.size()]);
    }

    @Override
    public void dispose() throws DebugException {
        if (disposed) return;
        new TCFDebugTask<Boolean>() {
            @Override
            public void run() {
                if (!disposed) {
                    onContextExited(ctx_id);
                    expression_value.dispose();
                    remote_expression.dispose();
                    disposed = true;
                }
                done(Boolean.TRUE);
            }
        }.getD();
    }

    @Override
    public int getAddressSize() throws DebugException {
        return new TCFDebugTask<Integer>(model.getChannel()) {
            @Override
            public void run() {
                TCFNode node = model.getNode(ctx_id);
                if (node == null) {
                    error("Context is disposed");
                }
                else if (node instanceof TCFNodeExecContext) {
                    TCFDataCache<IMemory.MemoryContext> cache = ((TCFNodeExecContext)node).getMemoryContext();
                    if (!cache.validate(this)) return;
                    if (cache.getError() != null) {
                        error(cache.getError());
                    }
                    else {
                        IMemory.MemoryContext mem = cache.getData();
                        if (mem == null) {
                            error("Context does not provide memory access");
                        }
                        else {
                            done(mem.getAddressSize());
                        }
                    }
                }
                else {
                    error("Context does not provide memory access");
                }
            }
        }.getD();
    }

    @Override
    public int getAddressableSize() throws DebugException {
        return new TCFDebugTask<Integer>(model.getChannel()) {
            @Override
            public void run() {
                TCFNode node = model.getNode(ctx_id);
                if (node == null) {
                    error("Context is disposed");
                }
                else if (node instanceof TCFNodeExecContext) {
                    TCFDataCache<IMemory.MemoryContext> cache = ((TCFNodeExecContext)node).getMemoryContext();
                    if (!cache.validate(this)) return;
                    if (cache.getError() != null) {
                        error(cache.getError());
                    }
                    else {
                        IMemory.MemoryContext mem = cache.getData();
                        if (mem == null) {
                            error("Context does not provide memory access");
                        }
                        else {
                            done(mem.getAddressableUnitSize());
                        }
                    }
                }
                else {
                    error("Context does not provide memory access");
                }
            }
        }.getD();
    }

    @Override
    public BigInteger getBigBaseAddress() throws DebugException {
        return new TCFDebugTask<BigInteger>(model.getChannel()) {
            @Override
            public void run() {
                if (!expression_value.validate()) {
                    expression_value.wait(this);
                }
                else if (expression_value.getError() != null) {
                    error(expression_value.getError());
                }
                else if (expression_value.getData() == null) {
                    error("Address expression evaluation failed");
                }
                else {
                    IExpressions.Value value = expression_value.getData();
                    if (value.getTypeClass() == ISymbols.TypeClass.array) {
                        BigInteger addr = JSON.toBigInteger(value.getAddress());
                        if (addr == null) {
                            error("Invalid expression: array without memory address");
                        }
                        else {
                            done(addr);
                        }
                    }
                    else {
                        byte[] data = value.getValue();
                        if (data == null || data.length == 0) {
                            error("Address expression value is empty (void)");
                        }
                        else {
                            boolean signed = value.getTypeClass() == ISymbols.TypeClass.integer;
                            done(TCFNumberFormat.toBigInteger(data, value.isBigEndian(), signed));
                        }
                    }
                }
            }
        }.getD();
    }

    @Override
    public MemoryByte[] getBytesFromAddress(final BigInteger address, final long units) throws DebugException {
        return new TCFDebugTask<MemoryByte[]>(model.getChannel()) {
            int offs = 0;
            @Override
            public void run() {
                if (mem_data != null &&
                        address.compareTo(mem_data.addr) >= 0 &&
                        address.add(BigInteger.valueOf(units)).compareTo(
                                mem_data.addr.add(BigInteger.valueOf(mem_data.data.length))) <= 0) {
                    offs = address.subtract(mem_data.addr).intValue();
                    MemoryByte[] res = mem_data.data;
                    if (units < mem_data.data.length) {
                        res = new MemoryByte[(int)units];
                        System.arraycopy(mem_data.data, offs, res, 0, res.length);
                    }
                    setHistoryFlags();
                    done(res);
                    return;
                }
                TCFNode node = model.getNode(ctx_id);
                if (node == null) {
                    error("Context is disposed");
                    return;
                }
                if (!(node instanceof TCFNodeExecContext)) {
                    error("Context does not provide memory access");
                    return;
                }
                TCFDataCache<IMemory.MemoryContext> cache = ((TCFNodeExecContext)node).getMemoryContext();
                if (!cache.validate(this)) return;
                if (cache.getError() != null) {
                    error(cache.getError());
                    return;
                }
                final IMemory.MemoryContext mem = cache.getData();
                if (mem == null) {
                    error("Context does not provide memory access");
                    return;
                }
                final int size = (int)units;
                final int mode = IMemory.MODE_CONTINUEONERROR | IMemory.MODE_VERIFY;
                final byte[] buf = new byte[size];
                final MemoryByte[] res = new MemoryByte[size];
                mem.get(address, 1, buf, 0, size, mode, new IMemory.DoneMemory() {
                    @Override
                    public void doneMemory(IToken token, MemoryError error) {
                        int big_endian = 0;
                        if (mem.getProperties().get(IMemory.PROP_BIG_ENDIAN) != null) {
                            big_endian |= MemoryByte.ENDIANESS_KNOWN;
                            if (mem.isBigEndian()) big_endian |= MemoryByte.BIG_ENDIAN;
                        }
                        int cnt = 0;
                        while (offs < size) {
                            int flags = big_endian;
                            if (error instanceof IMemory.ErrorOffset) {
                                IMemory.ErrorOffset ofs = (IMemory.ErrorOffset)error;
                                int status = ofs.getStatus(cnt);
                                if (status == IMemory.ErrorOffset.BYTE_VALID) {
                                    flags |= MemoryByte.READABLE | MemoryByte.WRITABLE;
                                }
                                else if ((status & IMemory.ErrorOffset.BYTE_UNKNOWN) != 0) {
                                    if (cnt > 0) break;
                                }
                            }
                            else if (error == null) {
                                flags |= MemoryByte.READABLE | MemoryByte.WRITABLE;
                            }
                            res[offs] = new MemoryByte(buf[offs], (byte)flags);
                            offs++;
                            cnt++;
                        }
                        if (offs < size) {
                            mem.get(address.add(BigInteger.valueOf(offs)), 1, buf, offs, size - offs, mode, this);
                        }
                        else {
                            mem_last = mem_data = new MemData(address, res);
                            setHistoryFlags();
                            done(res);
                        }
                    }
                });
            }
        }.getD();
    }

    private void setHistoryFlags() {
        if (mem_data == null) return;
        BigInteger addr = mem_data.addr;
        BigInteger his_start = null;
        BigInteger his_end = null;
        if (mem_prev != null) {
            his_start = mem_prev.addr;
            his_end = mem_prev.addr.add(BigInteger.valueOf(mem_prev.data.length));
        }
        for (MemoryByte b : mem_data.data) {
            int flags = b.getFlags();
            if (mem_prev != null && addr.compareTo(his_start) >= 0 && addr.compareTo(his_end) < 0) {
                flags |= MemoryByte.HISTORY_KNOWN;
                int offs = addr.subtract(his_start).intValue();
                if (b.getValue() != mem_prev.data[offs].getValue()) {
                    flags |= MemoryByte.CHANGED;
                }
            }
            else {
                flags &= ~(MemoryByte.HISTORY_KNOWN | MemoryByte.CHANGED);
            }
            b.setFlags((byte)flags);
            addr = addr.add(BigInteger.valueOf(1));
        }
    }

    @Override
    public MemoryByte[] getBytesFromOffset(BigInteger offset, long units) throws DebugException {
        return getBytesFromAddress(getBigBaseAddress().add(offset), units);
    }

    @Override
    public String getExpression() {
        return expression;
    }

    @Override
    public IMemoryBlockRetrieval getMemoryBlockRetrieval() {
        return block_retrieval;
    }

    @Override
    public long getStartAddress() {
        return 0; // Unbounded
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public BigInteger getMemoryBlockStartAddress() throws DebugException {
        return null; // Unbounded
    }

    @Override
    public BigInteger getMemoryBlockEndAddress() throws DebugException {
        return null; // Unbounded
    }

    @Override
    public BigInteger getBigLength() throws DebugException {
        return BigInteger.valueOf(length);
    }

    @Override
    public void setBaseAddress(BigInteger address) throws DebugException {
    }

    @Override
    public void setValue(BigInteger offset, final byte[] bytes) throws DebugException {
        final BigInteger address = getBigBaseAddress().add(offset);
        new TCFDebugTask<Object>(model.getChannel()) {
            @Override
            public void run() {
                TCFNode node = model.getNode(ctx_id);
                if (node == null) {
                    error("Context is disposed");
                    return;
                }
                if (!(node instanceof TCFNodeExecContext)) {
                    error("Context does not provide memory access");
                    return;
                }
                TCFDataCache<IMemory.MemoryContext> cache = ((TCFNodeExecContext)node).getMemoryContext();
                if (!cache.validate(this)) return;
                if (cache.getError() != null) {
                    error(cache.getError());
                    return;
                }
                final IMemory.MemoryContext mem = cache.getData();
                if (mem == null) {
                    error("Context does not provide memory access");
                    return;
                }
                final int mode = IMemory.MODE_CONTINUEONERROR | IMemory.MODE_VERIFY;
                mem.set(address, 1, bytes, 0, bytes.length, mode, new IMemory.DoneMemory() {
                    @Override
                    public void doneMemory(IToken token, MemoryError error) {
                        if (error != null) {
                            error(error);
                        }
                        else {
                            done(null);
                        }
                    }
                });
            }
        }.getD();
    }

    @Override
    public boolean supportBaseAddressModification() throws DebugException {
        return false;
    }

    @Override
    public boolean supportsChangeManagement() {
        return true;
    }

    @Override
    public byte[] getBytes() throws DebugException {
        if (mem_data == null) return null;
        return mem_data.bytes;
    }

    @Override
    public void setValue(long offset, byte[] bytes) throws DebugException {
        setValue(BigInteger.valueOf(offset), bytes);
    }

    @Override
    public boolean supportsValueModification() {
        return true;
    }

    @Override
    public IDebugTarget getDebugTarget() {
        return null;
    }

    @Override
    public ILaunch getLaunch() {
        return model.getLaunch();
    }

    @Override
    public String getModelIdentifier() {
        return ITCFConstants.ID_TCF_DEBUG_MODEL;
    }

    @Override
    public IModelProxy createModelProxy(Object element, IPresentationContext context) {
        assert element == this;
        return new ModelProxy(this, context.getWindow().getShell().getDisplay());
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class adapter) {
        if (adapter == IMemoryBlockRetrieval.class) return block_retrieval;
        if (adapter == IMemoryBlockRetrievalExtension.class) return block_retrieval;
        return super.getAdapter(adapter);
    }

    String getMemoryID() {
        return ctx_id;
    }

    void flushAllCaches() {
        mem_data = null;
    }

    void onMemoryChanged(boolean suspended) {
        assert Protocol.isDispatchThread();
        remote_expression.reset();
        expression_value.reset();
        if (suspended) mem_prev = mem_last;
        mem_data = null;
        synchronized (model_proxies) {
            for (ModelProxy p : model_proxies) {
                p.onMemoryChanged(suspended);
            }
        }
    }

    void onContextExited(String id) {
        assert Protocol.isDispatchThread();
        if (!id.equals(ctx_id)) return;
        remote_expression.reset();
        expression_value.reset();
    }

    /************************** Persistence ***************************************************/

    private static final String
        XML_NODE_MEMORY = "Memory",
        XML_NODE_BLOCK = "Block",
        XML_NODE_RENDERING = "Rendering",
        XML_ATTR_ID = "ID",
        XML_ATTR_VIEW = "View",
        XML_ATTR_PANE = "Pane",
        XML_ATTR_CTX = "Context",
        XML_ATTR_ADDR = "Addr",
        XML_ATTR_SIZE = "Size";

    private static final String XML_FILE_NAME = "memview.xml";
    private static final Display display = Display.getDefault();
    private static final Map<String,List<Element>> blocks_memento = new HashMap<String,List<Element>>();
    private static final Set<Runnable> pending_updates = new HashSet<Runnable>();

    private static boolean memento_loaded;

    private static void asyncExec(TCFModel model, Runnable r) {
        synchronized (pending_updates) {
            synchronized (Device.class) {
                if (display.isDisposed()) return;
                display.asyncExec(r);
            }
            pending_updates.add(r);
        }
    }

    private static Node cloneXML(Document document, Node node) {
        if (node instanceof Element) {
            Element x = (Element)node;
            Element y = document.createElement(x.getTagName());
            NamedNodeMap attrs = x.getAttributes();
            int l = attrs.getLength();
            for (int i = 0; i < l; i++) {
                Attr a = (Attr)attrs.item(i);
                y.setAttribute(a.getName(), a.getValue());
            }
            Node c = x.getFirstChild();
            while (c != null) {
                Node d = cloneXML(document, c);
                if (d != null) y.appendChild(d);
                c = c.getNextSibling();
            }
            return y;
        }
        return null;
    }

    static void onModelCreated(TCFModel model) {
        assert Protocol.isDispatchThread();
        if (memento_loaded) return;
        memento_loaded = true;
        try {
            synchronized (blocks_memento) {
                // Load memory monitors memento from workspace
                blocks_memento.clear();
                IPath path = Activator.getDefault().getStateLocation();
                File f = path.append(XML_FILE_NAME).toFile();
                if (!f.exists()) return;
                InputStream inp = new FileInputStream(f);
                try {
                    DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    parser.setErrorHandler(new DefaultHandler());
                    Element xml_memory = parser.parse(inp).getDocumentElement();
                    if (xml_memory.getTagName().equals(XML_NODE_MEMORY)) {
                        Node node = xml_memory.getFirstChild();
                        while (node != null) {
                            if (node instanceof Element && ((Element)node).getTagName().equals(XML_NODE_BLOCK)) {
                                Element xml_block = (Element)node;
                                String id = xml_block.getAttribute(XML_ATTR_CTX);
                                if (id != null) {
                                    List<Element> list = blocks_memento.get(id);
                                    if (list == null) {
                                        list = new ArrayList<Element>();
                                        blocks_memento.put(id, list);
                                    }
                                    list.add(xml_block);
                                }
                            }
                            node = node.getNextSibling();
                        }
                    }
                }
                finally {
                    inp.close();
                }
            }
        }
        catch (Exception x) {
            Activator.log("Cannot read memory monitors memento", x);
        }
    }

    static void onMemoryNodeCreated(final TCFNodeExecContext exe_ctx) {
        assert Protocol.isDispatchThread();
        synchronized (blocks_memento) {
            // Restore memory monitors associated with the node
            final List<Element> memento = blocks_memento.remove(exe_ctx.id);
            if (memento == null || memento.size() == 0) return;
            ArrayList<IMemoryBlock> list = new ArrayList<IMemoryBlock>();
            for (Element xml_block : memento) {
                String expr = xml_block.getAttribute(XML_ATTR_ADDR);
                long length = Long.parseLong(xml_block.getAttribute(XML_ATTR_SIZE));
                list.add(exe_ctx.model.getMemoryBlock(exe_ctx.id, expr, length));
            }
            final IMemoryBlock[] blks = list.toArray(new IMemoryBlock[list.size()]);
            asyncExec(exe_ctx.model, new Runnable() {
                @Override
                public void run() {
                    synchronized (pending_updates) {
                        pending_updates.remove(this);
                    }
                    try {
                        int i = 0;
                        DebugPlugin.getDefault().getMemoryBlockManager().addMemoryBlocks(blks);
                        IMemoryRenderingManager rmngr = DebugUITools.getMemoryRenderingManager();
                        for (Element xml_block : memento) {
                            IMemoryBlock mb = blks[i++];
                            Node node = xml_block.getFirstChild();
                            while (node != null) {
                                if (node instanceof Element && ((Element)node).getTagName().equals(XML_NODE_RENDERING)) {
                                    Element xml_rendering = (Element)node;
                                    String view_id = xml_rendering.getAttribute(XML_ATTR_VIEW);
                                    if (view_id != null && view_id.length() == 0) view_id = null;
                                    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                                    IMemoryRenderingSite part = (IMemoryRenderingSite)page.showView(IDebugUIConstants.ID_MEMORY_VIEW,
                                            view_id, IWorkbenchPage.VIEW_CREATE);
                                    IMemoryRenderingType rendering_type = rmngr.getRenderingType(xml_rendering.getAttribute(XML_ATTR_ID));
                                    IMemoryRendering rendering = rendering_type.createRendering();
                                    IMemoryRenderingContainer container = part.getContainer(xml_rendering.getAttribute(XML_ATTR_PANE));
                                    rendering.init(container, mb);
                                    container.addMemoryRendering(rendering);
                                }
                                node = node.getNextSibling();
                            }
                        }
                    }
                    catch (Exception x) {
                        Activator.log("Cannot restore memory monitors", x);
                    }
                }
            });
        }
    }

    static void onModelDisconnected(final TCFModel model) {
        assert Protocol.isDispatchThread();
        asyncExec(model, new Runnable() {
            @Override
            public void run() {
                synchronized (pending_updates) {
                    pending_updates.remove(this);
                }
                // Dispose memory monitors associated with the model, update memento
                ArrayList<IMemoryBlock> block_list = new ArrayList<IMemoryBlock>();
                IMemoryBlockManager manager = DebugPlugin.getDefault().getMemoryBlockManager();
                try {
                    Document document = DebugPlugin.newDocument();
                    Map<String,List<Element>> memento = new HashMap<String,List<Element>>();
                    Map<IMemoryBlock,Element> mb_to_xml = new HashMap<IMemoryBlock,Element>();
                    Element xml_memory = document.createElement(XML_NODE_MEMORY);
                    for (IMemoryBlock mb : manager.getMemoryBlocks()) {
                        if (mb instanceof TCFMemoryBlock) {
                            TCFMemoryBlock m = (TCFMemoryBlock)mb;
                            if (m.model != model) continue;
                            Element xml_block = document.createElement(XML_NODE_BLOCK);
                            xml_block.setAttribute(XML_ATTR_CTX, m.ctx_id);
                            xml_block.setAttribute(XML_ATTR_ADDR, m.expression);
                            xml_block.setAttribute(XML_ATTR_SIZE, Long.toString(m.length));
                            xml_memory.appendChild(xml_block);
                            mb_to_xml.put(m, xml_block);
                            List<Element> l = memento.get(m.ctx_id);
                            if (l == null) {
                                l = new ArrayList<Element>();
                                memento.put(m.ctx_id, l);
                            }
                            l.add(xml_block);
                            block_list.add(m);
                        }
                    }
                    for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
                        for (IWorkbenchPage page : window.getPages()) {
                            String[] pane_ids = {
                                    IDebugUIConstants.ID_RENDERING_VIEW_PANE_1,
                                    IDebugUIConstants.ID_RENDERING_VIEW_PANE_2 };
                            for (IViewReference ref : page.getViewReferences()) {
                                IViewPart part = ref.getView(false);
                                if (part instanceof IMemoryRenderingSite) {
                                    IMemoryRenderingSite memory_view = (IMemoryRenderingSite)part;
                                    for (String pane_id : pane_ids) {
                                        IMemoryRenderingContainer container = memory_view.getContainer(pane_id);
                                        if (container == null) continue;
                                        IMemoryRendering[] renderings = container.getRenderings();
                                        for (IMemoryRendering rendering : renderings) {
                                            Element xml_block = mb_to_xml.get(rendering.getMemoryBlock());
                                            if (xml_block == null) continue;
                                            Element xml_rendering = document.createElement(XML_NODE_RENDERING);
                                            xml_rendering.setAttribute(XML_ATTR_ID, rendering.getRenderingId());
                                            if (ref.getSecondaryId() != null) {
                                                xml_rendering.setAttribute(XML_ATTR_VIEW, ref.getSecondaryId());
                                            }
                                            xml_rendering.setAttribute(XML_ATTR_PANE, pane_id);
                                            xml_block.appendChild(xml_rendering);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    synchronized (blocks_memento) {
                        for (String id : blocks_memento.keySet()) {
                            if (memento.containsKey(id)) continue;
                            for (Element xml_block : blocks_memento.get(id)) {
                                xml_memory.appendChild(cloneXML(document, xml_block));
                            }
                        }
                        blocks_memento.clear();
                        Node node = xml_memory.getFirstChild();
                        while (node != null) {
                            if (node instanceof Element && ((Element)node).getTagName().equals(XML_NODE_BLOCK)) {
                                Element xml_block = (Element)node;
                                String id = xml_block.getAttribute(XML_ATTR_CTX);
                                if (id != null) {
                                    List<Element> l = blocks_memento.get(id);
                                    if (l == null) {
                                        l = new ArrayList<Element>();
                                        blocks_memento.put(id, l);
                                    }
                                    l.add(xml_block);
                                }
                            }
                            node = node.getNextSibling();
                        }
                        document.appendChild(xml_memory);
                        IPath path = Activator.getDefault().getStateLocation();
                        File f = path.append(XML_FILE_NAME).toFile();
                        FileOutputStream out = new FileOutputStream(f);
                        try {
                            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                            try {
                                wr.write(DebugPlugin.serializeDocument(document));
                            }
                            finally {
                                wr.close();
                            }
                        }
                        finally {
                            out.close();
                        }
                    }
                }
                catch (Exception x) {
                    Activator.log("Cannot save memory monitors", x);
                }
                if (block_list.size() != 0) manager.removeMemoryBlocks(block_list.toArray(new IMemoryBlock[block_list.size()]));
            }
        });
    }

    static void onWorkbenchShutdown() {
        // Wait until all pending updates are done
        display.syncExec(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    synchronized (pending_updates) {
                        if (pending_updates.size() == 0) return;
                    }
                    if (!display.readAndDispatch()) display.sleep();
                }
            }
        });
    }
}
