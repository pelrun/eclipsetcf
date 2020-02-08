/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.ui.internal.properties;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.tcf.te.launch.ui.model.LaunchNode;

/**
 * The filter to filter out non launch configuration nodes.
 */
public class LaunchConfigTypeFilter implements IFilter {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IFilter#select(java.lang.Object)
	 */
	@Override
	public boolean select(Object toTest) {
		if (toTest instanceof LaunchNode) {
			LaunchNode node = (LaunchNode)toTest;
			try {
				return node.isType(LaunchNode.TYPE_LAUNCH_CONFIG_TYPE);
			}
			catch (Exception e) {
			}
		}
		return false;
	}

}
