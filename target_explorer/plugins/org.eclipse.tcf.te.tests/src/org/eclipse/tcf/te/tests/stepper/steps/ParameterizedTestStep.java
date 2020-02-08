/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.stepper.steps;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

/**
 * Parameterized test step implementation.
 */
public class ParameterizedTestStep extends TestStep {
	public Map<?,?> params = null;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.extensions.AbstractStep#doSetInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	@Override
	public void doSetInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
	    super.doSetInitializationData(config, propertyName, data);

	    if (data instanceof Map<?,?>) params = (Map<?,?>)data;
	}
}
