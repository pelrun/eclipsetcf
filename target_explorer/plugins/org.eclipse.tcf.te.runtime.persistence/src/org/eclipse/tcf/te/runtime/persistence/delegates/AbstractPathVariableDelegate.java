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

import org.eclipse.core.runtime.Path;

/**
 * AbstractPathVariableDelegate
 */
public abstract class AbstractPathVariableDelegate extends AbstractVariableDelegate {

	/**
	 * Return true, if the key represents a path value.
	 * @param key The key to check.
	 * @return <code>true</code> if the key represents a path value.
	 */
	protected abstract boolean isPathKey(String key);

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.AbstractVariableDelegate#useVariable(java.lang.String, java.lang.Object, java.lang.String, java.lang.String)
	 */
	@Override
	protected Object useVariable(String key, Object value, String variableName, String variableValue) {
		if (isPathKey(key) && value instanceof String) {
			String valuePath = new Path((String)value).toString();
			String variablePath = new Path(variableValue).toString();
			return super.useVariable(key, valuePath, variableName, variablePath);
		}

		return super.useVariable(key, value, variableName, variableValue);
	}
}
