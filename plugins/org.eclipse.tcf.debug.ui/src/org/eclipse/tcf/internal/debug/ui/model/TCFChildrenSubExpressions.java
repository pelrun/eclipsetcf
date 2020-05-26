/*******************************************************************************
 * Copyright (c) 2008-22020 Wind River Systems, Inc. and others.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.tcf.debug.ui.ITCFPrettyExpressionProvider;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.services.IExpressions;
import org.eclipse.tcf.services.IExpressions.Value;
import org.eclipse.tcf.services.ISymbols;
import org.eclipse.tcf.services.ISymbols.Symbol;
import org.eclipse.tcf.util.TCFDataCache;

public class TCFChildrenSubExpressions extends TCFChildren {

    private final int par_level;
    private final int par_offs;
    private final int par_size;

    TCFChildrenSubExpressions(TCFNode node, int par_level, int par_offs, int par_size) {
        super(node, 128);
        this.par_level = par_level;
        this.par_offs = par_offs;
        this.par_size = par_size;
    }

    void onSuspended(boolean func_call) {
        reset();
        for (TCFNode n : getNodes()) {
            if (n instanceof TCFNodeExpression) ((TCFNodeExpression)n).onSuspended(func_call);
            if (n instanceof TCFNodeArrayPartition) ((TCFNodeArrayPartition)n).onSuspended(func_call);
        }
    }

    void onValueChanged() {
        reset();
        for (TCFNode n : getNodes()) {
            if (n instanceof TCFNodeExpression) ((TCFNodeExpression)n).onValueChanged();
            if (n instanceof TCFNodeArrayPartition) ((TCFNodeArrayPartition)n).onValueChanged();
        }
    }

    void onRegisterValueChanged() {
        reset();
        for (TCFNode n : getNodes()) {
            if (n instanceof TCFNodeExpression) ((TCFNodeExpression)n).onRegisterValueChanged();
            if (n instanceof TCFNodeArrayPartition) ((TCFNodeArrayPartition)n).onRegisterValueChanged();
        }
    }

    void onMemoryChanged() {
        reset();
        for (TCFNode n : getNodes()) {
            if (n instanceof TCFNodeExpression) ((TCFNodeExpression)n).onMemoryChanged();
            if (n instanceof TCFNodeArrayPartition) ((TCFNodeArrayPartition)n).onMemoryChanged();
        }
    }

    void onMemoryMapChanged() {
        reset();
        for (TCFNode n : getNodes()) {
            if (n instanceof TCFNodeExpression) ((TCFNodeExpression)n).onMemoryMapChanged();
            if (n instanceof TCFNodeArrayPartition) ((TCFNodeArrayPartition)n).onMemoryMapChanged();
        }
    }

    void onCastToTypeChanged() {
        cancel();
        TCFNode a[] = getNodes().toArray(new TCFNode[getNodes().size()]);
        for (int i = 0; i < a.length; i++) a[i].dispose();
    }

    TCFNodeExpression getField(String field_id, boolean deref) {
        assert field_id != null;
        for (TCFNode n : getNodes()) {
            if (n instanceof TCFNodeExpression) {
                TCFNodeExpression e = (TCFNodeExpression)n;
                if (field_id.equals(e.getFieldID()) && e.isDeref() == deref) return e;
            }
        }
        if (isValid()) return null;
        TCFNodeExpression e = new TCFNodeExpression(node, null, null, field_id, null, null, -1, deref);
        add(e);
        return e;
    }

    private boolean findFields(ISymbols.Symbol type, Map<String,TCFNode> map, boolean deref) {
        TCFDataCache<String[]> children_cache = node.model.getSymbolChildrenCache(type.getID());
        if (children_cache == null) return true;
        if (!children_cache.validate(this)) return false;
        String[] children = children_cache.getData();
        if (children == null) return true;
        TCFDataCache<?> pending = null;
        for (String id : children) {
            TCFDataCache<ISymbols.Symbol> sym_cache = node.model.getSymbolInfoCache(id);
            if (!sym_cache.validate()) {
                pending = sym_cache;
            }
            else {
                ISymbols.Symbol sym_data = sym_cache.getData();
                if (sym_data == null) continue;
                switch (sym_data.getSymbolClass()) {
                case reference:
                    if (sym_data.getFlag(ISymbols.SYM_FLAG_ARTIFICIAL)) continue;
                    if (sym_data.getName() == null && !sym_data.getFlag(ISymbols.SYM_FLAG_INHERITANCE)) {
                        if (!findFields(sym_data, map, deref)) return false;
                    }
                    else {
                        TCFNodeExpression n = getField(id, deref);
                        n.setSortPosition(map.size());
                        map.put(n.id, n);
                    }
                    break;
                case variant:
                    if (!findFields(sym_data, map, deref)) return false;
                    break;
                case variant_part:
                    if (node.model.isFilterVariantsByDiscriminant()) {
                        // find discriminant by offset
                        String discr_id = null;
                        int offset = sym_data.getOffset();
                        for (String id2 : children) {
                            if (id.equals(id2)) continue;
                            TCFDataCache<ISymbols.Symbol> discr_sym_cache = node.model.getSymbolInfoCache(id2);
                            if (!discr_sym_cache.validate()) {
                                pending = discr_sym_cache;
                                continue;
                            }
                            Symbol discr_sym_data = discr_sym_cache.getData();
                            if (discr_sym_data == null) continue;
                            if (discr_sym_data.getSymbolClass() == ISymbols.SymbolClass.variant_part) continue;
                            if (discr_sym_data.getOffset() == offset) {
                                discr_id = id2;
                                break;
                            }
                        }
                        if (discr_id == null) continue;
                        // filter variants by discriminant value
                        if (!filterVariants(sym_data, map, discr_id, deref)) return false;
                    }
                    else {
                        if (!findFields(sym_data, map, deref)) return false;
                    }
                    break;
                default:
                    break;
                }
            }
        }
        if (pending == null) return true;
        pending.wait(this);
        return false;
    }

    private boolean filterVariants(ISymbols.Symbol type, Map<String,TCFNode> map, String discr_id, boolean deref) {
        TCFNodeExpression discr_expr = getField(discr_id, deref);
        TCFDataCache<Value> discr_value_cache = discr_expr.getValue();
        if (!discr_value_cache.validate(this)) return false;
        Value discr_value = discr_value_cache.getData();
        if (discr_value == null) return true;
        BigInteger discr = TCFNumberFormat.toBigInteger(discr_value.getValue(), discr_value.isBigEndian(), false);
        TCFDataCache<String[]> children_cache = node.model.getSymbolChildrenCache(type.getID());
        if (children_cache == null) return true;
        if (!children_cache.validate(this)) return false;
        String[] children = children_cache.getData();
        if (children == null) return true;
        TCFDataCache<?> pending = null;
        ISymbols.Symbol variant_sym_data = null;
        ISymbols.Symbol default_sym_data = null;
        for (String id : children) {
            TCFDataCache<ISymbols.Symbol> sym_cache = node.model.getSymbolInfoCache(id);
            if (!sym_cache.validate()) {
                pending = sym_cache;
            }
            else {
                ISymbols.Symbol sym_data = sym_cache.getData();
                if (sym_data == null) continue;
                switch (sym_data.getSymbolClass()) {
                case variant:
                    TCFDataCache<Map<String,Object>> sym_loc_cache = node.model.getSymbolLocationCache(id);
                    if (!sym_loc_cache.validate()) {
                        pending = sym_loc_cache;
                        continue;
                    }
                    Map<String,Object> sym_loc_data = sym_loc_cache.getData();
                    Object discr_info = sym_loc_data.get("Discriminant");
                    if (discr_info instanceof List) {
                        List<?> values = (List<?>) discr_info;
                        for (Object value : values) {
                            if (value instanceof Number) {
                                BigInteger val = JSON.toBigInteger((Number) value);
                                if (discr.equals(val)) {
                                    variant_sym_data = sym_data;
                                    break;
                                }
                            }
                            else if (value instanceof Map) {
                                Map<?,?> range = (Map<?,?>) value;
                                Object x = range.get("X");
                                Object y = range.get("Y");
                                if (!(x instanceof Number) || !(y instanceof Number)) continue;
                                BigInteger lower = JSON.toBigInteger((Number) x);
                                BigInteger upper = JSON.toBigInteger((Number) y);
                                if (discr.compareTo(lower) >= 0 && discr.compareTo(upper) <= 0) {
                                    variant_sym_data = sym_data;
                                    break;
                                }
                            }
                        }
                    }
                    else if (discr_info == null) {
                        default_sym_data = sym_data;
                    }
                    break;
                default:
                    assert false : "Unexpected symbol class: " + sym_data.getSymbolClass();
                    break;
                }
            }
            if (variant_sym_data != null) break;
        }
        if (pending != null) {
            pending.wait(this);
            return false;
        }
        if (variant_sym_data == null) variant_sym_data = default_sym_data;
        if (variant_sym_data != null) {
            if (!findFields(variant_sym_data, map, deref)) return false;
        }
        return true;
    }

    private TCFNodeExpression findReg(String reg_id) {
        assert reg_id != null;
        for (TCFNode n : getNodes()) {
            if (n instanceof TCFNodeExpression) {
                TCFNodeExpression e = (TCFNodeExpression)n;
                if (reg_id.equals(e.getRegisterID())) return e;
            }
        }
        return null;
    }

    private boolean findRegs(TCFNodeRegister reg_node, Map<String,TCFNode> map) {
        TCFChildren reg_children = reg_node.getChildren();
        if (!reg_children.validate(this)) return false;
        for (TCFNode subnode : reg_children.toArray()) {
            TCFNodeExpression n = findReg(subnode.id);
            if (n == null) add(n = new TCFNodeExpression(node, null, null, null, null, subnode.id, -1, false));
            n.setSortPosition(map.size());
            map.put(n.id, n);
        }
        return true;
    }

    private TCFNodeExpression findIndex(int index) {
        assert index >= 0;
        for (TCFNode n : getNodes()) {
            if (n instanceof TCFNodeExpression) {
                TCFNodeExpression e = (TCFNodeExpression)n;
                if (e.getIndex() == index) return e;
            }
        }
        return null;
    }

    private TCFNodeExpression findDeref() {
        for (TCFNode n : getNodes()) {
            if (n instanceof TCFNodeExpression) {
                TCFNodeExpression e = (TCFNodeExpression)n;
                if (e.isDeref()) return e;
            }
        }
        return null;
    }

    private TCFNodeArrayPartition findPartition(int offs, int size) {
        assert offs >= 0;
        for (TCFNode n : getNodes()) {
            if (n instanceof TCFNodeArrayPartition) {
                TCFNodeArrayPartition e = (TCFNodeArrayPartition)n;
                if (e.getOffset() == offs && e.getSize() == size) return e;
            }
        }
        return null;
    }

    private TCFNodeExpression findScript(String s) {
        // TODO: need faster search
        for (TCFNode n : getNodes()) {
            if (n instanceof TCFNodeExpression) {
                TCFNodeExpression e = (TCFNodeExpression)n;
                if (s.equals(e.getScript())) return e;
            }
        }
        return null;
    }

    @Override
    protected boolean startDataRetrieval() {
        assert !isDisposed();
        TCFNode exp = node;
        while (!(exp instanceof TCFNodeExpression)) exp = exp.parent;
        for (ITCFPrettyExpressionProvider p : TCFPrettyExpressionProvider.getProviders()) {
            TCFDataCache<String[]> c = p.getChildren(exp);
            if (c != null) {
                if (!c.validate(this)) return false;
                if (c.getError() == null && c.getData() != null) {
                    int i = 0;
                    HashMap<String,TCFNode> data = new HashMap<String,TCFNode>();
                    for (String s : c.getData()) {
                        TCFNodeExpression n = findScript(s);
                        if (n == null) n = new TCFNodeExpression(node, s, null, null, null, null, -1, false);
                        n.setSortPosition(i++);
                        data.put(n.id, n);
                    }
                    set(null, null, data);
                    return true;
                }
            }
        }
        TCFDataCache<ISymbols.Symbol> type_cache = ((TCFNodeExpression)exp).getType();
        if (!type_cache.validate(this)) return false;
        ISymbols.Symbol type_data = type_cache.getData();
        if (type_data == null) {
            HashMap<String,TCFNode> data = new HashMap<String,TCFNode>();
            TCFDataCache<IExpressions.Value> val_cache = ((TCFNodeExpression)exp).getValue();
            if (!val_cache.validate(this)) return false;
            IExpressions.Value val_data = val_cache.getData();
            if (val_data != null) {
                String reg_id = val_data.getRegisterID();
                if (reg_id != null) {
                    if (!node.model.createNode(reg_id, this)) return false;
                    if (isValid()) return true;
                    TCFNodeRegister reg_node = (TCFNodeRegister)node.model.getNode(reg_id);
                    if (!findRegs(reg_node, data)) return false;
                }
            }
            set(null, null, data);
            return true;
        }
        ISymbols.TypeClass type_class = type_data.getTypeClass();
        Map<String,TCFNode> data = new HashMap<String,TCFNode>();
        if (par_level > 0 && type_class != ISymbols.TypeClass.array) {
            // Nothing
        }
        else if (type_class == ISymbols.TypeClass.composite) {
            if (!findFields(type_data, data, false)) return false;
        }
        else if (type_class == ISymbols.TypeClass.array) {
            int offs = par_level > 0 ? par_offs : 0;
            int size = par_level > 0 ? par_size : type_data.getLength();
            if (size <= 100) {
                for (int i = offs; i < offs + size; i++) {
                    TCFNodeExpression n = findIndex(i);
                    if (n == null) n = new TCFNodeExpression(node, null, null, null, null, null, i, false);
                    n.setSortPosition(i);
                    data.put(n.id, n);
                }
            }
            else {
                int next_size = 100;
                while (size / next_size > 100) next_size *= 100;
                for (int i = offs; i < offs + size; i += next_size) {
                    int sz = next_size;
                    if (i + sz > offs + size) sz = offs + size - i;
                    TCFNodeArrayPartition n = findPartition(i, sz);
                    if (n == null) n = new TCFNodeArrayPartition(node, par_level + 1, i, sz);
                    data.put(n.id, n);
                }
            }
        }
        else if (type_class == ISymbols.TypeClass.pointer) {
            TCFDataCache<IExpressions.Value> val_cache = ((TCFNodeExpression)exp).getValue();
            if (!val_cache.validate(this)) return false;
            IExpressions.Value val_data = val_cache.getData();
            if (val_data != null && (val_data.isImplicitPointer() || !isNull(val_data.getValue()))) {
                TCFDataCache<ISymbols.Symbol> base_type_cache = node.model.getSymbolInfoCache(type_data.getBaseTypeID());
                if (base_type_cache != null) {
                    if (!base_type_cache.validate(this)) return false;
                    ISymbols.Symbol base_type_data = base_type_cache.getData();
                    if (base_type_data != null && base_type_data.getTypeClass() != ISymbols.TypeClass.function && base_type_data.getSize() > 0) {
                        if (base_type_data.getTypeClass() == ISymbols.TypeClass.composite) {
                            if (!findFields(base_type_data, data, true)) return false;
                        }
                        else {
                            TCFNodeExpression n = findDeref();
                            if (n == null) n = new TCFNodeExpression(node, null, null, null, null, null, -1, true);
                            n.setSortPosition(0);
                            data.put(n.id, n);
                        }
                    }
                }
            }
        }
        set(null, null, data);
        return true;
    }

    private boolean isNull(byte[] data) {
        if (data == null) return true;
        for (byte b : data) {
            if (b != 0) return false;
        }
        return true;
    }
}
