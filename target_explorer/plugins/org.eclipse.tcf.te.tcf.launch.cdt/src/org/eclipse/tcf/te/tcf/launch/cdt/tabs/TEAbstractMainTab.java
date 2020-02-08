/*******************************************************************************
 * Copyright (c) 2015, 2018 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * Alexandru Dragut   - [535867] Make Path Mapping configurable in TCF Launch config UI
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.cdt.tabs;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.dsf.gdb.internal.ui.launching.CMainTab;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.core.interfaces.IPathMapResolverService;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.dialogs.FSOpenFileDialog;
import org.eclipse.tcf.te.tcf.launch.cdt.controls.TCFPeerSelector;
import org.eclipse.tcf.te.tcf.launch.cdt.interfaces.IRemoteTEConfigurationConstants;
import org.eclipse.tcf.te.tcf.launch.cdt.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.ui.controls.validator.NumberVerifyListener;

/**
 * Abstract custom main tab implementation.
 */
@SuppressWarnings("restriction")
public abstract class TEAbstractMainTab extends CMainTab {

	/* Labels and Error Messages */
	private static final String REMOTE_PROG_LABEL_TEXT = Messages.RemoteCMainTab_Program;
	private static final String SKIP_DOWNLOAD_BUTTON_TEXT = Messages.RemoteCMainTab_SkipDownload;
	private static final String REMOTE_PROG_TEXT_ERROR = Messages.RemoteCMainTab_ErrorNoProgram;
	private static final String CONNECTION_TEXT_ERROR = Messages.RemoteCMainTab_ErrorNoConnection;
	private static final String PRE_RUN_LABEL_TEXT = Messages.RemoteCMainTab_Prerun;
	private static final String PID_LABEL_TEXT = Messages.RemoteCMainTab_Pid;
	private static final String REMOTE_PROG_SYMBOLIC_TEXT_ERROR = Messages.RemoteCMainTab_ErrorSymbolicLink;
	private static final String REMOTE_USER_ID_LABEL_TEXT = Messages.RemoteCMainTab_RemoteUser_Label;

	protected TCFPeerSelector peerSelector;
	protected Label remoteProgLabel;
	protected Text remoteProgText;
	protected Button remoteBrowseButton;
	protected boolean remoteProgVisible = true;
	protected Button skipDownloadButton;
	protected boolean skipDownloadButtonVisible = true;

	protected boolean progTextFireNotification = true;
	protected boolean remoteProgTextFireNotification;
	protected boolean remoteProgValidation = true;

	protected Text preRunText;
	private Label preRunLabel;
	private Button preRunEditButton;
	private boolean preRunVisible = true;

	private Text userIdText;
	private Button userIdButton;

	private Label pidLabel;
	private Text pidText;
	private boolean pidVisible = false;

	public static final int NO_DOWNLOAD_GROUP = 2 << 8;
	public static final int NO_PRERUN_GROUP = 4 << 8;
	public static final int NO_REMOTE_PATH = 8 << 8;
	public static final int PID_GROUP = 16 << 8;

	/**
	 * Constructor
	 */
	public TEAbstractMainTab() {
		super();
	}

