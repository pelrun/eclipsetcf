/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.core.selection.interfaces;

import org.eclipse.core.resources.IProject;

/**
 * A selection context providing the project context for the launch.
 */
public interface IProjectSelectionContext extends ISelectionContext {

	/**
	 * Returns the project context.
	 *
	 * @return The project context or <code>null</code>.
	 */
	public IProject getProjectCtx();
}
