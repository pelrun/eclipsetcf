/**
 * VariableProvider.java
 * Created on 22.10.2012
 *
 * Copyright (c) 2012 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
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
