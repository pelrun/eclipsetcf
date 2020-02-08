/*******************************************************************************
 * Copyright (c) 2007, 2014 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;

/**
 * Computes the default source lookup path for a TCF launch configuration.
 */
public class TCFSourcePathComputerDelegate implements ISourcePathComputerDelegate {

    public ISourceContainer[] computeSourceContainers(
            ILaunchConfiguration configuration, IProgressMonitor monitor)
            throws CoreException {
        // Bug 440538:
        // Eclipse platform source lookup containers don't work well with TCF
        // therefore we don't provide default source containers.
        return new ISourceContainer[0];
    }
}
