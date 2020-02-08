/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.controls.interfaces;

import org.eclipse.jface.operation.IRunnableContext;

/**
 * Public interface of a runnable context provider.
 */
public interface IRunnableContextProvider {

	/**
	 * Returns the associated runnable context.
	 *
	 * @return The runnable context or <code>null</code> if none.
	 */
	public IRunnableContext getRunnableContext();
}
