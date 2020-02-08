/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.interfaces.services;

import java.util.List;

import org.eclipse.tcf.te.ui.tables.properties.NodePropertiesTableTableNode;

/**
 * Interface to be implemented by clients to filter the content of the
 * node properties table control.
 */
public interface INodePropertiesTableUIDelegate {

	/**
	 * Returns if or if not the given property is filtered from the node
	 * properties table control.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param name The property name. Must not be <code>null</code>.
	 * @param value The property value or <code>null</code>.
	 *
	 * @return <code>True</code> if the property is filtered, <code>false</code> otherwise.
	 */
	public boolean isFiltered(Object context, String name, Object value);

	/**
	 * Expand nodes after the list was generated and sorted.
	 * @param context The context. Must not be <code>null</code>.
	 * @param sortedNodes
	 */
	public void expandNodesAfterSort(Object context, List<NodePropertiesTableTableNode> sortedNodes);
}
