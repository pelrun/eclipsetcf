/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.internal.properties;

import org.eclipse.tcf.te.runtime.services.ServiceUtils;
import org.eclipse.tcf.te.tcf.processes.ui.interfaces.IProcessMonitorUIDelegate;


/**
 * Process monitor property tester (UI).
 */
public class PropertyTester extends org.eclipse.core.expressions.PropertyTester {

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    	// Get the UI delegate for the context (== receiver)
		IProcessMonitorUIDelegate delegate = ServiceUtils.getUIServiceDelegate(receiver, receiver, IProcessMonitorUIDelegate.class);
		if ("isColumnActive".equals(property) || "isFilterActive".equals(property)) { //$NON-NLS-1$ //$NON-NLS-2$
			// If no delegate is registered for the context, the column or filter are treated as active (== no activation expression)
			if (delegate == null) return true;

			// Column or the filter id is the only argument to be passed in
			String id = args.length > 0 && args[0] instanceof String ? (String)args[0] : null;
			if (id != null) {
				boolean isActive = "isColumnActive".equals(property) ? delegate.isColumnActive(receiver, id) : delegate.isFilterActive(receiver, id); //$NON-NLS-1$
				if (expectedValue instanceof Boolean) {
					return ((Boolean)expectedValue).equals(Boolean.valueOf(isActive));
				}
			}
		}

	    return false;
    }

}
