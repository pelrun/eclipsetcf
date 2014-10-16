/**
 * NodePropertiesTableUIDelegate.java
 * Created on Oct 22, 2013
 *
 * Copyright (c) 2013 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.tcf.ui.delegates;

import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.persistence.utils.DataHelper;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.utils.CommonUtils;
import org.eclipse.tcf.te.tcf.locator.utils.SimulatorUtils;
import org.eclipse.tcf.te.ui.interfaces.services.INodePropertiesTableUIDelegate;
import org.eclipse.tcf.te.ui.tables.properties.NodePropertiesTableTableNode;

/**
 * Node properties table filter UI delegate implementation.
 */
public class NodePropertiesTableUIDelegate implements INodePropertiesTableUIDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.services.INodePropertiesTableUIDelegate#isFiltered(java.lang.Object, java.lang.String, java.lang.Object)
	 */
	@Override
	public boolean isFiltered(Object context, String name, Object value) {
		Assert.isNotNull(context);
		Assert.isNotNull(name);

		if (context instanceof IPeerNode) {
			if ((name.equals(IPeerProperties.PROP_SIM_TYPE) || name.startsWith(IPeerProperties.PROP_SIM_PROPERTIES))) {
				return !SimulatorUtils.isSimulatorEnabled((IPeerNode)context);
			}
			if (name.equals(IPeerProperties.PROP_PING_TIMEOUT)) {
				return SimulatorUtils.isSimulatorEnabled((IPeerNode)context);
			}
			if (name.startsWith(IPeerProperties.PROP_MODE_PROPERTIES)) {
				return !IPeerProperties.MODE_STOP.equals(CommonUtils.getMode((IPeerNode)context));
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.services.INodePropertiesTableUIDelegate#expandNodesAfterSort(java.lang.Object, java.util.List)
	 */
	@Override
	public void expandNodesAfterSort(Object context, List<NodePropertiesTableTableNode> sortedNodes) {
		String[] keysToExpand = new String[]{IPeerProperties.PROP_SIM_PROPERTIES, IPeerProperties.PROP_MODE_PROPERTIES};
		for (String key : keysToExpand) {
			int i = sortedNodes.indexOf(new NodePropertiesTableTableNode(key, "")); //$NON-NLS-1$
			if (i >= 0) {
				NodePropertiesTableTableNode node = sortedNodes.get(i);
				IPropertiesContainer data = DataHelper.decodePropertiesContainer(node.value);
				if (data != null && !data.isEmpty()) {
					sortedNodes.remove(i);
					sortedNodes.add(i++, new NodePropertiesTableTableNode(key, "")); //$NON-NLS-1$
					for (Entry<String,Object> entry : data.getProperties().entrySet()) {
						if (entry.getValue() != null) {
							if (!isFiltered(context, entry.getKey(), entry.getValue())) {
								sortedNodes.add(i++, new NodePropertiesTableTableNode("     " + entry.getKey(), entry.getValue().toString())); //$NON-NLS-1$
							}
						}
					}
				}
			}
		}
	}
}
