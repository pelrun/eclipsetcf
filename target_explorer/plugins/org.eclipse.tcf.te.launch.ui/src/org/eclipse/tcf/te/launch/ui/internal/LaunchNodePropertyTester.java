/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.ui.internal;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.tcf.te.launch.ui.model.LaunchNode;

/**
 * The property tester for a launch tree node.
 */
public class LaunchNodePropertyTester extends PropertyTester {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (receiver instanceof LaunchNode) {
			LaunchNode node = (LaunchNode)receiver;
			if (property.equals("isLaunchConfigType")) { //$NON-NLS-1$
				if (LaunchNode.TYPE_LAUNCH_CONFIG_TYPE.equals(node.getType())) {
					boolean isValue = expectedValue == null || expectedValue.equals(node.getLaunchConfigurationType().getIdentifier());
					return isValue;
				}
			}
			else if (property.equals("isLaunchConfig")) { //$NON-NLS-1$
				return LaunchNode.TYPE_LAUNCH_CONFIG.equals(node.getType());
			}
		}
		return false;
	}
}