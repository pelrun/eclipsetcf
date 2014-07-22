/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.core.computers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.WorkspaceSourceContainer;
import org.eclipse.tcf.te.core.cdt.CdtUtils;
import org.eclipse.tcf.te.launch.core.interfaces.IReferencedProjectItem;
import org.eclipse.tcf.te.launch.core.persistence.projects.ReferencedProjectsPersistenceDelegate;

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
				// CProjectSourceContainer instances does not make sense for our launches
				// as the container won't be able to identify the correct project. We
				// are going to replace this source lookup container with ordinary project
				// source lookup containers based on our referenced projects.
				if ("CProjectSourceContainer".equals(candidate.getClass().getSimpleName())) { //$NON-NLS-1$
					IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

					// Get the list of referenced projects
					IReferencedProjectItem[] items = ReferencedProjectsPersistenceDelegate.getReferencedProjects(configuration);
					for (IReferencedProjectItem item : items) {
						IProject project = root.getProject(item.getProjectName());
						if (project != null && project.isAccessible()) {
							ISourceContainer container = new ProjectSourceContainer(project, true);
							if (!containers.contains(container)) containers.add(container);
						}
					}

					continue;
				}

				// Just add the source container to the list of containers
				if (!containers.contains(candidate)) containers.add(candidate);
			}
		}

		// Fallback to the whole workspace if there are no default source
		// lookup containers from the preferences
		if (containers.size() == 0) {
			containers.add(new WorkspaceSourceContainer());
		}

		return containers.toArray(new ISourceContainer[containers.size()]);
	}

}
