/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui.breakpoints;

import org.eclipse.cdt.debug.core.model.ICBreakpoint;
import org.eclipse.cdt.debug.core.model.ICBreakpointExtension;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.tcf.internal.cdt.ui.Activator;
import org.eclipse.tcf.internal.debug.model.TCFBreakpointsModel;

public class TCFBreakpointScopeExtension implements ICBreakpointExtension {

    private ICBreakpoint fBreakpoint;
    private IPreferenceStore fPreferenceStore;

    public void initialize(IPreferenceStore prefStore) {
        fPreferenceStore = prefStore;
    }

    public void initialize(ICBreakpoint breakpoint) throws CoreException {
        fBreakpoint = breakpoint;
    }

    public void setThreadFilter(final String[] threadIds) {
        String attr;
        if (threadIds == null) {
            attr = "";
        }
        else if (threadIds.length == 0) {
            // empty string is filtered out in TCFBreakpointsModel
            attr = " ";
        }
        else {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < threadIds.length - 1; i++) {
                buf.append(threadIds[i]).append(',');
            }
            buf.append(threadIds[threadIds.length - 1]);
            attr = buf.toString();
        }
        setRawContextIds(attr);
    }

    String getRawContextIds() {
        if (fPreferenceStore != null) {
            return fPreferenceStore.getString(TCFBreakpointsModel.ATTR_CONTEXTIDS);
        }
        if (fBreakpoint != null) {
            IMarker marker = fBreakpoint.getMarker();
            if (marker != null) {
                return marker.getAttribute(TCFBreakpointsModel.ATTR_CONTEXTIDS, "");
            }
        }
        return "";
    }

    void setRawContextIds(final String contextIDs) {
        if (fPreferenceStore!= null) {
            fPreferenceStore.setValue(TCFBreakpointsModel.ATTR_CONTEXTIDS, contextIDs);
        }
        else if (fBreakpoint != null) {
            try {
                ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
                    public void run(IProgressMonitor monitor) throws CoreException {
                        final IMarker m = fBreakpoint.getMarker();
                        if (m == null || !m.exists()) return;

                        m.setAttribute(TCFBreakpointsModel.ATTR_CONTEXTIDS, contextIDs);
                    }
                }, null);
            }
            catch (Exception e) {
                Activator.log(e);
            }
        }
    }

    public void setPropertiesFilter(String properties) {
        final String _properties = properties != null ? properties : "";

        if (fPreferenceStore!= null) {
            fPreferenceStore.setValue(TCFBreakpointsModel.ATTR_CONTEXT_QUERY, _properties);
        }
        else if (fBreakpoint != null) {
            try {
                ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
                    public void run(IProgressMonitor monitor) throws CoreException {
                        final IMarker m = fBreakpoint.getMarker();
                        if (m == null || !m.exists()) return;
                        m.setAttribute(TCFBreakpointsModel.ATTR_CONTEXT_QUERY, _properties);
                    }
                }, null);
            }
            catch (Exception e) {
                Activator.log(e);
            }
        }
    }

    public String getPropertiesFilter() {
        if (fPreferenceStore != null) {
            return fPreferenceStore.getString(TCFBreakpointsModel.ATTR_CONTEXT_QUERY);
        }
        if (fBreakpoint != null) {
            IMarker marker = fBreakpoint.getMarker();
            if (marker != null) {
                return marker.getAttribute(TCFBreakpointsModel.ATTR_CONTEXT_QUERY, "");
            }
        }
        return "";
    }

    public String[] getThreadFilters() {
        String contextIds = getRawContextIds();
        if (contextIds.length() != 0) {
            return contextIds.split(",\\s*");
        }
        return null;
    }
}
