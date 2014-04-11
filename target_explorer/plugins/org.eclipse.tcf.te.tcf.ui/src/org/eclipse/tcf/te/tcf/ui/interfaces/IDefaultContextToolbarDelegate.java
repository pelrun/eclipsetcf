/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.ui.interfaces;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.ui.handler.DefaultContextSelectorToolbarContribution;

/**
 * Provides dynamic information for the {@link DefaultContextSelectorToolbarContribution}
 */
public interface IDefaultContextToolbarDelegate {

	/**
	 * Returns a list of new configuration wizard ids to show
	 * in the context selector toolbar contribution.
	 * @param context The selected default context.
	 * @return Array of ids.
	 */
	public String[] getToolbarNewConfigWizardIds(Object context);

	/**
	 * Returns a list if history ids that should be used to diaply the "recently used actions" in the toolbar actions sub menu.
	 * @param peerNode The selected default context.
	 * @param historyIds
	 * @return Array of ids.
	 */
	public String[] getToolbarHistoryIds(IPeerNode peerNode, String[] historyIds);

	/**
	 * Get the label for a history action.
	 * @param peerNode
	 * @param historyId
	 * @param entry
	 * @return
	 */
	public String getLabel(IPeerNode peerNode, String historyId, String entry);

	/**
	 * Get the image descriptor for a history action.
	 * @param peerNode
	 * @param historyId
	 * @param entry
	 * @return
	 */
	public ImageDescriptor getImageDescriptor(IPeerNode peerNode, String historyId, String entry);

	/**
	 * Execute a history action.
	 * Depending on showDialog, a dialog is shown first.
	 * @param peerNode
	 * @param historyId
	 * @param entry
	 * @param showDialog
	 */
	public void execute(IPeerNode peerNode, String historyId, String entry, boolean showDialog);
}
