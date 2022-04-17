/*******************************************************************************
 * Copyright (c) 2010-2022 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui.disassembly;

import static org.eclipse.cdt.debug.internal.ui.disassembly.dsf.DisassemblyUtils.DEBUG;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.debug.internal.ui.disassembly.dsf.AbstractDisassemblyBackend;
import org.eclipse.cdt.debug.internal.ui.disassembly.dsf.AddressRangePosition;
import org.eclipse.cdt.debug.internal.ui.disassembly.dsf.DisassemblyUtils;
import org.eclipse.cdt.debug.internal.ui.disassembly.dsf.ErrorPosition;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.Position;
import org.eclipse.tcf.internal.cdt.ui.Activator;
import org.eclipse.tcf.internal.debug.launch.TCFSourceLookupDirector;
import org.eclipse.tcf.internal.debug.launch.TCFSourceLookupParticipant;
import org.eclipse.tcf.internal.debug.model.TCFContextState;
import org.eclipse.tcf.internal.debug.model.TCFSourceRef;
import org.eclipse.tcf.internal.debug.ui.model.TCFChildrenStackTrace;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeStackFrame;
import org.eclipse.tcf.internal.debug.ui.model.TCFNumberFormat;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IChannel.IChannelListener;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IDisassembly;
import org.eclipse.tcf.services.IDisassembly.DoneDisassemble;
import org.eclipse.tcf.services.IDisassembly.IDisassemblyLine;
import org.eclipse.tcf.services.IExpressions;
import org.eclipse.tcf.services.IExpressions.DoneCreate;
import org.eclipse.tcf.services.IExpressions.DoneDispose;
import org.eclipse.tcf.services.IExpressions.DoneEvaluate;
import org.eclipse.tcf.services.IExpressions.Expression;
import org.eclipse.tcf.services.IExpressions.Value;
import org.eclipse.tcf.services.ILineNumbers;
import org.eclipse.tcf.services.ILineNumbers.CodeArea;
import org.eclipse.tcf.services.ILineNumbers.DoneMapToSource;
import org.eclipse.tcf.services.IMemory.MemoryContext;
import org.eclipse.tcf.services.IMemory.MemoryError;
import org.eclipse.tcf.services.IMemory.MemoryListener;
import org.eclipse.tcf.services.IMemoryMap;
import org.eclipse.tcf.services.IMemoryMap.MemoryMapListener;
import org.eclipse.tcf.services.IMemory;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.services.IRunControl.RunControlContext;
import org.eclipse.tcf.services.IRunControl.RunControlListener;
import org.eclipse.tcf.services.ISymbols;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;
import org.eclipse.ui.PlatformUI;

@SuppressWarnings("restriction")
public class TCFDisassemblyBackend extends AbstractDisassemblyBackend {

    private static class AddressRange {
        BigInteger start;
        BigInteger end;
    }

    private static class FunctionOffset {
        static final FunctionOffset NONE = new FunctionOffset(null, null);
        String name;
        BigInteger offset;
        FunctionOffset(String name, BigInteger offset) {
            this.name = name;
            this.offset = offset;
        }
        @Override
        public String toString() {
            if (name == null || name.length() == 0) return "";
            if (isZeroOffset()) return name;
            return name + '+' + offset.toString();
        }
        boolean isZeroOffset() {
            return offset == null || offset.compareTo(BigInteger.ZERO) == 0;
        }
    }

    private class TCFLaunchListener implements ILaunchesListener {

        public void launchesRemoved(ILaunch[] launches) {
        }

        public void launchesAdded(ILaunch[] launches) {
        }

        public void launchesChanged(ILaunch[] launches) {
            if (fExecContext == null) return;
            for (ILaunch launch : launches) {
                if (launch == fExecContext.getModel().getLaunch()) {
                    if (launch.isTerminated()) {
                        handleSessionEnded();
                    }
                    break;
                }
            }
        }
    }

    private class TCFChannelListener implements IChannelListener {

        public void onChannelOpened() {
        }

        public void onChannelClosed(Throwable error) {
            handleSessionEnded();
        }

        public void congestionLevel(int level) {
        }
    }

    private class TCFRunControlListener implements RunControlListener {

        public void contextAdded(RunControlContext[] contexts) {
        }

        public void contextChanged(RunControlContext[] contexts) {
        }

        public void contextRemoved(String[] context_ids) {
            String id = fExecContext.getID();
            for (String contextId : context_ids) {
                if (id.equals(contextId)) {
                    fCallback.handleTargetEnded();
                    return;
                }
            }
        }

        public void contextSuspended(String context, String pc, String reason,
                Map<String, Object> params) {
            if (fExecContext.getID().equals(context)) {
                handleContextSuspended();
            }
        }

        public void contextResumed(String context) {
            if (fExecContext.getID().equals(context)) {
                handleContextResumed();
            }
        }

        public void containerSuspended(String context, String pc,
                String reason, Map<String, Object> params,
                String[] suspended_ids) {
            String id = fExecContext.getID();
            if (id.equals(context)) {
                handleContextSuspended();
                return;
            }
            for (String contextId : suspended_ids) {
                if (id.equals(contextId)) {
                    handleContextSuspended();
                    return;
                }
            }
        }

        public void containerResumed(String[] context_ids) {
            String id = fExecContext.getID();
            for (String contextId : context_ids) {
                if (id.equals(contextId)) {
                    handleContextResumed();
                    return;
                }
            }
        }

        public void contextException(String context, String msg) {
        }
    }

    private class TCFMemoryMapListener implements MemoryMapListener {

        @Override
        public void changed(String context_id) {
            if (fMemoryContext == null) return;
            if (!fMemoryContext.getID().equals(context_id)) return;
            if (fCallback == null) return;
            try {
                fCallback.getClass().getMethod("refresh").invoke(fCallback);
            }
            catch (Exception e) {
            }
        }
    }

    private class TCFMemoryListener implements MemoryListener {

        @Override
        public void contextAdded(MemoryContext[] contexts) {
        }

        @Override
        public void contextChanged(MemoryContext[] contexts) {
        }

        @Override
        public void contextRemoved(String[] context_ids) {
        }

        @Override
        public void memoryChanged(String context_id, Number[] addr, long[] size) {
            if (fMemoryContext == null) return;
            if (!fMemoryContext.getID().equals(context_id)) return;
            if (fCallback == null) return;
            try {
                fCallback.getClass().getMethod("refresh").invoke(fCallback);
            }
            catch (Exception e) {
            }
        }
    }

    private volatile boolean fSuspended;
    private volatile TCFNodeExecContext fExecContext;
    private volatile TCFNodeExecContext fMemoryContext;
    private volatile TCFNodeStackFrame fActiveFrame;
    private volatile TCFContextState fContextState;
    private volatile int fSuspendCount;
    private volatile int fContextCount;
    private volatile boolean disposed;

    /* Objects of the Request class represent pending disassembly update requests.
     * A request becomes obsolete and should be aborted if:
     * 1. debug context selection changes.
     * 2. debug context state changes.
     * 3. the view is disposed.
     */
    private class Request {
        final TCFNodeExecContext ctx;
        final TCFNodeExecContext mem;
        final int suspend_cnt;
        final int context_cnt;
        final long doc_mod_cnt;

        boolean done;

        Request() {
            /* Record request context */
            ctx = fExecContext;
            mem = fMemoryContext;
            suspend_cnt = fSuspendCount;
            context_cnt = fContextCount;
            doc_mod_cnt = getModCount();
            assert fCallback.getUpdatePending();
        }

        /* Return true if the request handling should continue,
         * otherwise reset pending state and return false */
        private boolean check() {
            boolean ok =
                !done &&
                !disposed &&
                fExecContext != null &&
                fMemoryContext != null &&
                ctx == fExecContext &&
                mem == fMemoryContext &&
                suspend_cnt == fSuspendCount &&
                context_cnt == fContextCount;
            if (ok) {
                if (Protocol.isDispatchThread()) {
                    ok = !ctx.isDisposed() && !mem.isDisposed();
                }
                else {
                    ok = doc_mod_cnt == getModCount() && fCallback.hasViewer();
                }
            }
            if (ok) return true;
            done();
            return false;
        }

        /* Reset request pending state */
        private void done() {
            if (Protocol.isDispatchThread()) {
                fCallback.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        done();
                    }
                });
            }
            else if (!done) {
                done = true;
                /* Don't call setUpdatePending() if pending state was reset by the view */
                if (!fCallback.getUpdatePending()) return;
                fCallback.setUpdatePending(false);
            }
        }
    }

    private final IRunControl.RunControlListener fRunControlListener = new TCFRunControlListener();
    private final IMemoryMap.MemoryMapListener fMemoryMapListener = new TCFMemoryMapListener();
    private final IMemory.MemoryListener fMemoryListener = new TCFMemoryListener();
    private final IChannelListener fChannelListener = new TCFChannelListener();
    private final ILaunchesListener fLaunchesListener = new TCFLaunchListener();

    public boolean supportsDebugContext(IAdaptable context) {
        return (context instanceof TCFNodeExecContext || context instanceof TCFNodeStackFrame)
            && hasDisassemblyService((TCFNode) context);
    }

    private boolean hasDisassemblyService(final TCFNode context) {
        Boolean hasService = new TCFTask<Boolean>() {
            public void run() {
                IDisassembly disass = null;
                IChannel channel = context.getChannel();
                if (channel != null && channel.getState() != IChannel.STATE_CLOSED) {
                    disass = channel.getRemoteService(IDisassembly.class);
                }
                done(disass != null);
            }
        }.getE();
        return hasService != null && hasService.booleanValue();
    }

    public boolean hasDebugContext() {
        return fExecContext != null;
    }

    public SetDebugContextResult setDebugContext(final IAdaptable context) {
        TCFNodeExecContext thread = null;
        SetDebugContextResult result = new SetDebugContextResult();
        if (context instanceof TCFNodeExecContext) {
            thread = (TCFNodeExecContext)context;
        }
        else if (context instanceof TCFNodeStackFrame) {
            thread = (TCFNodeExecContext)((TCFNodeStackFrame)context).getParent();
        }

        if (fExecContext != thread) {
            result.contextChanged = true;
            fContextCount++;
            if (fExecContext != null) removeListeners(fExecContext);
            fExecContext = thread;
            if (fExecContext != null) addListeners(fExecContext);
        }

        fSuspended = false;
        fContextState = null;
        fMemoryContext = null;
        fActiveFrame = null;
        if (fExecContext != null) {
            IChannel channel = thread.getChannel();
            try {
                 new TCFTask<Object>(fExecContext.getChannel()) {
                    public void run() {
                        TCFDataCache<TCFNodeExecContext> mem_cache = fExecContext.getMemoryNode();
                        if (!mem_cache.validate(this)) return;
                        TCFDataCache<TCFContextState> state_cache = fExecContext.getState();
                        if (!state_cache.validate(this)) return;
                        if (context instanceof TCFNodeStackFrame) {
                            fActiveFrame = (TCFNodeStackFrame)context;
                        }
                        fContextState = state_cache.getData();
                        fSuspended = fContextState != null && fContextState.is_suspended;
                        fMemoryContext = mem_cache.getData();
                        done(null);
                    }
                }.getE();
            }
            catch (Error x) {
                if (channel.getState() == IChannel.STATE_OPEN) throw x;
            }
        }
        result.sessionId = fExecContext != null ? fExecContext.getID() : null;

        if (!result.contextChanged && fExecContext != null) {
            fCallback.asyncExec(new Runnable() {
                public void run() {
                    fCallback.gotoFrameIfActive(getFrameLevel());
                }
            });
        }

        return result;
    }

    private void addListeners(final TCFNodeExecContext context) {
        assert context != null;
        Protocol.invokeAndWait(new Runnable() {
            public void run() {
                IChannel channel = context.getChannel();
                IRunControl rctl = channel.getRemoteService(IRunControl.class);
                if (rctl != null) rctl.addListener(fRunControlListener);
                IMemoryMap mmap = channel.getRemoteService(IMemoryMap.class);
                if (mmap != null) mmap.addListener(fMemoryMapListener);
                IMemory memory = channel.getRemoteService(IMemory.class);
                if (memory != null) memory.addListener(fMemoryListener);
                channel.addChannelListener(fChannelListener);
            }
        });
        DebugPlugin.getDefault().getLaunchManager().addLaunchListener(fLaunchesListener );
    }

    private void removeListeners(final TCFNodeExecContext context) {
        assert context != null;
        DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(fLaunchesListener);
        Protocol.invokeAndWait(new Runnable() {
            public void run() {
                IChannel channel = context.getChannel();
                IRunControl rctl = channel.getRemoteService(IRunControl.class);
                if (rctl != null) rctl.removeListener(fRunControlListener);
                IMemoryMap mmap = channel.getRemoteService(IMemoryMap.class);
                if (mmap != null) mmap.removeListener(fMemoryMapListener);
                IMemory memory = channel.getRemoteService(IMemory.class);
                if (memory != null) memory.removeListener(fMemoryListener);
                channel.removeChannelListener(fChannelListener);
            }
        });
    }

    private void handleContextSuspended() {
        fSuspendCount++;
        fSuspended = true;
        fContextState = null;
        fCallback.handleTargetSuspended();
    }

    private void handleContextResumed() {
        fSuspended = false;
        fContextState = null;
        fCallback.handleTargetResumed();
    }

    private void handleSessionEnded() {
        fContextCount++;
        fCallback.handleTargetEnded();
    }

    public void clearDebugContext() {
        fSuspended = false;
        fContextState = null;
        if (fExecContext != null) {
            removeListeners(fExecContext);
        }
        fExecContext = null;
        fMemoryContext = null;
        fActiveFrame = null;
    }

    public void retrieveFrameAddress(final int targetFrame) {
        final Request request = new Request();
        if (!request.check()) return;
        try {
            BigInteger address = !fSuspended ? null : new TCFTask<BigInteger>(request.ctx.getChannel()) {
                public void run() {
                    if (targetFrame == 0) {
                        TCFDataCache<BigInteger> addr = request.ctx.getAddress();
                        if (!addr.validate(this)) return;
                        done(addr.getData());
                        return;
                    }
                    TCFChildrenStackTrace stack = request.ctx.getStackTrace();
                    if (!stack.validate(this)) return;
                    Map<String,TCFNode> frameData = stack.getData();
                    for (TCFNode node : frameData.values()) {
                        if (node instanceof TCFNodeStackFrame) {
                            TCFNodeStackFrame frame = (TCFNodeStackFrame)node;
                            if (frame.getFrameNo() == targetFrame) {
                                TCFDataCache<BigInteger> addr = frame.getAddress();
                                if (!addr.validate(this)) return;
                                done(addr.getData());
                                return;
                            }
                        }
                    }
                    done(null);
                }
            }.getE();

            if (!request.check()) return;

            request.done();
            if (address == null) address = BigInteger.valueOf(-2);
            if (targetFrame == 0) {
                fCallback.updatePC(address);
            }
            else {
                fCallback.gotoFrame(targetFrame, address);
            }
        }
        catch (Throwable x) {
            request.done();
        }
    }

    public boolean isSuspended() {
        return fSuspended;
    }

    public boolean hasFrameContext() {
        return fActiveFrame != null || fContextState != null;
    }

    public int getFrameLevel() {
        if (fExecContext == null) return -1;
        if (fActiveFrame == null) return 0;
        return new TCFTask<Integer>(fExecContext.getChannel()) {
            public void run() {
                if (!fExecContext.getStackTrace().validate(this)) return;
                done(fActiveFrame.getFrameNo());
            }
        }.getE();
    }

    public String getFrameFile() {
        if (fExecContext == null) return null;
        return new TCFTask<String>(fExecContext.getChannel()) {
            public void run() {
                TCFDataCache<TCFSourceRef> sourceRefCache = null;
                if (fActiveFrame != null) {
                    sourceRefCache = fActiveFrame.getLineInfo();
                }
                else if (fContextState != null) {
                    BigInteger addr = new BigInteger(fContextState.suspend_pc);
                    sourceRefCache = fExecContext.getLineInfo(addr);

                }
                if (sourceRefCache != null) {
                    if (!sourceRefCache.validate(this)) return;
                    TCFSourceRef sourceRef = sourceRefCache.getData();
                    if (sourceRef != null && sourceRef.area != null) {
                        done(TCFSourceLookupParticipant.toFileName(sourceRef.area));
                        return;
                    }
                }
                done(null);
            }
        }.getE();
    }

    public int getFrameLine() {
        if (fExecContext == null) return -1;
        return new TCFTask<Integer>(fExecContext.getChannel()) {
            public void run() {
                TCFDataCache<TCFSourceRef> sourceRefCache = null;
                if (fActiveFrame != null) {
                    sourceRefCache = fActiveFrame.getLineInfo();
                }
                else if (fContextState != null) {
                    BigInteger addr = new BigInteger(fContextState.suspend_pc);
                    sourceRefCache = fExecContext.getLineInfo(addr);

                }
                if (sourceRefCache != null) {
                    if (!sourceRefCache.validate(this)) return;
                    TCFSourceRef sourceRef = sourceRefCache.getData();
                    if (sourceRef != null && sourceRef.area != null) {
                        done(sourceRef.area.start_line);
                        return;
                    }
                }
                done(-1);
            }
        }.getE();
    }

    public void retrieveDisassembly(final BigInteger startAddress,
            BigInteger endAddress, String file, int lineNumber, int lines,
            final boolean mixed, final boolean showSymbols, boolean showDisassembly,
            final int linesHint) {

        final Request request = new Request();

        Protocol.invokeLater(new Runnable() {

            IMemory.MemoryContext mem;
            boolean big_endian;
            int addr_bits;
            IDisassemblyLine[] disassembly;
            AddressRange range;
            boolean done_disassembly;
            ISymbols.Symbol[] symbol_array;
            boolean done_symbols;
            CodeArea[] code_areas;
            boolean done_line_numbers;
            byte[] code;
            boolean done_code;

            public void run() {
                if (!request.check()) return;
                IChannel channel = request.ctx.getChannel();
                IDisassembly disass = channel.getRemoteService(IDisassembly.class);
                if (disass == null) {
                    request.done();
                    return;
                }
                TCFDataCache<IMemory.MemoryContext> cache = request.mem.getMemoryContext();
                if (!cache.validate(this)) return;
                mem = cache.getData();
                if (mem == null) {
                    request.done();
                    return;
                }
                big_endian = mem.isBigEndian();
                addr_bits = mem.getAddressSize() * 8;

                int accessSize = 0;
                BigInteger mem_end = BigInteger.ONE.shiftLeft(addr_bits);
                mem_end = mem_end.subtract(BigInteger.ONE);

                final BigInteger requestedLineEndAddr = startAddress.add(BigInteger.valueOf(linesHint * mem.getAddressSize()));

                if (startAddress.compareTo(mem_end) > 0) {
                    fCallback.asyncExec(new Runnable() {
                        public void run() {
                            insertEmptySpace(request, startAddress, requestedLineEndAddr);
                        }
                    });
                    return;
                }

                if (requestedLineEndAddr.compareTo(mem_end) > 0) {
                    accessSize = mem_end.subtract(startAddress).intValue() + 1;
                }
                else {
                    accessSize = linesHint * mem.getAddressSize();
                }

                if (!done_disassembly) {
                    Map<String, Object> params = new HashMap<String, Object>();
                    /* Use thread, not memory context, to allow disassembler to check CPU mode. */
                    /* It can improve disassembler accuracy, at least around current PC. */
                    disass.disassemble(request.ctx.getID(), startAddress, accessSize, params, new DoneDisassemble() {
                        @Override
                        public void doneDisassemble(IToken token, final Throwable error, IDisassemblyLine[] res) {
                            if (error != null) {
                                fCallback.asyncExec(new Runnable() {
                                    public void run() {
                                        insertError(request, startAddress, error);
                                        if (fCallback.getAddressSize() < addr_bits) fCallback.addressSizeChanged(addr_bits);
                                    }
                                });
                                return;
                            }
                            if (res != null && res.length > 0) {
                                disassembly = res;
                                range = new AddressRange();
                                range.start = JSON.toBigInteger(res[0].getAddress());
                                IDisassemblyLine last = res[res.length - 1];
                                range.end = JSON.toBigInteger(last.getAddress()).add(BigInteger.valueOf(last.getSize()));
                            }
                            done_disassembly = true;
                            run();
                        }
                    });
                    return;
                }
                if (!done_symbols && (range == null || !showSymbols)) {
                    done_symbols = true;
                }
                if (!done_symbols) {
                    final ISymbols symbols = channel.getRemoteService(ISymbols.class);
                    if (symbols == null) {
                        done_symbols = true;
                    }
                    else {
                        final ArrayList<ISymbols.Symbol> symbol_list = new ArrayList<ISymbols.Symbol>();
                        IDisassemblyLine line = disassembly[0];
                        symbols.findByAddr(mem.getID(), line.getAddress(), new ISymbols.DoneFind() {
                            int idx = 0;
                            public void doneFind(IToken token, Exception error, String symbol_id) {
                                if (error == null && symbol_id != null) {
                                    symbols.getContext(symbol_id, new ISymbols.DoneGetContext() {
                                        public void doneGetContext(IToken token, Exception error, ISymbols.Symbol context) {
                                            BigInteger nextAddress = null;
                                            if (error == null && context != null) {
                                                if (context.getTypeClass().equals(ISymbols.TypeClass.function) &&
                                                    context.getAddress() != null && context.getSize() >= 0)
                                                {
                                                    symbol_list.add(context);
                                                    nextAddress = JSON.toBigInteger(context.getAddress()).add(BigInteger.valueOf(context.getSize()));
                                                }
                                            }
                                            findNextSymbol(nextAddress);
                                        }
                                    });
                                    return;
                                }
                                findNextSymbol(null);
                            }
                            private void findNextSymbol(BigInteger nextAddress) {
                                while (++idx < disassembly.length) {
                                    BigInteger instrAddress = JSON.toBigInteger(disassembly[idx].getAddress());
                                    if (nextAddress != null && instrAddress.compareTo(nextAddress) < 0) continue;
                                    symbols.findByAddr(mem.getID(), instrAddress, this);
                                    return;
                                }
                                symbol_array = symbol_list.toArray(new ISymbols.Symbol[symbol_list.size()]);
                                done_symbols = true;
                                run();
                            }
                        });
                        return;
                    }
                }
                if (!done_line_numbers && (range == null || !mixed)) {
                    done_line_numbers = true;
                }
                if (!done_line_numbers) {
                    ILineNumbers lineNumbers = channel.getRemoteService(ILineNumbers.class);
                    if (lineNumbers == null) {
                        done_line_numbers = true;
                    }
                    else {
                        lineNumbers.mapToSource(mem.getID(), range.start, range.end, new DoneMapToSource() {
                            public void doneMapToSource(IToken token, Exception error, final CodeArea[] areas) {
                                if (error != null) {
                                    Activator.log(error);
                                }
                                else {
                                    code_areas = areas;
                                }
                                done_line_numbers = true;
                                run();
                            }
                        });
                        return;
                    }
                }
                if (!done_code && range == null) {
                    done_code = true;
                }
                if (!done_code) {
                    code = new byte[range.end.subtract(range.start).intValue()];
                    mem.get(range.start, 1, code, 0, code.length, 0, new IMemory.DoneMemory() {
                        @Override
                        public void doneMemory(IToken token, MemoryError error) {
                            done_code = true;
                            run();
                        }
                    });
                    return;
                }
                fCallback.asyncExec(new Runnable() {
                    public void run() {
                        insertDisassembly(request, startAddress, code, range, big_endian,
                                disassembly, symbol_array, code_areas);
                        if (fCallback.getAddressSize() < addr_bits) fCallback.addressSizeChanged(addr_bits);
                    }
                });
            }
        });
    }

    private long getModCount() {
        return ((IDocumentExtension4)fCallback.getDocument()).getModificationStamp();
    }

    protected final void insertDisassembly(Request request, BigInteger startAddress, byte[] code, AddressRange range, boolean big_endian,
            IDisassemblyLine[] instructions, ISymbols.Symbol[] symbols, CodeArea[] codeAreas) {
        if (!request.check()) return;
        if (DEBUG) System.out.println("insertDisassembly "+ DisassemblyUtils.getAddressText(startAddress)); //$NON-NLS-1$
        boolean insertedAnyAddress = false;
        try {
            fCallback.lockScroller();

            AddressRangePosition p = null;
            if (instructions != null) for (IDisassemblyLine instruction : instructions) {
                BigInteger address = JSON.toBigInteger(instruction.getAddress());
                if (p == null || !p.containsAddress(address)) {
                    p = fCallback.getPositionOfAddress(address);
                }
                if (p instanceof ErrorPosition && p.fValid) {
                    p.fValid = false;
                    fCallback.getDocument().addInvalidAddressRange(p);
                }
                else if (p == null) {
                    if (DEBUG) System.out.println("Excess disassembly lines at " + DisassemblyUtils.getAddressText(address)); //$NON-NLS-1$
                    return;
                }
                else if (p.fValid) {
                    if (DEBUG) System.out.println("Excess disassembly lines at " + DisassemblyUtils.getAddressText(address)); //$NON-NLS-1$
//                    if (!p.fAddressOffset.equals(address)) {
//                        // override probably unaligned disassembly
//                        p.fValid = false;
//                        fCallback.getDocument().addInvalidAddressRange(p);
//                    }
//                    else {
                        continue;
//                    }
                }

                // insert source
                String sourceFile = null;
                int firstLine = -1;
                int lastLine = -1;
                CodeArea area = findCodeArea(address, codeAreas);
                if (area != null && area.file != null) {
                    IPath filePath = new Path(area.file);
                    if (!filePath.isAbsolute() && area.directory != null) {
                        filePath = new Path(area.directory).append(filePath);
                    }
                    sourceFile = filePath.toString();
                    firstLine = area.start_line - 1;
                    lastLine = area.end_line - 2;
                }
                if (sourceFile != null && firstLine >= 0) {
                    try {
                        p = fCallback.insertSource(p, address, sourceFile, firstLine, lastLine);
                    }
                    catch (NoSuchMethodError nsme) {
                        // use fallback
                        p = fCallback.insertSource(p, address, sourceFile, firstLine);
                    }
                }

                // insert symbol label
                FunctionOffset functionOffset = getFunctionOffset(address, symbols);
                if (functionOffset.name != null && functionOffset.isZeroOffset()) {
                    p = fCallback.getDocument().insertLabel(p, address, functionOffset.name, true);
                }

                // insert instruction
                int instrLength = instruction.getSize();
                Map<String,Object>[] instrAttrs = instruction.getInstruction();
                String instr = formatInstruction(instrAttrs);

                int offs = address.subtract(range.start).intValue();
                if (code != null && offs >= 0 && offs + instrLength <= code.length) {
                    try {
                        // Since CDT 8.4.200 the insert disassembly line takes a byte array for the opcode
                        Method method = fCallback.getDocument().getClass().getMethod("insertDisassemblyLine", AddressRangePosition.class,  //$NON-NLS-1$
                                BigInteger.class, int.class, String.class, Byte[].class, String.class, String.class, int.class);
                        Byte[] opcode = new Byte[instrLength];
                        for (int i = 0; i < instrLength; i++) {
                            opcode[i] = code[offs + i];
                        }
                        method.invoke(fCallback.getDocument(), p,
                                address, instrLength, functionOffset.toString(),
                                opcode, instr, sourceFile, firstLine);
                        insertedAnyAddress = true;
                    }
                    catch (Exception e) {
                        try {
                            // Fallback to support previous versions of CDT
                            Method method = fCallback.getDocument().getClass().getMethod("insertDisassemblyLine", AddressRangePosition.class,  //$NON-NLS-1$
                                    BigInteger.class, int.class, String.class, BigInteger.class, String.class, String.class, int.class);
                            BigInteger opcode = BigInteger.ZERO;
                            for (int i = 0; i < instrLength; i++) {
                                int j = big_endian ? i : instrLength - i - 1;
                                opcode = opcode.shiftLeft(8).add(BigInteger.valueOf(code[offs + j] & 0xff));
                            }
                            method.invoke(fCallback.getDocument(), p,
                                    address, instrLength, functionOffset.toString(),
                                    opcode, instr, sourceFile, firstLine);
                            insertedAnyAddress = true;
                        }
                        catch (Exception e1) {
                            // Handled below on "insertedAnyAddress" check
                        }
                    }
                }
                else {
                    p = fCallback.getDocument().insertDisassemblyLine(p,
                            address, instrLength, functionOffset.toString(),
                            instr, sourceFile, firstLine);
                    insertedAnyAddress = true;
                }
            }
            if (!insertedAnyAddress) {
                // Insert error in case of incomplete disassembly
                fCallback.insertError(startAddress, "cannot disassemble");
            }
        }
        catch (BadLocationException e) {
            // should not happen
            DisassemblyUtils.internalError(e);
        }
        finally {
            request.done();
            fCallback.updateInvalidSource();
            fCallback.unlockScroller();
            fCallback.doPending();
            fCallback.updateVisibleArea();
            request.ctx.getModel().updateAnnotations(fCallback.getSite().getWorkbenchWindow());
        }
    }

    private void insertError(Request request, BigInteger address, Throwable error) {
        if (!request.check()) return;
        fCallback.lockScroller();
        fCallback.insertError(address, TCFModel.getErrorMessage(error, false));
        request.done();
        fCallback.unlockScroller();
        fCallback.doPending();
        fCallback.updateVisibleArea();
        request.ctx.getModel().updateAnnotations(fCallback.getSite().getWorkbenchWindow());
    }

    private void insertEmptySpace(Request request, BigInteger startAddress, BigInteger endAddress) {
        if (!request.check()) return;
        try {
            fCallback.lockScroller();
            for (;;) {
                AddressRangePosition p = fCallback.getPositionOfAddress(startAddress);
                if (p != null && !p.fValid && p.containsAddress(startAddress)) {
                    fCallback.getDocument().insertDisassemblyLine(p, startAddress, 1, "", " ", null, 0);
                }
                startAddress = startAddress.add(BigInteger.ONE);
                if (startAddress.compareTo(endAddress) >= 0) break;
            }
        }
        catch (BadLocationException e) {
            DisassemblyUtils.internalError(e);
        }
        finally {
            request.done();
            fCallback.unlockScroller();
            fCallback.doPending();
            fCallback.updateVisibleArea();
        }
    }

    private FunctionOffset getFunctionOffset(BigInteger address, ISymbols.Symbol[] symbols) {
        if (symbols != null) {
            for (ISymbols.Symbol symbol : symbols) {
                if (symbol.getAddress() == null) continue;
                BigInteger symbolAddress = JSON.toBigInteger(symbol.getAddress());
                BigInteger offset = address.subtract(symbolAddress);
                switch (offset.compareTo(BigInteger.ZERO)) {
                case 0:
                    return new FunctionOffset(symbol.getName(), BigInteger.ZERO);
                case 1:
                    if (offset.compareTo(BigInteger.valueOf(symbol.getSize())) < 0) {
                        return new FunctionOffset(symbol.getName(), offset);
                    }
                    break;
                default:
                    break;
                }
            }
        }
        return FunctionOffset.NONE;
    }

    private CodeArea findCodeArea(BigInteger address, CodeArea[] codeAreas) {
        if (codeAreas != null) {
            for (CodeArea codeArea : codeAreas) {
                if (address.equals(JSON.toBigInteger(codeArea.start_address))) {
                    return codeArea;
                }
            }
        }
        return null;
    }

    /**
     * Format an instruction.
     *
     * @param instrAttrs
     * @return string representation
     */
    private String formatInstruction(Map<String, Object>[] instrAttrs) {
        StringBuilder buf = new StringBuilder(20);
        for (Map<String, Object> attrs : instrAttrs) {
            if (buf.length() > 0) buf.append(' ');
            Object type = attrs.get(IDisassembly.FIELD_TYPE);
            if (IDisassembly.FTYPE_STRING.equals(type) || IDisassembly.FTYPE_REGISTER.equals(type)) {
                Object text = attrs.get(IDisassembly.FIELD_TEXT);
                buf.append(text);
            }
            else {
                Object value = attrs.get(IDisassembly.FIELD_VALUE);
                BigInteger bigValue = new BigInteger(value.toString());
                // TODO number format
                buf.append("0x").append(bigValue.toString(16));
            }
        }
        return buf.toString();
    }

    public void gotoSymbol(final String symbol) {
        if (fExecContext == null) return;
        new TCFTask<String>(fExecContext.getChannel()) {
            public void run() {
                IChannel channel = fExecContext.getChannel();
                final IExpressions exprSvc = channel.getRemoteService(IExpressions.class);
                if (exprSvc != null) {
                    TCFNode evalContext = fActiveFrame == null || fActiveFrame.isEmulated() ? fExecContext : fActiveFrame;
                    exprSvc.create(evalContext.getID(), null, symbol, new DoneCreate() {
                        public void doneCreate(IToken token, Exception error, final Expression context) {
                            if (error == null) {
                                exprSvc.evaluate(context.getID(), new DoneEvaluate() {
                                    public void doneEvaluate(IToken token, Exception error, Value value) {
                                        if (error == null && value.getValue() != null) {
                                            final BigInteger address = TCFNumberFormat.toBigInteger(
                                                    value.getValue(), value.isBigEndian(), false);
                                            fCallback.asyncExec(new Runnable() {
                                                public void run() {
                                                    fCallback.gotoAddress(address);
                                                }
                                            });
                                        }
                                        else {
                                            handleError(error);
                                        }
                                        done(null);
                                        exprSvc.dispose(context.getID(), new DoneDispose() {
                                            public void doneDispose(IToken token, Exception error) {
                                                // no-op
                                            }
                                        });
                                    }
                                });
                            }
                            else {
                                handleError(error);
                                done(null);
                            }
                        }
                    });
                }
                else {
                    done(null);
                }
            }
            protected void handleError(final Exception error) {
                fCallback.asyncExec(new Runnable() {
                    public void run() {
                        Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, error.getLocalizedMessage(), error);
                        ErrorDialog.openError(fCallback.getSite().getShell(), "Error", null, status); //$NON-NLS-1$
                    }
                });
            }
        }.getE();
    }

    public void retrieveDisassembly(String file, int lines,
            BigInteger endAddress, boolean mixed, boolean showSymbols,
            boolean showDisassembly) {
        final Request request = new Request();
        // TODO disassembly for source file
        request.done();
    }

    public String evaluateExpression(final String expression) {
        if (fExecContext == null) return null;
        String value = new TCFTask<String>(fExecContext.getChannel()) {
            public void run() {
                IChannel channel = fExecContext.getChannel();
                final IExpressions exprSvc = channel.getRemoteService(IExpressions.class);
                if (exprSvc != null) {
                    TCFNode evalContext = fActiveFrame == null || fActiveFrame.isEmulated() ? fExecContext : fActiveFrame;
                    exprSvc.create(evalContext.getID(), null, expression, new DoneCreate() {
                        public void doneCreate(IToken token, Exception error, final Expression context) {
                            if (error == null) {
                                exprSvc.evaluate(context.getID(), new DoneEvaluate() {
                                    public void doneEvaluate(IToken token, Exception error, Value value) {
                                        if (error == null && value.getValue() != null) {
                                            BigInteger address = TCFNumberFormat.toBigInteger(
                                                    value.getValue(), value.isBigEndian(), false);
                                            done("0x" + address.toString(16));
                                        }
                                        else {
                                            done(null);
                                        }
                                        exprSvc.dispose(context.getID(), new DoneDispose() {
                                            public void doneDispose(IToken token, Exception error) {
                                                // no-op
                                            }
                                        });
                                    }
                                });
                            }
                            else {
                                done(null);
                            }
                        }
                    });
                }
                else {
                    done(null);
                }
            }
        }.getE();
        return value;
    }

    public void dispose() {
        disposed = true;
    }

    public Object insertSource(Position pos, BigInteger address, String file, int lineNumber) {
        TCFNodeExecContext ctx = fMemoryContext;
        if (ctx == null) return null;
        return TCFSourceLookupDirector.lookup(ctx.getModel().getLaunch(), ctx.getID(), file);
    }

    @Override
    public BigInteger evaluateAddressExpression(String expression, boolean suppressError) {
        String value = evaluateExpression(expression);
        if (value != null) {
            try {
                return DisassemblyUtils.decodeAddress(value);
            }
            catch (NumberFormatException e) {
                if (!suppressError) {
                    MessageDialog.openError(PlatformUI.getWorkbench().getDisplay().getActiveShell(),
                            "Error", "Expression does not evaluate to an address");
                }
            }
        }
        return null;
    }
}
