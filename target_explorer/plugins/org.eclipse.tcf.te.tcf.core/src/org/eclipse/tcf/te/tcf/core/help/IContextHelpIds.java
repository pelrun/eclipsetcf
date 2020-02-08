/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.help;

import org.eclipse.tcf.te.tcf.core.activator.CoreBundleActivator;

/**
 * Context help id definitions.
 */
public interface IContextHelpIds {

	/**
	 * Core plug-in common context help id prefix.
	 */
	public final static String PREFIX = CoreBundleActivator.getUniqueIdentifier() + "."; //$NON-NLS-1$

	/**
	 * Common context help id to signal once a generic job operation failed.
	 */
	public final static String MESSAGE_OPERATION_FAILED = PREFIX + ".status.operationFailed"; //$NON-NLS-1$
}
