/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.core.properties;

import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IDebugService;


/**
 * Launch framework property tester implementation.
 */
public class PropertyTester extends org.eclipse.core.expressions.PropertyTester {

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {

		// "isLaunched": Checks if the debugger has been attached for the given context
		if ("isLaunched".equals(property) && expectedValue instanceof Boolean) { //$NON-NLS-1$
			IDebugService dbgService = ServiceManager.getInstance().getService(receiver, IDebugService.class, false);
			if (dbgService != null) {
				return ((Boolean)expectedValue).booleanValue() == dbgService.isLaunched(receiver);
			}
		}

	    return false;
	}
}
