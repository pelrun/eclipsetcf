/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.runtime.persistence.interfaces;

import java.util.Map;

/**
 * IVariableDelegate
 */
public interface IVariableDelegate {

	/**
	 * Extract variables from map and use them inside map values.
	 * @param map The map to inspect.
	 * @return The used variables.
	 */
	public Map<String,String> getVariables(Map<String, Object> map);

	/**
	 * Replace all variables inside map values by current variable value or given default.
	 * @param map The map with variables to inspect.
	 * @param defaultVariables The default variables.
	 * @return The map without variables.
	 */
	public Map<String,Object> putVariables(Map<String,Object> map, Map<String,String> defaultVariables);
}
