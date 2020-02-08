/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.delegates;

import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.persistence.utils.DataHelper;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
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
		String[] keysToExpand = new String[]{IPeerNodeProperties.PROPERTY_WARNINGS};
		for (String key : keysToExpand) {
			int i = sortedNodes.indexOf(new NodePropertiesTableTableNode(key, "")); //$NON-NLS-1$
			if (i >= 0) {
				NodePropertiesTableTableNode node = sortedNodes.get(i);
				IPropertiesContainer data = DataHelper.decodePropertiesContainer(node.value);
				if (data != null && !data.isEmpty()) {
					sortedNodes.remove(i);
					boolean firstDone = false;
					for (Entry<String,Object> entry : data.getProperties().entrySet()) {
						if (entry.getValue() != null) {
							sortedNodes.add(i++, new NodePropertiesTableTableNode(firstDone ? "" : key, entry.getValue().toString())); //$NON-NLS-1$
							firstDone = true;
						}
					}
				}
			}
		}

		keysToExpand = new String[]{IPeerProperties.PROP_SIM_PROPERTIES, IPeerProperties.PROP_MODE_PROPERTIES};
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
							sortedNodes.add(i++, new NodePropertiesTableTableNode("     " + entry.getKey(), entry.getValue().toString())); //$NON-NLS-1$
						}
					}
				}
			}
		}

		String[] keysToSplit = new String[]{IPeerNodeProperties.PROPERTY_LOCAL_SERVICES, IPeerNodeProperties.PROPERTY_REMOTE_SERVICES, IPeerProperties.PROP_OFFLINE_SERVICES};
		for (String key : keysToSplit) {
			int i = sortedNodes.indexOf(new NodePropertiesTableTableNode(key, "")); //$NON-NLS-1$
			if (i >= 0) {
				NodePropertiesTableTableNode node = sortedNodes.get(i);
				String[] services = node.value.split(","); //$NON-NLS-1$
				if (services.length > 10) {
					sortedNodes.remove(i);
					String list = ""; //$NON-NLS-1$
					int added = 0;
					for (int s=0;s<services.length;s++) {
						list += (added > 0 ? ", " : "") + services[s].trim(); //$NON-NLS-1$ //$NON-NLS-2$
						added++;
						if (added == 10) {
							if (s < 10) {
								sortedNodes.add(i++, new NodePropertiesTableTableNode(key, list));
							}
							else {
								sortedNodes.add(i++, new NodePropertiesTableTableNode("", list)); //$NON-NLS-1$
							}
							added = 0;
							list = ""; //$NON-NLS-1$
						}
					}
				}
			}
		}

	}
}
