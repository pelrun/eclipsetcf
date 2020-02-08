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

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.ui.tabbed.AbstractMapPropertiesSection;
/**
 * The property section to display the general properties of a peerNode.
 */
public class PeerNodePropertiesSection extends AbstractMapPropertiesSection {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.tabbed.AbstractMapPropertiesSection#getLabelProvider(org.eclipse.jface.viewers.TableViewer)
	 */
	@Override
    protected ILabelProvider getLabelProvider(TableViewer viewer) {
		return new PeerNodePropertiesSectionLabelProvider(viewer);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.tabbed.AbstractMapPropertiesSection#getContentProvider(org.eclipse.jface.viewers.TableViewer)
	 */
	@Override
    protected IContentProvider getContentProvider(TableViewer viewer) {
		return new PeerNodePropertiesSectionContentProvider(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.tabbed.AbstractMapPropertiesSection#getViewerInput()
	 */
	@Override
	protected Object getViewerInput() {
	    return getPeerNode(provider);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.tabbed.BaseTitledSection#getText()
	 */
    @Override
    protected String getText() {
	    return Messages.PeerNodePropertiesSection_title;
    }

}
