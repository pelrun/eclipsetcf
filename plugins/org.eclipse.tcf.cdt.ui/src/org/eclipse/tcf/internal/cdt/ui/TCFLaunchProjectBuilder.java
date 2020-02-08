/*******************************************************************************
 * Copyright (c) 2016 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.launch.LaunchUtils;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.tcf.debug.ITCFLaunchProjectBuilder;
import org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate;

/**
 * Build CDT project.
 * This is an specialization of the default project build logic.
 * Unfortunately, CDT does not profile an API for building a project. See bug 313927.
 * See org.eclipse.cdt.launch.AbstractCLaunchDelegate2 for more details.
 */
public class TCFLaunchProjectBuilder implements ITCFLaunchProjectBuilder {

    @Override
    public boolean isSupportedProject(String name) {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
        return CCorePlugin.getDefault().getCoreModel().create(project) != null;
    }

    @Override
    public IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode) throws CoreException {
        String name = configuration.getAttribute(TCFLaunchDelegate.ATTR_PROJECT_NAME, "");
        if (name.length() == 0) return null;
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
        if (project == null) return null;
        ICProject c_project = CCorePlugin.getDefault().getCoreModel().create(project);
        if (c_project == null) return null;

        IProject[] ordered_projects = null;
        HashSet<IProject> project_set = new HashSet<IProject>();
        getReferencedProjectSet(c_project.getProject(), project_set);

        String[] ordered_names = ResourcesPlugin.getWorkspace().getDescription().getBuildOrder();
        if (ordered_names != null) {
            // Projects may not be in the build order but should still be built if selected
            ArrayList<IProject> unordered_list = new ArrayList<IProject>(project_set.size());
            ArrayList<IProject> ordered_list = new ArrayList<IProject>(project_set.size());
            unordered_list.addAll(project_set);

            for (String nm : ordered_names) {
                for (IProject proj : unordered_list) {
                    if (proj.getName().equals(nm)) {
                        ordered_list.add(proj);
                        unordered_list.remove(proj);
                        break;
                    }
                }
            }

            // Add any remaining projects to the end of the list
            ordered_list.addAll(unordered_list);
            ordered_projects = ordered_list.toArray(new IProject[ordered_list.size()]);
        }
        else {
            // Try the project prerequisite order then
            IProject[] projects = project_set.toArray(new IProject[project_set.size()]);
            ordered_projects = ResourcesPlugin.getWorkspace().computeProjectOrder(projects).projects;
        }
        return ordered_projects;
    }

    /* Recursively creates a set of projects referenced by a project */
    private void getReferencedProjectSet(IProject proj, Set<IProject> set) throws CoreException {
        set.add(proj);
        for (IProject ref : proj.getReferencedProjects()) {
            if (ref.exists() && !set.contains(ref)) {
                getReferencedProjectSet(ref, set);
            }
        }
    }

    /**
     * Builds the project referenced in the launch configuration
     */
    public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
        try {
            SubMonitor submon = SubMonitor.convert(monitor, "", 1); //$NON-NLS-1$

            String name = configuration.getAttribute(TCFLaunchDelegate.ATTR_PROJECT_NAME, "");
            if (name.length() == 0) return true;
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
            if (project == null) return true;

            String buildConfigID = null;

            if (configuration.getAttribute(TCFLaunchDelegate.ATTR_PROJECT_BUILD_CONFIG_AUTO, false)) {
                String program_path = configuration.getAttribute(TCFLaunchDelegate.ATTR_LOCAL_PROGRAM_FILE, ""); //$NON-NLS-1$
                program_path = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(program_path);
                ICConfigurationDescription buildConfig = LaunchUtils.getBuildConfigByProgramPath(project, program_path);
                if (buildConfig != null) buildConfigID = buildConfig.getId();
            }

            if (buildConfigID == null) buildConfigID = configuration.getAttribute(TCFLaunchDelegate.ATTR_PROJECT_BUILD_CONFIG_ID, (String)null);

            // There's no guarantee the ID stored in the launch config is valid.
            // The user may have deleted the build configuration.
            if (buildConfigID != null) {
                boolean idIsGood = false;
                ICProjectDescription desc = CCorePlugin.getDefault().getProjectDescription(project, false);
                if (desc != null) idIsGood = desc.getConfigurationById(buildConfigID) != null;
                if (!idIsGood) buildConfigID = null;   // use active configuration
            }

            buildProject(project, buildConfigID, submon.newChild(1));
            return false;
        }
        finally {
            if (monitor != null) monitor.done();
        }
    }

    /**
     * This is an specialization of the platform method
     * LaunchConfigurationDelegate#buildProjects(IProject[], IProgressMonitor).
     * It builds only one project and it builds a particular CDT build
     * configuration of it. It was added to address bug 309126 and 312709
     */
    public void buildProject(final IProject project, final String buildConfigID, IProgressMonitor monitor) throws CoreException {
        final int TOTAL_TICKS = 1000;

        // Some day, this will hopefully be a simple pass-thru to a cdt.core
        // utility. See bug 313927

        IWorkspaceRunnable build = new IWorkspaceRunnable(){
            @Override
            public void run(IProgressMonitor pm) throws CoreException {
                SubMonitor localmonitor = SubMonitor.convert(pm, "", TOTAL_TICKS); //$NON-NLS-1$
                try {
                    // Number of times we'll end up calling IProject.build()
                    final int buildCount = (buildConfigID == null) ? 1 : project.getDescription().getBuildSpec().length;
                    if (buildCount == 0) return; // the case for an imported-executable project; see bugzilla 315396
                    final int subtaskTicks = TOTAL_TICKS / buildCount;

                    if (buildConfigID != null) {
                        // Build a specific configuration

                        // To pass args, we have to specify the builder name.
                        // There can be multiple so this can require multiple
                        // builds. Note that this happens under the covers in
                        // the 'else' (args-less) case below
                        Map<String,String> cfgIdArgs = cfgIdsToMap(new String[] {buildConfigID}, new HashMap<String,String>());
                        cfgIdArgs.put(CONTENTS, CONTENTS_CONFIGURATION_IDS);
                        ICommand[] commands = project.getDescription().getBuildSpec();
                        assert buildCount == commands.length;
                        for (ICommand command : commands) {
                            Map<String, String> args = command.getArguments();
                            if (args == null) {
                                args = new HashMap<String, String>(cfgIdArgs);
                            }
                            else {
                                args.putAll(cfgIdArgs);
                            }

                            if (localmonitor.isCanceled()) {
                                throw new OperationCanceledException();
                            }
                            project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, command.getBuilderName(), args, localmonitor.newChild(subtaskTicks));
                        }
                    }
                    else {
                        // Build the active configuration
                        project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, localmonitor.newChild(subtaskTicks));
                    }
                }
                finally {
                    if (pm != null) pm.done();
                }
            }
        };
        ResourcesPlugin.getWorkspace().run(build, new SubProgressMonitor(monitor, TOTAL_TICKS, 0) {
            private boolean cancelled;
            @Override
            public void setCanceled(boolean b) {
                // Only cancel this operation, not the top-level launch.
                cancelled = b;
            }
            @Override
            public boolean isCanceled() {
                // Canceled if this monitor has been explicitly canceled
                //  || parent has been canceled.
                return cancelled || super.isCanceled();
            }
        });
    }

    /** TODO: Temporarily duplicated from BuilderFactory. Remove when 313927 is addressed */
    private static final String CONFIGURATION_IDS = "org.eclipse.cdt.make.core.configurationIds"; //$NON-NLS-1$

    /** TODO: Temporarily duplicated from BuilderFactory. Remove when 313927 is addressed */
    private static final String CONTENTS = "org.eclipse.cdt.make.core.contents"; //$NON-NLS-1$

    /** TODO: Temporarily duplicated from BuilderFactory. Remove when 313927 is addressed */
    private static final String CONTENTS_CONFIGURATION_IDS = "org.eclipse.cdt.make.core.configurationIds"; //$NON-NLS-1$

    /** TODO: Temporarily duplicated from BuilderFactory. Remove when 313927 is addressed */
    private static Map<String, String> cfgIdsToMap(String ids[], Map<String, String> map){
        map.put(CONFIGURATION_IDS, encodeList(Arrays.asList(ids)));
        return map;
    }

    /** TODO: Temporarily duplicated from BuilderFactory. Remove when 313927 is addressed */
    private static String encodeList(List<String> values) {
        StringBuffer str = new StringBuffer();
        Iterator<String> entries = values.iterator();
        while (entries.hasNext()) {
            String entry = entries.next();
            str.append(escapeChars(entry, "|\\", '\\')); //$NON-NLS-1$
            str.append("|"); //$NON-NLS-1$
        }
        return str.toString();
    }

    /** TODO: Temporarily duplicated from BuilderFactory. Remove when 313927 is addressed */
    private static String escapeChars(String string, String escapeChars, char escapeChar) {
        StringBuffer str = new StringBuffer(string);
        for (int i = 0; i < str.length(); i++) {
            if (escapeChars.indexOf(str.charAt(i)) != -1) {
                str.insert(i, escapeChar);
                i++;
            }
        }
        return str.toString();
    }
}
