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
 * Interface to be implemented by a launch configuration type binding element.
 */
public interface ILaunchBinding {

	/**
	 * Returns the id of the bound element.
	 */
	public String getId();

	/**
	 * Returns if or if not the given launch mode is handled by this binding.
	 * <p>
	 * <b>Note:</b> The launch mode is considered valid if <code>null</code> or an empty string is passed.
	 *
	 * @param mode The launch mode or <code>null</code>.
	 * @return <code>True</code> if the launch mode is valid, <code>false</code> otherwise.
	 */
	public boolean isValidLaunchMode(String mode);
}
