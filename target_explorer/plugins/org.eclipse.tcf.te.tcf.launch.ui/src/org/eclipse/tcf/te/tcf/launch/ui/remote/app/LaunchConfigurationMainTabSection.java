/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.remote.app;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.tcf.te.launch.core.persistence.DefaultPersistenceDelegate;
import org.eclipse.tcf.te.launch.core.persistence.launchcontext.LaunchContextsPersistenceDelegate;
import org.eclipse.tcf.te.launch.ui.interfaces.ILaunchConfigurationTabFormPart;
import org.eclipse.tcf.te.launch.ui.tabs.AbstractLaunchConfigurationTab;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.tcf.filesystem.core.model.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.dialogs.FSOpenFileDialog;
import org.eclipse.tcf.te.tcf.launch.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.steps.IProcessesStepAttributes;
import org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl;
import org.eclipse.tcf.te.ui.forms.parts.AbstractSection;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Remote application launch configuration main tab section implementation.
 */
public class LaunchConfigurationMainTabSection extends AbstractSection implements ILaunchConfigurationTabFormPart {

	/* default */ BaseEditBrowseTextControl processImage;
	/* default */ BaseEditBrowseTextControl processArguments;
	private Button stopAtEntry;
	private Button stopAtMain;
	private Button attachChildren;

	/* default */ IModelNode firstSelection = null;

	/**
	 * Constructor.
	 *
	 * @param form The parent managed form. Must not be <code>null</code>.
	 * @param parent The parent composite. Must not be <code>null</code>.
	 */
	public LaunchConfigurationMainTabSection(IManagedForm form, Composite parent) {
		super(form, parent, ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);
		getSection().setBackground(parent.getBackground());
		createClient(getSection(), form.getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	@Override
	protected void createClient(final Section section, FormToolkit toolkit) {
		Assert.isNotNull(section);
		Assert.isNotNull(toolkit);

		// Configure the section
		section.setText(Messages.LaunchConfigurationMainTabSection_title);
		if (section.getParent().getLayout() instanceof GridLayout) {
			section.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL, SWT.CENTER, true, false));
		}

		// Create the section client
		Composite client = createClientContainer(section, 3, toolkit);
		Assert.isNotNull(client);
		section.setClient(client);
		client.setBackground(section.getBackground());

		// Create a toolbar for the section
		createSectionToolbar(section, toolkit);

		// Create the section sub controls
		processImage = new BaseEditBrowseTextControl(null) {
			@Override
			protected void onButtonControlSelected() {
				if (firstSelection != null) {
					FSOpenFileDialog dialog = new FSOpenFileDialog(section.getShell());
					dialog.setFilterPath(getEditFieldControlText());
					dialog.setInput(firstSelection);
					if (dialog.open() == Window.OK) {
						Object candidate = dialog.getFirstResult();
						if (candidate instanceof FSTreeNode) {
							String absPath = ((FSTreeNode) candidate).getLocation();
							if (absPath != null) {
								processImage.setEditFieldControlText(absPath);
							}
						}
					}
				}
			}
			@Override
			public void modifyText(ModifyEvent e) {
				super.modifyText(e);
				getManagedForm().dirtyStateChanged();
			}
			@Override
			protected boolean isAdjustEditFieldControlWidthHint() {
			    return true;
			}
		};
		processImage.setEditFieldLabel(Messages.LaunchConfigurationMainTabSection_processImage_label);
		processImage.setIsGroup(false);
		processImage.setHideBrowseButton(false);
		processImage.setAdjustBackgroundColor(true);
		processImage.setParentControlIsInnerPanel(true);
		processImage.setFormToolkit(toolkit);
		processImage.setupPanel(client);
		processImage.doCreateControlDecoration(processImage.getEditFieldControl());

		processArguments = new BaseEditBrowseTextControl(null) {
			@Override
			public void modifyText(ModifyEvent e) {
				super.modifyText(e);
				getManagedForm().dirtyStateChanged();
			}
			@Override
			protected boolean isAdjustEditFieldControlWidthHint() {
			    return true;
			}
		};
		processArguments.setEditFieldLabel(Messages.LaunchConfigurationMainTabSection_processArguments_label);
		processArguments.setIsGroup(false);
		processArguments.setHideBrowseButton(true);
		processArguments.setAdjustBackgroundColor(true);
		processArguments.setParentControlIsInnerPanel(true);
		processArguments.setFormToolkit(toolkit);
		processArguments.setupPanel(client);

		Object container = getManagedForm().getContainer();
		if (container instanceof AbstractLaunchConfigurationTab) {
			ILaunchConfigurationDialog dialog = ((AbstractLaunchConfigurationTab)container).getLaunchConfigurationDialog();
			String mode = dialog != null ? dialog.getMode() : null;
			if (ILaunchManager.DEBUG_MODE.equals(mode)) {
				// Add the debug options to the launch tab
				Label label = new Label(client, SWT.HORIZONTAL);
				GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
				layoutData.horizontalSpan = 3;
				label.setLayoutData(layoutData);

				stopAtEntry = new Button(client, SWT.CHECK);
				stopAtEntry.setText(Messages.LaunchConfigurationMainTabSection_stopAtEntry_label);
				layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
				layoutData.horizontalSpan = 3;
				stopAtEntry.setLayoutData(layoutData);
				stopAtEntry.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						getManagedForm().dirtyStateChanged();
					}
				});

