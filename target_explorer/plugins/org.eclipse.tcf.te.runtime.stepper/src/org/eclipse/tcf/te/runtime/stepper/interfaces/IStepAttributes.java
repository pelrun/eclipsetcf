/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.stepper.interfaces;

/**
 * Defines common step data attribute id's.
 */
public interface IStepAttributes {

	/**
	 * Define the prefix used by all other attribute id's as prefix.
	 */
	public static final String ATTR_PREFIX = "org.eclipse.tcf.te.runtime.stepper"; //$NON-NLS-1$

	/**
	 * The active context the launch is operating with.
	 */
	public static final String ATTR_ACTIVE_CONTEXT = ATTR_PREFIX + ".active_context"; //$NON-NLS-1$
}