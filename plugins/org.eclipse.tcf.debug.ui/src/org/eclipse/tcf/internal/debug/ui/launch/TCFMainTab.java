/*******************************************************************************
 * Copyright (c) 2008-2020 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.launch;

import java.io.File;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.debug.ui.ITCFLaunchContext;
import org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class TCFMainTab extends AbstractLaunchConfigurationTab {

    private static final String LAUNCHING_PREFERENCE_PAGE_ID = "org.eclipse.debug.ui.LaunchingPreferencePage"; //$NON-NLS-1$

    private Text project_text;
    private Text local_program_text;
    private Text remote_program_text;
    private Text working_dir_text;
    private Button disable_build;
    private Button workspace_build;
    private Button default_dir_button;
    private Button attach_children_button;
    private Button stop_at_entry_button;
    private Button stop_at_main_button;
    private Button disconnect_on_ctx_exit;
    private Button terminal_button;
    private Button filter_button;
    private Combo build_config_combo;
    private Link workpsace_link;
    private Exception init_error;

    protected boolean isLocal() {
        return false;
    }

    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        setControl(comp);

        GridLayout topLayout = new GridLayout();
        comp.setLayout(topLayout);

        createVerticalSpacer(comp, 1);
        createHeader(comp);
        createVerticalSpacer(comp, 1);
        createProjectGroup(comp);
        createBuildGroup(comp);
        createApplicationGroup(comp);
        createWorkingDirGroup(comp);
        createVerticalSpacer(comp, 1);
        createOptionButtons(comp, 1);
    }

    private void createHeader(Composite parent) {
        Label label = new Label(parent, SWT.NONE);
        label.setText("Launching an application requires a target that supports Processes service");
        GridData gd = new GridData();
        label.setLayoutData(gd);
    }

    private void createProjectGroup(Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setText("Project");

        Label label = new Label(group, SWT.NONE);
        label.setText("Project Name:");
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);

        project_text = new Text(group, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        project_text.setLayoutData(gd);
        project_text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent evt) {
                updateBuildConfigCombo(false, null);
                updateLaunchConfigurationDialog();
            }
        });

        Button project_button = createPushButton(group, "Browse...", null);
        project_button.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent evt) {
                handleProjectButtonSelected();
                updateLaunchConfigurationDialog();
            }
        });
    }

    private void createBuildGroup(Composite parent) {
        final Shell shell = parent.getShell();
        Group group = new Group(parent, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setText("Build (if required) before launching");
        createBuildConfigCombo(group, layout.numColumns);
        disable_build = new Button(group, SWT.RADIO);
        disable_build.setText("Disable auto build");
        disable_build.setToolTipText("Requires manually building project before launching");
        disable_build.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });
        workspace_build = new Button(group, SWT.RADIO);
        workspace_build.setText("Use workspace settings");
        workspace_build.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });
        workpsace_link = new Link(group, SWT.NONE);
        workpsace_link.setText("<a>Configure Workspace Settings...</a>");
        workpsace_link.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                PreferencesUtil.createPreferenceDialogOn(
                        shell,
                        LAUNCHING_PREFERENCE_PAGE_ID,
                        null,
                        null).open();
            }
        });
    }

    protected void createBuildConfigCombo(Composite parent, int colspan) {
        Composite comp = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        comp.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = colspan;
        comp.setLayoutData(gd);
        Label label = new Label(comp, SWT.NONE);
        label.setText("Build configuration:");
        build_config_combo = new Combo(comp, SWT.READ_ONLY | SWT.DROP_DOWN);
        build_config_combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        build_config_combo.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateLaunchConfigurationDialog();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                updateLaunchConfigurationDialog();
            }
        });
    }

    private void createApplicationGroup(Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setText("Application");

        createLocalExeFileGroup(group);
        if (!isLocal()) createRemoteExeFileGroup(group);
    }

    private void createLocalExeFileGroup(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        comp.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        comp.setLayoutData(gd);

        Label program_label = new Label(comp, SWT.NONE);
        if (isLocal()) {
            program_label.setText("Application File Path:");
        }
        else {
            program_label.setText("Local File Path:");
        }
        gd = new GridData();
        gd.horizontalSpan = 3;
        program_label.setLayoutData(gd);

        local_program_text = new Text(comp, SWT.SINGLE | SWT.BORDER);
        local_program_text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        local_program_text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });

        Button search_button = createPushButton(comp, "Search...", null);
        search_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                handleSearchButtonSelected();
                updateLaunchConfigurationDialog();
            }
        });

        Button browse_button = createPushButton(comp, "Browse...", null);
        browse_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                handleBinaryBrowseButtonSelected();
                updateLaunchConfigurationDialog();
            }
        });
    }

    private void createRemoteExeFileGroup(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        comp.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        comp.setLayoutData(gd);

        Label program_label = new Label(comp, SWT.NONE);
        program_label.setText("Remote File Path:");
        gd = new GridData();
        gd.horizontalSpan = 3;
        program_label.setLayoutData(gd);

        remote_program_text = new Text(comp, SWT.SINGLE | SWT.BORDER);
        remote_program_text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        remote_program_text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });
    }

    private void createWorkingDirGroup(Composite comp) {
        Group group = new Group(comp, SWT.NONE);
        GridLayout layout = new GridLayout();
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setText("Working directory");

        working_dir_text = new Text(group, SWT.SINGLE | SWT.BORDER);
        working_dir_text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        working_dir_text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });

        default_dir_button = new Button(group, SWT.CHECK);
        default_dir_button.setText("Use default");
        default_dir_button.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        default_dir_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });
    }

    private void createOptionButtons(Composite parent, int col_span) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout terminal_layout = new GridLayout();
        terminal_layout.numColumns = 1;
        terminal_layout.marginHeight = 0;
        terminal_layout.marginWidth = 0;
        composite.setLayout(terminal_layout);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = col_span;
        composite.setLayoutData(gd);

        attach_children_button = createCheckButton(composite, "Auto-attach process children");
        attach_children_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });
        attach_children_button.setEnabled(true);

        stop_at_entry_button = createCheckButton(composite, "Stop at program entry");
        stop_at_entry_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });
        stop_at_entry_button.setEnabled(true);

        stop_at_main_button = createCheckButton(composite, "Stop at 'main'");
        stop_at_main_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });
        stop_at_main_button.setEnabled(true);

        disconnect_on_ctx_exit = createCheckButton(composite, "Disconnect when last debug context exits");
        disconnect_on_ctx_exit.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });
        disconnect_on_ctx_exit.setEnabled(true);

        terminal_button = createCheckButton(composite, "Use pseudo-terminal for process standard I/O");
        terminal_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });
        terminal_button.setEnabled(true);

        filter_button = createCheckButton(composite, "Hide debug contexts started by other debug sessions");
        filter_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });
        filter_button.setEnabled(true);
    }

    @Override
    protected void updateLaunchConfigurationDialog() {
        super.updateLaunchConfigurationDialog();
        working_dir_text.setEnabled(!default_dir_button.getSelection());
    }

    protected void updateBuildConfigCombo(boolean auto, String selection) {
        int selection_index = auto ? 1 : 0;
        build_config_combo.removeAll();
        build_config_combo.add("Use Active");
        build_config_combo.add("Select Automatically");
        IProject project = getProject();
        if (project != null) {
            ITCFLaunchContext launch_context = TCFLaunchContext.getLaunchContext(project);
            if (launch_context != null) {
                Map<String,String> map = launch_context.getBuildConfigIDs(project);
                if (map != null) {
                    int cnt = 2;
                    for (String id : map.keySet()) {
                        build_config_combo.add(map.get(id));
                        if (id.equals(selection)) selection_index = cnt;
                        build_config_combo.setData(Integer.toString(cnt++), id);
                    }
                }
            }
        }
        build_config_combo.select(selection_index);
    }

    public void initializeFrom(ILaunchConfiguration config) {
        setErrorMessage(null);
        setMessage(null);
        try {
            project_text.setText(config.getAttribute(TCFLaunchDelegate.ATTR_PROJECT_NAME, ""));
            boolean auto = config.getAttribute(TCFLaunchDelegate.ATTR_PROJECT_BUILD_CONFIG_AUTO, false);
            updateBuildConfigCombo(auto, config.getAttribute(TCFLaunchDelegate.ATTR_PROJECT_BUILD_CONFIG_ID, (String)null));
            int build = TCFLaunchDelegate.BUILD_BEFORE_LAUNCH_USE_WORKSPACE_SETTING;
            build = config.getAttribute(TCFLaunchDelegate.ATTR_BUILD_BEFORE_LAUNCH, build);
            disable_build.setSelection(build == TCFLaunchDelegate.BUILD_BEFORE_LAUNCH_DISABLED);
            workspace_build.setSelection(build == TCFLaunchDelegate.BUILD_BEFORE_LAUNCH_USE_WORKSPACE_SETTING);
            local_program_text.setText(config.getAttribute(TCFLaunchDelegate.ATTR_LOCAL_PROGRAM_FILE, ""));
            if (remote_program_text != null) remote_program_text.setText(config.getAttribute(TCFLaunchDelegate.ATTR_REMOTE_PROGRAM_FILE, ""));
            working_dir_text.setText(config.getAttribute(TCFLaunchDelegate.ATTR_WORKING_DIRECTORY, ""));
            default_dir_button.setSelection(!config.hasAttribute(TCFLaunchDelegate.ATTR_WORKING_DIRECTORY));
            attach_children_button.setSelection(config.getAttribute(TCFLaunchDelegate.ATTR_ATTACH_CHILDREN, true));
            stop_at_entry_button.setSelection(config.getAttribute(TCFLaunchDelegate.ATTR_STOP_AT_ENTRY, true));
            stop_at_main_button.setSelection(config.getAttribute(TCFLaunchDelegate.ATTR_STOP_AT_MAIN, true));
            disconnect_on_ctx_exit.setSelection(config.getAttribute(TCFLaunchDelegate.ATTR_DISCONNECT_ON_CTX_EXIT, true));
            terminal_button.setSelection(config.getAttribute(TCFLaunchDelegate.ATTR_USE_TERMINAL, true));
            filter_button.setSelection(config.getAttribute(TCFLaunchDelegate.ATTR_USE_CONTEXT_FILTER, true));
            working_dir_text.setEnabled(!default_dir_button.getSelection());
        }
        catch (Exception e) {
            init_error = e;
            setErrorMessage("Cannot read launch configuration: " + e);
            Activator.log(e);
        }
    }

    private IProject getProject() {
        String name = project_text.getText().trim();
        if (name.length() == 0) return null;
        return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
    }

    public void performApply(ILaunchConfigurationWorkingCopy config) {
        config.setAttribute(TCFLaunchDelegate.ATTR_PROJECT_NAME, project_text.getText());
        int config_index = build_config_combo.getSelectionIndex();
        if (config_index == 0) {
            config.removeAttribute(TCFLaunchDelegate.ATTR_PROJECT_BUILD_CONFIG_ID);
            config.removeAttribute(TCFLaunchDelegate.ATTR_PROJECT_BUILD_CONFIG_AUTO);
        }
        else if (config_index == 1) {
            config.removeAttribute(TCFLaunchDelegate.ATTR_PROJECT_BUILD_CONFIG_ID);
            config.setAttribute(TCFLaunchDelegate.ATTR_PROJECT_BUILD_CONFIG_AUTO, true);
        }
        else {
            String config_id = (String)build_config_combo.getData(Integer.toString(config_index));
            config.setAttribute(TCFLaunchDelegate.ATTR_PROJECT_BUILD_CONFIG_ID, config_id);
            config.removeAttribute(TCFLaunchDelegate.ATTR_PROJECT_BUILD_CONFIG_AUTO);
        }
        int build = TCFLaunchDelegate.BUILD_BEFORE_LAUNCH_USE_WORKSPACE_SETTING;
        if (disable_build.getSelection()) build = TCFLaunchDelegate.BUILD_BEFORE_LAUNCH_DISABLED;
        config.setAttribute(TCFLaunchDelegate.ATTR_BUILD_BEFORE_LAUNCH, build);
        config.setAttribute(TCFLaunchDelegate.ATTR_LOCAL_PROGRAM_FILE, local_program_text.getText());
        if (isLocal()) {
            config.removeAttribute(TCFLaunchDelegate.ATTR_PEER_ID);
            config.setAttribute(TCFLaunchDelegate.ATTR_RUN_LOCAL_SERVER, false);
            config.setAttribute(TCFLaunchDelegate.ATTR_RUN_LOCAL_AGENT, true);
            config.setAttribute(TCFLaunchDelegate.ATTR_USE_LOCAL_AGENT, true);
            config.removeAttribute(TCFLaunchDelegate.ATTR_REMOTE_PROGRAM_FILE);
        }
        else {
            config.setAttribute(TCFLaunchDelegate.ATTR_REMOTE_PROGRAM_FILE, remote_program_text.getText());
        }
        if (default_dir_button.getSelection()) {
            config.removeAttribute(TCFLaunchDelegate.ATTR_WORKING_DIRECTORY);
        }
        else {
            config.setAttribute(TCFLaunchDelegate.ATTR_WORKING_DIRECTORY, working_dir_text.getText());
        }
        config.setAttribute(TCFLaunchDelegate.ATTR_ATTACH_CHILDREN, attach_children_button.getSelection());
        config.setAttribute(TCFLaunchDelegate.ATTR_STOP_AT_ENTRY, stop_at_entry_button.getSelection());
        config.setAttribute(TCFLaunchDelegate.ATTR_STOP_AT_MAIN, stop_at_main_button.getSelection());
        config.setAttribute(TCFLaunchDelegate.ATTR_DISCONNECT_ON_CTX_EXIT, disconnect_on_ctx_exit.getSelection());
        config.setAttribute(TCFLaunchDelegate.ATTR_USE_TERMINAL, terminal_button.getSelection());
        if (filter_button.getSelection()) {
            config.removeAttribute(TCFLaunchDelegate.ATTR_USE_CONTEXT_FILTER);
        }
        else {
            config.setAttribute(TCFLaunchDelegate.ATTR_USE_CONTEXT_FILTER, false);
        }
    }

    /**
     * Show a dialog that lists all executable files in currently selected project.
     */
    private void handleSearchButtonSelected() {
        IProject project = getProject();
        if (project == null) {
            MessageDialog.openInformation(getShell(),
                    "Project required",
                    "Enter project before searching for program");
            return;
        }
        ITCFLaunchContext launch_context = TCFLaunchContext.getLaunchContext(project);
        if (launch_context == null) return;
        String path = launch_context.chooseBinary(getShell(), project);
        if (path != null) local_program_text.setText(path);
    }

    /**
     * Show a dialog that lets the user select a project. This in turn provides context for the main
     * type, allowing the user to key a main type name, or constraining the search for main types to
     * the specified project.
     */
    private void handleBinaryBrowseButtonSelected() {
        FileDialog file_dialog = new FileDialog(getShell(), SWT.NONE);
        file_dialog.setFileName(local_program_text.getText());
        String path = file_dialog.open();
        if (path != null) local_program_text.setText(path);
    }

    /**
     * Show a dialog that lets the user select a project. This in turn provides context for the main
     * type, allowing the user to key a main type name, or constraining the search for main types to
     * the specified project.
     */
    private void handleProjectButtonSelected() {
        try {
            IProject project = chooseProject();
            if (project == null) return;
            project_text.setText(project.getName());
        }
        catch (Exception e) {
            Activator.log("Cannot get project description", e);
        }
    }

    /**
     * Show project list dialog and return the first selected project, or null.
     */
    private IProject chooseProject() {
        try {
            IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
            ILabelProvider label_provider = new LabelProvider() {

                @Override
                public String getText(Object element) {
                    if (element == null) return "";
                    return ((IProject)element).getName();
                }
            };
            ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), label_provider);
            dialog.setTitle("Project Selection");
            dialog.setMessage("Choose project to constrain search for program");
            dialog.setElements(projects);

            IProject cProject = getProject();
            if (cProject != null) dialog.setInitialSelections(new Object[]{cProject});
            if (dialog.open() == Window.OK) return (IProject)dialog.getFirstResult();
        }
        catch (Exception e) {
            Activator.log("Cannot show project list dialog", e);
        }
        return null;
    }

    @Override
    public boolean isValid(ILaunchConfiguration config) {
        setErrorMessage(null);
        setMessage(null);

        if (init_error != null) {
            setErrorMessage("Cannot read launch configuration: " + init_error);
            return false;
        }

        String project_name = project_text.getText().trim();
        if (project_name.length() != 0) {
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(project_name);
            if (!project.exists()) {
                setErrorMessage("Project does not exist");
                return false;
            }
            if (!project.isOpen()) {
                setErrorMessage("Project must be opened");
                return false;
            }
        }
        String local_name = local_program_text.getText().trim();
        if (local_name.equals(".") || local_name.equals("..")) { //$NON-NLS-1$ //$NON-NLS-2$
            setErrorMessage("Invalid local program name");
            return false;
        }
        if (remote_program_text != null) {
            String remote_name = remote_program_text.getText().trim();
            if (remote_name.equals(".") || remote_name.equals("..")) { //$NON-NLS-1$ //$NON-NLS-2$
                setErrorMessage("Invalid remote program name");
                return false;
            }
        }
        if (local_name.length() > 0) {
            IProject project = getProject();
            IPath program_path = new Path(local_name);
            if (!program_path.isAbsolute()) {
                if (project == null) {
                    File ws = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
                    File file = new File(ws, local_name);
                    if (!file.exists()) {
                        setErrorMessage("File not found: " + file);
                        return false;
                    }
                    if (file.isDirectory()) {
                        setErrorMessage("Program path is directory name: " + file);
                        return false;
                    }
                    program_path = new Path(file.getAbsolutePath());
                }
                else if (!project.getFile(local_name).exists()) {
                    setErrorMessage("Program does not exist");
                    return false;
                }
                else {
                    program_path = project.getFile(local_name).getLocation();
                }
            }
            else {
                File file = program_path.toFile();
                if (!file.exists()) {
                    setErrorMessage("Program file does not exist");
                    return false;
                }
                if (file.isDirectory()) {
                    setErrorMessage("Program path is directory name");
                    return false;
                }
            }
            if (project != null) {
                try {
                    ITCFLaunchContext launch_context = TCFLaunchContext.getLaunchContext(project);
                    if (launch_context != null && !launch_context.isBinary(project, program_path)) {
                        setErrorMessage("Program is not a recongnized executable");
                        return false;
                    }
                }
                catch (CoreException e) {
                    Activator.log(e);
                    setErrorMessage(e.getLocalizedMessage());
                    return false;
                }
            }
        }
        return true;
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy config) {
        config.setAttribute(TCFLaunchDelegate.ATTR_PROJECT_NAME, "");
        config.setAttribute(TCFLaunchDelegate.ATTR_ATTACH_CHILDREN, true);
        config.setAttribute(TCFLaunchDelegate.ATTR_STOP_AT_ENTRY, true);
        config.setAttribute(TCFLaunchDelegate.ATTR_STOP_AT_MAIN, true);
        config.setAttribute(TCFLaunchDelegate.ATTR_DISCONNECT_ON_CTX_EXIT, true);
        config.setAttribute(TCFLaunchDelegate.ATTR_USE_TERMINAL, true);
        config.removeAttribute(TCFLaunchDelegate.ATTR_USE_CONTEXT_FILTER);
        config.removeAttribute(TCFLaunchDelegate.ATTR_WORKING_DIRECTORY);
        ITCFLaunchContext launch_context = TCFLaunchContext.getLaunchContext(null);
        if (launch_context != null) launch_context.setDefaults(getLaunchConfigurationDialog(), config);
    }

    public String getName() {
        return "Application";
    }

    @Override
    public Image getImage() {
        return ImageCache.getImage(ImageCache.IMG_APPLICATION_TAB);
    }
}
