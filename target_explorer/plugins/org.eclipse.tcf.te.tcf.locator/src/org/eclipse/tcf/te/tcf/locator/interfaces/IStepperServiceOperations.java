/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.locator.interfaces;

/**
 * IStepperServiceOperations
 */
public interface IStepperServiceOperations {

	public static final String CONNECT = "connect";  //$NON-NLS-1$
	public static final String DISCONNECT = "disconnect";  //$NON-NLS-1$
	public static final String CONNECTION_LOST = "connectionLost";  //$NON-NLS-1$
	public static final String CONNECTION_RECOVERING = "connectionRecovering";  //$NON-NLS-1$
}
