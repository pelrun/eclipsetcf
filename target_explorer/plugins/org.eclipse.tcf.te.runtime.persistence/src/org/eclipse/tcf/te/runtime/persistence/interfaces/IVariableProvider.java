/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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
