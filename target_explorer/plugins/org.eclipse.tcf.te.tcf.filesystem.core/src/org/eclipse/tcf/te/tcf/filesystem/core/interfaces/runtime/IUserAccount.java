/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime;

public interface IUserAccount {

	/**
	 * Returns the effective user id of the remote agent
	 */
	int getEUID();

	/**
	 * Returns the effective group id of the remote agent
	 */
	int getEGID();
}
