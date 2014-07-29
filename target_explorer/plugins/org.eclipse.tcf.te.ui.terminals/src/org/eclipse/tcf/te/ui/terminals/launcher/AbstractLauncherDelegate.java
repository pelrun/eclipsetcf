/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.launcher;

import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.tcf.te.runtime.extensions.ExecutableExtension;
import org.eclipse.tcf.te.ui.terminals.interfaces.ILauncherDelegate;

/**
 * Abstract launcher delegate implementation.
 */
public abstract class AbstractLauncherDelegate extends ExecutableExtension implements ILauncherDelegate {
	// The converted expression
	private Expression expression;
	// The hidden attribute
	private boolean hidden;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.extensions.ExecutableExtension#doSetInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	@Override
	public void doSetInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
	    super.doSetInitializationData(config, propertyName, data);

	    if (config == null) return;

		// Read the sub elements of the extension
		IConfigurationElement[] children = config.getChildren();
		// The "enablement" element is the only expected one
		if (children != null && children.length > 0) {
			expression = ExpressionConverter.getDefault().perform(children[0]);
		}

		// Read "hidden" attribute
		String value = config.getAttribute("hidden"); //$NON-NLS-1$
		if (value != null && !"".equals(value.trim())) { //$NON-NLS-1$
			hidden = Boolean.parseBoolean(value);
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.ILauncherDelegate#getEnablement()
	 */
	@Override
    public Expression getEnablement() {
		return expression;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.ILauncherDelegate#isHidden()
	 */
	@Override
	public boolean isHidden() {
	    return hidden;
	}
}
