/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.core.cdt;

import org.eclipse.cdt.debug.core.CDebugCorePlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;

/**
 * Provides CDT utilities.
 */
public final class CdtUtils {

    public static ISourceLookupDirector getDefaultSourceLookupDirector() throws CoreException {
        return CDebugCorePlugin.getDefault().getCommonSourceLookupDirector();
    }
}
