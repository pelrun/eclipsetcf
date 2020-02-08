/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.extensions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.tcf.te.ui.views.internal.extensions.AbstractEditorPageBindingsElement;

/**
 * Editor page binding implementation.
 */
public class EditorPageBinding extends AbstractEditorPageBindingsElement {
	// The insertBefore element
	private String insertBefore;
	// The insertAfter element
	private String insertAfter;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.extensions.ExecutableExtension#doSetInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	@Override
	public void doSetInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
	    super.doSetInitializationData(config, propertyName, data);

		// Read the "insertBefore" attribute
		insertBefore = config != null ? config.getAttribute("insertBefore") : null; //$NON-NLS-1$
		// Read the "insertAfter" attribute
		insertAfter = config != null ? config.getAttribute("insertAfter") : null; //$NON-NLS-1$
	}

	/**
	 * Returns the &quot;insertBefore&quot; property for this binding.
	 *
	 * @return The &quot;insertBefore&quot; property or an empty string.
	 */
	public String getInsertBefore() {
		return insertBefore != null ? insertBefore.trim() : ""; //$NON-NLS-1$
	}

	/**
	 * Returns the &quot;insertAfter&quot; property for this binding.
	 *
	 * @return The &quot;insertAfter&quot; property or an empty string.
	 */
	public String getInsertAfter() {
		return insertAfter != null ? insertAfter.trim() : ""; //$NON-NLS-1$
	}
}
