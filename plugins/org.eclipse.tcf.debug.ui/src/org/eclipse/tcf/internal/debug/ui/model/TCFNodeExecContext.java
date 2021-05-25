/*******************************************************************************
 * Copyright (c) 2007-2021 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenCountUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IHasChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerInputUpdate;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.memory.IMemoryRenderingSite;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.tcf.debug.ui.ITCFDebugUIConstants;
import org.eclipse.tcf.debug.ui.ITCFExecContext;
import org.eclipse.tcf.internal.debug.actions.TCFAction;
import org.eclipse.tcf.internal.debug.model.TCFBreakpointsModel;
import org.eclipse.tcf.internal.debug.model.TCFContextState;
import org.eclipse.tcf.internal.debug.model.TCFFunctionRef;
import org.eclipse.tcf.internal.debug.model.TCFSourceRef;
import org.eclipse.tcf.internal.debug.model.TCFSymFileRef;
import org.eclipse.tcf.internal.debug.ui.ColorCache;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.protocol.IErrorReport;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IContextReset;
import org.eclipse.tcf.services.ILineNumbers;
import org.eclipse.tcf.services.IMemory;
import org.eclipse.tcf.services.IMemoryMap;
import org.eclipse.tcf.services.IProcesses;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.services.ISymbols;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.ui.IWorkbenchPart;

public class TCFNodeExecContext extends TCFNode implements ISymbolOwner, ITCFExecContext {

    private final TCFChildrenExecContext children_exec;
    private final TCFChildrenStackTrace children_stack;
    private final TCFChildrenRegisters children_regs;
    private final TCFChildrenExpressions children_exps;
    private final TCFChildrenHoverExpressions children_hover_exps;
    private final TCFChildrenLogExpressions children_log_exps;
    private final TCFChildrenModules children_modules;
    private final TCFChildrenContextQuery children_query;

    private final TCFData<IMemory.MemoryContext> mem_context;
    private final TCFData<IRunControl.RunControlContext> run_context;
    private final TCFData<MemoryRegion[]> memory_map;
    private final TCFData<IProcesses.ProcessContext> prs_context;
    private final TCFData<TCFContextState> state;
    private final TCFData<TCFContextState> min_state;
    private final TCFData<BigInteger> address; // Current PC as BigInteger
    private final TCFData<Collection<Map<String,Object>>> signal_list;
    private final TCFData<SignalMask[]> signal_mask;
    private final TCFData<TCFNodeExecContext> memory_node;
    private final TCFData<TCFNodeExecContext> symbols_node;
    private final TCFData<String> full_name;
    private final TCFData<Collection<Map<String,Object>>> reset_capabilities;

    private LinkedHashMap<BigInteger,TCFDataCache<TCFSymFileRef>> syms_info_lookup_cache;
    private LinkedHashMap<BigInteger,TCFDataCache<TCFSourceRef>> line_info_lookup_cache;
    private LinkedHashMap<BigInteger,TCFDataCache<TCFFunctionRef>> func_info_lookup_cache;
    private LookupCacheTimer lookup_cache_timer;

    private int mem_seq_no;
    private int exe_seq_no;

    private static final TCFNode[] empty_node_array = new TCFNode[0];

    /*
     * LookupCacheTimer is executed periodically to dispose least-recently
     * accessed entries in line_info_lookup_cache and func_info_lookup_cache.
     * The timer disposes itself when both caches become empty.
     */
    private class LookupCacheTimer implements Runnable {

        LookupCacheTimer() {
            Protocol.invokeLater(4000, this);
        }

        public void run() {
            if (isDisposed()) return;
            if (syms_info_lookup_cache != null) {
                BigInteger addr = syms_info_lookup_cache.keySet().iterator().next();
                TCFDataCache<?> cache = syms_info_lookup_cache.get(addr);
                if (!cache.isPending()) {
                    syms_info_lookup_cache.remove(addr).dispose();
                    if (syms_info_lookup_cache.size() == 0) syms_info_lookup_cache = null;
                }
            }
            if (line_info_lookup_cache != null) {
                BigInteger addr = line_info_lookup_cache.keySet().iterator().next();
                TCFDataCache<?> cache = line_info_lookup_cache.get(addr);
                if (!cache.isPending()) {
                    line_info_lookup_cache.remove(addr).dispose();
                    if (line_info_lookup_cache.size() == 0) line_info_lookup_cache = null;
                }
            }
            if (func_info_lookup_cache != null) {
                BigInteger addr = func_info_lookup_cache.keySet().iterator().next();
                TCFDataCache<?> cache = func_info_lookup_cache.get(addr);
                if (!cache.isPending()) {
                    func_info_lookup_cache.remove(addr).dispose();
                    if (func_info_lookup_cache.size() == 0) func_info_lookup_cache = null;
                }
            }
            if (syms_info_lookup_cache == null && line_info_lookup_cache == null && func_info_lookup_cache == null) {
                lookup_cache_timer = null;
            }
            else {
                Protocol.invokeLater(2500, this);
            }
        }
    }

    public static class ChildrenStateInfo {
        public boolean running;
        public boolean suspended;
        public boolean not_active;
        public boolean breakpoint;
    }

    private final Map<String,TCFNodeSymbol> symbols = new HashMap<String,TCFNodeSymbol>();

    private int resumed_cnt;
    private boolean resume_pending;
    private boolean resumed_by_action;
    private TCFNode[] last_stack_trace;
    private TCFNode[] last_children_list;
    private String last_label;
    private ImageDescriptor last_image;
    private ChildrenStateInfo last_children_state_info;
    private boolean delayed_children_list_delta;

    /**
     * Wrapper class for IMemoryMap.MemoryRegion.
     * The class help to search memory region by address by
     * providing contains() method.
     */
    public static class MemoryRegion {

        private final BigInteger addr_start;
        private final BigInteger addr_end;

        public final IMemoryMap.MemoryRegion region;

        private MemoryRegion(IMemoryMap.MemoryRegion region) {
            this.region = region;
            Number addr = region.getAddress();
            Number size = region.getSize();
            if (addr == null || size == null) {
                addr_start = null;
                addr_end = null;
            }
            else {
                addr_start = JSON.toBigInteger(addr);
                addr_end = addr_start.add(JSON.toBigInteger(size));
            }
        }

        public boolean contains(BigInteger addr) {
            return
                addr_start != null && addr_end != null &&
                addr_start.compareTo(addr) <= 0 &&
                addr_end.compareTo(addr) > 0;
        }

        @Override
        public String toString() {
            return region.getProperties().toString();
        }
    }

    public static class SignalMask {

        protected Map<String,Object> props;
        protected boolean dont_stop;
        protected boolean dont_pass;
        protected boolean pending;

        public Number getIndex() {
            return (Number)props.get(IProcesses.SIG_INDEX);
        }

        public Number getCode() {
            return (Number)props.get(IProcesses.SIG_CODE);
        }

        public Map<String,Object> getProperties() {
            return props;
        }

        public boolean isDontStop() {
            return dont_stop;
        }

        public boolean isDontPass() {
            return dont_pass;
        }

        public boolean isPending() {
            return pending;
        }

        @Override
        public String toString() {
            StringBuffer bf = new StringBuffer();
            bf.append("[attrs=");
            bf.append(props.toString());
            if (dont_stop) bf.append(",don't stop");
            if (dont_pass) bf.append(",don't pass");
            if (pending) bf.append(",pending");
            bf.append(']');
            return bf.toString();
        }
    }

    TCFNodeExecContext(TCFNode parent, final String id) {
        super(parent, id);
        children_exec = new TCFChildrenExecContext(this);
        children_stack = new TCFChildrenStackTrace(this);
        children_regs = new TCFChildrenRegisters(this);
        children_exps = new TCFChildrenExpressions(this);
        children_hover_exps = new TCFChildrenHoverExpressions(this);
        children_log_exps = new TCFChildrenLogExpressions(this);
        children_modules = new TCFChildrenModules(this);
        children_query = new TCFChildrenContextQuery(this);
        mem_context = new TCFData<IMemory.MemoryContext>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                assert command == null;
                IMemory mem = launch.getService(IMemory.class);
                if (mem == null) {
                    set(null, null, null);
                    return true;
                }
                command = mem.getContext(id, new IMemory.DoneGetContext() {
                    public void doneGetContext(IToken token, Exception error, IMemory.MemoryContext context) {
                        set(token, error, context);
                    }
                });
                return false;
            }
        };
        run_context = new TCFData<IRunControl.RunControlContext>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                assert command == null;
                IRunControl run = launch.getService(IRunControl.class);
                if (run == null) {
                    set(null, null, null);
                    return true;
                }
                command = run.getContext(id, new IRunControl.DoneGetContext() {
                    public void doneGetContext(IToken token, Exception error, IRunControl.RunControlContext context) {
                        if (context != null) model.getContextMap().put(id, context);
                        set(token, error, context);
                    }
                });
                return false;
            }
        };
        prs_context = new TCFData<IProcesses.ProcessContext>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                assert command == null;
                IProcesses prs = launch.getService(IProcesses.class);
                if (prs == null) {
                    set(null, null, null);
                    return true;
                }
                command = prs.getContext(id, new IProcesses.DoneGetContext() {
                    public void doneGetContext(IToken token, Exception error, IProcesses.ProcessContext context) {
                        set(token, error, context);
                    }
                });
                return false;
            }
        };
        memory_map = new TCFData<MemoryRegion[]>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                assert command == null;
                IMemoryMap mmap = launch.getService(IMemoryMap.class);
                if (mmap == null) {
                    set(null, null, null);
                    return true;
                }
                command = mmap.get(id, new IMemoryMap.DoneGet() {
                    public void doneGet(IToken token, Exception error, IMemoryMap.MemoryRegion[] map) {
                        MemoryRegion[] arr = null;
                        if (map != null) {
                            int i = 0;
                            arr = new MemoryRegion[map.length];
                            for (IMemoryMap.MemoryRegion r : map) arr[i++] = new MemoryRegion(r);
                        }
                        set(token, error, arr);
                    }
                });
                return false;
            }
        };
        state = new TCFData<TCFContextState>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                assert command == null;
                if (!run_context.validate(this)) return false;
                IRunControl.RunControlContext ctx = run_context.getData();
                if (ctx == null || !ctx.hasState()) {
                    set(null, null, null);
                    return true;
                }
                command = ctx.getState(new IRunControl.DoneGetState() {
                    public void doneGetState(IToken token, Exception error, boolean suspended, String pc, String reason, Map<String,Object> params) {
                        TCFContextState s = new TCFContextState();
                        s.is_suspended = suspended;
                        s.suspend_pc = pc;
                        s.suspend_reason = reason;
                        s.suspend_params = params;
                        set(token, error, s);
                    }
                });
                return false;
            }
        };
        min_state = new TCFData<TCFContextState>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                assert command == null;
                if (!run_context.validate(this)) return false;
                IRunControl.RunControlContext ctx = run_context.getData();
                if (ctx == null || !ctx.hasState()) {
                    set(null, null, null);
                    return true;
                }
                if (model.no_min_state || state.isValid()) {
                    if (!state.validate(this)) return false;
                    Throwable e = state.getError();
                    TCFContextState s = state.getData();
                    TCFContextState m = null;
                    if (s != null) {
                        m = new TCFContextState();
                        m.is_suspended = s.is_suspended;
                        m.suspend_reason = s.suspend_reason;
                        m.suspend_params = s.suspend_params;
                    }
                    set(null, e, m);
                    return true;
                }
                command = ctx.getMinState(new IRunControl.DoneGetMinState() {
                    public void doneGetMinState(IToken token, Exception error, boolean suspended, String reason, Map<String,Object> params) {
                        if (error instanceof IErrorReport && ((IErrorReport)error).getErrorCode() == IErrorReport.TCF_ERROR_INV_COMMAND) {
                            model.no_min_state = true;
                            command = null;
                            validate();
                            return;
                        }
                        TCFContextState m = new TCFContextState();
                        m.is_suspended = suspended;
                        m.suspend_reason = reason;
                        m.suspend_params = params;
                        set(token, error, m);
                    }
                });
                return false;
            }
        };
        address = new TCFData<BigInteger>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                if (!run_context.validate(this)) return false;
                IRunControl.RunControlContext ctx = run_context.getData();
                if (ctx == null || !ctx.hasState()) {
                    set(null, run_context.getError(), null);
                    return true;
                }
                if (!state.validate(this)) return false;
                TCFContextState s = state.getData();
                if (s == null) {
                    set(null, state.getError(), null);
                    return true;
                }
                if (!s.is_suspended || s.suspend_pc == null) {
                    set(null, null, null);
                    return true;
                }
                set(null, null, new BigInteger(s.suspend_pc));
                return true;
            }
        };
        signal_list = new TCFData<Collection<Map<String,Object>>>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                IProcesses prs = channel.getRemoteService(IProcesses.class);
                if (prs == null) {
                    set(null, null, null);
                    return true;
                }
                command = prs.getSignalList(id, new IProcesses.DoneGetSignalList() {
                    public void doneGetSignalList(IToken token, Exception error, Collection<Map<String, Object>> list) {
                        set(token, error, list);
                    }
                });
                return false;
            }
        };
        signal_mask = new TCFData<SignalMask[]>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                if (!signal_list.validate(this)) return false;
                IProcesses prs = channel.getRemoteService(IProcesses.class);
                final Collection<Map<String,Object>> sigs = signal_list.getData();
                if (prs == null || sigs == null) {
                    set(null, signal_list.getError(), null);
                    return true;
                }
                command = prs.getSignalMask(id, new IProcesses.DoneGetSignalMaskSets() {
                    public void doneGetSignalMask(IToken token, Exception error,
                            Set<Integer> dont_stop, Set<Integer> dont_pass, Set<Integer> pending) {
                        int n = 0;
                        SignalMask[] list = new SignalMask[sigs.size()];
                        for (Map<String,Object> m : sigs) {
                            SignalMask s = list[n++] = new SignalMask();
                            s.props = m;
                            int i = s.getIndex().intValue();
                            s.dont_stop = dont_stop.contains(i);
                            s.dont_pass = dont_pass.contains(i);
                            s.pending = pending.contains(i);
                        }
                        set(token, error, list);
                    }
                });
                return false;
            }
        };
        memory_node = new TCFData<TCFNodeExecContext>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                String mem_id = null;
                if (!run_context.validate(this)) return false;
                Throwable err = run_context.getError();
                if (err == null) {
                    IRunControl.RunControlContext ctx = run_context.getData();
                    if (ctx != null) mem_id = ctx.getProcessID();
                }
                if (err != null) {
                    set(null, err, null);
                }
                else if (mem_id == null) {
                    set(null, new Exception("Context does not provide memory access"), null);
                }
                else {
                    if (!model.createNode(mem_id, this)) return false;
                    if (!isValid()) set(null, null, (TCFNodeExecContext)model.getNode(mem_id));
                }
                return true;
            }
        };
        symbols_node = new TCFData<TCFNodeExecContext>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                String syms_id = null;
                if (!run_context.validate(this)) return false;
                Throwable err = run_context.getError();
                if (err == null) {
                    IRunControl.RunControlContext ctx = run_context.getData();
                    if (ctx != null) {
                        syms_id = ctx.getSymbolsGroup();
                        if (syms_id == null) syms_id = ctx.getProcessID();
                    }
                }
                if (err != null) {
                    set(null, err, null);
                }
                else if (syms_id == null) {
                    set(null, new Exception("Context does not support symbol groups"), null);
                }
                else {
                    if (!model.createNode(syms_id, this)) return false;
                    if (!isValid()) set(null, null, (TCFNodeExecContext)model.getNode(syms_id));
                }
                return true;
            }
        };
        full_name = new TCFData<String>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                if (!run_context.validate(this)) return false;
                IRunControl.RunControlContext ctx = run_context.getData();
                String res = null;
                if (ctx != null) {
                    res = ctx.getName();
                    if (res == null) {
                        res = ctx.getID();
                    }
                    else {
                        // Add ancestor names
                        TCFNodeExecContext p = TCFNodeExecContext.this;
                        ArrayList<String> lst = new ArrayList<String>();
                        lst.add(res);
                        while (p.parent instanceof TCFNodeExecContext) {
                            p = (TCFNodeExecContext)p.parent;
                            TCFDataCache<IRunControl.RunControlContext> run_ctx_cache = p.run_context;
                            if (!run_ctx_cache.validate(this)) return false;
                            IRunControl.RunControlContext run_ctx_data = run_ctx_cache.getData();
                            String name = null;
                            if (run_ctx_data != null) name = run_ctx_data.getName();
                            if (name == null) name = "";
                            lst.add(name);
                        }
                        StringBuffer bf = new StringBuffer();
                        for (int i = lst.size(); i > 0; i--) {
                            String name = lst.get(i - 1);
                            boolean quote = name.indexOf('/') >= 0;
                            bf.append('/');
                            if (quote) bf.append('"');
                            bf.append(name);
                            if (quote) bf.append('"');
                        }
                        res = bf.toString();
                    }
                }
                set(null, null, res);
                return true;
            }
        };

        reset_capabilities = new TCFData<Collection<Map<String, Object>>>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                IContextReset reset = launch.getService(IContextReset.class);
                if (reset == null) {
                    set(null, null, null);
                    return true;
                }
                command = reset.getCapabilities(id, new IContextReset.DoneGetCapabilities() {
                    @Override
                    public void doneGetCapabilities(IToken token, Exception error, Collection<Map<String, Object>> capabilities) {
                        set(token, error, capabilities);
                    }
                });
                return false;
            }
        };
        TCFMemoryBlock.onMemoryNodeCreated(this);
        updateTerminal();
    }

    @Override
    void dispose() {
        assert !isDisposed();
        ArrayList<TCFNodeSymbol> l = new ArrayList<TCFNodeSymbol>(symbols.values());
        for (TCFNodeSymbol s : l) s.dispose();
        assert symbols.size() == 0;
        super.dispose();
    }

    void setMemSeqNo(int no) {
        mem_seq_no = no;
    }

    void setExeSeqNo(int no) {
        exe_seq_no = no;
    }

    TCFChildren getHoverExpressionCache(String expression) {
        children_hover_exps.setExpression(expression);
        return children_hover_exps;
    }

    public TCFChildrenLogExpressions getLogExpressionCache() {
        return children_log_exps;
    }

    void setRunContext(IRunControl.RunControlContext ctx) {
        run_context.reset(ctx);
    }

    void setProcessContext(IProcesses.ProcessContext ctx) {
        prs_context.reset(ctx);
    }

    void setMemoryContext(IMemory.MemoryContext ctx) {
        mem_context.reset(ctx);
    }

    public TCFDataCache<TCFNodeExecContext> getSymbolsNode() {
        return symbols_node;
    }

    public TCFDataCache<TCFNodeExecContext> getMemoryNode() {
        return memory_node;
    }

    public TCFDataCache<MemoryRegion[]> getMemoryMap() {
        return memory_map;
    }

    public TCFDataCache<Collection<Map<String,Object>>> getSignalList() {
        return signal_list;
    }

    public TCFDataCache<SignalMask[]> getSignalMask() {
        return signal_mask;
    }

    public TCFDataCache<TCFSymFileRef> getSymFileInfo(final BigInteger addr) {
        if (addr == null || isDisposed()) return null;
        TCFDataCache<TCFSymFileRef> ref_cache;
        if (syms_info_lookup_cache != null) {
            ref_cache = syms_info_lookup_cache.get(addr);
            if (ref_cache != null) return ref_cache;
        }
        final ISymbols syms = launch.getService(ISymbols.class);
        if (syms == null) return null;
        if (syms_info_lookup_cache == null) {
            syms_info_lookup_cache = new LinkedHashMap<BigInteger,TCFDataCache<TCFSymFileRef>>(11, 0.75f, true);
            if (lookup_cache_timer == null) lookup_cache_timer = new LookupCacheTimer();
        }
        syms_info_lookup_cache.put(addr, ref_cache = new TCFData<TCFSymFileRef>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                if (!memory_node.validate(this)) return false;
                IMemory.MemoryContext mem_data = null;
                TCFNodeExecContext mem = memory_node.getData();
                if (mem != null) {
                    TCFDataCache<IMemory.MemoryContext> mem_cache = mem.mem_context;
                    if (!mem_cache.validate(this)) return false;
                    mem_data = mem_cache.getData();
                }
                final TCFSymFileRef ref_data = new TCFSymFileRef();
                if (mem_data != null) {
                    ref_data.context_id = mem_data.getID();
                    ref_data.address_size = mem_data.getAddressSize();
                }
                command = syms.getSymFileInfo(ref_data.context_id, addr, new ISymbols.DoneGetSymFileInfo() {
                    public void doneGetSymFileInfo(IToken token, Exception error, Map<String,Object> props) {
                        ref_data.address = addr;
                        ref_data.error = error;
                        ref_data.props = props;
                        set(token, null, ref_data);
                    }
                });
                return false;
            }
        });
        return ref_cache;
    }

    public TCFDataCache<TCFSourceRef> getLineInfo(final BigInteger addr) {
        if (isDisposed()) return null;
        TCFDataCache<TCFSourceRef> ref_cache;
        if (line_info_lookup_cache != null) {
            ref_cache = line_info_lookup_cache.get(addr);
            if (ref_cache != null) return ref_cache;
        }
        final ILineNumbers ln = launch.getService(ILineNumbers.class);
        if (ln == null) return null;
        final BigInteger n0 = addr;
        final BigInteger n1 = n0.add(BigInteger.valueOf(1));
        if (line_info_lookup_cache == null) {
            line_info_lookup_cache = new LinkedHashMap<BigInteger,TCFDataCache<TCFSourceRef>>(11, 0.75f, true);
            if (lookup_cache_timer == null) lookup_cache_timer = new LookupCacheTimer();
        }
        line_info_lookup_cache.put(addr, ref_cache = new TCFData<TCFSourceRef>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                if (!memory_node.validate(this)) return false;
                IMemory.MemoryContext mem_data = null;
                TCFNodeExecContext mem = memory_node.getData();
                if (mem != null) {
                    TCFDataCache<IMemory.MemoryContext> mem_cache = mem.mem_context;
                    if (!mem_cache.validate(this)) return false;
                    mem_data = mem_cache.getData();
                }
                final TCFSourceRef ref_data = new TCFSourceRef();
                if (mem_data != null) {
                    ref_data.context_id = mem_data.getID();
                    ref_data.address_size = mem_data.getAddressSize();
                }
                command = ln.mapToSource(id, n0, n1, new ILineNumbers.DoneMapToSource() {
                    public void doneMapToSource(IToken token, Exception error, ILineNumbers.CodeArea[] areas) {
                        ref_data.address = addr;
                        if (error == null && areas != null && areas.length > 0) {
                            for (ILineNumbers.CodeArea area : areas) {
                                BigInteger a0 = JSON.toBigInteger(area.start_address);
                                BigInteger a1 = JSON.toBigInteger(area.end_address);
                                if (n0.compareTo(a0) >= 0 && n0.compareTo(a1) < 0) {
                                    boolean add = true;
                                    if (ref_data.area != null) {
                                        BigInteger b0 = JSON.toBigInteger(ref_data.area.start_address);
                                        BigInteger b1 = JSON.toBigInteger(ref_data.area.end_address);
                                        add = a1.subtract(a0).compareTo(b1.subtract(b0)) < 0;
                                    }
                                    if (add) {
                                        if (area.start_address != a0 || area.end_address != a1) {
                                            area = new ILineNumbers.CodeArea(area.directory, area.file,
                                                    area.start_line, area.start_column,
                                                    area.end_line, area.end_column,
                                                    a0, a1, area.isa,
                                                    area.is_statement, area.basic_block,
                                                    area.prologue_end, area.epilogue_begin);
                                        }
                                        ref_data.area = area;
                                    }
                                }
                            }
                        }
                        ref_data.error = error;
                        set(token, null, ref_data);
                    }
                });
                return false;
            }
        });
        return ref_cache;
    }

    public TCFDataCache<TCFFunctionRef> getFuncInfo(final BigInteger addr) {
        if (isDisposed()) return null;
        TCFDataCache<TCFFunctionRef> ref_cache;
        if (func_info_lookup_cache != null) {
            ref_cache = func_info_lookup_cache.get(addr);
            if (ref_cache != null) return ref_cache;
        }
        final ISymbols syms = launch.getService(ISymbols.class);
        if (syms == null) return null;
        if (func_info_lookup_cache == null) {
            func_info_lookup_cache = new LinkedHashMap<BigInteger,TCFDataCache<TCFFunctionRef>>(11, 0.75f, true);
            if (lookup_cache_timer == null) lookup_cache_timer = new LookupCacheTimer();
        }
        func_info_lookup_cache.put(addr, ref_cache = new TCFData<TCFFunctionRef>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                if (!memory_node.validate(this)) return false;
                IMemory.MemoryContext mem_data = null;
                TCFNodeExecContext mem = memory_node.getData();
                if (mem != null) {
                    TCFDataCache<IMemory.MemoryContext> mem_cache = mem.mem_context;
                    if (!mem_cache.validate(this)) return false;
                    mem_data = mem_cache.getData();
                }
                final TCFFunctionRef ref_data = new TCFFunctionRef();
                if (mem_data != null) {
                    ref_data.context_id = mem_data.getID();
                    ref_data.address_size = mem_data.getAddressSize();
                }
                ref_data.address = addr;
                command = syms.findByAddr(id, addr, new ISymbols.DoneFind() {
                    public void doneFind(IToken token, Exception error, String symbol_id) {
                        ref_data.error = error;
                        ref_data.symbol_id = symbol_id;
                        set(token, null, ref_data);
                    }
                });
                return false;
            }
        });
        return ref_cache;
    }

    private void clearLookupCaches() {
        if (syms_info_lookup_cache != null) {
            Iterator<TCFDataCache<TCFSymFileRef>> i = syms_info_lookup_cache.values().iterator();
            while (i.hasNext()) {
                TCFDataCache<TCFSymFileRef> cache = i.next();
                if (cache.isPending()) continue;
                cache.dispose();
                i.remove();
            }
            if (syms_info_lookup_cache.size() == 0) syms_info_lookup_cache = null;
        }
        if (line_info_lookup_cache != null) {
            Iterator<TCFDataCache<TCFSourceRef>> i = line_info_lookup_cache.values().iterator();
            while (i.hasNext()) {
                TCFDataCache<TCFSourceRef> cache = i.next();
                if (cache.isPending()) continue;
                cache.dispose();
                i.remove();
            }
            if (line_info_lookup_cache.size() == 0) line_info_lookup_cache = null;
        }
        if (func_info_lookup_cache != null) {
            Iterator<TCFDataCache<TCFFunctionRef>> i = func_info_lookup_cache.values().iterator();
            while (i.hasNext()) {
                TCFDataCache<TCFFunctionRef> cache = i.next();
                if (cache.isPending()) continue;
                cache.dispose();
                i.remove();
            }
            if (func_info_lookup_cache.size() == 0) func_info_lookup_cache = null;
        }
    }

    private void updateTerminal() {
        new Runnable() {
            @Override
            public void run() {
                if (isDisposed()) return;
                if (!run_context.validate(this)) return;
                IRunControl.RunControlContext ctx = run_context.getData();
                if (ctx != null) {
                    @SuppressWarnings("unchecked")
                    Map<String,Object> uart = (Map<String,Object>)ctx.getProperties().get("UART");
                    if (uart != null) launch.openUartStreams(id, uart);
                }
            }
        }.run();
    }

    @Override
    public TCFNode getParent(IPresentationContext ctx) {
        assert Protocol.isDispatchThread();
        if (IDebugUIConstants.ID_DEBUG_VIEW.equals(ctx.getId())) {
            Set<String> ids = launch.getContextFilter();
            if (ids != null) {
                if (ids.contains(id)) return model.getRootNode();
                if (parent instanceof TCFNodeLaunch) return null;
            }
        }
        return parent;
    }

    public TCFDataCache<IRunControl.RunControlContext> getRunContext() {
        return run_context;
    }

    public TCFDataCache<IProcesses.ProcessContext> getProcessContext() {
        return prs_context;
    }

    public TCFDataCache<IMemory.MemoryContext> getMemoryContext() {
        return mem_context;
    }

    public TCFDataCache<BigInteger> getAddress() {
        return address;
    }

    public TCFDataCache<TCFContextState> getState() {
        return state;
    }

    public TCFDataCache<TCFContextState> getMinState() {
        return min_state;
    }

    public TCFChildrenStackTrace getStackTrace() {
        return children_stack;
    }

    public TCFChildren getRegisters() {
        return children_regs;
    }

    public TCFChildren getModules() {
        return children_modules;
    }

    public TCFChildren getChildren() {
        return children_exec;
    }

    public TCFNodeStackFrame getLastTopFrame() {
        if (!resume_pending) return null;
        if (last_stack_trace == null || last_stack_trace.length == 0) return null;
        return (TCFNodeStackFrame)last_stack_trace[0];
    }

    public TCFNodeStackFrame getViewBottomFrame() {
        if (last_stack_trace == null || last_stack_trace.length == 0) return null;
        return (TCFNodeStackFrame)last_stack_trace[last_stack_trace.length - 1];
    }

    /**
     * Get context full name - including all ancestor names.
     * Return context ID if the context does not have a name.
     * @return cache item with the context full name.
     */
    public TCFDataCache<String> getFullName() {
        return full_name;
    }

    public TCFDataCache<Collection<Map<String, Object>>> getResetCapabilities() {
        return reset_capabilities;
    }

    public void reset(String reset_type, Map<String, Object> params) {
        IContextReset ctx_reset = launch.getService(IContextReset.class);
        if (ctx_reset != null) {
            ctx_reset.reset(id, reset_type, params, new IContextReset.DoneReset() {
                @Override
                public void doneReset(IToken token, Exception error) {
                    if (error != null) {
                        model.showMessageBox("Cannot reset context", error);
                    }
                }
            });
        }
    }

    public void addSymbol(TCFNodeSymbol s) {
        assert symbols.get(s.id) == null;
        symbols.put(s.id, s);
    }

    public void removeSymbol(TCFNodeSymbol s) {
        assert symbols.get(s.id) == s;
        symbols.remove(s.id);
    }

    /**
     * Return true if this context cannot be accessed because it is not active.
     * Not active means the target is suspended, but this context is not one that is
     * currently scheduled to run on a target CPU, and the debuggers don't support
     * access to register values and other properties of such contexts.
     */
    public boolean isNotActive() {
        TCFContextState state_data = state.getData();
        if (state_data != null) return state_data.isNotActive();
        return false;
    }

    private boolean okToShowLastStack() {
        return resume_pending && last_stack_trace != null;
    }

    private boolean okToHideStack() {
        TCFAction action = model.getActiveAction(id);
        if (action != null && action.showRunning()) return true;
        TCFContextState state_data = min_state.getData();
        if (state_data == null) return true;
        assert state_data.suspend_pc == null;
        if (!state_data.is_suspended) return true;
        if (state_data.isNotActive()) return true;
        return false;
    }

    @Override
    protected boolean getData(IChildrenCountUpdate result, Runnable done) {
        TCFChildren children = null;
        String view_id = result.getPresentationContext().getId();
        if (IDebugUIConstants.ID_DEBUG_VIEW.equals(view_id)) {
            if (!run_context.validate(done)) return false;
            IRunControl.RunControlContext ctx = run_context.getData();
            if (ctx != null && ctx.hasState()) {
                if (okToShowLastStack()) {
                    result.setChildCount(last_stack_trace.length);
                    return true;
                }
                if (!min_state.validate(done)) return false;
                if (okToHideStack()) {
                    last_stack_trace = empty_node_array;
                    result.setChildCount(0);
                    return true;
                }
                children = children_stack;
            }
            else {
                if (!model.getAutoChildrenListUpdates() && last_children_list != null) {
                    result.setChildCount(last_children_list.length);
                    return true;
                }
                children = children_exec;
            }
        }
        else if (IDebugUIConstants.ID_REGISTER_VIEW.equals(view_id)) {
            children = children_regs;
        }
        else if (IDebugUIConstants.ID_EXPRESSION_VIEW.equals(view_id)) {
            if (!run_context.validate(done)) return false;
            IRunControl.RunControlContext ctx = run_context.getData();
            if (ctx != null) children = children_exps;
        }
        else if (TCFModel.ID_EXPRESSION_HOVER.equals(view_id)) {
            if (!run_context.validate(done)) return false;
            IRunControl.RunControlContext ctx = run_context.getData();
            if (ctx != null && ctx.hasState()) children = children_hover_exps;
        }
        else if (IDebugUIConstants.ID_MODULE_VIEW.equals(view_id)) {
            if (!mem_context.validate(done)) return false;
            IMemory.MemoryContext ctx = mem_context.getData();
            if (ctx != null) children = children_modules;
        }
        else if (ITCFDebugUIConstants.ID_CONTEXT_QUERY_VIEW.equals(view_id)) {
            if (!children_query.setQuery(result, done)) return false;
            children = children_query;
        }
        if (children != null) {
            if (!children.validate(done)) return false;
            if (children == children_stack) last_stack_trace = children_stack.toArray();
            if (children == children_exec) last_children_list = children_exec.toArray();
            result.setChildCount(children.size());
        }
        else {
            result.setChildCount(0);
        }
        return true;
    }

    private void setResultChildren(IChildrenUpdate result, TCFNode[] arr) {
        int offset = 0;
        int r_offset = result.getOffset();
        int r_length = result.getLength();
        for (TCFNode n : arr) {
            if (offset >= r_offset && offset < r_offset + r_length) {
                result.setChild(n, offset);
            }
            offset++;
        }
    }

    @Override
    protected boolean getData(IChildrenUpdate result, Runnable done) {
        TCFChildren children = null;
        String view_id = result.getPresentationContext().getId();
        if (IDebugUIConstants.ID_DEBUG_VIEW.equals(view_id)) {
            if (!run_context.validate(done)) return false;
            IRunControl.RunControlContext ctx = run_context.getData();
            if (ctx != null && ctx.hasState()) {
                if (okToShowLastStack()) {
                    setResultChildren(result, last_stack_trace);
                    return true;
                }
                if (!min_state.validate(done)) return false;
                if (okToHideStack()) {
                    last_stack_trace = empty_node_array;
                    return true;
                }
                // Force creation of register nodes.
                // It helps dispatching of registerChanged events to stack frames.
                if (!children_regs.validate(done)) return false;
                children = children_stack;
            }
            else {
                if (!model.getAutoChildrenListUpdates() && last_children_list != null) {
                    setResultChildren(result, last_children_list);
                    return true;
                }
                children = children_exec;
            }
        }
        else if (IDebugUIConstants.ID_REGISTER_VIEW.equals(view_id)) {
            children = children_regs;
        }
        else if (IDebugUIConstants.ID_EXPRESSION_VIEW.equals(view_id)) {
            if (!run_context.validate(done)) return false;
            IRunControl.RunControlContext ctx = run_context.getData();
            if (ctx != null) children = children_exps;
        }
        else if (TCFModel.ID_EXPRESSION_HOVER.equals(view_id)) {
            if (!run_context.validate(done)) return false;
            IRunControl.RunControlContext ctx = run_context.getData();
            if (ctx != null && ctx.hasState()) children = children_hover_exps;
        }
        else if (IDebugUIConstants.ID_MODULE_VIEW.equals(view_id)) {
            if (!mem_context.validate(done)) return false;
            IMemory.MemoryContext ctx = mem_context.getData();
            if (ctx != null) children = children_modules;
        }
        else if (ITCFDebugUIConstants.ID_CONTEXT_QUERY_VIEW.equals(view_id)) {
            if (!children_query.setQuery(result, done)) return false;
            children = children_query;
        }
        if (children == null) return true;
        if (children == children_stack) {
            if (!children.validate(done)) return false;
            last_stack_trace = children_stack.toArray();
        }
        if (children == children_exec) {
            if (!children.validate(done)) return false;
            last_children_list = children_exec.toArray();
        }
        return children.getData(result, done);
    }

    @Override
    protected boolean getData(IHasChildrenUpdate result, Runnable done) {
        TCFChildren children = null;
        String view_id = result.getPresentationContext().getId();
        if (IDebugUIConstants.ID_DEBUG_VIEW.equals(view_id)) {
            if (!run_context.validate(done)) return false;
            IRunControl.RunControlContext ctx = run_context.getData();
            if (ctx != null && ctx.hasState()) {
                if (okToShowLastStack()) {
                    result.setHasChilren(last_stack_trace.length > 0);
                    return true;
                }
                if (!min_state.validate(done)) return false;
                if (okToHideStack()) {
                    last_stack_trace = empty_node_array;
                    result.setHasChilren(false);
                    return true;
                }
                Boolean has_children = children_stack.checkHasChildren(done);
                if (has_children == null) return false;
                result.setHasChilren(has_children);
                return true;
            }
            else {
                if (!model.getAutoChildrenListUpdates() && last_children_list != null) {
                    result.setHasChilren(last_children_list.length > 0);
                    return true;
                }
                children = children_exec;
            }
        }
        else if (IDebugUIConstants.ID_REGISTER_VIEW.equals(view_id)) {
            children = children_regs;
        }
        else if (IDebugUIConstants.ID_EXPRESSION_VIEW.equals(view_id)) {
            if (!run_context.validate(done)) return false;
            IRunControl.RunControlContext ctx = run_context.getData();
            if (ctx != null) children = children_exps;
        }
        else if (TCFModel.ID_EXPRESSION_HOVER.equals(view_id)) {
            if (!run_context.validate(done)) return false;
            IRunControl.RunControlContext ctx = run_context.getData();
            if (ctx != null && ctx.hasState()) children = children_hover_exps;
        }
        else if (IDebugUIConstants.ID_MODULE_VIEW.equals(view_id)) {
            if (!mem_context.validate(done)) return false;
            IMemory.MemoryContext ctx = mem_context.getData();
            if (ctx != null) children = children_modules;
        }
        else if (ITCFDebugUIConstants.ID_CONTEXT_QUERY_VIEW.equals(view_id)) {
            if (!children_query.setQuery(result, done)) return false;
            children = children_query;
        }
        if (children != null) {
            if (!children.validate(done)) return false;
            if (children == children_stack) last_stack_trace = children_stack.toArray();
            if (children == children_exec) last_children_list = children_exec.toArray();
            result.setHasChilren(children.size() > 0);
        }
        else {
            result.setHasChilren(false);
        }
        return true;
    }

    private String addStateName(StringBuffer label, TCFContextState state_data) {
        String image_name = ImageCache.IMG_THREAD_UNKNOWN_STATE;
        assert !state_data.is_suspended;
        if (state_data.suspend_params != null) {
            String name = (String)state_data.suspend_params.get(IRunControl.STATE_NAME);
            if (name != null) {
                label.append(" (");
                label.append(name);
                label.append(")");
                return image_name;
            }
        }
        if (state_data.isReversing()) {
            image_name = ImageCache.IMG_THREAD_REVERSING;
            label.append(" (Reversing)");
        }
        else {
            image_name = ImageCache.IMG_THREAD_RUNNNIG;
            label.append(" (Running)");
        }
        return image_name;
    }

    @Override
    protected boolean getData(ILabelUpdate result, Runnable done) {
        if (!run_context.validate(done)) return false;
        String image_name = null;
        boolean suspended_by_bp = false;
        ChildrenStateInfo children_state_info = null;
        StringBuffer label = new StringBuffer();
        Throwable error = run_context.getError();
        if (error != null) {
            result.setForeground(ColorCache.rgb_error, 0);
            label.append(id);
            label.append(": ");
            label.append(TCFModel.getErrorMessage(error, false));
        }
        else {
            String view_id = result.getPresentationContext().getId();
            if (ITCFDebugUIConstants.ID_CONTEXT_QUERY_VIEW.equals(view_id)) {
                TCFChildrenContextQuery.Descendants des = TCFChildrenContextQuery.getDescendants(this, result, done);
                if (des == null) return false;
                if (des.map != null && des.map.size() > 0) {
                    label.append("(");
                    label.append(des.map.size());
                    label.append(") ");
                }
                if (!des.include_parent) result.setForeground(ColorCache.rgb_disabled, 0);
            }
            IRunControl.RunControlContext ctx = run_context.getData();
            if (ctx == null) {
                label.append(id);
            }
            else {
                String nm = ctx.getName();
                if (nm == null && !ctx.hasState()) {
                    String prs = ctx.getProcessID();
                    if (prs != null) {
                        if (!prs_context.validate(done)) return false;
                        IProcesses.ProcessContext pctx = prs_context.getData();
                        if (pctx != null) nm = pctx.getName();
                    }
                }
                label.append(nm != null ? nm : id);
                Object info = ctx.getProperties().get("AdditionalInfo");
                if (info != null) label.append(info.toString());
                if (TCFModel.ID_PINNED_VIEW.equals(view_id) || ITCFDebugUIConstants.ID_CONTEXT_QUERY_VIEW.equals(view_id)) {
                    image_name = ctx.hasState() ? ImageCache.IMG_THREAD_UNKNOWN_STATE : ImageCache.IMG_PROCESS_RUNNING;
                }
                else if (ctx.hasState()) {
                    // Thread
                    TCFAction action = model.getActiveAction(id);
                    if (resume_pending && resumed_by_action || action != null) {
                        if (action != null && action.showRunning()) {
                            image_name = ImageCache.IMG_THREAD_RUNNNIG;
                        }
                        else {
                            if (!min_state.validate(done)) return false;
                            TCFContextState state_data = min_state.getData();
                            image_name = ImageCache.IMG_THREAD_UNKNOWN_STATE;
                            if (state_data != null) {
                                if (!state_data.is_suspended) {
                                    image_name = addStateName(label, state_data);
                                }
                                else {
                                    suspended_by_bp = IRunControl.REASON_BREAKPOINT.equals(state_data.suspend_reason);
                                }
                            }
                            if (resume_pending && last_label != null) {
                                result.setImageDescriptor(ImageCache.getImageDescriptor(image_name), 0);
                                result.setLabel(last_label, 0);
                                return true;
                            }
                        }
                    }
                    else if (resume_pending && last_label != null && last_image != null) {
                        result.setImageDescriptor(last_image, 0);
                        result.setLabel(last_label, 0);
                        return true;
                    }
                    else {
                        if (!min_state.validate(done)) return false;
                        TCFContextState state_data = min_state.getData();
                        if (state_data != null && state_data.isNotActive()) {
                            image_name = ImageCache.IMG_THREAD_NOT_ACTIVE;
                            label.append(" (Not active)");
                            if (state_data.suspend_reason != null && !state_data.suspend_reason.equals(IRunControl.REASON_USER_REQUEST)) {
                                label.append(" - ");
                                label.append(state_data.suspend_reason);
                            }
                        }
                        else {
                            image_name = ImageCache.IMG_THREAD_UNKNOWN_STATE;
                            if (state_data != null) {
                                if (!state_data.is_suspended) {
                                    image_name = addStateName(label, state_data);
                                }
                                else {
                                    image_name = ImageCache.IMG_THREAD_SUSPENDED;
                                    String sig_name = null;
                                    String bp_names = null;
                                    String suspend_reason = model.getContextActionResult(id);
                                    if (suspend_reason == null) suspend_reason = state_data.suspend_reason;
                                    if (state_data.suspend_params != null) {
                                        sig_name = (String)state_data.suspend_params.get(IRunControl.STATE_SIGNAL_DESCRIPTION);
                                        if (sig_name == null) sig_name = (String)state_data.suspend_params.get(IRunControl.STATE_SIGNAL_NAME);
                                        Object ids = state_data.suspend_params.get(IRunControl.STATE_BREAKPOINT_IDS);
                                        if (ids != null) {
                                            @SuppressWarnings("unchecked")
                                            Collection<String> bp_ids = (Collection<String>)ids;
                                            TCFBreakpointsModel bp_model = TCFBreakpointsModel.getBreakpointsModel();
                                            for (String bp_id : bp_ids) {
                                                IBreakpoint bp = bp_model.getBreakpoint(bp_id);
                                                if (bp != null) {
                                                    String bp_name = null;
                                                    IMarker m = bp.getMarker();
                                                    if (m != null) {
                                                        bp_name = m.getAttribute(TCFBreakpointsModel.ATTR_ADDRESS, null);
                                                        if (bp_name == null) bp_name = m.getAttribute(TCFBreakpointsModel.ATTR_FUNCTION, null);
                                                        if (bp_name == null) bp_name = m.getAttribute(TCFBreakpointsModel.ATTR_EXPRESSION, null);
                                                        if (bp_name == null) {
                                                            String file = m.getAttribute(TCFBreakpointsModel.ATTR_REQESTED_FILE, null);
                                                            int line = m.getAttribute(TCFBreakpointsModel.ATTR_REQESTED_LINE, 0);
                                                            if (file == null) file = m.getAttribute(TCFBreakpointsModel.ATTR_FILE, null);
                                                            if (line == 0) line = m.getAttribute(TCFBreakpointsModel.ATTR_LINE, 0);
                                                            if (file != null && line > 0) {
                                                                bp_name = new Path(file).lastSegment() + ":" + line;
                                                            }
                                                        }
                                                    }
                                                    if (bp_name == null) bp_name = bp_id;
                                                    if (bp_names == null) bp_names = bp_name;
                                                    else bp_names = bp_names + ", " + bp_name;
                                                }
                                            }
                                        }
                                    }
                                    if (suspend_reason == null) suspend_reason = "Suspended";
                                    suspended_by_bp = IRunControl.REASON_BREAKPOINT.equals(suspend_reason) || bp_names != null;
                                    label.append(" (");
                                    label.append(suspend_reason);
                                    if (IRunControl.REASON_SIGNAL.equals(suspend_reason) && sig_name != null) {
                                        label.append(": ");
                                        label.append(sig_name);
                                        sig_name = null;
                                    }
                                    if (IRunControl.REASON_BREAKPOINT.equals(suspend_reason) && bp_names != null) {
                                        label.append(": ");
                                        label.append(bp_names);
                                        bp_names = null;
                                    }
                                    if (sig_name != null) {
                                        label.append("; ");
                                        label.append(IRunControl.REASON_SIGNAL);
                                        label.append(": ");
                                        label.append(sig_name);
                                    }
                                    if (bp_names != null) {
                                        label.append("; ");
                                        label.append(IRunControl.REASON_BREAKPOINT);
                                        label.append(": ");
                                        label.append(bp_names);
                                    }
                                    label.append(")");
                                    if (state_data.suspend_params != null) {
                                        String prs = (String)state_data.suspend_params.get(IRunControl.STATE_CONTEXT);
                                        if (prs != null) {
                                            label.append(", ");
                                            label.append(prs);
                                        }
                                        String cpu = (String)state_data.suspend_params.get(IRunControl.STATE_CPU);
                                        if (cpu != null) {
                                            label.append(", ");
                                            label.append(cpu);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    // Thread container (process)
                    children_state_info = new ChildrenStateInfo();
                    if (!hasSuspendedChildren(children_state_info, done)) return false;
                    if (children_state_info.suspended) image_name = ImageCache.IMG_PROCESS_SUSPENDED;
                    else image_name = ImageCache.IMG_PROCESS_RUNNING;
                    suspended_by_bp = children_state_info.breakpoint;
                }
            }
        }
        last_children_state_info = children_state_info;
        last_image = ImageCache.getImageDescriptor(image_name);
        if (suspended_by_bp) last_image = ImageCache.addOverlay(last_image, ImageCache.IMG_BREAKPOINT_OVERLAY);
        result.setImageDescriptor(last_image, 0);
        result.setLabel(last_label = label.toString(), 0);
        return true;
    }

    @Override
    protected boolean getData(IViewerInputUpdate result, Runnable done) {
        result.setInputElement(this);
        String view_id = result.getPresentationContext().getId();
        if (IDebugUIConstants.ID_VARIABLE_VIEW.equals(view_id)) {
            if (!children_stack.validate(done)) return false;
            TCFNodeStackFrame frame = children_stack.getTopFrame();
            if (frame != null) result.setInputElement(frame);
        }
        else if (IDebugUIConstants.ID_MODULE_VIEW.equals(view_id)) {
            // TODO: need to post view input delta when memory context changes
            TCFDataCache<TCFNodeExecContext> mem = model.searchMemoryContext(this);
            if (mem == null) return true;
            if (!mem.validate(done)) return false;
            if (mem.getData() == null) return true;
            result.setInputElement(mem.getData());
        }
        return true;
    }

    @Override
    public void refresh(IWorkbenchPart part) {
        if (part instanceof IMemoryRenderingSite) {
            model.onMemoryChanged(id, false, false, false);
        }
        else {
            last_children_list = null;
            last_children_state_info = null;
            last_stack_trace = null;
            last_label = null;
            last_image = null;
            super.refresh(part);
        }
    }

    void postAllChangedDelta() {
        postContentChangedDelta();
        postStateChangedDelta();
    }

    void postContextAddedDelta() {
        if (parent instanceof TCFNodeExecContext) {
            TCFNodeExecContext exe = (TCFNodeExecContext)parent;
            ChildrenStateInfo info = exe.last_children_state_info;
            if (info != null) {
                if (!model.getAutoChildrenListUpdates()) {
                    // Manual updates.
                    return;
                }
                if (!info.suspended && !info.not_active && model.getDelayChildrenListUpdates()) {
                    // Delay content update until a child is suspended.
                    exe.delayed_children_list_delta = true;
                    return;
                }
            }
        }
        for (TCFModelProxy p : model.getModelProxies()) {
            String view_id = p.getPresentationContext().getId();
            if (IDebugUIConstants.ID_DEBUG_VIEW.equals(view_id)) {
                p.addDelta(this, IModelDelta.INSERTED);
            }
            else if (ITCFDebugUIConstants.ID_CONTEXT_QUERY_VIEW.equals(view_id)) {
                p.addDelta(parent, IModelDelta.CONTENT);
            }
        }
    }

    private void postContextRemovedDelta() {
        if (parent instanceof TCFNodeExecContext) {
            TCFNodeExecContext exe = (TCFNodeExecContext)parent;
            ChildrenStateInfo info = exe.last_children_state_info;
            if (info != null) {
                if (!model.getAutoChildrenListUpdates()) {
                    // Manual updates.
                    return;
                }
                if (!info.suspended && !info.not_active && model.getDelayChildrenListUpdates()) {
                    // Delay content update until a child is suspended.
                    exe.delayed_children_list_delta = true;
                    return;
                }
            }
        }
        for (TCFModelProxy p : model.getModelProxies()) {
            String view_id = p.getPresentationContext().getId();
            if (IDebugUIConstants.ID_DEBUG_VIEW.equals(view_id)) {
                p.addDelta(this, IModelDelta.REMOVED);
            }
            else if (ITCFDebugUIConstants.ID_CONTEXT_QUERY_VIEW.equals(view_id)) {
                p.addDelta(parent, IModelDelta.CONTENT);
            }
        }
        // Update parent icon overlays
        TCFNode n = parent;
        while (n instanceof TCFNodeExecContext) {
            TCFNodeExecContext e = (TCFNodeExecContext)n;
            ChildrenStateInfo info = e.last_children_state_info;
            if (info != null && info.suspended) e.postStateChangedDelta();
            n = n.parent;
        }
    }

    private void postContentChangedDelta() {
        delayed_children_list_delta = false;
        for (TCFModelProxy p : model.getModelProxies()) {
            int flags = 0;
            String view_id = p.getPresentationContext().getId();
            if ( (IDebugUIConstants.ID_DEBUG_VIEW.equals(view_id) ||
                  ITCFDebugUIConstants.ID_CONTEXT_QUERY_VIEW.equals(view_id)) &&
                 (launch.getContextActionsCount(id) == 0 || !model.getDelayStackUpdateUtilLastStep()))
            {
                flags |= IModelDelta.CONTENT;
            }
            if (IDebugUIConstants.ID_REGISTER_VIEW.equals(view_id) ||
                    IDebugUIConstants.ID_EXPRESSION_VIEW.equals(view_id) ||
                    TCFModel.ID_EXPRESSION_HOVER.equals(view_id)) {
                if (p.getInput() == this) flags |= IModelDelta.CONTENT;
            }
            if (flags == 0) continue;
            p.addDelta(this, flags);
        }
    }

    private void postAllAndParentsChangedDelta() {
        postContentChangedDelta();
        TCFNode n = this;
        while (n instanceof TCFNodeExecContext) {
            TCFNodeExecContext e = (TCFNodeExecContext)n;
            if (e.delayed_children_list_delta) e.postContentChangedDelta();
            e.postStateChangedDelta();
            n = n.parent;
        }
    }

    public void postStateChangedDelta() {
        for (TCFModelProxy p : model.getModelProxies()) {
            if (IDebugUIConstants.ID_DEBUG_VIEW.equals(p.getPresentationContext().getId())) {
                p.addDelta(this, IModelDelta.STATE);
            }
        }
    }

    private void postModulesChangedDelta() {
        for (TCFModelProxy p : model.getModelProxies()) {
            if (IDebugUIConstants.ID_MODULE_VIEW.equals(p.getPresentationContext().getId())) {
                p.addDelta(this, IModelDelta.CONTENT);
            }
        }
    }

    private void postStackChangedDelta() {
        for (TCFModelProxy p : model.getModelProxies()) {
            if (IDebugUIConstants.ID_DEBUG_VIEW.equals(p.getPresentationContext().getId())) {
                p.addDelta(this, IModelDelta.CONTENT);
            }
        }
    }

    void onContextAdded(IRunControl.RunControlContext context) {
        model.setDebugViewSelection(this, TCFModel.SELECT_ADDED);
        children_exec.onContextAdded(context);
    }

    void onContextChanged(IRunControl.RunControlContext context) {
        assert !isDisposed();
        full_name.reset();
        run_context.reset(context);
        symbols_node.reset();
        memory_node.reset();
        signal_mask.reset();
        reset_capabilities.reset();
        if (state.isValid()) {
            TCFContextState s = state.getData();
            if (s == null || s.is_suspended) state.reset();
        }
        if (min_state.isValid()) {
            TCFContextState s = min_state.getData();
            if (s == null || s.is_suspended) min_state.reset();
        }
        children_stack.reset();
        children_stack.onSourceMappingChange();
        children_regs.reset();
        children_exec.onAncestorContextChanged();
        for (TCFNodeSymbol s : symbols.values()) s.onMemoryMapChanged();
        postAllChangedDelta();
        updateTerminal();
    }

    void onAncestorContextChanged() {
        full_name.reset();
    }

    void onContextAdded(IMemory.MemoryContext context) {
        children_exec.onContextAdded(context);
    }

    void onContextChanged(IMemory.MemoryContext context) {
        assert !isDisposed();
        clearLookupCaches();
        mem_context.reset(context);
        for (TCFNodeSymbol s : symbols.values()) s.onMemoryMapChanged();
        postAllChangedDelta();
    }

    void onContextRemoved() {
        assert !isDisposed();
        resumed_cnt++;
        resume_pending = false;
        resumed_by_action = false;
        dispose();
        postContextRemovedDelta();
        launch.removeContextActions(id);
    }

    void onExpressionAddedOrRemoved() {
        children_exps.cancel();
        children_stack.onExpressionAddedOrRemoved();
    }

    void onContainerSuspended(boolean func_call) {
        assert !isDisposed();
        if (run_context.isValid()) {
            IRunControl.RunControlContext ctx = run_context.getData();
            if (ctx != null && !ctx.hasState()) return;
        }
        onContextSuspended(null, null, null, func_call);
    }

    void onContainerResumed() {
        assert !isDisposed();
        if (run_context.isValid()) {
            IRunControl.RunControlContext ctx = run_context.getData();
            if (ctx != null && !ctx.hasState()) return;
        }
        onContextResumed();
    }

    void onContextSuspended(String pc, String reason, Map<String,Object> params, boolean func_call) {
        assert !isDisposed();
        if (pc != null) {
            TCFContextState s = new TCFContextState();
            s.is_suspended = true;
            s.suspend_pc = pc;
            s.suspend_reason = reason;
            s.suspend_params = params;
            state.reset(s);
        }
        else {
            state.reset();
        }
        min_state.reset();
        address.reset();
        signal_mask.reset();
        children_stack.onSuspended(func_call);
        children_exps.onSuspended(func_call);
        children_hover_exps.onSuspended(func_call);
        children_regs.onSuspended(func_call);
        if (!func_call) {
            children_log_exps.onSuspended();
        }
        for (TCFNodeSymbol s : symbols.values()) s.onExeStateChange();
        if (model.getActiveAction(id) == null) {
            boolean update_now = pc != null || resumed_by_action;
            resumed_cnt++;
            resume_pending = false;
            resumed_by_action = false;
            if (update_now) {
                children_stack.postAllChangedDelta();
                postAllAndParentsChangedDelta();
            }
            else {
                final int cnt = resumed_cnt;
                Protocol.invokeLater(500, new Runnable() {
                    public void run() {
                        if (cnt != resumed_cnt) return;
                        if (isDisposed()) return;
                        children_stack.postAllChangedDelta();
                        postAllAndParentsChangedDelta();
                    }
                });
            }
        }
    }

    void onContextResumed() {
        assert !isDisposed();
        state.reset();
        min_state.reset();
        if (!resume_pending) {
            final int cnt = ++resumed_cnt;
            resume_pending = true;
            resumed_by_action = model.getActiveAction(id) != null;
            if (resumed_by_action) postAllChangedDelta();
            Protocol.invokeLater(400, new Runnable() {
                public void run() {
                    if (cnt != resumed_cnt) return;
                    if (isDisposed()) return;
                    resume_pending = false;
                    postAllAndParentsChangedDelta();
                    model.onContextRunning();
                }
            });
        }
    }

    void onContextStateChanged() {
        assert !isDisposed();
        state.reset();
        min_state.reset();
        postStateChangedDelta();
    }

    void onContextActionDone() {
        if (state.getData() == null || state.getData().is_suspended) {
            resumed_cnt++;
            resume_pending = false;
            resumed_by_action = false;
        }
        postAllChangedDelta();
        children_stack.postAllChangedDelta();
    }

    void onContextException(String msg) {
    }

    void onOtherContextSuspended() {
        // Other context suspended in same memory space
        // Expressions with global variables should be invalidated
        children_exps.onMemoryChanged();
        children_hover_exps.onMemoryChanged();
        children_log_exps.onMemoryChanged();
    }

    void onMemoryChanged(Number[] addr, long[] size) {
        assert !isDisposed();
        children_stack.onMemoryChanged();
        children_exps.onMemoryChanged();
        children_hover_exps.onMemoryChanged();
        children_log_exps.onMemoryChanged();
        postContentChangedDelta();
    }

    void onMemoryMapChanged() {
        clearLookupCaches();
        memory_map.reset();
        children_modules.onMemoryMapChanged();
        children_stack.onMemoryMapChanged();
        children_exps.onMemoryMapChanged();
        children_hover_exps.onMemoryMapChanged();
        children_log_exps.onMemoryMapChanged();
        postContentChangedDelta();
        postModulesChangedDelta();
    }

    void onRegistersChanged() {
        children_stack.onRegistersChanged();
        children_regs.onRegistersChanged();
        postContentChangedDelta();
    }

    void onRegisterValueChanged() {
        if (state.isValid()) {
            TCFContextState s = state.getData();
            if (s == null || s.is_suspended) state.reset();
        }
        if (min_state.isValid()) {
            TCFContextState s = min_state.getData();
            if (s == null || s.is_suspended) min_state.reset();
        }
        address.reset();
        children_stack.onRegisterValueChanged();
        children_exps.onRegisterValueChanged();
        children_hover_exps.onRegisterValueChanged();
        children_log_exps.onRegisterValueChanged();
        postContentChangedDelta();
    }

    void onPreferencesChanged() {
        if (delayed_children_list_delta && !model.getDelayChildrenListUpdates() ||
                model.getAutoChildrenListUpdates()) postContentChangedDelta();
        children_stack.onPreferencesChanged();
        postStackChangedDelta();
    }

    void riseTraceLimit() {
        children_stack.riseTraceLimit();
        postStackChangedDelta();
    }

    boolean appendPointedObject(StyledStringBuffer bf, BigInteger addr, Runnable done) {
        TCFDataCache<TCFNodeExecContext> mem_node_cache = model.searchMemoryContext(this);
        if (mem_node_cache == null) return true;
        if (!mem_node_cache.validate(done)) return false;
        if (mem_node_cache.getData() == null) return true;
        TCFDataCache<TCFFunctionRef> func_info_cache = mem_node_cache.getData().getFuncInfo(addr);
        if (func_info_cache == null) return true;
        if (!func_info_cache.validate(done)) return false;
        TCFFunctionRef func_ref = func_info_cache.getData();
        if (func_ref != null && func_ref.symbol_id != null) {
            TCFDataCache<ISymbols.Symbol> sym_cache = model.getSymbolInfoCache(func_ref.symbol_id);
            if (!sym_cache.validate(done)) return false;
            ISymbols.Symbol sym_data = sym_cache.getData();
            if (sym_data != null && sym_data.getName() != null) {
                bf.append(", ");
                bf.append("At: ", SWT.BOLD);
                bf.append(sym_data.getName());
                if (sym_data.getSymbolClass() == ISymbols.SymbolClass.function) {
                    bf.append("()");
                }
                BigInteger func_addr = JSON.toBigInteger(sym_data.getAddress());
                if (func_addr != null) {
                    BigInteger addr_offs = addr.subtract(func_addr);
                    int cmp = addr_offs.compareTo(BigInteger.ZERO);
                    if (cmp > 0) {
                        bf.append(" + 0x" + addr_offs.toString(16));
                    }
                    else if (cmp < 0) {
                        bf.append(" - 0x" + addr_offs.abs().toString(16));
                    }
                }
            }
        }
        return true;
    }

    public boolean hasSuspendedChildren(ChildrenStateInfo info, Runnable done) {
        if (!children_exec.validate(done)) return false;
        Map<String,TCFNode> m = children_exec.getData();
        if (m == null || m.size() == 0) return true;
        for (TCFNode n : m.values()) {
            if (!(n instanceof TCFNodeExecContext)) continue;
            TCFNodeExecContext e = (TCFNodeExecContext)n;
            if (!e.run_context.validate(done)) return false;
            IRunControl.RunControlContext ctx = e.run_context.getData();
            if (ctx != null && ctx.hasState()) {
                TCFDataCache<TCFContextState> state_cache = e.getMinState();
                if (!state_cache.validate(done)) return false;
                TCFContextState state_data = state_cache.getData();
                if (state_data != null) {
                    if (!state_data.is_suspended) {
                        info.running = true;
                    }
                    else if (state_data.isNotActive()) {
                        info.not_active = true;
                    }
                    else {
                        info.suspended = true;
                        String r = model.getContextActionResult(e.id);
                        if (r == null) r = state_data.suspend_reason;
                        if (IRunControl.REASON_BREAKPOINT.equals(r)) info.breakpoint = true;
                    }
                }
            }
            else {
                if (!e.hasSuspendedChildren(info, done)) return false;
            }
            if (info.breakpoint && info.running) break;
        }
        return true;
    }

    @Override
    public int compareTo(TCFNode n) {
        if (n instanceof TCFNodeExecContext) {
            TCFNodeExecContext f = (TCFNodeExecContext)n;
            if (exe_seq_no < f.exe_seq_no) return -1;
            if (exe_seq_no > f.exe_seq_no) return +1;
            if (mem_seq_no < f.mem_seq_no) return -1;
            if (mem_seq_no > f.mem_seq_no) return +1;
        }
        return id.compareTo(n.id);
    }
}
