/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.interfaces.services;

import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;

/**
 * Interface for a selection service to get the current default selection.
 */
public interface ISelectionService extends IService {

	/**
	 * Filter for default selection.
	 */
	public static interface ISelectionFilter {

		/**
		 * Check a peer model node if it should be used.
		 * @param peerModel The peer model node.
		 * @return <code>true</code> if the given peer model node is a possible candidate.
		 */
		public boolean select(IPeerModel peerModel);
	}

	/**
	 * Return a list of possible peer model node candidates.
	 * The Se
	 * If a selection is given and the filter applies, it will be used as the first element(s).
	 * Otherwise if a default selection was set using setDefaultSelection(IPeerModel)
	 * and the filter applies, it will be used as the first element.
	 * UI implementations of the service should then check the current editor and the system management selection.
	 * @param currentSelection The current selection (i.e. from a command handler) or <code>null</code>.
	 * @param filter The filter for the peer model node candidates or <code>null</code>
	 * @return Array of peer model nodes or an empty array.
	 */
	public IPeerModel[] getCandidates(Object currentSelection, ISelectionFilter filter);

	/**
	 * Set a new default selection.
	 * @param peerModel The default peer model node or <code>null</code> to reset the default.
	 */
	public void setDefaultSelection(IPeerModel peerModel);

	/**
	 * Get the default selection.
	 * @param filter The filter for the peer model node candidates or <code>null</code>
	 * @return The set default selection if set and the filter applies, <code>null</code> otherwise.
	 */
	public IPeerModel getDefaultSelection(ISelectionFilter filter);
}
