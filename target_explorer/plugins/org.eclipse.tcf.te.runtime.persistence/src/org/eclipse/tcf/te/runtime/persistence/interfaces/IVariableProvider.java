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
 * IVariableProvider
 */
public interface IVariableProvider {

	/**
	 * Get the list of provided variables.
	 * @return The provided variables.
	 */
	public Map<String,String> getVariables();
}
