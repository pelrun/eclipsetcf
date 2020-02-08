/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.ui.interfaces;

import org.eclipse.swt.graphics.Image;
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
	 * Returns a list of handled step group ids.
	 * @param peerNode The selected default context.
	 * @return
	 */
	public String[] getHandledStepGroupIds(IPeerNode peerNode);

	/**
	 * Get the label for a history action.
	 * @param peerNode
	 * @param entry
	 * @return
	 */
	public String getLabel(IPeerNode peerNode, String entry);

	/**
	 * Get the description for a history action.
	 * @param peerNode
	 * @param entry
	 * @return
	 */
	public String getDescription(IPeerNode peerNode,String entry);

	/**
	 * Get the image for a history action.
	 * @param peerNode
	 * @param entry
	 * @return
	 */
	public Image getImage(IPeerNode peerNode, String entry);

	/**
	 * Execute a history action.
	 * Depending on showDialog, a dialog is shown first.
	 * @param peerNode
	 * @param entry
	 * @param showDialog
	 * @param The executed entry or <code>null</code> if not executed.
	 */
	public String execute(IPeerNode peerNode, String entry, boolean showDialog);

	/**
	 * Validate the entry.
	 * @param peerNode
	 * @param entry
	 * @return
	 */
	public boolean validate(IPeerNode peerNode, String entry);

}
