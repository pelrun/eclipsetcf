/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.notifications.preferences;

import org.eclipse.tcf.te.ui.notifications.activator.UIPlugin;


/**
 * Preference key identifiers.
 */
public interface IPreferenceKeys {
	/**
	 * Common prefix for all core preference keys
	 */
	public final String PREFIX = UIPlugin.getUniqueIdentifier();

	/**
	 * If set to <code>true</code>, the notifications service is enabled.
	 */
	public static final String PREF_SERVICE_ENABLED = PREFIX + ".service.enabled"; //$NON-NLS-1$

}
