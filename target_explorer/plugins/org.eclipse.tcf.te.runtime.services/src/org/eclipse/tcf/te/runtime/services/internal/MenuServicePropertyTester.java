/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.services.internal;

import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IMenuService;


/**
 * Services plug-in property tester implementation.
 */
public class MenuServicePropertyTester extends org.eclipse.core.expressions.PropertyTester {

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		// Get the menu service instance for the given receiver
		IMenuService service = ServiceManager.getInstance().getService(receiver, IMenuService.class);
		if (service != null) {

			// "isVisible": Checks if a given menu contribution shall be visible for the given receiver.
			if ("isVisible".equals(property)) { //$NON-NLS-1$
				// The menu contribution ID is the first argument
				String contributionID = args.length > 0 && args[0] instanceof String ? (String)args[0] : null;
				boolean isVisible = service.isVisible(receiver, contributionID);
				return expectedValue instanceof Boolean ? ((Boolean)expectedValue).booleanValue() == isVisible : false;
			}
		}

		return false;
	}

}