	/**
	 * Constructor
	 *
	 * @param flags The flags to configure the main tab.
	 */
	public TEAbstractMainTab(int flags) {
		super(flags);
		if ((flags & DONT_CHECK_PROGRAM) != 0) {
			remoteProgValidation = false;
		}
		if ((flags & NO_DOWNLOAD_GROUP) != 0) {
			skipDownloadButtonVisible = false;
		}
		if ((flags & NO_PRERUN_GROUP) != 0) {
			preRunVisible = false;
		}
		if ((flags & NO_REMOTE_PATH) != 0) {
			remoteProgVisible = false;
		}
		if ((flags & PID_GROUP) != 0) {
			pidVisible = true;
		}
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite comp = (Composite) getControl();
		/* The TE Connection dropdown */
		createVerticalSpacer(comp, 1);
		createRemoteConnectionGroup(comp);
		/* The remote binary location and skip download option */
		if (remoteProgVisible || preRunVisible || pidVisible) createVerticalSpacer(comp, 1);
		createTargetExePathGroup(comp);

		/* If the local binary path changes, modify the remote binary location */
		fProgText.addModifyListener(new ModifyListener() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void modifyText(ModifyEvent evt) {
				if (progTextFireNotification) {
					setRemotePathFromLocalPath();
				}
			}
		});
	}

	/*
	 * isValid
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid
	 */
	@Override
	public boolean isValid(ILaunchConfiguration config) {
		boolean retVal = super.isValid(config);
		if (retVal == true) {
			setErrorMessage(null);
			if (peerSelector.getPeerId() == null) {
				setErrorMessage(CONNECTION_TEXT_ERROR);
				retVal = false;
			}
			if (retVal && remoteProgValidation && remoteProgVisible) {
				String name = remoteProgText.getText().trim();
				if (name.length() == 0) {
					setErrorMessage(REMOTE_PROG_TEXT_ERROR);
					retVal = false;
				}
			}
		} else {
			try {
				if ( Files.isSymbolicLink(Paths.get(fProgText.getText())) ) {
					setErrorMessage(NLS.bind(REMOTE_PROG_SYMBOLIC_TEXT_ERROR, Files.readSymbolicLink(Paths.get(fProgText.getText()))));
				}
			} catch (Exception e) { /* Omitted on purpose */ }
		}
		return retVal;
	}

	protected void createRemoteConnectionGroup(Composite parent) {
		peerSelector = new TCFPeerSelector(parent, SWT.NONE, 2);
		peerSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		peerSelector.addModifyListener(new ModifyListener() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void modifyText(ModifyEvent e) {
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		});
	}

	/*
	 * createTargetExePath This creates the remote path user-editable textfield on the Main Tab.
	 */
	protected void createTargetExePathGroup(Composite parent) {
		if (!remoteProgVisible && !preRunVisible) return;

		Composite mainComp = new Composite(parent, SWT.NONE);
		GridLayout mainLayout = new GridLayout();
		mainLayout.numColumns = 2;
		mainLayout.marginHeight = 0;
		mainLayout.marginWidth = 0;
		mainComp.setLayout(mainLayout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		mainComp.setLayoutData(gd);

		if (remoteProgVisible) {
			remoteProgLabel = new Label(mainComp, SWT.NONE);
			remoteProgLabel.setText(REMOTE_PROG_LABEL_TEXT);
			gd = new GridData();
			gd.horizontalSpan = 2;
			remoteProgLabel.setLayoutData(gd);

			remoteProgText = new Text(mainComp, SWT.SINGLE | SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 1;
			remoteProgText.setLayoutData(gd);
			remoteProgText.addModifyListener(new ModifyListener() {

				@SuppressWarnings("synthetic-access")
				@Override
				public void modifyText(ModifyEvent evt) {
					if (remoteProgTextFireNotification) {
						setLocalPathFromRemotePath();
					}
					updateLaunchConfigurationDialog();
				}
			});
			remoteBrowseButton = createPushButton(mainComp, Messages.RemoteCMainTab_Remote_Path_Browse_Button, null);
			remoteBrowseButton.addSelectionListener(new SelectionAdapter() {

				@SuppressWarnings("synthetic-access")
				@Override
				public void widgetSelected(SelectionEvent evt) {
					handleRemoteBrowseSelected();
					updateLaunchConfigurationDialog();
				}
			});
		}

		if (skipDownloadButtonVisible) createDownloadOption(mainComp);

		if (pidVisible) {
			pidLabel = new Label(mainComp, SWT.NONE);
			pidLabel.setText(PID_LABEL_TEXT);
			gd = new GridData();
			gd.horizontalSpan = 2;
			pidLabel.setLayoutData(gd);

			pidText = new Text(mainComp, SWT.SINGLE | SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			pidText.setLayoutData(gd);
			pidText.addModifyListener(new ModifyListener() {

				@SuppressWarnings("synthetic-access")
				@Override
				public void modifyText(ModifyEvent evt) {
					updateLaunchConfigurationDialog();
				}
			});
			pidText.addVerifyListener(new NumberVerifyListener());
		}

		if (preRunVisible) {
			createVerticalSpacer(mainComp, 2);

			// Launch with remote user
			Composite userIdComp = new Composite(mainComp, SWT.NONE);
			GridLayout userIdCompLayout = new GridLayout();
			userIdCompLayout.numColumns = 2;
			userIdCompLayout.marginHeight = 0;
			userIdCompLayout.marginWidth = 0;
			userIdComp.setLayout(userIdCompLayout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			userIdComp.setLayoutData(gd);

			userIdButton = createCheckButton(userIdComp, REMOTE_USER_ID_LABEL_TEXT);
			userIdButton.addSelectionListener(new SelectionAdapter() {

				@SuppressWarnings("synthetic-access")
				@Override
				public void widgetSelected(SelectionEvent evt) {
					updateLaunchRemoteUserControls();
					updateLaunchConfigurationDialog();
				}
			});
			userIdButton.setEnabled(true);

			userIdText = new Text(userIdComp, SWT.SINGLE | SWT.BORDER);
			gd = new GridData();
			gd.grabExcessHorizontalSpace = true;
			gd.horizontalAlignment = SWT.FILL;
			userIdText.setLayoutData(gd);
			userIdText.addModifyListener(new ModifyListener() {

				@SuppressWarnings("synthetic-access")
				@Override
				public void modifyText(ModifyEvent evt) {
					updateLaunchConfigurationDialog();
				}
			});

			createVerticalSpacer(mainComp, 2);

			// Commands to run before execution
			preRunLabel = new Label(mainComp, SWT.NONE);
			preRunLabel.setText(PRE_RUN_LABEL_TEXT);
			gd = new GridData();
			gd.horizontalSpan = 2;
			preRunLabel.setLayoutData(gd);

			preRunText = new Text(mainComp, SWT.MULTI | SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 1;
			gd.heightHint = 3 * preRunText.getLineHeight();
			preRunText.setLayoutData(gd);
			preRunText.addModifyListener(new ModifyListener() {

				@SuppressWarnings("synthetic-access")
				@Override
				public void modifyText(ModifyEvent evt) {
					updateLaunchConfigurationDialog();
				}
			});

			preRunEditButton = createPushButton(mainComp, Messages.RemoteCMainTab_Prerun_Edit_Button, null);
			gd = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
			preRunEditButton.setLayoutData(gd);
			preRunEditButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					showCommandsEditor();
				}
			});
		}
	}

	/*
	 * createDownloadOption This creates the skip download check button.
	 */
	protected void createDownloadOption(Composite parent) {
		Composite mainComp = new Composite(parent, SWT.NONE);
		GridLayout mainLayout = new GridLayout();
		mainLayout.marginHeight = 0;
		mainLayout.marginWidth = 0;
		mainComp.setLayout(mainLayout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		mainComp.setLayoutData(gd);

		skipDownloadButton = createCheckButton(mainComp, SKIP_DOWNLOAD_BUTTON_TEXT);
		skipDownloadButton.addSelectionListener(new SelectionAdapter() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void widgetSelected(SelectionEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		skipDownloadButton.setEnabled(true);
	}

	protected void handleRemoteBrowseSelected() {
		IPeerNode connection = peerSelector.getPeerNode();
		if (connection != null) {
			FSOpenFileDialog dialog = new FSOpenFileDialog(getShell());
			dialog.setInput(connection);
			if (dialog.open() == Window.OK) {
				Object candidate = dialog.getFirstResult();
				if (candidate instanceof FSTreeNode) {
					String absPath = ((FSTreeNode) candidate).getLocation();
					if (absPath != null) {
						remoteProgText.setText(absPath);
					}
				}
			}
		}
	}

	protected void updateTargetProgFromConfig(ILaunchConfiguration config) {
		if (remoteProgText != null) {
			String targetPath = ""; //$NON-NLS-1$
			try {
				targetPath = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_PATH, ""); //$NON-NLS-1$
			}
			catch (CoreException e) { /* ignored on purpose */ }

			boolean prevRemoteProgTextFireNotification = remoteProgTextFireNotification;
			remoteProgTextFireNotification = false;
			remoteProgText.setText(targetPath);
			remoteProgTextFireNotification = prevRemoteProgTextFireNotification;
		}

		if (pidText != null) {
			String pid = ""; //$NON-NLS-1$
			try {
				pid = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_PID, ""); //$NON-NLS-1$
			}
			catch (CoreException e) { /* ignored on purpose */ }

			pidText.setText(pid);
		}

		if (preRunText != null) {
			String prelaunchCmd = ""; //$NON-NLS-1$
			try {
				prelaunchCmd = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_PRERUN_COMMANDS, ""); //$NON-NLS-1$
			}
			catch (CoreException e) { /* ignored on purpose */ }

			preRunText.setText(prelaunchCmd);
		}

		if (userIdText != null) {
			String user = ""; //$NON-NLS-1$
			try {
				user = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_USER_ID, ""); //$NON-NLS-1$
			} catch (CoreException e) { /* ignored on purpose */ }

			userIdText.setText(user);
		}
	}

	protected void updateSkipDownloadFromConfig(ILaunchConfiguration config) {
		if (skipDownloadButton != null) {
			boolean downloadToTarget = true;
			try {
				downloadToTarget = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_SKIP_DOWNLOAD_TO_TARGET, IRemoteTEConfigurationConstants.ATTR_SKIP_DOWNLOAD_TO_TARGET_DEFAULT);
			}
			catch (CoreException e) { /* ignored on purpose */ }

			skipDownloadButton.setSelection(downloadToTarget);
		}
	}

	protected void updateLaunchRemoteUserFromConfig(ILaunchConfiguration config) {
		if (userIdButton != null) {
			boolean launchRemoteUser = true;
			try {
				launchRemoteUser = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_LAUNCH_REMOTE_USER, IRemoteTEConfigurationConstants.ATTR_LAUNCH_REMOTE_USER_DEFAULT);
			} catch (CoreException e) { /* ignored on purpose */ }

			userIdButton.setSelection(launchRemoteUser);
			updateLaunchRemoteUserControls();
		}
	}

	private void updateLaunchRemoteUserControls() {
		if (userIdText != null && userIdButton != null) {
			userIdText.setEnabled(userIdButton.getSelection());
		}
	}

	/**
	 * Sets the remote path from the local path. Apply path mappings before
	 * setting the remote path.
	 */
	private void setRemotePathFromLocalPath() {
		String programName = fProgText.getText().trim();

		if (programName != null && !"".equals(programName) && remoteProgText != null) { //$NON-NLS-1$
			IPeerNode connection = peerSelector.getPeerNode();
			if (connection != null) {
				IPathMapResolverService svc = ServiceManager.getInstance().getService(connection, IPathMapResolverService.class);
				if (svc != null) {
					String remoteName = null;

					// The program name may contain variables, resolve them first
					try {
		                programName = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(programName);
	                }
	                catch (CoreException e) {
	    				// Silently ignore substitution failure (for consistency with "Arguments" and "Work directory" fields)
	                }

					// The path might be project relative
					IPath p = new Path(programName);
					if (!p.isAbsolute()) {
						ICProject project = getCProject();
						if (project != null && project.isOpen()) {
							URI uri = project.getLocationURI();
							File f = URIUtil.toFile(uri);
							if (f != null) {
								programName = new File(f, p.toString()).getAbsolutePath();
							}
						}
					}

					// Perform the path resolution
					remoteName = svc.findTargetPath(connection, programName);
					if (remoteName != null) {
						boolean prevRemoteProgTextFireNotification = remoteProgTextFireNotification;
						remoteProgTextFireNotification = false;
						remoteProgText.setText(remoteName);
						remoteProgTextFireNotification = prevRemoteProgTextFireNotification;
					}
				}
			}
		}
	}

	/**
	 * Sets the local path from the remote path. Apply path mappings before
	 * setting the local path.
	 */
	private void setLocalPathFromRemotePath() {
		String remoteName = remoteProgText.getText().trim();

		if (remoteName != null && !"".equals(remoteName)) { //$NON-NLS-1$
			IPeerNode connection = peerSelector.getPeerNode();
			if (connection != null) {
				IPathMapResolverService svc = ServiceManager.getInstance().getService(connection, IPathMapResolverService.class);
				if (svc != null) {
					String programName = svc.findHostPath(connection, remoteName);
					if (programName == null) {
						// If there is no match, try to use the parent directory
						String programParent = svc.findHostPath(connection, Path.fromPortableString(remoteName).removeLastSegments(1).toPortableString());
						if (programParent != null) {
							programName = new File(programParent, Path.fromPortableString(remoteName).lastSegment()).toString();
						}
						else {
							programName = ""; //$NON-NLS-1$
						}
					}

					boolean prevProgTextFireNotification = progTextFireNotification;
					progTextFireNotification = false;
					fProgText.setText(programName);
					progTextFireNotification = prevProgTextFireNotification;
				}
			}
		}
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) {
		String remoteConnection = null;
		try {
			remoteConnection = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_CONNECTION, ""); //$NON-NLS-1$
		}
		catch (CoreException ce) {
			// Ignore
		}
		
		try {
			progTextFireNotification = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_AUTO_PATH_MAPPING_FROM_LOCAL_TO_REMOTE, true);
		}
		catch (CoreException e) {
			// Ignore
		}
		
		try {
			remoteProgTextFireNotification = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_AUTO_PATH_MAPPING_FROM_REMOTE_TO_LOCAL, true);
		}
		catch (CoreException e) {
			// Ignore
		}
		peerSelector.updateSelectionFrom(remoteConnection);
		super.initializeFrom(config);

		updateTargetProgFromConfig(config);
		updateSkipDownloadFromConfig(config);
		updateLaunchRemoteUserFromConfig(config);
	}

	/*
	 * performApply
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply
	 */
	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {

		String currentSelection = peerSelector.getPeerId();
		config.setAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_CONNECTION, currentSelection != null ? currentSelection : null);
		if (remoteProgText != null) {
			String value = remoteProgText.getText();
			config.setAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_PATH, !"".equals(value.trim()) ? value.trim() : (String)null); //$NON-NLS-1$
		}
		if (pidText != null) {
			String value = pidText.getText();
			config.setAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_PID, !"".equals(value.trim()) ? value.trim() : (String)null); //$NON-NLS-1$
		}
		if (skipDownloadButton != null) {
			config.setAttribute(IRemoteTEConfigurationConstants.ATTR_SKIP_DOWNLOAD_TO_TARGET, skipDownloadButton.getSelection());
		}
		if (preRunText != null) {
			String value = preRunText.getText();
			config.setAttribute(IRemoteTEConfigurationConstants.ATTR_PRERUN_COMMANDS, !"".equals(value.trim()) ? value.trim() : (String)null); //$NON-NLS-1$
		}
		if (userIdText != null) {
			String value = userIdText.getText();
			config.setAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_USER_ID, !"".equals(value.trim()) ? value.trim() : (String)null); //$NON-NLS-1$
		}
		if (userIdButton != null) {
			config.setAttribute(IRemoteTEConfigurationConstants.ATTR_LAUNCH_REMOTE_USER, userIdButton.getSelection());
		}
		super.performApply(config);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		super.setDefaults(config);
		config.setAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_CONNECTION, (String)null);
		config.setAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_PATH, (String)null);
		config.setAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_PID, (String)null);
		config.setAttribute(IRemoteTEConfigurationConstants.ATTR_SKIP_DOWNLOAD_TO_TARGET, IRemoteTEConfigurationConstants.ATTR_SKIP_DOWNLOAD_TO_TARGET_DEFAULT);
		config.setAttribute(IRemoteTEConfigurationConstants.ATTR_PRERUN_COMMANDS, (String)null);
		config.setAttribute(IRemoteTEConfigurationConstants.ATTR_AUTO_PATH_MAPPING_FROM_LOCAL_TO_REMOTE, true);
		config.setAttribute(IRemoteTEConfigurationConstants.ATTR_AUTO_PATH_MAPPING_FROM_REMOTE_TO_LOCAL, true);
	}

	protected void showCommandsEditor() {
		Dialog dialog = new Dialog(getShell()) {
			private StyledText textArea = null;

			@Override
			protected Control createDialogArea(Composite parent) {
				Composite baseComposite = (Composite) super.createDialogArea(parent);
				baseComposite.getShell().setText(Messages.RemoteCMainTab_Prerun_Edit_Dialog_Title);
				textArea = new StyledText(baseComposite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
				textArea.setAlwaysShowScrollBars(false);
				GridData textAreaGD = new GridData(GridData.FILL_BOTH);
				textAreaGD.heightHint = 180;
				textAreaGD.widthHint = 300;
				textArea.setLayoutData(textAreaGD);
				textArea.setText(preRunText.getText());
				return baseComposite;
			}

			@Override
			protected void okPressed() {
				if (preRunText != null && textArea != null) {
					preRunText.setText(textArea.getText());
				}
				super.okPressed();
			}

			@Override
			protected boolean isResizable() {
				return true;
			}
		};
		dialog.open();
	}
}
