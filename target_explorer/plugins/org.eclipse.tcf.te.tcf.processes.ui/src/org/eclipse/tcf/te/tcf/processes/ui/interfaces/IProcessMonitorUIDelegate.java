/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.interfaces;

import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.ui.interfaces.ISearchable;


/**
 * Process monitor UI delegate.
 */
public interface IProcessMonitorUIDelegate {

	/**
	 * Returns the message for the given key.
	 *
	 * @param key The message key. Must not be <code>null</code>.
	 * @return The message or <code>null</code>.
	 */
	public String getMessage(String key);

	/**
	 * Returns the process monitor table column text for the given column
	 * based on the given original text.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param columnId The column id. Must not be <code>null</code>.
	 * @param text The original text to show in the column fetched from the label provider, or <code>null</code>.
	 *
	 * @return The new text to show in the column or <code>null</code>.
	 */
	public String getText(Object context, String columnId, String text);

	/**
	 * Returns if or if not the given column is active for the given context.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param columnId The column id. Must not be <code>null</code>.
	 *
	 * @return <code>True</code> if the column is active for the given context, <code>false</code> otherwise.
	 */
	public boolean isColumnActive(Object context, String columnId);

	/**
	 * Returns if or if not the given filter is active for the given context.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param filterId The filter id. Must not be <code>null</code>.
	 *
	 * @return <code>True</code> if the filter is active for the given context, <code>false</code> otherwise.
	 */
	public boolean isFilterActive(Object context, String filterId);

	/**
	 * Returns the list of searchables to use to find processes in the
	 * process monitor.
	 *
	 * @param node The peer model node context. Must not be <code>null</code>.
	 * @return The list of searchables to use or <code>null</code>.
	 */
	public ISearchable[] getSearchables(IPeerNode node);
}
