/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.interfaces.extensions;


/**
 * Executable extension public interface declaration.
 */
public interface IExecutableExtension extends org.eclipse.core.runtime.IExecutableExtension {

	/**
	 * Returns the unique id of the extension. The returned
	 * id must be never <code>null</code> or an empty string.
	 *
	 * @return The unique id.
	 */
	public String getId();

	/**
	 * Returns the label or UI name of the extension.
	 *
	 * @return The label or UI name. An empty string if not set.
	 */
	public String getLabel();

	/**
	 * Returns the description of the extension.
	 *
	 * @return The description or an empty string.
	 */
	public String getDescription();
}
