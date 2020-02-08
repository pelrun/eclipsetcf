/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.internal.tabbed;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.ui.tabbed.AbstractMapPropertiesSection;

/**
 * The property section to display the advanced properties of a process context.
 */
public class SystemMonitorContextPropertiesSection extends AbstractMapPropertiesSection {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.tabbed.AbstractMapPropertiesSection#getViewerInput()
	 */
	@Override
	protected Object getViewerInput() {
        final AtomicReference<Map<String, Object>> props = new AtomicReference<Map<String,Object>>();
		Runnable runnable = new Runnable() {
			@SuppressWarnings("synthetic-access")
            @Override
			public void run() {
				if (provider instanceof IProcessContextNode) {
					props.set(((IProcessContextNode)provider).getSysMonitorContext().getProperties());
				}
			}
		};
		Assert.isTrue(!Protocol.isDispatchThread());
		Protocol.invokeAndWait(runnable);

        return props.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.tabbed.BaseTitledSection#getText()
	 */
	@Override
	protected String getText() {
	    return Messages.SystemMonitorContextPropertiesSection_Title;
	}
}
