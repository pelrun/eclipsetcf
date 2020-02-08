/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.help;

import org.eclipse.tcf.te.tcf.launch.core.interfaces.ILaunchTypes;
import org.eclipse.tcf.te.tcf.launch.ui.activator.UIPlugin;


/**
 * Context help id definitions.
 */
public interface IContextHelpIds {

	/**
	 * UI plug-in common context help id prefix.
	 */
	public final static String PREFIX = UIPlugin.getUniqueIdentifier() + "."; //$NON-NLS-1$

	/**
	 * Remote Linux Application launch tab group context help id.
	 */
	public final static String REMOTE_LINUX_APPLICATION = ILaunchTypes.REMOTE_APPLICATION + ".tabGroup"; //$NON-NLS-1$

	/**
	 * Path map editor page: Apply path map failed.
	 */
	public final static String MESSAGE_APPLY_PATHMAP_FAILED = PREFIX + ".status.messageApplyPathMapFailed"; //$NON-NLS-1$
}