				stopAtMain = new Button(client, SWT.CHECK);
				stopAtMain.setText(Messages.LaunchConfigurationMainTabSection_stopAtMain_label);
				layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
				layoutData.horizontalSpan = 3;
				stopAtMain.setLayoutData(layoutData);
				stopAtMain.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						getManagedForm().dirtyStateChanged();
					}
				});

				attachChildren = new Button(client, SWT.CHECK);
				attachChildren.setText(Messages.LaunchConfigurationMainTabSection_attachChildren_label);
				layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
				layoutData.horizontalSpan = 3;
				attachChildren.setLayoutData(layoutData);
				attachChildren.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						getManagedForm().dirtyStateChanged();
					}
				});
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.interfaces.ILaunchConfigurationTabFormPart#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		Assert.isNotNull(configuration);

		if (processImage != null) {
			String image = DefaultPersistenceDelegate.getAttribute(configuration, IProcessesStepAttributes.ATTR_PROCESS_IMAGE, ""); //$NON-NLS-1$
			processImage.setEditFieldControlText(image);
		}

		if (processArguments != null) {
			String arguments = DefaultPersistenceDelegate.getAttribute(configuration, IProcessesStepAttributes.ATTR_PROCESS_ARGUMENTS, ""); //$NON-NLS-1$
			processArguments.setEditFieldControlText(arguments);
		}

		if (stopAtEntry != null) {
			boolean selected = DefaultPersistenceDelegate.getAttribute(configuration, IProcessesStepAttributes.ATTR_STOP_AT_ENTRY, false);
			stopAtEntry.setSelection(selected);
		}

		if (stopAtMain != null) {
			boolean selected = DefaultPersistenceDelegate.getAttribute(configuration, IProcessesStepAttributes.ATTR_STOP_AT_MAIN, false);
			stopAtMain.setSelection(selected);
		}

		if (attachChildren != null) {
			boolean selected = DefaultPersistenceDelegate.getAttribute(configuration, IProcessesStepAttributes.ATTR_ATTACH_CHILDREN, false);
			attachChildren.setSelection(selected);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.interfaces.ILaunchConfigurationTabFormPart#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		Assert.isNotNull(configuration);

		if (processImage != null) {
			String image = processImage.getEditFieldControlText();

			if (image != null && image.trim().length() > 0) {
				DefaultPersistenceDelegate.setAttribute(configuration, IProcessesStepAttributes.ATTR_PROCESS_IMAGE, image);
			} else {
				DefaultPersistenceDelegate.setAttribute(configuration, IProcessesStepAttributes.ATTR_PROCESS_IMAGE, (String)null);
			}
		} else {
			DefaultPersistenceDelegate.setAttribute(configuration, IProcessesStepAttributes.ATTR_PROCESS_IMAGE, (String)null);
		}

		if (processArguments != null) {
			String arguments = processArguments.getEditFieldControlText();

			if (arguments != null && arguments.trim().length() > 0) {
				DefaultPersistenceDelegate.setAttribute(configuration, IProcessesStepAttributes.ATTR_PROCESS_ARGUMENTS, arguments);
			} else {
				DefaultPersistenceDelegate.setAttribute(configuration, IProcessesStepAttributes.ATTR_PROCESS_ARGUMENTS, (String)null);
			}
		} else {
			DefaultPersistenceDelegate.setAttribute(configuration, IProcessesStepAttributes.ATTR_PROCESS_ARGUMENTS, (String)null);
		}

		if (stopAtEntry != null) {
			DefaultPersistenceDelegate.setAttribute(configuration, IProcessesStepAttributes.ATTR_STOP_AT_ENTRY, stopAtEntry.getSelection());
		}

		if (stopAtMain != null) {
			DefaultPersistenceDelegate.setAttribute(configuration, IProcessesStepAttributes.ATTR_STOP_AT_MAIN, stopAtMain.getSelection());
		}

		if (attachChildren != null) {
			DefaultPersistenceDelegate.setAttribute(configuration, IProcessesStepAttributes.ATTR_ATTACH_CHILDREN, attachChildren.getSelection());
		}
	}

	@Override
	public boolean isValid(ILaunchConfiguration configuration) {
		firstSelection = null;
		IModelNode[] contexts = LaunchContextsPersistenceDelegate.getLaunchContexts(configuration);
		if (contexts != null && contexts.length > 0) {
			firstSelection = contexts[0];
		}
		processImage.getButtonControl().setEnabled(firstSelection != null);

		if (processImage.getEditFieldControlText().trim().length() > 0) {
			setMessage(null, IMessageProvider.NONE);
		}
		else {
			setMessage(Messages.LaunchConfigurationMainTabSection_error_missingProcessImage, IMessageProvider.ERROR);
		}
		processImage.updateControlDecoration(getMessage(), getMessageType());

		return processImage.getEditFieldControlText().trim().length() > 0;
	}
}
