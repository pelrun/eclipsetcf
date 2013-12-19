/**
 * AbstractMapPropertiesSection.java
 * Created on Sep 14, 2013
 *
 * Copyright (c) 2013 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.tcf.processes.ui.internal.tabbed;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.ui.tabbed.AbstractMapPropertiesSection;

/**
 * The property section to display the properties of a module.
 */
public class ProcessNodePropertiesSection extends AbstractMapPropertiesSection {

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
					props.set(((IProcessContextNode)provider).getProperties());
				}
			}
		};
		Assert.isTrue(!Protocol.isDispatchThread());
		Protocol.invokeAndWait(runnable);

		return props.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#getText()
	 */
	@Override
	protected String getText() {
		return Messages.ProcessNodePropertiesSection_Title;
	}
}
