/*******************************************************************************
 * Copyright (c) 2008, 2014 Wind River Systems, Inc. and others.
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

import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenCountUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IHasChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.services.ISymbols;
import org.eclipse.tcf.util.TCFDataCache;

public class TCFNodeArrayPartition extends TCFNode {

    private final int offs;
    private final int size;
    private final TCFChildrenSubExpressions children;

    TCFNodeArrayPartition(TCFNode parent, int level, int offs, int size) {
        super(parent, "AP" + level + "." + offs + "." + parent.id);
        this.offs = offs;
        this.size = size;
        children = new TCFChildrenSubExpressions(this, level, offs, size);
    }

    int getOffset() {
        return offs;
    }

    int getSize() {
        return size;
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
    protected boolean getData(IChildrenCountUpdate result, Runnable done) {
        if (!children.validate(done)) return false;
        result.setChildCount(children.size());
        return true;
    }

    @Override
    protected boolean getData(IChildrenUpdate result, Runnable done) {
        return children.getData(result, done);
    }

    @Override
    protected boolean getData(IHasChildrenUpdate result, Runnable done) {
        result.setHasChilren(true);
        return true;
    }

    @Override
    protected boolean getData(ILabelUpdate result, Runnable done) {
        BigInteger lower_bound = getLowerBound(done);
        if (lower_bound == null) return false;
        result.setImageDescriptor(ImageCache.getImageDescriptor(ImageCache.IMG_ARRAY_PARTITION), 0);
        String[] cols = result.getColumnIds();
        BigInteger index = lower_bound.add(BigInteger.valueOf(offs));
        String name = "[" + index + ".." + (index.add(BigInteger.valueOf(size - 1))) + "]";
        if (cols == null || cols.length <= 1) {
            result.setLabel(name, 0);
        }
        else {
            for (int i = 0; i < cols.length; i++) {
                String c = cols[i];
                if (c.equals(TCFColumnPresentationExpression.COL_NAME)) {
                    result.setLabel(name, i);
                }
                else {
                    result.setLabel("", i);
                }
            }
        }
        return true;
    }

    void onSuspended(boolean func_call) {
        children.onSuspended(func_call);
    }

    void onValueChanged() {
        children.onValueChanged();
    }

    void onRegisterValueChanged() {
        children.onRegisterValueChanged();
    }

    void onMemoryChanged() {
        children.onMemoryChanged();
    }

    void onMemoryMapChanged() {
        children.onMemoryMapChanged();
    }

    @Override
    public int compareTo(TCFNode n) {
        TCFNodeArrayPartition p = (TCFNodeArrayPartition)n;
        if (offs < p.offs) return -1;
        if (offs > p.offs) return +1;
        return 0;
    }
}
