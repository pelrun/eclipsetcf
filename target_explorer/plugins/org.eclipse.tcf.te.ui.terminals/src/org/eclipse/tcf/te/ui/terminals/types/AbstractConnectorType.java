/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.types;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.te.ui.terminals.activator.UIPlugin;
import org.eclipse.tcf.te.ui.terminals.interfaces.IConnectorType;
import org.eclipse.tcf.te.ui.terminals.nls.Messages;

/**
 * Abstract terminal connector type implementation.
 */
public abstract class AbstractConnectorType extends PlatformObject implements IConnectorType {
	// The mandatory id of the extension
	private String id = null;

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		Assert.isNotNull(config);

		// Initialize the id field by reading the <id> extension attribute.
		// Throws an exception if the id is empty or null.
		id = config.getAttribute("id"); //$NON-NLS-1$
		if (id == null || "".equals(id.trim())) { //$NON-NLS-1$
			throw createMissingMandatoryAttributeException("id", config.getContributor().getName()); //$NON-NLS-1$
		}
	}

	/**
	 * Creates a new {@link CoreException} to be thrown if a mandatory extension attribute
	 * is missing.
	 *
	 * @param attributeName The attribute name. Must not be <code>null</code>.
	 * @param extensionId The extension id. Must not be <code>null</code>.
	 *
	 * @return The {@link CoreException} instance.
	 */
	protected CoreException createMissingMandatoryAttributeException(String attributeName, String extensionId) {
		Assert.isNotNull(attributeName);
		Assert.isNotNull(extensionId);

		return new CoreException(new Status(IStatus.ERROR,
				UIPlugin.getUniqueIdentifier(),
				0,
				NLS.bind(Messages.Extension_error_missingRequiredAttribute, attributeName, extensionId),
				null));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.IConnectorType#getId()
	 */
	@Override
	public String getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AbstractConnectorType) {
			return id.equals(((AbstractConnectorType)obj).id);
		}
	    return super.equals(obj);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
	    return id.hashCode();
	}
}
