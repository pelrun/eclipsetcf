/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.cdt.interfaces;

import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.core.runtime.CoreException;

/**
 * Gdbserver launch delegate interface.
 */
public interface IGdbserverLaunchHandlerDelegate {

	/**
	 * Normalize the gdbserver launch failure details message.
	 *
	 * @param launch The launch. Must not be <code>null</code>
	 * @param details The details message or <code>null</code>.
	 *
	 * @return The normalized details message or <code>null</code>.
	 *
	 * @throws CoreException In case of an failure accessing any launch configuration attribute or similar.
	 */
	public String normalizeGdbserverLaunchFailureDetailsMessage( GdbLaunch launch, String details) throws CoreException;
}
