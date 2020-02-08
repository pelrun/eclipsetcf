/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui.breakpoints;

import org.eclipse.cdt.debug.core.ICWatchpointTarget;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExpression;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ISymbols;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;

/**
 * TCF "Add Watchpoint" target implementation.
 */
public class TCFWatchpointTarget implements ICWatchpointTarget {

    private final TCFNodeExpression node;

    public TCFWatchpointTarget(TCFNodeExpression node) {
        this.node = node;
    }

    public void canSetWatchpoint(CanCreateWatchpointRequest request) {
        request.setCanCreate(true);
        request.done();
    }

    public String getExpression() {
        final TCFDataCache<String> expressionText = node.getExpressionText();
        String expr = new TCFTask<String>(node.getChannel()) {
            public void run() {
                if (!expressionText.validate(this)) return;
                done(expressionText.getData());
            }
        }.getE();
        return expr != null ? expr : "";
    }

    public void getSize(final GetSizeRequest request) {
        final TCFDataCache<ISymbols.Symbol> expressionType = node.getType();
        Protocol.invokeLater(new Runnable() {
            public void run() {
                if (!expressionType.validate(this)) return;
                ISymbols.Symbol type = expressionType.getData();
                request.setSize(type != null ? type.getSize() : 1);
                request.done();
            }
        });
    }
}
