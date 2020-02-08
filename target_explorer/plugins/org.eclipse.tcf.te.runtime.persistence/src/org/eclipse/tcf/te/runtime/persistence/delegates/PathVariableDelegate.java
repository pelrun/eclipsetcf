/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.runtime.persistence.delegates;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

/**
 * General path variable delegate that can be configured directly in the contributions.
 *
 * To set the list of handled keys you have to use the <code>class</code> and <code>parameter</code> tags
 * with <b>keysToHandle</b> as parameter name and a comma separated list of handled keys
 * (i.e. "file,directory") as parameter value.
 */
public class PathVariableDelegate extends AbstractPathVariableDelegate {

	private String[] keysToHandle = new String[0];

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.AbstractPathVariableDelegate#isPathKey(java.lang.String)
	 */
	@Override
	protected boolean isPathKey(String key) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.AbstractVariableDelegate#getKeysToHandle()
	 */
	@Override
	protected String[] getKeysToHandle() {
		return keysToHandle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		if (data instanceof Map) {
			String keys = (String)((Map<?,?>)data).get("keysToHandle"); //$NON-NLS-1$
			if (keys != null) {
				keysToHandle = keys.split("\\W*,\\W*"); //$NON-NLS-1$
			}
		}
	}
}
