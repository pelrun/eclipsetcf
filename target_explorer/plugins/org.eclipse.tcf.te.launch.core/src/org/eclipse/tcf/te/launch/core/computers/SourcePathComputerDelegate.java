/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.core.computers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.tcf.te.core.cdt.CdtUtils;

/**
 * Computes default source path containers based on the CDT preferences.
 */
public class SourcePathComputerDelegate implements ISourcePathComputerDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate#computeSourceContainers(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		List<ISourceContainer> containers = new ArrayList<ISourceContainer>();

		// Get the default CDT source lookup director to access the default source lookup
		// settings from the preferences
		ISourceLookupDirector director = CdtUtils.getDefaultSourceLookupDirector();
		if (director != null) {
			// Get the default source lookup containers
			ISourceContainer[] candidates = director.getSourceContainers();
			for (ISourceContainer candidate : candidates) {
				// Bug #440538, WB4-4384: Ignore all default source containers
				// except "Path Mapping" type containers.
				if ("MappingSourceContainer".equals(candidate.getClass().getSimpleName())) { //$NON-NLS-1$
					// Add the source container to the list of containers
					if (!containers.contains(candidate)) containers.add(candidate);
				}
			}
		}

		return containers.toArray(new ISourceContainer[containers.size()]);
	}

}
