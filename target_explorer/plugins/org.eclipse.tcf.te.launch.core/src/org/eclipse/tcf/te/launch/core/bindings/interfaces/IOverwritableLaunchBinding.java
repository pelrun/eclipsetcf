/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.core.bindings.interfaces;

/**
 * Interface to be implemented by a overwritable launch configuration type binding element.
 */
public interface IOverwritableLaunchBinding extends ILaunchBinding {

	/**
	 * Returns if or if not this binding overwrites the given bindings id.
	 *
	 * @param id The id. Must not be <code>null</code>.
	 * @return <code>True</code> if this binding overwrites the given bindings id, <code>false</code> if not.
	 */
	public boolean overwrites(String id);
}
