/*******************************************************************************
 * Copyright (c) 2012, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.remote.app;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.tcf.te.launch.core.persistence.DefaultPersistenceDelegate;
import org.eclipse.tcf.te.launch.ui.interfaces.ILaunchConfigurationTabFormPart;
import org.eclipse.tcf.te.tcf.launch.ui.nls.Messages;
import org.eclipse.tcf.te.ui.forms.parts.AbstractSection;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ILineSeparatorConstants;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Remote application advanced properties section implementation.
 */
public class LaunchConfigurationAdvancedTabSection extends AbstractSection implements ILaunchConfigurationTabFormPart {
	/* default */ Button lineSeparatorDefault;
	/* default */ Button lineSeparatorLF;
	/* default */ Button lineSeparatorCRLF;
	/* default */ Button lineSeparatorCR;

	/**
	 * Constructor.
	 *
	 * @param form The parent managed form. Must not be <code>null</code>.
	 * @param parent The parent composite. Must not be <code>null</code>.
	 */
	public LaunchConfigurationAdvancedTabSection(IManagedForm form, Composite parent) {
		super(form, parent, ExpandableComposite.TWISTIE);
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
		section.setText(Messages.LaunchConfigurationAdvancedTabSection_title);
		if (section.getParent().getLayout() instanceof GridLayout) {
			section.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL, SWT.CENTER, true, false));
		}

		// Create the section client
		Composite client = createClientContainer(section, 1, toolkit);
		Assert.isNotNull(client);
		GridLayout layout = new GridLayout();
		layout.marginLeft = 0; layout.marginHeight = 0;
		client.setLayout(layout);
		section.setClient(client);
		client.setBackground(section.getBackground());

		Group group = new Group(client, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		group.setText(Messages.LaunchConfigurationAdvancedTabSection_group_label);
		group.setLayout(new GridLayout());

		Label label = new Label(group, SWT.NONE);
		label.setText(Messages.LaunchConfigurationAdvancedTabSection_lineseparator_label);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite panel2 = new Composite(group, SWT.NONE);
		layout = new GridLayout(4, false);
		layout.marginLeft = 15; layout.marginHeight = 2;
		panel2.setLayout(layout);
		panel2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		lineSeparatorDefault = toolkit.createButton(panel2, Messages.LaunchConfigurationAdvancedTabSection_lineseparator_default, SWT.RADIO);
		lineSeparatorDefault.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (lineSeparatorDefault.getSelection()) {
					SWTControlUtil.setSelection(lineSeparatorLF, false);
					SWTControlUtil.setSelection(lineSeparatorCRLF, false);
					SWTControlUtil.setSelection(lineSeparatorCR, false);
				}
			}
		});

		lineSeparatorLF = toolkit.createButton(panel2, Messages.LaunchConfigurationAdvancedTabSection_lineseparator_lf, SWT.RADIO);
		lineSeparatorLF.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (lineSeparatorDefault.getSelection()) {
					SWTControlUtil.setSelection(lineSeparatorDefault, false);
					SWTControlUtil.setSelection(lineSeparatorCRLF, false);
					SWTControlUtil.setSelection(lineSeparatorCR, false);
				}
			}
		});

		lineSeparatorCRLF = toolkit.createButton(panel2, Messages.LaunchConfigurationAdvancedTabSection_lineseparator_crlf, SWT.RADIO);
		lineSeparatorCRLF.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (lineSeparatorDefault.getSelection()) {
					SWTControlUtil.setSelection(lineSeparatorDefault, false);
					SWTControlUtil.setSelection(lineSeparatorLF, false);
					SWTControlUtil.setSelection(lineSeparatorCR, false);
				}
			}
		});

		lineSeparatorCR = toolkit.createButton(panel2, Messages.LaunchConfigurationAdvancedTabSection_lineseparator_cr, SWT.RADIO);
		lineSeparatorCR.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (lineSeparatorDefault.getSelection()) {
					SWTControlUtil.setSelection(lineSeparatorDefault, false);
					SWTControlUtil.setSelection(lineSeparatorLF, false);
					SWTControlUtil.setSelection(lineSeparatorCRLF, false);
				}
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.interfaces.ILaunchConfigurationTabFormPart#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
    	Assert.isNotNull(configuration);

		String lineSeparator = DefaultPersistenceDelegate.getAttribute(configuration, ITerminalsConnectorConstants.PROP_LINE_SEPARATOR, (String)null);
		if (lineSeparator == null) {
			SWTControlUtil.setSelection(lineSeparatorDefault, true);
		}
		else if (ILineSeparatorConstants.LINE_SEPARATOR_LF.equals(lineSeparator)) {
			SWTControlUtil.setSelection(lineSeparatorLF, true);
		}
		else if (ILineSeparatorConstants.LINE_SEPARATOR_CRLF.equals(lineSeparator)) {
			SWTControlUtil.setSelection(lineSeparatorCRLF, true);
		}
		else if (ILineSeparatorConstants.LINE_SEPARATOR_CR.equals(lineSeparator)) {
			SWTControlUtil.setSelection(lineSeparatorCR, true);
		}
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.interfaces.ILaunchConfigurationTabFormPart#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    	Assert.isNotNull(configuration);

		String lineSeparator = null;
		if (SWTControlUtil.getSelection(lineSeparatorLF)) {
			lineSeparator = ILineSeparatorConstants.LINE_SEPARATOR_LF;
		}
		else if (SWTControlUtil.getSelection(lineSeparatorCRLF)) {
			lineSeparator = ILineSeparatorConstants.LINE_SEPARATOR_CRLF;
		}
		else if (SWTControlUtil.getSelection(lineSeparatorCR)) {
			lineSeparator = ILineSeparatorConstants.LINE_SEPARATOR_CR;
		}
		DefaultPersistenceDelegate.setAttribute(configuration, ITerminalsConnectorConstants.PROP_LINE_SEPARATOR, lineSeparator);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.interfaces.ILaunchConfigurationTabFormPart#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
    @Override
    public boolean isValid(ILaunchConfiguration configuration) {
	    return true;
    }
}
