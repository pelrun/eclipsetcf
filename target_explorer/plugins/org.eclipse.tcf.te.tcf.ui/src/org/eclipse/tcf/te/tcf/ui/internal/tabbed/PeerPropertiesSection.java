/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal.tabbed;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.ui.tabbed.AbstractMapPropertiesSection;
import org.eclipse.ui.IWorkbenchPart;
/**
 * The property section to display the general properties of a peer.
 */
public class PeerPropertiesSection extends AbstractMapPropertiesSection {

	IPeer peer = null;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.tabbed.BaseTitledSection#setInput(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
	    super.setInput(part, selection);
        Assert.isTrue(selection instanceof IStructuredSelection);
        Object input = ((IStructuredSelection) selection).getFirstElement();
        if (input instanceof ILocatorNode) {
        	peer = ((ILocatorNode)input).getPeer();
        }
        else if (input instanceof IPeer) {
        	peer = (IPeer)input;
        }
        else {
        	peer = null;
        }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.tabbed.AbstractMapPropertiesSection#getViewerInput()
	 */
	@Override
	protected Object getViewerInput() {
		final AtomicReference<Map<String,String>> props = new AtomicReference<Map<String,String>>();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if (peer != null) {
					props.set(peer.getAttributes());
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
	    return Messages.PeerPropertiesSection_title;
    }

}
