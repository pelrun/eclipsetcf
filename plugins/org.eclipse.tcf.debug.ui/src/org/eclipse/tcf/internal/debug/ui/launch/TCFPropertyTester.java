/*******************************************************************************
 * Copyright (c) 2008-2019 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.launch;

import java.util.Collection;
import java.util.Map;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.tcf.debug.ui.ITCFLaunchContext;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;

public class TCFPropertyTester extends PropertyTester {

    public boolean test(Object receiver, String property, Object[] args, Object expected_value) {
        if (property.equals("areUpdatePoliciesSupported")) return testUpdatePoliciesSupported(receiver);
        if (property.equals("isExecutable")) return testIsExecutable(receiver, expected_value);
        if (property.equals("canReset")) return testCanReset(receiver);
        return false;
    }

    private boolean testUpdatePoliciesSupported(Object receiver) {
        return receiver instanceof IDebugView;
    }

    private boolean testIsExecutable(Object receiver, Object expected_value) {
        Object value = null;
        try {
            if (receiver instanceof IAdaptable) {
                IAdaptable selection = (IAdaptable)receiver;
                ITCFLaunchContext context = TCFLaunchContext.getLaunchContext(selection);
                if (context != null) {
                    IProject project = context.getProject(selection);
                    IPath path = context.getPath(selection);
                    if (project != null && path != null) {
                        value = context.isBinary(project, path);
                    }
                }
            }
        }
        catch (Throwable x) {
            Activator.log(x);
        }
        if (expected_value != null) return expected_value.equals(value);
        return (value instanceof Boolean) && ((Boolean)value).booleanValue();
    }

    private boolean testCanReset(Object receiver) {
        boolean canReset = false;

        if (receiver instanceof TCFNode) {
            TCFNode node = (TCFNode)receiver;
            while (!canReset && node != null) {
                if (node instanceof TCFNodeExecContext) {
                    final TCFNodeExecContext exec = (TCFNodeExecContext)node;
                    try {
                        canReset = new TCFTask<Boolean>(exec.getChannel()) {
                            @Override
                            public void run() {
                                TCFDataCache<Collection<Map<String, Object>>> cache = exec.getResetCapabilities();
                                if (!cache.validate(this)) {
                                    return;
                                }
                                Collection<Map<String, Object>> caps = cache.getData();
                                boolean ok = caps != null && !caps.isEmpty();
                                done(ok);
                            }
                        }.getE();
                    } catch (Throwable x) {
                        Activator.log(x);
                    }
                }
                node = node.getParent();
            }
        }
        return canReset;
    }
}
