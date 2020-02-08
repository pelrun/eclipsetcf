/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.core.cdt;

import org.eclipse.cdt.debug.core.CDebugCorePlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;

/**
 * Provides CDT utilities.
 */
@SuppressWarnings("deprecation")
public final class CdtUtils {

	/**
	 * Returns the default source lookup directory providing access to the default
	 * source lookup setting within the preferences.
	 *
	 * @return The default source lookup director.
	 * @throws CoreException In case the operation fails.
	 */
    public static ISourceLookupDirector getDefaultSourceLookupDirector() throws CoreException {
        return CDebugCorePlugin.getDefault().getCommonSourceLookupDirector();
    }

    /**
     * Provides access to the (deprecated) CDT debug core plugin preferences.
     *
     * @return The plugin preferences instance.
     */
    public static Preferences getDebugCorePluginPreferences() {
    	return CDebugCorePlugin.getDefault().getPluginPreferences();
    }
}
