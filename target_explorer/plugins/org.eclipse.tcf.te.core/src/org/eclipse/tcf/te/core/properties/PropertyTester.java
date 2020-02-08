/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.core.properties;

import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.core.utils.ConnectStateHelper;

/**
 * Adapter helper property tester implementation.
 */
public class PropertyTester extends org.eclipse.core.expressions.PropertyTester {

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		try {
			IAdapterManager manager = Platform.getAdapterManager();
			if (manager == null) return false;

			// "hasAdapter": Checks if the adapter given by the arguments is registered for the given receiver
			if ("hasAdapter".equals(property)) { //$NON-NLS-1$
				// The class to adapt to is within the expected value
				String adapterType = expectedValue instanceof String ? (String)expectedValue : null;
				if (adapterType != null) {
					return manager.hasAdapter(receiver, adapterType);
				}
			}
			if ("canAdaptTo".equals(property)) { //$NON-NLS-1$
				// Read the arguments and look for "forceAdapterLoad"
				boolean forceAdapterLoad = false;
				for (Object arg : args) {
					if (arg instanceof String && "forceAdapterLoad".equalsIgnoreCase((String)arg)) { //$NON-NLS-1$
						forceAdapterLoad = true;
					}
				}

				// The class to adapt to is within the expected value
				String adapterType = expectedValue instanceof String ? (String)expectedValue : null;
				if (adapterType != null) {
					Object adapter = manager.getAdapter(receiver, adapterType);
					if (adapter != null) return true;

					// No adapter. This can happen too if the plug-in contributing the adapter
					// factory hasn't been loaded yet.
					if (forceAdapterLoad) adapter = manager.loadAdapter(receiver, adapterType);
					if (adapter != null) return true;
				}
			}

			// "hasEnvVar" tests for the existence of a system property or environment variable
			// with the name passed in via the arguments.
			if ("hasEnvVar".equals(property) && expectedValue instanceof Boolean) { //$NON-NLS-1$
				boolean hasEnvVar = false;

				String name = null;
				for (Object arg : args) {
					if (arg instanceof String) {
						name = (String)arg;
						break;
					}
				}

				if (name != null) {
					String value = System.getProperty(name);
					if (value == null) value = System.getenv(name);
					hasEnvVar = value != null;
				}

				return hasEnvVar == ((Boolean)expectedValue).booleanValue();
			}

			// "envVar" tests for the value of a system property or environment variable
			// with the name passed in via the arguments.
			if ("envVar".equals(property)) { //$NON-NLS-1$
				String value = null;

				String name = null;
				for (Object arg : args) {
					if (arg instanceof String) {
						name = (String)arg;
						break;
					}
				}

				if (name != null) {
					value = System.getProperty(name);
					if (value == null) value = System.getenv(name);
				}

				// Always check against the string value
				return value != null ? expectedValue.toString().equals(value) : false;
			}

			if ("isConnectStateChangeActionAllowed".equals(property) && receiver instanceof IConnectable && expectedValue instanceof String) { //$NON-NLS-1$
				return ((IConnectable)receiver).isConnectStateChangeActionAllowed(ConnectStateHelper.getConnectAction((String)expectedValue));
			}
			if ("isConnectState".equals(property) && receiver instanceof IConnectable && expectedValue instanceof String) { //$NON-NLS-1$
				return ((IConnectable)receiver).getConnectState() == ConnectStateHelper.getConnectState((String)expectedValue);
			}
		} catch (Error e) {
			if (!(e.getCause() instanceof InterruptedException)) {
				throw e;
			}
		}

	    return false;
	}
}
