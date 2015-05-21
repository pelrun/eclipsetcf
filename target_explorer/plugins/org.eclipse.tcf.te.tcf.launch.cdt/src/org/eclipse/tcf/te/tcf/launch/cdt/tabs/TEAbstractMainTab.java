/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.cdt.tabs;

import java.io.File;
import java.net.URI;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.dsf.gdb.internal.ui.launching.CMainTab;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
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

	protected Button remoteBrowseButton;
	protected TCFPeerSelector peerSelector;
	protected Label remoteProgLabel;
	protected Text remoteProgText;
	protected Button skipDownloadButton;
	protected boolean skipDownloadButtonVisible = true;

	protected boolean progTextFireNotification;
	protected boolean remoteProgTextFireNotification;
	protected boolean remoteProgValidation = true;

	private Text preRunText;
	private Label preRunLabel;
	private boolean preRunVisible = true;

	public static final int NO_DOWNLOAD_GROUP = 2 << 8;
	public static final int NO_PRERUN_GROUP = 4 << 8;

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
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite comp = (Composite) getControl();
		/* The TE Connection dropdown */
		createVerticalSpacer(comp, 1);
		createRemoteConnectionGroup(comp);
		/* The remote binary location and skip download option */
		createVerticalSpacer(comp, 1);
		createTargetExePathGroup(comp);
		if (skipDownloadButtonVisible) createDownloadOption(comp);

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
		progTextFireNotification = true;
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
			if (retVal && remoteProgValidation) {
				String name = remoteProgText.getText().trim();
				if (name.length() == 0) {
					setErrorMessage(REMOTE_PROG_TEXT_ERROR);
					retVal = false;
				}
			}
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
		Composite mainComp = new Composite(parent, SWT.NONE);
		GridLayout mainLayout = new GridLayout();
		mainLayout.numColumns = 2;
		mainLayout.marginHeight = 0;
		mainLayout.marginWidth = 0;
		mainComp.setLayout(mainLayout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		mainComp.setLayoutData(gd);

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
		remoteProgTextFireNotification = true;

		remoteBrowseButton = createPushButton(mainComp, Messages.RemoteCMainTab_Remote_Path_Browse_Button, null);
		remoteBrowseButton.addSelectionListener(new SelectionAdapter() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void widgetSelected(SelectionEvent evt) {
				handleRemoteBrowseSelected();
				updateLaunchConfigurationDialog();
			}
		});

		if (preRunVisible) {
			// Commands to run before execution
			preRunLabel = new Label(mainComp, SWT.NONE);
			preRunLabel.setText(PRE_RUN_LABEL_TEXT);
			gd = new GridData();
			gd.horizontalSpan = 2;
			preRunLabel.setLayoutData(gd);

			preRunText = new Text(mainComp, SWT.MULTI | SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			preRunText.setLayoutData(gd);
			preRunText.addModifyListener(new ModifyListener() {

				@SuppressWarnings("synthetic-access")
				@Override
				public void modifyText(ModifyEvent evt) {
					updateLaunchConfigurationDialog();
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
		String targetPath = null;
		try {
			targetPath = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_PATH, IRemoteTEConfigurationConstants.ATTR_REMOTE_PATH_DEFAULT);
		}
		catch (CoreException e) {
			// Ignore
		}

		boolean prevRemoteProgTextFireNotification = remoteProgTextFireNotification;
		remoteProgTextFireNotification = false;
		remoteProgText.setText(targetPath);
		remoteProgTextFireNotification = prevRemoteProgTextFireNotification;

		if (preRunText != null) {
			String prelaunchCmd = null;
			try {
				prelaunchCmd = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_PRERUN_COMMANDS, ""); //$NON-NLS-1$
			}
			catch (CoreException e) {
				// Ignore
			}
			if (prelaunchCmd != null) preRunText.setText(prelaunchCmd);
		}
	}

	protected void updateSkipDownloadFromConfig(ILaunchConfiguration config) {
		if (skipDownloadButton != null) {
			boolean downloadToTarget = true;
			try {
				downloadToTarget = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_SKIP_DOWNLOAD_TO_TARGET, IRemoteTEConfigurationConstants.ATTR_SKIP_DOWNLOAD_TO_TARGET_DEFAULT);
			}
			catch (CoreException e) {
				// Ignore for now
			}
			skipDownloadButton.setSelection(downloadToTarget);
		}
	}

	/**
	 * Sets the remote path from the local path. Apply path mappings before
	 * setting the remote path.
	 */
	private void setRemotePathFromLocalPath() {
		String programName = fProgText.getText().trim();

		if (programName != null && !"".equals(programName)) { //$NON-NLS-1$
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
					if (programName != null) {
						boolean prevProgTextFireNotification = progTextFireNotification;
						progTextFireNotification = false;
						fProgText.setText(programName);
						progTextFireNotification = prevProgTextFireNotification;
					}
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

		peerSelector.updateSelectionFrom(remoteConnection);
		super.initializeFrom(config);

		updateTargetProgFromConfig(config);
		updateSkipDownloadFromConfig(config);
	}

	/*
	 * performApply
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply
	 */
	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {

		String currentSelection = peerSelector.getPeerId();
		config.setAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_CONNECTION, currentSelection != null ? currentSelection : null);
		config.setAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_PATH, remoteProgText.getText());
		if (skipDownloadButton != null) {
			config.setAttribute(IRemoteTEConfigurationConstants.ATTR_SKIP_DOWNLOAD_TO_TARGET, skipDownloadButton.getSelection());
		}
		if (preRunText != null) {
			config.setAttribute(IRemoteTEConfigurationConstants.ATTR_PRERUN_COMMANDS, preRunText.getText());
		}
		super.performApply(config);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		super.setDefaults(config);
		config.setAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_CONNECTION, EMPTY_STRING);
		config.setAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_PATH, IRemoteTEConfigurationConstants.ATTR_REMOTE_PATH_DEFAULT);
		config.setAttribute(IRemoteTEConfigurationConstants.ATTR_SKIP_DOWNLOAD_TO_TARGET, IRemoteTEConfigurationConstants.ATTR_SKIP_DOWNLOAD_TO_TARGET_DEFAULT);
		config.setAttribute(IRemoteTEConfigurationConstants.ATTR_PRERUN_COMMANDS, EMPTY_STRING);
	}

}
