/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.trees;

/**
 * The persistable filter action used by TreeViewerState to save and restore the tree viewer's filter
 * action.
 */
class FilterState {
	// The filter's id.
	private String filterId;
	// If the filter is enabled.
	private boolean enabled;

	/**
	 * Get the filter's id.
	 * 
	 * @return The filter's id.
	 */
	public String getFilterId() {
		return filterId;
	}

	/**
	 * Set the filter's id.
	 * 
	 * @param filterId The new filter id.
	 */
	public void setFilterId(String filterId) {
		this.filterId = filterId;
	}

	/**
	 * Return if the filter is enabled.
	 * 
	 * @return If the filter is enabled.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set the filer's enabled action.
	 * 
	 * @param enabled true if the filter is enabled.
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
