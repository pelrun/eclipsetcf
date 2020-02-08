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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.tcf.te.runtime.persistence.PersistenceManager;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IVariableDelegate;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IVariableProvider;
import org.eclipse.tcf.te.runtime.utils.Host;

/**
 * AbstractVariableDelegate
 */
public abstract class AbstractVariableDelegate implements IVariableDelegate, IExecutableExtension  {

	/**
	 * Get the list of keys this delegate is handling.
	 * @return The list of handled keys.
	 */
	protected abstract String[] getKeysToHandle();

	/**
	 * Try to use a variable inside the given value.
	 *
	 * @param key The key of the value.
	 * @param value The value to inspect.
	 * @param variableName The variable name to use.
	 * @param variableValue The variable value.
	 *
	 * @return The new value if the variable was used, <code>null</code> otherwise.
	 */
	protected Object useVariable(String key, Object value, String variableName, String variableValue) {
		if (value instanceof String) {
			boolean contains = Host.isWindowsHost() ? ((String)value).toLowerCase().contains(variableValue.toLowerCase()) : ((String)value).contains(variableValue);
			if (contains) {
				if (Host.isWindowsHost()) {
					return Pattern.compile(variableValue, Pattern.CASE_INSENSITIVE).matcher((String)value).replaceAll("<"+variableName+">"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				return ((String)value).replaceAll(variableValue, "<"+variableName+">"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return null;
	}

	/**
	 * Try to restore a variable inside the given value.
	 * @param key The key of the value.
	 * @param value The value to inspect.
	 * @param variableName The variable name to restore.
	 * @param variableValue The variable value.
	 * @return The new value if the variable was used, <code>null</code> otherwise.
	 */
	protected Object restoreValue(String key, Object value, String variableName, String variableValue) {
		if (value instanceof String && ((String)value).contains("<"+variableName+">")) { //$NON-NLS-1$ //$NON-NLS-2$
			return ((String)value).replaceAll("<"+variableName+">", variableValue); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.interfaces.IVariableDelegate#getVariables(java.util.Map)
	 */
	@Override
	public Map<String,String> getVariables(Map<String, Object> map) {
		Map<String,String> usedVariables = new HashMap<String, String>();

		for (String	key : getKeysToHandle()) {
			if (map.get(key) != null) {
				for (Entry<String,String> variable : getVariablesMap(null).entrySet()) {
					Object newValue = useVariable(key, map.get(key), variable.getKey(), variable.getValue());
					if (newValue != null) {
						map.put(key, newValue);
						usedVariables.put(variable.getKey(), variable.getValue());
					}
				}
			}
		}

		return usedVariables;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.interfaces.IVariableDelegate#putVariables(java.util.Map, java.util.Map)
	 */
	@Override
	public Map<String,Object> putVariables(Map<String,Object> map, Map<String,String> defaultVariables) {
		for (String	key : getKeysToHandle()) {
			if (map.get(key) != null) {
				for (Entry<String,String> variable : getVariablesMap(defaultVariables).entrySet()) {
					Object newValue = restoreValue(key, map.get(key), variable.getKey(), variable.getValue());
					if (newValue != null) {
						map.put(key, newValue);
					}
				}
			}
		}

		return map;
	}

	/**
	 * Calculate and return the map of variables to use.
	 * If a map of default variables is given, all variables will be added to the result
	 * map if the name does not exist in the list of provided {@link IVariableProvider} variables.
	 * @param defaultVariables Default variables or <code>null</code>.
	 * @return Map of variables to use.
	 */
	protected Map<String,String> getVariablesMap(Map<String,String> defaultVariables) {
		Map<String,String> variables = new HashMap<String, String>();
		for (IVariableProvider provider : PersistenceManager.getInstance().getVariableProviders()) {
			variables.putAll(provider.getVariables());
		}

		if (defaultVariables != null) {
			for (Entry<String,String> defaultVariable : defaultVariables.entrySet()) {
				if (!variables.containsKey(defaultVariable.getKey())) {
					variables.put(defaultVariable.getKey(), defaultVariable.getValue());
				}
			}
		}

		return variables;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
	}
}
