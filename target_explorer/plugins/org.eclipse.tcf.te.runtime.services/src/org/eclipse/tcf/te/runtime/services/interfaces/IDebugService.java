/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.services.interfaces;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;

/**
 * Debug service.
 * <p>
 * Allow to start and control the debugger for a set of given debug contexts.
 */
public interface IDebugService extends IService {

	public static final String PROPERTY_DEBUGGER_DETACHED = "debuggerDetached"; //$NON-NLS-1$

	/**
	 * Launches a debug session for the given context and attaches to it. The attach
	 * can be parameterized via the data properties.
	 *
	 * @param context The debug context. Must not be <code>null</code>.
	 * @param data The data properties to parameterize the attach. Must not be <code>null</code>.
	 * @param monitor The progress monitor.
	 * @param callback The callback to invoke once the operation completed. Must not be <code>null</code>.
	 */
	public void attach(Object context, IPropertiesContainer data, IProgressMonitor monitor, ICallback callback);

	/**
	 * Terminates a debug session for the given context and detaches it. The detach
	 * can be parameterized via the data properties.
	 *
	 * @param context The debug context. Must not be <code>null</code>.
	 * @param data The data properties to parameterize the detach. Must not be <code>null</code>.
	 * @param monitor The progress monitor.
	 * @param callback The callback to invoke once the operation completed. Must not be <code>null</code>.
	 */
	public void detach(Object context, IPropertiesContainer data, IProgressMonitor monitor, ICallback callback);

	/**
	 * Returns if or if not the debugger has been launched for the given context.
	 *
	 * @param context The debug context. Must not be <code>null</code>.
	 * @return <code>True</code> if the debugger has been launched for the context, <code>false</code> otherwise.
	 */
	public boolean isLaunched(Object context);
}
