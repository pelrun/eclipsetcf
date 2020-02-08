/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.locator.interfaces;

/**
 * IStepGroupIds
 */
public interface IStepGroupIds {

	public static final String CONNECT = "org.eclipse.tcf.te.tcf.locator.connectStepGroup";  //$NON-NLS-1$
	public static final String DISCONNECT = "org.eclipse.tcf.te.tcf.locator.disconnectStepGroup";  //$NON-NLS-1$
	public static final String CONNECTON_LOST = "org.eclipse.tcf.te.tcf.locator.connectionLostStepGroup";  //$NON-NLS-1$
	public static final String CONNECTION_RECOVERING = "org.eclipse.tcf.te.tcf.locator.connectionRecoveringStepGroup";  //$NON-NLS-1$
}
