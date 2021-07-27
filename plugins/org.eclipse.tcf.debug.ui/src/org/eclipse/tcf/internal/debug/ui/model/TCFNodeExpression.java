/*******************************************************************************
 * Copyright (c) 2008-2021 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenCountUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementEditor;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IHasChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.debug.ui.ITCFExpression;
import org.eclipse.tcf.debug.ui.ITCFPrettyExpressionProvider;
import org.eclipse.tcf.internal.debug.actions.TCFAction;
import org.eclipse.tcf.internal.debug.model.TCFContextState;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.internal.debug.ui.ColorCache;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.services.IExpressions;
import org.eclipse.tcf.services.IMemory;
import org.eclipse.tcf.services.IMemory.MemoryError;
import org.eclipse.tcf.services.IRegisters;
import org.eclipse.tcf.services.ISymbols;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;

public class TCFNodeExpression extends TCFNode implements IElementEditor, ICastToType,
        IWatchInExpressions, IDetailsProvider, ITCFExpression {

    // TODO: User commands: Add Global Variables, Remove Global Variables
    // TODO: enable Change Value user command

    private final String script;
    private final IExpression platform_expression;
    private final String field_id;
    private final String reg_id;
    private final int index;
    private final boolean deref;
    private final TCFData<String> base_text;
    private final TCFData<IExpressions.Expression> var_expression;
    private final TCFData<IExpressions.Expression> rem_expression;
    private final TCFData<IExpressions.Value> value;
    private final TCFData<ISymbols.Symbol> type;
    private final TCFData<String> type_name;
    private final TCFData<StyledStringBuffer> string;
    private final TCFData<String> expression_text;
    private final TCFChildrenSubExpressions children;
    private final boolean is_empty;
    private int base_text_priority = 0;
    private int expr_text_priority = 0;
    private int sort_pos;
    private boolean enabled = true;
    private IExpressions.Value prev_value;
    private IExpressions.Value next_value;
    private byte[] parent_value;
    private String remote_expression_id;
    private Object update_generation;

    private static int expr_cnt;

    private final static int max_type_chain_length = 256;

    private final Runnable post_delta = new Runnable() {
        @Override
        public void run() {
            postAllChangedDelta();
        }
    };

    TCFNodeExpression(final TCFNode parent, String expression_script, IExpression platform_expression,
            final String field_id, final String var_id, final String reg_id,
            final int index, final boolean deref) {
        super(parent, var_id != null ? var_id : "Expr" + expr_cnt++);
        if (platform_expression == null) {
            this.script = expression_script;
            this.platform_expression = null;
        }
        else {
            this.script = platform_expression.getExpressionText();
            this.platform_expression = platform_expression;
        }
        this.field_id = field_id;
        this.reg_id = reg_id;
        this.index = index;
        this.deref = deref;
        is_empty = script == null && var_id == null && field_id == null && reg_id == null && index < 0 && !deref;
        var_expression = new TCFData<IExpressions.Expression>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                IExpressions exps = launch.getService(IExpressions.class);
                if (exps == null || var_id == null) {
                    set(null, null, null);
                    return true;
                }
                command = exps.getContext(var_id, new IExpressions.DoneGetContext() {
                    public void doneGetContext(IToken token, Exception error, IExpressions.Expression context) {
                        set(token, error, context);
                    }
                });
                return false;
            }
        };
        base_text = new TCFData<String>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                /* Compute expression script, not including type cast */
                base_text_priority = 0;
                parent_value = null;
                int expr_priority = 0;
                String expr_script = null;
                Throwable expr_error = null;
                if (is_empty) {
                    expr_script = null;
                }
                else if (script != null) {
                    expr_script = script;
                    if (script.matches("\\w*")) expr_priority = 2;
                }
                else if (var_id != null) {
                    if (!var_expression.validate(this)) return false;
                    String exp = null;
                    if (var_expression.getData() == null) {
                        expr_error = var_expression.getError();
                    }
                    else {
                        exp = var_expression.getData().getExpression();
                        if (exp == null) expr_error = new Exception("Missing 'Expression' property");
                        else if (exp.matches("\\w*")) expr_priority = 2;
                    }
                    expr_script = exp;
                }
                else if (reg_id != null) {
                    expr_script = "${" + reg_id + "}";
                    expr_priority = 2;
                }
                else {
                    TCFNode n = parent;
                    while (n instanceof TCFNodeArrayPartition) n = n.parent;
                    String cast = model.getCastToType(n.id);
                    if (cast == null && deref) {
                        TCFNodeExpression exp = (TCFNodeExpression)n;
                        if (!exp.value.validate(this)) return false;
                        IExpressions.Value v = exp.value.getData();
                        if (v != null && v.getTypeID() != null) {
                            parent_value = v.getValue();
                            if (parent_value != null) {
                                expr_script = "(${" + v.getTypeID() + "})0x" + TCFNumberFormat.toBigInteger(
                                        parent_value, v.isBigEndian(), false).toString(16);
                                expr_priority = 1;
                            }
                        }
                    }
                    if (expr_script == null) {
                        TCFDataCache<String> t = ((TCFNodeExpression)n).base_text;
                        if (!t.validate(this)) return false;
                        expr_script = t.getData();
                        if (expr_script == null) {
                            set(null, t.getError(), null);
                            return true;
                        }
                        expr_priority = ((TCFNodeExpression)n).base_text_priority;
                    }
                    if (cast != null) {
                        if (expr_priority < 1) expr_script = "(" + expr_script + ")";
                        expr_script = "(" + cast + ")" + expr_script;
                        expr_priority = 1;
                    }
                    if (field_id != null) {
                        if (expr_priority < 2) expr_script = "(" + expr_script + ")";
                        expr_script = expr_script + (deref ? "->" : ".") + "${" + field_id + "}";
                        expr_priority = 2;
                    }
                    else if (index >= 0) {
                        BigInteger lower_bound = getLowerBound(this);
                        if (lower_bound == null) return false;
                        if (expr_priority < 2) expr_script = "(" + expr_script + ")";
                        expr_script = expr_script + "[" + lower_bound.add(BigInteger.valueOf(index)) + "]";
                        expr_priority = 2;
                    }
                    else if (deref) {
                        if (expr_priority < 1) expr_script = "(" + expr_script + ")";
                        expr_script = "*" + expr_script;
                        expr_priority = 1;
                    }
                }
                base_text_priority = expr_priority;
                set(null, expr_error, expr_script);
                return true;
            }
        };
        expression_text = new TCFData<String>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                /* Compute human readable expression script,
                 * including type cast, and using variable names instead of IDs */
                expr_text_priority = 0;
                int expr_priority = 0;
                String expr_script = null;
                if (is_empty) {
                    expr_script = null;
                }
                else if (script != null) {
                    expr_script = script;
                    if (expr_script.matches("\\w*")) expr_priority = 2;
                }
                else if (var_id != null) {
                    if (!var_expression.validate(this)) return false;
                    IExpressions.Expression e = var_expression.getData();
                    if (e != null) {
                        TCFDataCache<ISymbols.Symbol> var = model.getSymbolInfoCache(e.getSymbolID());
                        if (var != null) {
                            if (!var.validate(this)) return false;
                            if (var.getData() != null) {
                                expr_script = var.getData().getName();
                                if (expr_script.matches("\\w*")) expr_priority = 2;
                            }
                        }
                    }
                }
                else if (reg_id != null) {
                    if (!model.createNode(reg_id, this)) return false;
                    if (isValid()) return true;
                    TCFNodeRegister reg_node = (TCFNodeRegister)model.getNode(reg_id);
                    for (;;) {
                        TCFDataCache<IRegisters.RegistersContext> ctx_cache = reg_node.getContext();
                        if (!ctx_cache.validate(this)) return false;
                        IRegisters.RegistersContext ctx_data = ctx_cache.getData();
                        if (ctx_data == null) {
                            set(null, ctx_cache.getError(), null);
                            return true;
                        }
                        expr_script = expr_script == null ? ctx_data.getName() : ctx_data.getName() + "." + expr_script;
                        if (!(reg_node.parent instanceof TCFNodeRegister)) break;
                        reg_node = (TCFNodeRegister)reg_node.parent;
                    }
                    expr_script = "$" + expr_script;
                    expr_priority = 2;
                }
                else {
                    TCFNode n = parent;
                    while (n instanceof TCFNodeArrayPartition) n = n.parent;
                    TCFDataCache<?> pending = null;
                    TCFDataCache<String> parent_text_cache = ((TCFNodeExpression)n).expression_text;
                    TCFDataCache<ISymbols.Symbol> field = model.getSymbolInfoCache(field_id);
                    if (!parent_text_cache.validate()) pending = parent_text_cache;
                    if (field != null && !field.validate()) pending = field;
                    if (!base_text.validate()) pending = base_text;
                    if (pending != null) {
                        pending.wait(this);
                        return false;
                    }
                    expr_script = parent_text_cache.getData();
                    expr_priority = ((TCFNodeExpression)n).expr_text_priority;
                    if (field != null) {
                        ISymbols.Symbol field_data = field.getData();
                        if (field_data != null) {
                            if (field_data.getName() != null) {
                                if (expr_priority < 2) expr_script = "(" + expr_script + ")";
                                expr_script = expr_script + (deref ? "->" : ".") + field_data.getName();
                                expr_priority = 2;
                            }
                            else if (field_data.getFlag(ISymbols.SYM_FLAG_INHERITANCE)) {
                                TCFDataCache<ISymbols.Symbol> type = model.getSymbolInfoCache(field_data.getTypeID());
                                if (type != null) {
                                    if (!type.validate(this)) return false;
                                    ISymbols.Symbol type_data = type.getData();
                                    if (type_data != null) {
                                        String type_name = type_data.getName();
                                        if (expr_priority < 1) expr_script = "(" + expr_script + ")";
                                        expr_script = "*(" + type_name + "*)" + (deref ? "" : "&") + expr_script;
                                        expr_priority = 1;
                                    }
                                }
                            }
                        }
                    }
                    else if (index >= 0) {
                        BigInteger lower_bound = getLowerBound(this);
                        if (lower_bound == null) return false;
                        if (expr_priority < 2) expr_script = "(" + expr_script + ")";
                        expr_script = expr_script + "[" + lower_bound.add(BigInteger.valueOf(index)) + "]";
                        expr_priority = 2;
                    }
                    else if (deref) {
                        if (expr_priority < 1) expr_script = "(" + expr_script + ")";
                        expr_script = "*" + expr_script;
                        expr_priority = 1;
                    }
                    if (expr_script == null && base_text.getData() != null) {
                        expr_script = base_text.getData();
                        expr_priority = base_text_priority;
                    }
                }
                if (expr_script != null) {
                    String cast = model.getCastToType(id);
                    if (cast != null) {
                        if (expr_priority < 1) expr_script = "(" + expr_script + ")";
                        expr_script = "(" + cast + ")(" + expr_script + ")";
                        expr_priority = 1;
                    }
                }
                expr_text_priority = expr_priority;
                set(null, null, expr_script);
                return true;
            }
        };
        rem_expression = new TCFData<IExpressions.Expression>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                IExpressions exps = launch.getService(IExpressions.class);
                if (exps == null) {
                    set(null, null, null);
                    return true;
                }
                String cast = model.getCastToType(id);
                if (var_id != null && cast == null) {
                    if (!var_expression.validate(this)) return false;
                    set(null, var_expression.getError(), var_expression.getData());
                    return true;
                }
                if (!base_text.validate(this)) return false;
                String e = base_text.getData();
                if (e == null) {
                    set(null, base_text.getError(), null);
                    return true;
                }
                if (cast != null) e = "(" + cast + ")(" + e + ")";
                TCFNode n = getRootExpression().parent;
                if (n instanceof TCFNodeStackFrame && ((TCFNodeStackFrame)n).isEmulated()) n = n.parent;
                command = exps.create(n.id, null, e, new IExpressions.DoneCreate() {
                    public void doneCreate(IToken token, Exception error, IExpressions.Expression context) {
                        disposeRemoteExpression();
                        if (context != null) remote_expression_id = context.getID();
                        if (!isDisposed()) set(token, error, context);
                        else disposeRemoteExpression();
                    }
                });
                return false;
            }
        };
        value = new TCFData<IExpressions.Value>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                Boolean b = usePrevValue(this);
                if (b == null) return false;
                if (b) {
                    set(null, null, prev_value);
                    return true;
                }
                if (!rem_expression.validate(this)) return false;
                final IExpressions.Expression exp = rem_expression.getData();
                if (exp == null) {
                    set(null, rem_expression.getError(), null);
                    return true;
                }
                final TCFDataCache<?> cache = this;
                IExpressions exps = launch.getService(IExpressions.class);
                command = exps.evaluate(exp.getID(), new IExpressions.DoneEvaluate() {
                    public void doneEvaluate(IToken token, Exception error, IExpressions.Value value) {
                        if (command != token) return;
                        command = null;
                        if (error != null) {
                            Boolean b = usePrevValue(cache);
                            if (b == null) return;
                            if (b) {
                                set(null, null, prev_value);
                                return;
                            }
                        }
                        set(null, error, value);
                    }
                });
                return false;
            }
        };
        type = new TCFData<ISymbols.Symbol>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                String type_id = null;
                if (model.getCastToType(id) == null && field_id != null) {
                    TCFDataCache<ISymbols.Symbol> sym_cache = model.getSymbolInfoCache(field_id);
                    if (sym_cache != null) {
                        if (!sym_cache.validate(this)) return false;
                        ISymbols.Symbol sym_data = sym_cache.getData();
                        if (sym_data != null) type_id = sym_data.getTypeID();
                    }
                }
                if (type_id == null) {
                    if (!value.validate(this)) return false;
                    IExpressions.Value val = value.getData();
                    if (val != null) type_id = val.getTypeID();
                }
                if (type_id == null) {
                    if (!rem_expression.validate(this)) return false;
                    IExpressions.Expression exp = rem_expression.getData();
                    if (exp != null) type_id = exp.getTypeID();
                }
                if (type_id == null) {
                    set(null, value.getError(), null);
                    return true;
                }
                TCFDataCache<ISymbols.Symbol> type_cache = model.getSymbolInfoCache(type_id);
                if (type_cache == null) {
                    set(null, null, null);
                    return true;
                }
                if (!type_cache.validate(this)) return false;
                set(null, type_cache.getError(), type_cache.getData());
                return true;
            }
        };
        string = new TCFData<StyledStringBuffer>(channel) {
            ISymbols.Symbol base_type_data;
            BigInteger addr;
            byte[] buf;
            int size;
            int offs;
            @Override
            @SuppressWarnings("incomplete-switch")
            protected boolean startDataRetrieval() {
                if (addr != null) return continueMemRead();
                if (!value.validate(this)) return false;
                if (!type.validate(this)) return false;
                IExpressions.Value value_data = value.getData();
                ISymbols.Symbol type_data = type.getData();
                if (value_data != null && value_data.getValue() != null && type_data != null) {
                    switch (type_data.getTypeClass()) {
                    case pointer:
                    case array:
                        TCFDataCache<ISymbols.Symbol> base_type_cache = model.getSymbolInfoCache(type_data.getBaseTypeID());
                        if (base_type_cache == null) break;
                        if (!base_type_cache.validate(this)) return false;
                        base_type_data = base_type_cache.getData();
                        if (base_type_data == null) break;
                        size = base_type_data.getSize();
                        if (size > 0x1000) break;
                        if (size <= 0) break;
                        switch (base_type_data.getTypeClass()) {
                        case integer:
                        case cardinal:
                            Boolean is_char = isCharType(base_type_data);
                            if (is_char == null) return false;
                            if (!is_char) break;
                            // c-string: read until character = 0
                            if (type_data.getTypeClass() == ISymbols.TypeClass.array) {
                                byte[] data = value_data.getValue();
                                StyledStringBuffer bf = new StyledStringBuffer();
                                bf.append(toASCIIString(data, 0, data.length, '"'), StyledStringBuffer.MONOSPACED);
                                set(null, null, bf);
                                return true;
                            }
                            // pointer, read c-string data from memory
                            size = 0; // read until 0
                            return startMemRead(value_data);
                        case composite:
                            if (type_data.getTypeClass() == ISymbols.TypeClass.array) break;
                            // pointer, read struct data from memory
                            return startMemRead(value_data);
                        }
                        break;
                    case integer:
                    case cardinal:
                        {
                            Boolean is_char = isCharType(type_data);
                            if (is_char == null) return false;
                            if (is_char) {
                                byte[] data = value_data.getValue();
                                if (data.length > 0) {
                                    if (data.length > 1) {
                                        BigInteger value_int = TCFNumberFormat.toBigInteger(
                                                data, value_data.isBigEndian(), false);
                                        if (value_int.compareTo(BigInteger.valueOf(0x80)) >= 0) break;
                                        data = new byte[1];
                                        data[0] = value_int.byteValue();
                                    }
                                    StyledStringBuffer bf = new StyledStringBuffer();
                                    bf.append(toASCIIString(data, 0, 1, '\''), StyledStringBuffer.MONOSPACED);
                                    set(null, null, bf);
                                    return true;
                                }
                            }
                        }
                        break;
                    case enumeration:
                        TCFDataCache<String[]> type_children_cache = model.getSymbolChildrenCache(type_data.getID());
                        if (!type_children_cache.validate(this)) return false;
                        String[] type_children_data = type_children_cache.getData();
                        if (type_children_data == null) break;
                        BigInteger value_int = TCFNumberFormat.toBigInteger(
                                value_data.getValue(), value_data.isBigEndian(), false);
                        for (String const_id : type_children_data) {
                            TCFDataCache<ISymbols.Symbol> const_cache = model.getSymbolInfoCache(const_id);
                            if (!const_cache.validate(this)) return false;
                            ISymbols.Symbol const_data = const_cache.getData();
                            if (const_data != null && const_data.getName() != null && const_data.getValue() != null) {
                                BigInteger const_int = TCFNumberFormat.toBigInteger(
                                        const_data.getValue(), const_data.isBigEndian(), false);
                                if (value_int.equals(const_int)) {
                                    StyledStringBuffer bf = new StyledStringBuffer();
                                    bf.append(const_data.getName());
                                    set(null, null, bf);
                                    return true;
                                }
                            }
                        }
                        break;
                    }
                }
                set(null, null, null);
                return true;
            }
            @Override
            public void reset() {
                super.reset();
                addr = null;
            }
            private Boolean isCharType(ISymbols.Symbol type) {
                for (int i = 0; i < max_type_chain_length; i++) {
                    if (type == null) return false;
                    if (type.getSize() != 1) return false;
                    if (type.getProperties().get(ISymbols.PROP_BINARY_SCALE) != null) return false;
                    if (type.getProperties().get(ISymbols.PROP_DECIMAL_SCALE) != null) return false;
                    if (type.getFlag(ISymbols.SYM_FLAG_ARTIFICIAL)) return false;
                    String id = type.getTypeID();
                    if (id == null || id.equals(type.getID())) break;
                    TCFDataCache<ISymbols.Symbol> type_cache = model.getSymbolInfoCache(id);
                    if (!type_cache.validate(this)) return null;
                    type = type_cache.getData();
                }
                return true;
            }
            private boolean startMemRead(IExpressions.Value value_data) {
                byte[] data = value_data.getValue();
                BigInteger a = TCFNumberFormat.toBigInteger(data, value_data.isBigEndian(), false);
                if (!a.equals(BigInteger.valueOf(0))) {
                    addr = a;
                    offs = 0;
                    return continueMemRead();
                }
                set(null, null, null);
                return true;
            }
            private boolean continueMemRead() {
                // indirect value, need to read it from memory
                TCFDataCache<TCFNodeExecContext> mem_node_cache = model.searchMemoryContext(parent);
                if (mem_node_cache == null) {
                    set(null, new Exception("Context does not provide memory access"), null);
                    return true;
                }
                if (!mem_node_cache.validate(this)) return false;
                if (mem_node_cache.getError() != null) {
                    set(null, mem_node_cache.getError(), null);
                    return true;
                }
                TCFNodeExecContext mem_node = mem_node_cache.getData();
                if (mem_node == null) {
                    set(null, new Exception("Context does not provide memory access"), null);
                    return true;
                }
                TCFDataCache<IMemory.MemoryContext> mem_ctx_cache = mem_node.getMemoryContext();
                if (!mem_ctx_cache.validate(this)) return false;
                if (mem_ctx_cache.getError() != null) {
                    set(null, mem_ctx_cache.getError(), null);
                    return true;
                }
                IMemory.MemoryContext mem_space_data = mem_ctx_cache.getData();
                if (mem_space_data == null) {
                    set(null, new Exception("Context does not provide memory access"), null);
                    return true;
                }
                if (size == 0) {
                    // c-string: read until 0
                    BigInteger get_addr = addr.add(BigInteger.valueOf(offs));
                    final int get_size = 16 - (get_addr.intValue() & 0xf);
                    if (buf == null) buf = new byte[256];
                    if (offs + get_size > buf.length) {
                        byte[] tmp = new byte[buf.length * 2];
                        System.arraycopy(buf, 0, tmp, 0, buf.length);
                        buf = tmp;
                    }
                    command = mem_space_data.get(get_addr, 1, buf, offs, get_size, 0, new IMemory.DoneMemory() {
                        public void doneMemory(IToken token, MemoryError error) {
                            if (command != token) return;
                            IMemory.ErrorOffset err_offs = null;
                            if (error instanceof IMemory.ErrorOffset) err_offs = (IMemory.ErrorOffset)error;
                            for (int i = 0; i < get_size; i++) {
                                MemoryError byte_error = null;
                                if (error != null && (err_offs == null || err_offs.getStatus(i) != IMemory.ErrorOffset.BYTE_VALID)) {
                                    byte_error = error;
                                    if (offs == 0) {
                                        set(command, byte_error, null);
                                        return;
                                    }
                                }
                                if (buf[offs] == 0 || offs >= 2048 || byte_error != null) {
                                    StyledStringBuffer bf = new StyledStringBuffer();
                                    bf.append(toASCIIString(buf, 0, offs, '"'), StyledStringBuffer.MONOSPACED);
                                    set(command, null, bf);
                                    return;
                                }
                                offs++;
                            }
                            command = null;
                            run();
                        }
                    });
                    return false;
                }
                if (offs == 0) {
                    buf = new byte[size];
                    command = mem_space_data.get(addr, 1, buf, 0, size, 0, new IMemory.DoneMemory() {
                        public void doneMemory(IToken token, MemoryError error) {
                            if (error != null) {
                                set(command, error, null);
                            }
                            else if (command == token) {
                                command = null;
                                offs++;
                                run();
                            }
                        }
                    });
                    return false;
                }
                StyledStringBuffer bf = new StyledStringBuffer();
                bf.append('{');
                if (!appendCompositeValueText(bf, 1, base_type_data, null, TCFNodeExpression.this, true,
                        buf, 0, size, base_type_data.isBigEndian(), this)) return false;
                bf.append('}');
                set(null, null, bf);
                return true;
            }
        };
        type_name = new TCFData<String>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                if (!type.validate(this)) return false;
                if (type.getData() != null) {
                    StringBuffer bf = new StringBuffer();
                    if (!getTypeName(bf, type, model.isShowQualifiedTypeNamesEnabled(), this)) return false;
                    set(null, null, bf.toString());
                    return true;
                }
                if (!value.validate(this)) return false;
                IExpressions.Value val = value.getData();
                if (val != null && val.getValue() != null) {
                    String s = getTypeName(val.getTypeClass(), val.getValue().length);
                    if (s != null) {
                        set(null, null, s);
                        return true;
                    }
                }
                if (!rem_expression.validate(this)) return false;
                IExpressions.Expression exp = rem_expression.getData();
                if (exp != null) {
                    String s = getTypeName(exp.getTypeClass(), exp.getSize());
                    if (s != null) {
                        set(null, null, s);
                        return true;
                    }
                }
                set(null, null, "N/A");
                return true;
            }
        };
        children = new TCFChildrenSubExpressions(this, 0, 0, 0);
    }

    void onPreferencesChanged() {
        type_name.reset();
        children.reset();
        postAllChangedDelta();
    }

    private void disposeRemoteExpression() {
        if (remote_expression_id != null && channel.getState() == IChannel.STATE_OPEN) {
            IExpressions exps = channel.getRemoteService(IExpressions.class);
            exps.dispose(remote_expression_id, new IExpressions.DoneDispose() {
                public void doneDispose(IToken token, Exception error) {
                    if (error == null) return;
                    if (channel.getState() != IChannel.STATE_OPEN) return;
                    Activator.log("Error disposing remote expression evaluator", error);
                }
            });
            remote_expression_id = null;
        }
    }

    @Override
    public void dispose() {
        for (ITCFPrettyExpressionProvider p : TCFPrettyExpressionProvider.getProviders()) p.dispose(this);
        disposeRemoteExpression();
        super.dispose();
    }

    private TCFNodeExpression getRootExpression() {
        TCFNode n = this;
        while (n.parent instanceof TCFNodeExpression || n.parent instanceof TCFNodeArrayPartition) n = n.parent;
        return (TCFNodeExpression)n;
    }

    private void postAllChangedDelta() {
        TCFNodeExpression n = getRootExpression();
        for (TCFModelProxy p : model.getModelProxies()) {
            String id = p.getPresentationContext().getId();
            if (IDebugUIConstants.ID_EXPRESSION_VIEW.equals(id) && n.script != null ||
                        TCFModel.ID_EXPRESSION_HOVER.equals(id) && n.script != null ||
                    IDebugUIConstants.ID_VARIABLE_VIEW.equals(id) && n.script == null) {
                p.addDelta(this, IModelDelta.STATE | IModelDelta.CONTENT);
            }
        }
    }

    private void resetBaseText() {
        if (parent_value != null && base_text.isValid()) {
            base_text.reset();
            rem_expression.cancel();
            for (ITCFPrettyExpressionProvider p : TCFPrettyExpressionProvider.getProviders()) p.cancel(this);
            string.cancel();
            value.cancel();
        }
    }

    public void update(Object generation) {
        if (update_generation == generation) return;
        prev_value = null;
        update_generation = generation;
        if (rem_expression.isValid() && rem_expression.getError() != null) rem_expression.reset();
        type.reset();
        type_name.reset();
        value.reset();
        string.reset();
        for (ITCFPrettyExpressionProvider p : TCFPrettyExpressionProvider.getProviders()) p.cancel(this);
        children.onSuspended(false);
        resetBaseText();
    }

    void onSuspended(boolean func_call) {
        update_generation = null;
        if (!func_call) {
            prev_value = next_value;
            type.reset();
            type_name.reset();
        }
        if (rem_expression.isValid() && rem_expression.getError() != null) rem_expression.reset();
        if (!func_call || value.isValid() && value.getError() != null) value.reset();
        if (!func_call || string.isValid() && string.getError() != null) string.reset();
        for (ITCFPrettyExpressionProvider p : TCFPrettyExpressionProvider.getProviders()) p.cancel(this);
        children.onSuspended(func_call);
        if (!func_call) resetBaseText();
        // No need to post delta: parent posted CONTENT
    }

    void onRegisterValueChanged() {
        prev_value = null;
        value.reset();
        type.reset();
        type_name.reset();
        string.reset();
        for (ITCFPrettyExpressionProvider p : TCFPrettyExpressionProvider.getProviders()) p.cancel(this);
        children.onRegisterValueChanged();
        resetBaseText();
        postAllChangedDelta();
    }

    void onMemoryChanged() {
        prev_value = null;
        value.reset();
        type.reset();
        type_name.reset();
        string.reset();
        for (ITCFPrettyExpressionProvider p : TCFPrettyExpressionProvider.getProviders()) p.cancel(this);
        children.onMemoryChanged();
        resetBaseText();
        if (parent instanceof TCFNodeExpression) return;
        if (parent instanceof TCFNodeArrayPartition) return;
        postAllChangedDelta();
    }

    void onMemoryMapChanged() {
        value.reset();
        type.reset();
        type_name.reset();
        string.reset();
        for (ITCFPrettyExpressionProvider p : TCFPrettyExpressionProvider.getProviders()) p.cancel(this);
        children.onMemoryMapChanged();
        resetBaseText();
        if (parent instanceof TCFNodeExpression) return;
        if (parent instanceof TCFNodeArrayPartition) return;
        postAllChangedDelta();
    }

    void onValueChanged() {
        prev_value = next_value;
        value.reset();
        type.reset();
        type_name.reset();
        string.reset();
        for (ITCFPrettyExpressionProvider p : TCFPrettyExpressionProvider.getProviders()) p.cancel(this);
        children.onValueChanged();
        resetBaseText();
        postAllChangedDelta();
    }

    public void onCastToTypeChanged() {
        rem_expression.cancel();
        value.cancel();
        type.cancel();
        type_name.cancel();
        string.cancel();
        for (ITCFPrettyExpressionProvider p : TCFPrettyExpressionProvider.getProviders()) p.cancel(this);
        expression_text.cancel();
        children.onCastToTypeChanged();
        resetBaseText();
        postAllChangedDelta();
    }

    public boolean isEmpty() {
        return is_empty;
    }

    public String getScript() {
        return script;
    }

    public IExpression getPlatformExpression() {
        return platform_expression;
    }

    String getFieldID() {
        return field_id;
    }

    String getRegisterID() {
        return reg_id;
    }

    int getIndex() {
        return index;
    }

    boolean isDeref() {
        return deref;
    }

    void setSortPosition(int sort_pos) {
        this.sort_pos = sort_pos;
    }

    void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        postAllChangedDelta();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get expression properties cache that represents a variable.
     * The cache is empty if the node does not represent a variable.
     * @return The expression properties cache.
     */
    public TCFDataCache<IExpressions.Expression> getVariable() {
        return var_expression;
    }

    /**
     * Get expression properties cache.
     * If the node represents a variable, return same data same as getVariable().
     * @return The expression properties cache.
     */
    public TCFDataCache<IExpressions.Expression> getExpression() {
        return rem_expression;
    }

    /**
     * Get expression value cache.
     * @return The expression value cache.
     */
    public TCFDataCache<IExpressions.Value> getValue() {
        return value;
    }

    /**
     * Get expression type cache.
     * @return The expression type cache.
     */
    public TCFDataCache<ISymbols.Symbol> getType() {
        return type;
    }

    /**
     * Get human readable expression script,
     * including type cast, and using variable names instead of IDs.
     */
    public TCFDataCache<String> getExpressionText() {
        return expression_text;
    }

    private Boolean usePrevValue(Runnable done) {
        // Check if view should show old value.
        // Old value is shown if context is running or
        // stack trace does not contain expression parent frame.
        // Return null if waiting for cache update.
        if (prev_value == null) return false;
        TCFNode p = getRootExpression().parent;
        if (p instanceof TCFNodeStackFrame) {
            TCFNodeExecContext exe = (TCFNodeExecContext)p.parent;
            TCFAction action = model.getActiveAction(exe.id);
            if (action != null && (action.showRunning() || model.getDelayStackUpdateUtilLastStep())) return true;
            TCFDataCache<TCFContextState> state_cache = exe.getState();
            if (!state_cache.validate(done)) return null;
            TCFContextState state = state_cache.getData();
            if (state == null || !state.is_suspended) return true;
            TCFChildrenStackTrace stack_trace_cache = exe.getStackTrace();
            if (!stack_trace_cache.validate(done)) return null;
            if (stack_trace_cache.getData().get(p.id) == null) return true;
        }
        else if (p instanceof TCFNodeExecContext) {
            TCFNodeExecContext exe = (TCFNodeExecContext)p;
            TCFAction action = model.getActiveAction(exe.id);
            if (action != null && (action.showRunning() || model.getDelayStackUpdateUtilLastStep())) return true;
            TCFDataCache<TCFContextState> state_cache = exe.getState();
            if (!state_cache.validate(done)) return null;
            TCFContextState state = state_cache.getData();
            if (state == null || !state.is_suspended) return true;
        }
        return false;
    }

    @Override
    void flushAllCaches() {
        prev_value = null;
        next_value = null;
        super.flushAllCaches();
    }

    @SuppressWarnings("incomplete-switch")
    private String getTypeName(ISymbols.TypeClass type_class, int size) {
        switch (type_class) {
        case integer:
            if (size == 0) return "<Void>";
            return "<Integer-" + (size * 8) + ">";
        case cardinal:
            if (size == 0) return "<Void>";
            return "<Unsigned-" + (size * 8) + ">";
        case real:
            if (size == 0) return null;
            return "<Float-" + (size * 8) + ">";
        case complex:
            if (size == 0) return null;
            return "<Complex-" + (size / 2 * 8) + ">";
        }
        return null;
    }

    @SuppressWarnings("incomplete-switch")
    private boolean getTypeName(StringBuffer bf, TCFDataCache<ISymbols.Symbol> type_cache, boolean qualified, Runnable done) {
        String name = null;
        boolean name_left = false;
        for (int i = 0; i < max_type_chain_length; i++) {
            String s = null;
            boolean get_base_left = false;
            boolean get_base_right = false;
            if (!type_cache.validate(done)) return false;
            ISymbols.Symbol type_symbol = type_cache.getData();
            if (type_symbol != null) {
                int flags = type_symbol.getFlags();
                s = type_symbol.getName();
                if (s != null) {
                    if (qualified && (flags & (ISymbols.SYM_FLAG_UNION_TYPE|ISymbols.SYM_FLAG_CLASS_TYPE|ISymbols.SYM_FLAG_STRUCT_TYPE|ISymbols.SYM_FLAG_ENUM_TYPE)) != 0) {
                        String prefix = getQualifiedTypeNamePrefix(type_symbol, done);
                        if (prefix == null) return false;
                        s = prefix + s;
                    }
                    if ((flags & ISymbols.SYM_FLAG_UNION_TYPE) != 0) s = "union " + s;
                    else if ((flags & ISymbols.SYM_FLAG_CLASS_TYPE) != 0) s = "class " + s;
                    else if ((flags & ISymbols.SYM_FLAG_INTERFACE_TYPE) != 0) s = "interface " + s;
                    else if ((flags & ISymbols.SYM_FLAG_STRUCT_TYPE) != 0) s = "struct " + s;
                    else if ((flags & ISymbols.SYM_FLAG_ENUM_TYPE) != 0) s = "enum " + s;
                }
                else if (!type_symbol.getID().equals(type_symbol.getTypeID())) {
                    // modified type without name, like "volatile int"
                    TCFDataCache<ISymbols.Symbol> base_type_cache = model.getSymbolInfoCache(type_symbol.getTypeID());
                    if (base_type_cache != null) {
                        StringBuffer sb = new StringBuffer();
                        if (!getTypeName(sb, base_type_cache, qualified, done)) return false;
                        s = sb.toString();
                    }
                }
                if (s == null) s = getTypeName(type_symbol.getTypeClass(), type_symbol.getSize());
                if (s == null) {
                    switch (type_symbol.getTypeClass()) {
                    case pointer:
                        s = "*";
                        if ((flags & ISymbols.SYM_FLAG_REFERENCE) != 0) s = "&";
                        get_base_left = true;
                        break;
                    case member_pointer:
                        {
                            String id = type_symbol.getContainerID();
                            if (id != null) {
                                TCFDataCache<ISymbols.Symbol> cls_cache = model.getSymbolInfoCache(id);
                                if (!cls_cache.validate(done)) return false;
                                ISymbols.Symbol cls_data = cls_cache.getData();
                                if (cls_data != null) {
                                    String cls_name = cls_data.getName();
                                    if (cls_name != null) {
                                        s = cls_name + "::*";
                                        if (qualified) {
                                            String prefix = getQualifiedTypeNamePrefix(cls_data, done);
                                            if (prefix == null) return false;
                                            s = prefix + s;
                                        }
                                    }
                                }
                            }
                            if (s == null) s = "::*";
                        }
                        get_base_left = true;
                        break;
                    case array:
                        s = "[" + type_symbol.getLength() + "]";
                        get_base_right = true;
                        break;
                    case composite:
                        s = "<Structure>";
                        break;
                    case function:
                        {
                            TCFDataCache<String[]> children_cache = model.getSymbolChildrenCache(type_symbol.getID());
                            if (!children_cache.validate(done)) return false;
                            String[] children = children_cache.getData();
                            if (children != null) {
                                StringBuffer args = new StringBuffer();
                                if (name != null) {
                                    args.append('(');
                                    args.append(name);
                                    args.append(')');
                                    name = null;
                                }
                                args.append('(');
                                for (String id : children) {
                                    if (id != children[0]) args.append(',');
                                    if (!getTypeName(args, model.getSymbolInfoCache(id), qualified, done)) return false;
                                }
                                args.append(')');
                                s = args.toString();
                                get_base_right = true;
                                break;
                            }
                        }
                        s = "<Function>";
                        break;
                    }
                }
                if (s != null) {
                    if ((flags & ISymbols.SYM_FLAG_VOLATILE_TYPE) != 0) s = "volatile " + s;
                    if ((flags & ISymbols.SYM_FLAG_CONST_TYPE) != 0) s = "const " + s;
                }
            }
            if (s == null) {
                name = "N/A";
                break;
            }
            if (name == null) {
                name = s;
            }
            else if (get_base_left) {
                name = s + name;
            }
            else if (get_base_right) {
                if (name_left) name = "(" + name + ")" + s;
                else name = name + s;
            }
            else {
                name = s + " " + name;
            }

            if (!get_base_left && !get_base_right) break;
            name_left = get_base_left;

            if (name.length() > 0x1000) {
                /* Must be invalid symbols data */
                name = "<Unknown>";
                break;
            }

            type_cache = model.getSymbolInfoCache(type_symbol.getBaseTypeID());
            if (type_cache == null) {
                name = "<Unknown> " + name;
                break;
            }
        }
        bf.append(name);
        return true;
    }

    private String getQualifiedTypeNamePrefix(ISymbols.Symbol type_symbol, Runnable done) {
        String prefix = "";
        String containerId = type_symbol.getContainerID();
        while (containerId != null) {
            TCFDataCache<ISymbols.Symbol> ns_cache = model.getSymbolInfoCache(containerId);
            if (!ns_cache.validate(done)) return null;
            containerId = null;
            ISymbols.Symbol ns_data = ns_cache.getData();
            if (ns_data != null && (ns_data.getSymbolClass() == ISymbols.SymbolClass.namespace
                    || ns_data.getSymbolClass() == ISymbols.SymbolClass.type)) {
                String ns_name = ns_data.getName();
                if (ns_name != null) {
                    prefix = ns_name + "::" + prefix;
                    containerId = ns_data.getContainerID();
                }
            }
        }
        return prefix;
    }

    private String toASCIIString(byte[] data, int offs, int size, char quote_char) {
        StringBuffer bf = new StringBuffer();
        bf.append(quote_char);
        for (int i = 0; i < size; i++) {
            int ch = data[offs + i] & 0xff;
            if (ch >= ' ' && ch < 0x7f) {
                bf.append((char)ch);
            }
            else {
                switch (ch) {
                case '\r': bf.append("\\r"); break;
                case '\n': bf.append("\\n"); break;
                case '\b': bf.append("\\b"); break;
                case '\t': bf.append("\\t"); break;
                case '\f': bf.append("\\f"); break;
                default:
                    bf.append('\\');
                    bf.append((char)('0' + ch / 64));
                    bf.append((char)('0' + ch / 8 % 8));
                    bf.append((char)('0' + ch % 8));
                }
            }
        }
        if (data.length <= offs + size || data[offs + size] == 0) bf.append(quote_char);
        else bf.append("...");
        return bf.toString();
    }

    @SuppressWarnings("incomplete-switch")
    private String toNumberString(int radix, ISymbols.TypeClass t, byte[] data, int offs, int size,
            boolean big_endian, Number bin_scale, Number dec_scale) {
        if (size <= 0) return "";
        if (radix != 16) {
            switch (t) {
            case array:
            case composite:
                return "";
            }
        }
        if (data == null) return "N/A";
        if (radix == 2) {
            StringBuffer bf = new StringBuffer();
            int i = size * 8;
            while (i > 0) {
                if (i % 4 == 0 && bf.length() > 0) bf.append(',');
                i--;
                int j = i / 8;
                if (big_endian) j = size - j - 1;
                if ((data[offs + j] & (1 << (i % 8))) != 0) {
                    bf.append('1');
                }
                else {
                    bf.append('0');
                }
            }
            return bf.toString();
        }
        if (radix == 10) {
            switch (t) {
            case integer:
            case cardinal:
                boolean sign_extend = t == ISymbols.TypeClass.integer;
                if (bin_scale != null) {
                    int s = bin_scale.intValue();
                    BigInteger n = TCFNumberFormat.toBigInteger(data, offs, size, big_endian, sign_extend);
                    if (s < 0) {
                        BigDecimal d = new BigDecimal(n);
                        while (s < 0) {
                            d = d.divide(BigDecimal.valueOf(2));
                            s++;
                        }
                        return d.toString();
                    }
                    return n.shiftLeft(s).toString() + '.';
                }
                if (dec_scale != null) {
                    BigDecimal d = new BigDecimal(TCFNumberFormat.toBigInteger(
                            data, offs, size, big_endian, sign_extend));
                    d = d.scaleByPowerOfTen(dec_scale.intValue());
                    return d.toString();
                }
                return TCFNumberFormat.toBigInteger(data, offs, size, big_endian, sign_extend).toString();
            case real:
                if (size > 16) return "";
                return TCFNumberFormat.toFPString(data, offs, size, big_endian);
            case complex:
                if (size > 32) return "";
                return TCFNumberFormat.toComplexFPString(data, offs, size, big_endian);
            }
        }
        String s = TCFNumberFormat.toBigInteger(data, offs, size, big_endian, false).toString(radix);
        switch (radix) {
        case 8:
            if (!s.startsWith("0")) s = "0" + s;
            break;
        case 16:
            if (s.length() < size * 2) {
                StringBuffer bf = new StringBuffer();
                while (bf.length() + s.length() < size * 2) bf.append('0');
                bf.append(s);
                s = bf.toString();
            }
            break;
        }
        assert s != null;
        return s;
    }

    private String toNumberString(int radix) {
        String s = null;
        IExpressions.Value val = value.getData();
        if (val != null) {
            byte[] data = val.getValue();
            if (data != null) {
                ISymbols.TypeClass t = val.getTypeClass();
                if (t == ISymbols.TypeClass.unknown && type.getData() != null) t = type.getData().getTypeClass();
                Number bin_scale = (Number)val.getProperties().get(IExpressions.VAL_BINARY_SCALE);
                Number dec_scale = (Number)val.getProperties().get(IExpressions.VAL_DECIMAL_SCALE);
                s = toNumberString(radix, t, data, 0, data.length, val.isBigEndian(), bin_scale, dec_scale);
            }
        }
        if (s == null) s = "N/A";
        return s;
    }

    private void setLabel(ILabelUpdate result, String name, int col, int radix) {
        String s = toNumberString(radix);
        if (name == null) {
            result.setLabel(s, col);
        }
        else {
            result.setLabel(name + " = " + s, col);
        }
    }

    private boolean isValueChanged(IExpressions.Value x, IExpressions.Value y) {
        if (x == null || y == null) return false;
        byte[] xb = x.getValue();
        byte[] yb = y.getValue();
        if (xb == null || yb == null) return false;
        if (xb.length != yb.length) return true;
        for (int i = 0; i < xb.length; i++) {
            if (xb[i] != yb[i]) return true;
        }
        return false;
    }

    private boolean isShowTypeNamesEnabled(IPresentationContext context) {
        Boolean attribute = (Boolean)context.getProperty(IDebugModelPresentation.DISPLAY_VARIABLE_TYPE_NAMES);
        return attribute != null && attribute;
    }

    private BigInteger getLowerBound(Runnable done) {
        TCFNode n = parent;
        while (n instanceof TCFNodeArrayPartition) n = n.parent;
        TCFDataCache<ISymbols.Symbol> t = ((TCFNodeExpression)n).getType();
        if (!t.validate(done)) return null;
        ISymbols.Symbol s = t.getData();
        if (s != null) {
            Number l = s.getLowerBound();
            if (l != null) return JSON.toBigInteger(l);
        }
        return BigInteger.valueOf(0);
    }

    @Override
    protected boolean getData(ILabelUpdate result, Runnable done) {
        if (is_empty) {
            result.setLabel("Add new expression", 0);
            result.setImageDescriptor(ImageCache.getImageDescriptor(ImageCache.IMG_NEW_EXPRESSION), 0);
        }
        else if (enabled || script == null) {
            TCFDataCache<ISymbols.Symbol> field = model.getSymbolInfoCache(field_id);
            TCFDataCache<?> pending = null;
            if (field != null && !field.validate()) pending = field;
            if (reg_id != null && !expression_text.validate(done)) pending = expression_text;
            if (!var_expression.validate()) pending = var_expression;
            if (!base_text.validate()) pending = base_text;
            if (!value.validate()) pending = value;
            if (!type.validate()) pending = type;
            if (pending != null) {
                if (script != null) {
                    if (!rem_expression.validate(done)) return false;
                    IExpressions.Expression exp_data = rem_expression.getData();
                    if (exp_data != null && exp_data.hasFuncCall()) {
                        /* Don't wait, it can take very long time */
                        pending.wait(post_delta);
                        result.setForeground(ColorCache.rgb_disabled, 0);
                        result.setLabel(script + " (Running)", 0);
                        return true;
                    }
                }
                pending.wait(done);
                return false;
            }
            String name = null;
            if (field != null) {
                ISymbols.Symbol field_data = field.getData();
                name = field_data.getName();
                if (name == null && field_data.getFlag(ISymbols.SYM_FLAG_INHERITANCE)) {
                    TCFDataCache<ISymbols.Symbol> type = model.getSymbolInfoCache(field_data.getTypeID());
                    if (type != null) {
                        if (!type.validate(done)) return false;
                        ISymbols.Symbol type_data = type.getData();
                        if (type_data != null) name = type_data.getName();
                    }
                }
            }
            else if (index >= 0) {
                BigInteger lower_bound = getLowerBound(done);
                if (lower_bound == null) return false;
                name = "[" + lower_bound.add(JSON.toBigInteger(index)) + "]";
            }
            else if (deref) {
                name = "*";
            }
            if (name == null && reg_id != null && expression_text.getData() != null) {
                name = expression_text.getData();
            }
            if (name == null && var_expression.getData() != null) {
                TCFDataCache<ISymbols.Symbol> var = model.getSymbolInfoCache(var_expression.getData().getSymbolID());
                if (var != null) {
                    if (!var.validate(done)) return false;
                    ISymbols.Symbol var_data = var.getData();
                    if (var_data != null) {
                        name = var_data.getName();
                        if (name == null && var_data.getFlag(ISymbols.SYM_FLAG_VARARG)) name = "<VarArg>";
                        if (name == null) name = "<" + var_data.getID() + ">";
                    }
                }
            }
            if (name == null && base_text.getData() != null) {
                name = base_text.getData();
            }
            if (name != null) {
                String cast = model.getCastToType(id);
                if (cast != null) name = "(" + cast + ")(" + name + ")";
            }
            Throwable error = base_text.getError();
            if (error == null) error = value.getError();
            String[] cols = result.getColumnIds();
            if (error != null) {
                if (cols == null || cols.length <= 1) {
                    result.setForeground(ColorCache.rgb_error, 0);
                    if (isShowTypeNamesEnabled(result.getPresentationContext())) {
                        if (!type_name.validate(done)) return false;
                        result.setLabel(name + ": N/A" + " , Type = " + type_name.getData(), 0);
                    }
                    else {
                        result.setLabel(name + ": N/A", 0);
                    }
                }
                else {
                    for (int i = 0; i < cols.length; i++) {
                        String c = cols[i];
                        if (c.equals(TCFColumnPresentationExpression.COL_NAME)) {
                            result.setLabel(name, i);
                        }
                        else if (c.equals(TCFColumnPresentationExpression.COL_TYPE)) {
                            if (!type_name.validate(done)) return false;
                            result.setLabel(type_name.getData(), i);
                        }
                        else {
                            result.setForeground(ColorCache.rgb_error, i);
                            result.setLabel("N/A", i);
                        }
                    }
                }
            }
            else {
                if (cols == null) {
                    StyledStringBuffer s = getPrettyExpression(done);
                    if (s == null) return false;
                    if (isShowTypeNamesEnabled(result.getPresentationContext())) {
                        if (!type_name.validate(done)) return false;
                        result.setLabel(name + " = " + s + " , Type = " + type_name.getData(), 0);
                    }
                    else {
                        result.setLabel(name + " = " + s, 0);
                    }
                }
                else {
                    for (int i = 0; i < cols.length; i++) {
                        String c = cols[i];
                        if (c.equals(TCFColumnPresentationExpression.COL_NAME)) {
                            result.setLabel(name, i);
                        }
                        else if (c.equals(TCFColumnPresentationExpression.COL_TYPE)) {
                            if (!type_name.validate(done)) return false;
                            result.setLabel(type_name.getData(), i);
                        }
                        else if (c.equals(TCFColumnPresentationExpression.COL_HEX_VALUE)) {
                            setLabel(result, null, i, 16);
                        }
                        else if (c.equals(TCFColumnPresentationExpression.COL_DEC_VALUE)) {
                            setLabel(result, null, i, 10);
                        }
                        else if (c.equals(TCFColumnPresentationExpression.COL_VALUE)) {
                            StyledStringBuffer s = getPrettyExpression(done);
                            if (s == null) return false;
                            result.setLabel(s.toString(), i);
                        }
                    }
                }
            }
            next_value = value.getData();
            if (isValueChanged(prev_value, next_value)) {
                if (cols != null) {
                    for (int i = 1; i < cols.length; i++) {
                        result.setBackground(ColorCache.rgb_highlight, i);
                    }
                }
                else {
                    result.setForeground(ColorCache.rgb_no_columns_color_change, 0);
                }
            }
            ISymbols.TypeClass type_class = ISymbols.TypeClass.unknown;
            ISymbols.Symbol type_symbol = type.getData();
            if (type_symbol != null) {
                type_class = type_symbol.getTypeClass();
            }
            switch (type_class) {
            case pointer:
                result.setImageDescriptor(ImageCache.getImageDescriptor(ImageCache.IMG_VARIABLE_POINTER), 0);
                break;
            case composite:
            case array:
                result.setImageDescriptor(ImageCache.getImageDescriptor(ImageCache.IMG_VARIABLE_AGGREGATE), 0);
                break;
            default:
                result.setImageDescriptor(ImageCache.getImageDescriptor(ImageCache.IMG_VARIABLE), 0);
            }
        }
        else {
            String[] cols = result.getColumnIds();
            if (cols == null || cols.length <= 1) {
                result.setForeground(ColorCache.rgb_disabled, 0);
                result.setLabel(script, 0);
            }
            else {
                for (int i = 0; i < cols.length; i++) {
                    String c = cols[i];
                    if (c.equals(TCFColumnPresentationExpression.COL_NAME)) {
                        result.setForeground(ColorCache.rgb_disabled, i);
                        result.setLabel(script, i);
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected void getFontData(ILabelUpdate update, String view_id) {
        if (is_empty) {
            update.setFontData(TCFModelFonts.getItalicFontData(view_id), 0);
        }
        else {
            FontData fn = TCFModelFonts.getNormalFontData(view_id);
            String[] cols = update.getColumnIds();
            if (cols == null || cols.length == 0) {
                update.setFontData(fn, 0);
            }
            else {
                String[] ids = update.getColumnIds();
                for (int i = 0; i < cols.length; i++) {
                    if (TCFColumnPresentationExpression.COL_HEX_VALUE.equals(ids[i]) ||
                            TCFColumnPresentationExpression.COL_DEC_VALUE.equals(ids[i]) ||
                            TCFColumnPresentationExpression.COL_VALUE.equals(ids[i])) {
                        update.setFontData(TCFModelFonts.getMonospacedFontData(view_id), i);
                    }
                    else {
                        update.setFontData(fn, i);
                    }
                }
            }
        }
    }

    private StyledStringBuffer getPrettyExpression(Runnable done) {
        for (ITCFPrettyExpressionProvider p : TCFPrettyExpressionProvider.getProviders()) {
            TCFDataCache<String> c = p.getText(this);
            if (c != null) {
                if (!c.validate(done)) return null;
                if (c.getError() == null && c.getData() != null) {
                    StyledStringBuffer bf = new StyledStringBuffer();
                    bf.append(c.getData(), StyledStringBuffer.MONOSPACED);
                    return bf;
                }
            }
        }
        if (!value.validate(done)) return null;
        if (!string.validate(done)) return null;
        StyledStringBuffer bf = new StyledStringBuffer();
        if (string.getData() != null) {
            bf.append(string.getData());
        }
        else {
            IExpressions.Value v = value.getData();
            if (v != null) {
                byte[] data = v.getValue();
                if (data != null) {
                    if (!appendValueText(bf, 1, v.getTypeID(), null, this,
                            data, 0, data.length, v.isBigEndian(), done)) return null;
                }
            }
        }
        return bf;
    }

    private boolean appendArrayValueText(StyledStringBuffer bf, int level,
            ISymbols.Symbol type, Set<String> enclosing_structs,
            byte[] data, int offs, int size, boolean big_endian, Number bit_stride, Runnable done) {
        assert offs + size <= data.length;
        int length = type.getLength();
        bf.append('[');
        if (length > 0) {
            for (int n = 0; n < length; n++) {
                if (n >= 100) {
                    bf.append("...");
                    break;
                }
                if (n > 0) bf.append(", ");
                if (bit_stride != null) {
                    int bits = bit_stride.intValue();
                    String base_type_id = type.getBaseTypeID();
                    ISymbols.Symbol base_type_data = null;
                    if (base_type_id != null) {
                        TCFDataCache<ISymbols.Symbol> type_cache = model.getSymbolInfoCache(base_type_id);
                        if (!type_cache.validate(done)) return false;
                        base_type_data = type_cache.getData();
                    }
                    int base_type_size = 0;
                    if (base_type_data != null) base_type_size = base_type_data.getSize();
                    if (base_type_size * 8 < bits) base_type_size = (bits + 7) / 8;
                    byte[] buf = new byte[base_type_size];
                    if (big_endian) {
                        for (int i = 0; i < bits; i++) {
                            int j = n * bits + i;
                            int k = j / 8;
                            int l = base_type_size * 8 - bits + i;
                            if (k < size && offs + k < data.length && (data[offs + k] & (1 << (7 - j % 8))) != 0) {
                                buf[l / 8] |= 1 << (7 - l % 8);
                            }
                        }
                        if (base_type_data != null && base_type_data.getTypeClass() == ISymbols.TypeClass.integer) {
                            /* Sign extension */
                            int sign_offs = base_type_size * 8 - bits;
                            boolean sign = (buf[sign_offs / 8] & (1 << (7 - sign_offs % 8))) != 0;
                            if (sign) {
                                for (int i = 0; i < sign_offs; i++) buf[i / 8] |= 1 << (7 - i % 8);
                            }
                        }
                    }
                    else {
                        for (int i = 0; i < bits; i++) {
                            int j = n * bits + i;
                            int k = j / 8;
                            if (k < size && offs + k < data.length && (data[offs + k] & (1 << (j % 8))) != 0) {
                                buf[i / 8] |= 1 << (i % 8);
                            }
                        }
                        if (base_type_data != null && base_type_data.getTypeClass() == ISymbols.TypeClass.integer) {
                            /* Sign extension */
                            int sign_offs = bits - 1;
                            boolean sign = (buf[sign_offs / 8] & (1 << (sign_offs % 8))) != 0;
                            if (sign) {
                                for (int i = bits; i < base_type_size * 8; i++) buf[i / 8] |= 1 << (i % 8);
                            }
                        }
                    }
                    if (!appendValueText(bf, level + 1, base_type_id, enclosing_structs, null,
                            buf, 0, buf.length, big_endian, done)) return false;
                }
                else {
                    int elem_size = size / length;
                    if (!appendValueText(bf, level + 1, type.getBaseTypeID(), enclosing_structs, null,
                            data, offs + n * elem_size, elem_size, big_endian, done)) return false;
                }
            }
        }
        bf.append(']');
        return true;
    }

    private boolean appendCompositeValueText(
            StyledStringBuffer bf, int level,
            ISymbols.Symbol type, Set<String> enclosing_structs,
            TCFNodeExpression data_node, boolean data_deref,
            byte[] data, int offs, int size, boolean big_endian, Runnable done) {
        TCFDataCache<String[]> children_cache = model.getSymbolChildrenCache(type.getID());
        if (children_cache == null) {
            bf.append("...");
            return true;
        }
        if (!children_cache.validate(done)) return false;
        String[] children_data = children_cache.getData();
        if (children_data == null) {
            bf.append("...");
            return true;
        }
        Set<String> structs = new HashSet<String>();
        if (enclosing_structs != null) structs.addAll(enclosing_structs);
        structs.add(type.getID());
        int cnt = 0;
        TCFDataCache<?> pending = null;
        for (String id : children_data) {
            TCFDataCache<ISymbols.Symbol> field_cache = model.getSymbolInfoCache(id);
            if (!field_cache.validate()) {
                pending = field_cache;
                continue;
            }
            ISymbols.Symbol field_props = field_cache.getData();
            if (field_props == null) continue;
            if (field_props.getSymbolClass() != ISymbols.SymbolClass.reference) continue;
            if (field_props.getFlag(ISymbols.SYM_FLAG_ARTIFICIAL)) continue;
            String name = field_props.getName();
            if (name == null && field_props.getFlag(ISymbols.SYM_FLAG_INHERITANCE)) {
                name = type.getName();
            }
            if (structs.contains(field_props.getTypeID())) {
                /*
                 * Avoid infinite loop caused by declaration like: class X { static X x; ... }
                 * See: https://bugs.eclipse.org/bugs/show_bug.cgi?id=506266
                 */
                if (cnt > 0) bf.append(", ");
                bf.append(name);
                bf.append('=');
                bf.append("...");
                cnt++;
                continue;
            }
            TCFNodeExpression field_node = null;
            if (data_node != null) {
                if (!data_node.children.validate(done)) return false;
                field_node = data_node.children.getField(id, data_deref);
            }
            if (field_props.getProperties().get(ISymbols.PROP_OFFSET) == null) {
                // Bitfield - use field_node to retrieve the value
                if (name == null || field_node == null) continue;
                if (cnt > 0) bf.append(", ");
                bf.append(name);
                bf.append('=');
                if (!field_node.value.validate(done)) return false;
                IExpressions.Value field_value = field_node.value.getData();
                byte[] field_data = field_value != null ? field_value.getValue() : null;
                if (field_data == null) {
                    bf.append('?');
                }
                else {
                    if (!field_node.appendValueText(bf, level + 1, field_props.getTypeID(), structs, field_node,
                            field_data, 0, field_data.length, field_value.isBigEndian(), done)) return false;
                }
                cnt++;
                continue;
            }
            int f_offs = field_props.getOffset();
            int f_size = field_props.getSize();
            if (cnt > 0) bf.append(", ");
            if (name != null) {
                bf.append(name);
                bf.append('=');
            }
            if (offs + f_offs + f_size > data.length) {
                bf.append('?');
            }
            else {
                boolean big_endian_field = big_endian;
                if ((field_props.getFlags() & ISymbols.SYM_FLAG_BIG_ENDIAN) != 0) big_endian_field = true;
                if ((field_props.getFlags() & ISymbols.SYM_FLAG_LITTLE_ENDIAN) != 0) big_endian_field = false;
                if (!appendValueText(bf, level + 1, field_props.getTypeID(), structs, field_node,
                        data, offs + f_offs, f_size, big_endian_field, done)) return false;
            }
            cnt++;
        }
        if (pending == null) return true;
        pending.wait(done);
        return false;
    }

    private boolean appendNumericValueText( StyledStringBuffer bf, ISymbols.TypeClass type_class, Runnable done) {
        if (!type.validate(done)) return false;
        IExpressions.Value v = value.getData();
        Number bin_scale = (Number)v.getProperties().get(IExpressions.VAL_BINARY_SCALE);
        Number dec_scale = (Number)v.getProperties().get(IExpressions.VAL_DECIMAL_SCALE);
        boolean big_endian = v.isBigEndian();
        byte[] data = v.getValue();
        bf.append("Hex: ", SWT.BOLD);
        bf.append(toNumberString(16, type_class, data, 0, data.length, big_endian, bin_scale, dec_scale), StyledStringBuffer.MONOSPACED);
        bf.append(", ");
        bf.append("Dec: ", SWT.BOLD);
        bf.append(toNumberString(10, type_class, data, 0, data.length, big_endian, bin_scale, dec_scale), StyledStringBuffer.MONOSPACED);
        bf.append(", ");
        bf.append("Oct: ", SWT.BOLD);
        bf.append(toNumberString(8, type_class, data, 0, data.length, big_endian, bin_scale, dec_scale), StyledStringBuffer.MONOSPACED);
        if (v.getTypeClass() == ISymbols.TypeClass.pointer) {
            TCFNode p = parent;
            while (p != null) {
                if (p instanceof TCFNodeExecContext) {
                    TCFNodeExecContext exe = (TCFNodeExecContext)p;
                    BigInteger addr = TCFNumberFormat.toBigInteger(data, 0, data.length, big_endian, false);
                    if (!exe.appendPointedObject(bf, addr, done)) return false;
                    break;
                }
                p = p.parent;
            }
        }
        bf.append('\n');
        bf.append("Bin: ", SWT.BOLD);
        bf.append(toNumberString(2), StyledStringBuffer.MONOSPACED);
        bf.append('\n');
        return true;
    }

    @SuppressWarnings("incomplete-switch")
    private boolean appendValueText(
            StyledStringBuffer bf, int level, String type_id, Set<String> enclosing_structs,
            TCFNodeExpression data_node, byte[] data, int offs, int size, boolean big_endian, Runnable done) {
        if (data == null) return true;
        ISymbols.Symbol type_data = null;
        if (type_id != null) {
            TCFDataCache<ISymbols.Symbol> type_cache = model.getSymbolInfoCache(type_id);
            if (!type_cache.validate(done)) return false;
            type_data = type_cache.getData();
        }
        if (type_data == null) {
            ISymbols.TypeClass type_class = ISymbols.TypeClass.unknown;
            if (!value.validate(done)) return false;
            if (value.getData() != null) type_class = value.getData().getTypeClass();
            if (level == 0) {
                assert offs == 0 && size == data.length && data_node == this;
                if (size > 0 && !appendNumericValueText(bf, type_class, done)) return false;
            }
            else if (size == 0) {
                bf.append("N/A", StyledStringBuffer.MONOSPACED);
            }
            else if (type_class == ISymbols.TypeClass.integer ||
                    type_class == ISymbols.TypeClass.real ||
                    type_class == ISymbols.TypeClass.complex) {
                bf.append(toNumberString(10, type_class, data, offs, size, big_endian, null, null), StyledStringBuffer.MONOSPACED);
            }
            else {
                bf.append("0x", StyledStringBuffer.MONOSPACED);
                bf.append(toNumberString(16, type_class, data, offs, size, big_endian, null, null), StyledStringBuffer.MONOSPACED);
            }
            return true;
        }
        if (level == 0) {
            StyledStringBuffer s = getPrettyExpression(done);
            if (s == null) return false;
            if (s.length() > 0) {
                bf.append(s);
                bf.append('\n');
            }
            else if (string.getError() != null) {
                bf.append("Cannot read pointed value: ", SWT.BOLD, null, ColorCache.rgb_error);
                bf.append(TCFModel.getErrorMessage(string.getError(), false), SWT.ITALIC, null, ColorCache.rgb_error);
                bf.append('\n');
            }
        }
        if (type_data.getSize() > 0) {
            if (level >= 32) {
                /* Stop potentially infinite recursion */
                bf.append("...");
                return true;
            }
            ISymbols.TypeClass type_class = type_data.getTypeClass();
            Number bin_scale = null;
            Number dec_scale = null;
            Number bit_stride = null;
            ISymbols.Symbol base_type = type_data;
            for (int i = 0; i < max_type_chain_length; i++) {
                if (base_type == null) break;
                if (type_class == ISymbols.TypeClass.array) {
                    if ((bit_stride = (Number)base_type.getProperties().get(ISymbols.PROP_BIT_STRIDE)) != null) break;
                }
                else {
                    if ((bin_scale = (Number)base_type.getProperties().get(ISymbols.PROP_BINARY_SCALE)) != null) break;
                    if ((dec_scale = (Number)base_type.getProperties().get(ISymbols.PROP_DECIMAL_SCALE)) != null) break;
                }
                String id = base_type.getTypeID();
                if (id == null || id.equals(base_type.getID())) break;
                TCFDataCache<ISymbols.Symbol> type_cache = model.getSymbolInfoCache(id);
                if (!type_cache.validate(done)) return false;
                base_type = type_cache.getData();
            }
            switch (type_class) {
            case enumeration:
            case integer:
            case cardinal:
            case real:
            case complex:
                if (level == 0) {
                    assert offs == 0 && size == data.length && data_node == this;
                    if (!appendNumericValueText(bf, type_class, done)) return false;
                }
                else if (type_data.getTypeClass() == ISymbols.TypeClass.cardinal) {
                    bf.append("0x", StyledStringBuffer.MONOSPACED);
                    bf.append(toNumberString(16, type_class, data, offs, size, big_endian, bin_scale, dec_scale), StyledStringBuffer.MONOSPACED);
                }
                else {
                    bf.append(toNumberString(10, type_class, data, offs, size, big_endian, bin_scale, dec_scale), StyledStringBuffer.MONOSPACED);
                }
                break;
            case pointer:
            case function:
            case member_pointer:
                if (level == 0) {
                    assert offs == 0 && size == data.length && data_node == this;
                    if (!appendNumericValueText(bf, type_class, done)) return false;
                }
                else {
                    bf.append("0x", StyledStringBuffer.MONOSPACED);
                    bf.append(toNumberString(16, type_class, data, offs, size, big_endian, null, null), StyledStringBuffer.MONOSPACED);
                }
                break;
            case array:
                if (level > 0) {
                    if (!appendArrayValueText(bf, level, type_data, enclosing_structs,
                            data, offs, size, big_endian, bit_stride, done)) return false;
                }
                break;
            case composite:
                if (level > 0) {
                    bf.append('{');
                    if (!appendCompositeValueText(bf, level, type_data, enclosing_structs, data_node, false,
                            data, offs, size, big_endian, done)) return false;
                    bf.append('}');
                }
                break;
            }
        }
        return true;
    }

    private String getRegisterName(String reg_id, Runnable done) {
        String name = reg_id;
        if (!model.createNode(reg_id, done)) return null;
        TCFNodeRegister reg_node = (TCFNodeRegister)model.getNode(reg_id);
        if (reg_node != null) {
            TCFDataCache<IRegisters.RegistersContext> reg_ctx_cache = reg_node.getContext();
            if (!reg_ctx_cache.validate(done)) return null;
            IRegisters.RegistersContext reg_ctx_data = reg_ctx_cache.getData();
            if (reg_ctx_data != null && reg_ctx_data.getName() != null) name = reg_ctx_data.getName();
        }
        return name;
    }

    public boolean getDetailText(StyledStringBuffer bf, Runnable done) {
        if (is_empty) return true;
        if (!enabled) {
            bf.append("Disabled");
            return true;
        }
        if (!rem_expression.validate(done)) return false;
        if (rem_expression.getError() == null) {
            if (!value.validate(done)) return false;
            if (!type_name.validate(done)) return false;
            IExpressions.Value v = value.getData();
            if (v != null) {
                String type_id = v.getTypeID();
                if (v.isImplicitPointer()) {
                    bf.append("Implicit pointer, value not available\n", SWT.BOLD);
                }
                else if (value.getError() == null) {
                    byte[] data = v.getValue();
                    if (data != null) {
                        boolean big_endian = v.isBigEndian();
                        if (!appendValueText(bf, 0, type_id, null, this,
                            data, 0, data.length, big_endian, done)) return false;
                    }
                }
                ISymbols.Symbol type_data = null;
                if (type_id != null) {
                    TCFDataCache<ISymbols.Symbol> type_cache = model.getSymbolInfoCache(type_id);
                    if (!type_cache.validate(done)) return false;
                    type_data = type_cache.getData();
                }
                Map<String,Object> value_props = v.getProperties();
                Number bin_scale = (Number)value_props.get(IExpressions.VAL_BINARY_SCALE);
                Number dec_scale = (Number)value_props.get(IExpressions.VAL_DECIMAL_SCALE);
                Number bit_stride = (Number)value_props.get(IExpressions.VAL_BIT_STRIDE);
                boolean fst = true;
                if (bin_scale != null) {
                    bf.append("Binary Scale: ", SWT.BOLD);
                    bf.append(bin_scale.toString(), StyledStringBuffer.MONOSPACED);
                    fst = false;
                }
                if (dec_scale != null) {
                    if (!fst) bf.append(", ");
                    bf.append("Decimal Scale: ", SWT.BOLD);
                    bf.append(dec_scale.toString(), StyledStringBuffer.MONOSPACED);
                    fst = false;
                }
                if (bit_stride != null) {
                    if (!fst) bf.append(", ");
                    bf.append("Stride: ", SWT.BOLD);
                    bf.append(bit_stride.toString(), StyledStringBuffer.MONOSPACED);
                    bf.append(bit_stride.longValue() == 1 ? " bit" : " bits");
                    fst = false;
                }
                if (v.getValue() != null) {
                    int sz = v.getValue().length;
                    if (!fst) bf.append(", ");
                    bf.append("Size: ", SWT.BOLD);
                    bf.append(Integer.toString(sz), StyledStringBuffer.MONOSPACED);
                    bf.append(sz == 1 ? " byte" : " bytes");
                    fst = false;
                }
                else if (type_data != null) {
                    int sz = type_data.getSize();
                    if (!fst) bf.append(", ");
                    bf.append("Size: ", SWT.BOLD);
                    bf.append(Integer.toString(sz), StyledStringBuffer.MONOSPACED);
                    bf.append(sz == 1 ? " byte" : " bytes");
                    fst = false;
                }
                else {
                    IExpressions.Expression exp = rem_expression.getData();
                    if (exp != null) {
                        int sz = exp.getSize();
                        if (!fst) bf.append(", ");
                        bf.append("Size: ", SWT.BOLD);
                        bf.append(Integer.toString(sz), StyledStringBuffer.MONOSPACED);
                        bf.append(sz == 1 ? " byte" : " bytes");
                        fst = false;
                    }
                }
                String tnm = type_name.getData();
                if (tnm != null) {
                    if (!fst) bf.append(", ");
                    bf.append("Type: ", SWT.BOLD);
                    bf.append(tnm);
                    fst = false;
                }
                if (!fst) bf.append('\n');
                @SuppressWarnings("unchecked")
                List<Map<String,Object>> pieces = (List<Map<String,Object>>)value_props.get(IExpressions.VAL_PIECES);
                if (pieces != null) {
                    bf.append("Pieces: ", SWT.BOLD);
                    int piece_cnt = 0;
                    for (Map<String,Object> props : pieces) {
                        int cnt = 0;
                        if (piece_cnt > 0) bf.append("; ");
                        String reg_id = (String)props.get("Register");
                        if (reg_id != null) {
                            String nm = getRegisterName(reg_id, done);
                            if (nm == null) return false;
                            bf.append("Register: ", SWT.BOLD);
                            bf.append(nm);
                            cnt++;
                        }
                        byte[] data = (byte[])props.get("Value");
                        if (data != null) {
                            bf.append("<Bin Value>", SWT.BOLD);
                        }
                        Number addr = (Number)props.get("Address");
                        if (addr != null) {
                            BigInteger i = JSON.toBigInteger(addr);
                            if (cnt > 0) bf.append(", ");
                            bf.append("Address: ", SWT.BOLD);
                            bf.append("0x", StyledStringBuffer.MONOSPACED);
                            bf.append(i.toString(16), StyledStringBuffer.MONOSPACED);
                            cnt++;
                        }
                        Number bit_offs = (Number)props.get("BitOffs");
                        if (bit_offs == null && props.get("BitSize") != null) bit_offs = BigInteger.ZERO;
                        if (bit_offs != null) {
                            BigInteger i = JSON.toBigInteger(bit_offs);
                            if (cnt > 0) bf.append(", ");
                            bf.append("Bit Offset: ", SWT.BOLD);
                            bf.append(i.toString(10), StyledStringBuffer.MONOSPACED);
                            cnt++;
                        }
                        Number bit_size = (Number)props.get("BitSize");
                        if (bit_size != null) {
                            BigInteger i = JSON.toBigInteger(bit_size);
                            if (cnt > 0) bf.append(", ");
                            bf.append("Bit Count: ", SWT.BOLD);
                            bf.append(i.toString(10), StyledStringBuffer.MONOSPACED);
                            cnt++;
                        }
                        Number byte_size = (Number)props.get("Size");
                        if (byte_size != null) {
                            BigInteger i = JSON.toBigInteger(byte_size);
                            if (cnt > 0) bf.append(", ");
                            bf.append("Byte Count: ", SWT.BOLD);
                            bf.append(i.toString(10), StyledStringBuffer.MONOSPACED);
                            cnt++;
                        }
                        piece_cnt++;
                    }
                    bf.append('\n');
                }
                else {
                    int cnt = 0;
                    String reg_id = v.getRegisterID();
                    if (reg_id != null) {
                        String nm = getRegisterName(reg_id, done);
                        if (nm == null) return false;
                        bf.append("Register: ", SWT.BOLD);
                        bf.append(nm);
                        cnt++;
                    }
                    TCFDataCache<ISymbols.Symbol> field_cache = model.getSymbolInfoCache(field_id);
                    if (field_cache != null) {
                        if (!field_cache.validate(done)) return false;
                        ISymbols.Symbol field_props = field_cache.getData();
                        if (field_props != null && field_props.getProperties().get(ISymbols.PROP_OFFSET) != null) {
                            if (cnt > 0) bf.append(", ");
                            bf.append("Offset: ", SWT.BOLD);
                            bf.append(Integer.toString(field_props.getOffset()), StyledStringBuffer.MONOSPACED);
                            cnt++;
                        }
                    }
                    Number addr = v.getAddress();
                    if (addr != null) {
                        BigInteger i = JSON.toBigInteger(addr);
                        if (cnt > 0) bf.append(", ");
                        bf.append("Address: ", SWT.BOLD);
                        bf.append("0x", StyledStringBuffer.MONOSPACED);
                        bf.append(i.toString(16), StyledStringBuffer.MONOSPACED);
                        cnt++;
                    }
                    if (cnt > 0) bf.append('\n');
                }
            }
            if (value.getError() != null) {
                bf.append(value.getError(), ColorCache.rgb_error);
            }
        }
        else {
            bf.append(rem_expression.getError(), ColorCache.rgb_error);
        }
        return true;
    }

    public String getValueText(boolean add_error_text, Runnable done) {
        if (!rem_expression.validate(done)) return null;
        if (!value.validate(done)) return null;
        StyledStringBuffer bf = new StyledStringBuffer();
        IExpressions.Value v = value.getData();
        if (v != null) {
            byte[] data = v.getValue();
            if (data != null) {
                boolean big_endian = v.isBigEndian();
                if (!appendValueText(bf, 1, v.getTypeID(), null, this,
                        data, 0, data.length, big_endian, done)) return null;
            }
        }
        if (add_error_text) {
            if (bf.length() == 0 && rem_expression.getError() != null) {
                bf.append(TCFModel.getErrorMessage(rem_expression.getError(), false));
            }
            if (bf.length() == 0 && value.getError() != null) {
                bf.append(TCFModel.getErrorMessage(value.getError(), false));
            }
        }
        return bf.toString();
    }

    @Override
    protected boolean getData(IChildrenCountUpdate result, Runnable done) {
        if (!is_empty && enabled) {
            if (!children.validate(done)) return false;
            result.setChildCount(children.size());
        }
        else {
            result.setChildCount(0);
        }
        return true;
    }

    @Override
    protected boolean getData(IChildrenUpdate result, Runnable done) {
        if (is_empty || !enabled) return true;
        return children.getData(result, done);
    }

    @Override
    protected boolean getData(IHasChildrenUpdate result, Runnable done) {
        if (!is_empty && enabled) {
            if (!children.validate(done)) return false;
            result.setHasChilren(children.size() > 0);
        }
        else {
            result.setHasChilren(false);
        }
        return true;
    }

    @Override
    public int compareTo(TCFNode n) {
        TCFNodeExpression e = (TCFNodeExpression)n;
        if (sort_pos < e.sort_pos) return -1;
        if (sort_pos > e.sort_pos) return +1;
        return 0;
    }

    public CellEditor getCellEditor(IPresentationContext context, String column_id, Object element, Composite parent) {
        assert element == this;
        if (TCFColumnPresentationExpression.COL_NAME.equals(column_id)) {
            return new TextCellEditor(parent);
        }
        if (TCFColumnPresentationExpression.COL_HEX_VALUE.equals(column_id)) {
            return new TextCellEditor(parent);
        }
        if (TCFColumnPresentationExpression.COL_DEC_VALUE.equals(column_id)) {
            return new TextCellEditor(parent);
        }
        if (TCFColumnPresentationExpression.COL_VALUE.equals(column_id)) {
            return new TextCellEditor(parent);
        }
        return null;
    }

    private static final ICellModifier cell_modifier = new ICellModifier() {
        private Object original_value;

        public boolean canModify(Object element, final String property) {
            final TCFNodeExpression node = (TCFNodeExpression)element;
            try {
                return new TCFTask<Boolean>(node.channel) {
                    public void run() {
                        if (TCFColumnPresentationExpression.COL_NAME.equals(property)) {
                            done(node.is_empty || node.script != null);
                            return;
                        }
                        TCFNode n = node.getRootExpression().parent;
                        if (n instanceof TCFNodeStackFrame) {
                            TCFNodeExecContext exe = (TCFNodeExecContext)n.parent;
                            TCFDataCache<TCFContextState> state_cache = exe.getState();
                            if (!state_cache.validate(this)) return;
                            TCFContextState state_data = state_cache.getData();
                            if (state_data == null || !state_data.is_suspended) {
                                done(Boolean.FALSE);
                                return;
                            }
                        }
                        if (!node.is_empty && node.enabled) {
                            if (!node.rem_expression.validate(this)) return;
                            IExpressions.Expression exp = node.rem_expression.getData();
                            if (exp != null && exp.canAssign()) {
                                if (!node.value.validate(this)) return;
                                if (!node.type.validate(this)) return;
                                if (TCFColumnPresentationExpression.COL_HEX_VALUE.equals(property)) {
                                    done(TCFNumberFormat.isValidHexNumber(node.toNumberString(16)) == null);
                                    return;
                                }
                                if (TCFColumnPresentationExpression.COL_DEC_VALUE.equals(property)) {
                                    done(TCFNumberFormat.isValidDecNumber(true, node.toNumberString(10)) == null);
                                    return;
                                }
                                if (TCFColumnPresentationExpression.COL_VALUE.equals(property)) {
                                    IExpressions.Value eval = node.value.getData();
                                    StyledStringBuffer bf = node.getPrettyExpression(this);
                                    if (bf == null) return;
                                    String s = bf.toString();
                                    done(s.startsWith("0x") ||
                                            s.startsWith("'") && s.endsWith("'") ||
                                            eval != null && eval.getTypeClass() == ISymbols.TypeClass.enumeration ||
                                            TCFNumberFormat.isValidDecNumber(true, s) == null);
                                    return;
                                }
                            }
                        }
                        done(Boolean.FALSE);
                    }
                }.get(1, TimeUnit.SECONDS);
            }
            catch (Exception e) {
                return false;
            }
        }

        public Object getValue(Object element, final String property) {
            original_value = null;
            final TCFNodeExpression node = (TCFNodeExpression)element;
            try {
                return original_value = new TCFTask<String>() {
                    public void run() {
                        if (node.is_empty) {
                            done("");
                            return;
                        }
                        if (TCFColumnPresentationExpression.COL_NAME.equals(property)) {
                            done(node.script);
                            return;
                        }
                        if (!node.value.validate(this)) return;
                        if (node.value.getData() != null) {
                            if (TCFColumnPresentationExpression.COL_HEX_VALUE.equals(property)) {
                                done(node.toNumberString(16));
                                return;
                            }
                            if (TCFColumnPresentationExpression.COL_DEC_VALUE.equals(property)) {
                                done(node.toNumberString(10));
                                return;
                            }
                            if (TCFColumnPresentationExpression.COL_VALUE.equals(property)) {
                                StyledStringBuffer bf = node.getPrettyExpression(this);
                                if (bf == null) return;
                                done(bf.toString());
                                return;
                            }
                        }
                        done(null);
                    }
                }.get(1, TimeUnit.SECONDS);
            }
            catch (Exception e) {
                return null;
            }
        }

        public void modify(Object element, final String property, final Object value) {
            if (value == null) return;
            if (original_value != null && original_value.equals(value)) return;
            final TCFNodeExpression node = (TCFNodeExpression)element;
            try {
                new TCFTask<Boolean>() {
                    @SuppressWarnings("incomplete-switch")
                    public void run() {
                        try {
                            if (TCFColumnPresentationExpression.COL_NAME.equals(property)) {
                                if (value instanceof String) {
                                    String s = ((String)value).trim();
                                    IExpressionManager m = node.model.getExpressionManager();
                                    if (node.is_empty) {
                                        if (s.length() > 0) m.addExpression(m.newWatchExpression(s));
                                    }
                                    else if (node.platform_expression != null && !s.equals(node.script)) {
                                        m.removeExpression(node.platform_expression);
                                        if (s.length() > 0) m.addExpression(m.newWatchExpression(s));
                                    }
                                }
                                done(Boolean.TRUE);
                                return;
                            }
                            if (!node.rem_expression.validate(this)) return;
                            IExpressions.Expression exp = node.rem_expression.getData();
                            if (exp != null && exp.canAssign()) {
                                byte[] bf = null;
                                int size = exp.getSize();
                                boolean is_enum = false;
                                boolean is_float = false;
                                boolean big_endian = false;
                                boolean signed = false;
                                if (!node.type.validate(this)) return;
                                if (!node.value.validate(this)) return;
                                IExpressions.Value eval = node.value.getData();
                                Number bin_scale = null;
                                Number dec_scale = null;
                                if (eval != null) {
                                    switch(eval.getTypeClass()) {
                                    case enumeration:
                                        is_enum = true;
                                        break;
                                    case real:
                                        is_float = true;
                                        signed = true;
                                        break;
                                    case integer:
                                        signed = true;
                                        break;
                                    }
                                    bin_scale = (Number)eval.getProperties().get(IExpressions.VAL_BINARY_SCALE);
                                    dec_scale = (Number)eval.getProperties().get(IExpressions.VAL_DECIMAL_SCALE);
                                    big_endian = eval.isBigEndian();
                                    size = eval.getValue().length;
                                }
                                String error = null;
                                String input = ((String)value).trim();
                                if (TCFColumnPresentationExpression.COL_HEX_VALUE.equals(property)) {
                                    if (input.startsWith("0x")) input = input.substring(2);
                                    error = TCFNumberFormat.isValidHexNumber(input);
                                    if (error == null) bf = TCFNumberFormat.toByteArray(input, 16, false, size, signed, big_endian);
                                }
                                else if (TCFColumnPresentationExpression.COL_DEC_VALUE.equals(property)) {
                                    if (bin_scale != null) {
                                        int n = bin_scale.intValue();
                                        BigDecimal d = new BigDecimal(input);
                                        while (n < 0) {
                                            d = d.multiply(BigDecimal.valueOf(2));
                                            n++;
                                        }
                                        bf = TCFNumberFormat.toByteArray(d.toBigInteger().toString(), 10, false, size, signed, big_endian);
                                    }
                                    else if (dec_scale != null) {
                                        BigDecimal d = new BigDecimal(input).scaleByPowerOfTen(-dec_scale.intValue());
                                        bf = TCFNumberFormat.toByteArray(d.toBigInteger().toString(), 10, false, size, signed, big_endian);
                                    }
                                    else {
                                        error = TCFNumberFormat.isValidDecNumber(is_float, input);
                                        if (error == null) bf = TCFNumberFormat.toByteArray(input, 10, is_float, size, signed, big_endian);
                                    }
                                }
                                else if (TCFColumnPresentationExpression.COL_VALUE.equals(property)) {
                                    if (input.startsWith("0x")) {
                                        String s = input.substring(2);
                                        error = TCFNumberFormat.isValidHexNumber(s);
                                        if (error == null) bf = TCFNumberFormat.toByteArray(s, 16, false, size, signed, big_endian);
                                    }
                                    else if (input.startsWith("'") && input.endsWith("'")) {
                                        String s = input.substring(1, input.length() - 1);
                                        int l = s.length();
                                        int i = 0;
                                        int n = 0;
                                        if (i < l) {
                                            char ch = s.charAt(i++);
                                            if (ch == '\\' && i < l) {
                                                ch = s.charAt(i++);
                                                switch (ch) {
                                                case 'r': n = '\r'; break;
                                                case 'n': n = '\n'; break;
                                                case 'b': n = '\b'; break;
                                                case 't': n = '\t'; break;
                                                case 'f': n = '\f'; break;
                                                default:
                                                    while (ch >= '0' && ch <= '7') {
                                                        n = n * 8 + (ch - '0');
                                                        if (i >= l) break;
                                                        ch = s.charAt(i++);
                                                    }
                                                }
                                            }
                                            else {
                                                n = ch;
                                            }
                                        }
                                        if (i < l) throw new Exception("Invalid character literal: " + value);
                                        bf = TCFNumberFormat.toByteArray(Integer.toString(n), 10, false, size, signed, big_endian);
                                    }
                                    else if (bin_scale != null) {
                                        int n = bin_scale.intValue();
                                        BigDecimal d = new BigDecimal(input);
                                        while (n < 0) {
                                            d = d.multiply(BigDecimal.valueOf(2));
                                            n++;
                                        }
                                        bf = TCFNumberFormat.toByteArray(d.toBigInteger().toString(), 10, false, size, signed, big_endian);
                                    }
                                    else if (dec_scale != null) {
                                        BigDecimal d = new BigDecimal(input).scaleByPowerOfTen(-dec_scale.intValue());
                                        bf = TCFNumberFormat.toByteArray(d.toBigInteger().toString(), 10, false, size, signed, big_endian);
                                    }
                                    else {
                                        error = TCFNumberFormat.isValidDecNumber(is_float, input);
                                        if (error == null) {
                                            bf = TCFNumberFormat.toByteArray(input, 10, is_float, size, signed, big_endian);
                                        }
                                        else if (is_enum) {
                                            TCFModel model = node.model;
                                            ISymbols.Symbol type_data = node.type.getData();
                                            TCFDataCache<String[]> type_children_cache = model.getSymbolChildrenCache(type_data.getID());
                                            if (!type_children_cache.validate(this)) return;
                                            String[] type_children_data = type_children_cache.getData();
                                            if (type_children_data != null) {
                                                for (String const_id : type_children_data) {
                                                    TCFDataCache<ISymbols.Symbol> const_cache = model.getSymbolInfoCache(const_id);
                                                    if (!const_cache.validate(this)) return;
                                                    ISymbols.Symbol const_data = const_cache.getData();
                                                    if (const_data != null && input.equals(const_data.getName()) && const_data.getValue() != null) {
                                                        bf = const_data.getValue();
                                                        error = null;
                                                        break;
                                                    }
                                                }
                                            }
                                            if (error != null && (input.length() == 0 || input.charAt(0) < '0' || input.charAt(0) > '9')) {
                                                error = "Expected enumerator name or integer number";
                                            }
                                        }
                                    }
                                }
                                if (error != null) throw new Exception("Invalid value: " + value, new Exception(error));
                                if (bf != null) {
                                    IExpressions exps = node.launch.getService(IExpressions.class);
                                    exps.assign(exp.getID(), bf, new IExpressions.DoneAssign() {
                                        public void doneAssign(IToken token, Exception error) {
                                            node.getRootExpression().onValueChanged();
                                            if (error != null) {
                                                node.model.showMessageBox("Cannot modify element value", error);
                                                done(Boolean.FALSE);
                                            }
                                            else {
                                                done(Boolean.TRUE);
                                            }
                                        }
                                    });
                                    return;
                                }
                            }
                            done(Boolean.FALSE);
                        }
                        catch (Throwable x) {
                            node.model.showMessageBox("Cannot modify element value", x);
                            done(Boolean.FALSE);
                        }
                    }
                }.get(10, TimeUnit.SECONDS);
            }
            catch (TimeoutException e) {
                node.model.showMessageBox("Timeout modifying element value", new Exception("No response for 10 seconds."));
            }
            catch (Exception e) {
                node.model.showMessageBox("Error modifying element value", e);
            }
        }
    };

    public ICellModifier getCellModifier(IPresentationContext context, Object element) {
        assert element == this;
        return cell_modifier;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class adapter) {
        if (platform_expression != null) {
            if (adapter == IExpression.class) {
                return platform_expression;
            }
            if (adapter == IWatchExpression.class) {
                if (platform_expression instanceof IWatchExpression) {
                    return platform_expression;
                }
            }
        }
        return super.getAdapter(adapter);
    }
}
