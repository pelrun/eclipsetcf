/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.persistence.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IVariableProvider;

/**
 *
 */
public class VariableProvider implements IVariableProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.interfaces.IVariableProvider#getVariables()
	 */
	@Override
	public Map<String, String> getVariables() {
		Map<String,String> variables = new HashMap<String, String>();

		try {
			IDynamicVariable variable = VariablesPlugin.getDefault().getStringVariableManager().getDynamicVariable("workspace_loc"); //$NON-NLS-1$
			if (variable != null) {
				String value = variable.getValue(null);
				if (value != null) {
					variables.put("WORKSPACE_LOC", new Path(value).toString()); //$NON-NLS-1$
				}
			}
		}
		catch (Exception e) {
		}

		return variables;
	}

}
