/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.core.lm.interfaces;

/**
 * Defines the launch configuration attribute id's to access the launch step contexts.
 */
public interface IFileTransferLaunchAttributes {

	/**
	 * Launch configuration attribute: The file transfer items the launch is operating with. Use
	 * 								   {@link FileTransferPersistanceDelegate} to access
	 * 								   this attribute within a launch configuration.
	 */
	public static final String ATTR_FILE_TRANSFERS = ICommonLaunchAttributes.ATTR_PREFIX + ".file_transfers"; //$NON-NLS-1$
}
