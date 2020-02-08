/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.interfaces.steps;

/**
 * Defines filesystem related step data attribute id's.
 */
public interface IFileSystemStepAttributes {

    /**
     * Define the prefix used by all other attribute id's as prefix.
     */
    public static final String ATTR_PREFIX = "org.eclipse.tcf.te.tcf.filesystem.core"; //$NON-NLS-1$

	/**
	 * The file transfer item the stepper is currently operating with.
	 */
	public static final String ATTR_FILE_TRANSFER_ITEM = ATTR_PREFIX + ".file_transfer_item"; //$NON-NLS-1$
}
