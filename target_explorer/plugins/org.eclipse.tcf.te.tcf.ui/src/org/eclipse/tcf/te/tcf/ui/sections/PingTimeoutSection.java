/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.sections;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.forms.parts.AbstractSection;
import org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Ping timeout section implementation.
 */
public class PingTimeoutSection extends AbstractSection implements IDataExchangeNode {
	// The section sub controls
	/* default */ Combo verbosity;
	/* default */ Label verbosityLabel;

	/**
	 * Constructor.
	 *
	 * @param form The parent managed form. Must not be <code>null</code>.
	 * @param parent The parent composite. Must not be <code>null</code>.
	 */
	public PingTimeoutSection(IManagedForm form, Composite parent) {
		super(form, parent, SWT.NONE);
		createClient(getSection(), form.getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		Assert.isNotNull(section);
		Assert.isNotNull(toolkit);

		// Configure the section
		section.setText(Messages.PingTimeoutSection_title);
		if (section.getParent().getLayout() instanceof GridLayout) {
			section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}

		// Create the section client
		Composite client = createClientContainer(section, 1, toolkit);
		Assert.isNotNull(client);
		section.setClient(client);

		Composite panel = toolkit.createComposite(client);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0; layout.marginWidth = 0;
		panel.setLayout(layout);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		panel.setBackground(client.getBackground());

		verbosityLabel = toolkit.createLabel(panel, Messages.PingTimeoutSection_timeout_label, SWT.HORIZONTAL);
		GridData layoutData = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		verbosityLabel.setLayoutData(layoutData);
		verbosityLabel.setBackground(client.getBackground());

		verbosity = new Combo(panel, SWT.DROP_DOWN | SWT.READ_ONLY);
		toolkit.adapt(verbosity, true, true);
		verbosity.setItems(new String[] { "0", "10", "20", "30", "60"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		verbosity.select(0);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		layoutData.widthHint = SWTControlUtil.convertWidthInCharsToPixels(verbosity, 5);
		verbosity.setLayoutData(layoutData);
	}

	/**
	 * Initialize the widgets from the given attributes.
	 *
	 * @param attributes The attributes. Must not be <code>null</code>.
	 */
	public void initializeWidgets(IPropertiesContainer attributes) {
		Assert.isNotNull(attributes);

		if (verbosity != null) {
			int timeout = attributes.getIntProperty(IPeerNodeProperties.PROP_PING_TIMEOUT);
			if (timeout >= 0) {
				SWTControlUtil.select(verbosity, SWTControlUtil.indexOf(verbosity, timeout+"")); //$NON-NLS-1$
			}
			else {
				SWTControlUtil.select(verbosity, SWTControlUtil.indexOf(verbosity, "10")); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Updates the given set of attributes with the current values of the
	 * page widgets.
	 *
	 * @param attributes The attributes to update. Must not be <code>null</code>:
	 */
	public void updateAttributes(IPropertiesContainer attributes) {
		Assert.isNotNull(attributes);

		String value = SWTControlUtil.getText(verbosity);
		if (value != null && !"".equals(value)) { //$NON-NLS-1$
			attributes.setProperty(IPeerNodeProperties.PROP_PING_TIMEOUT, value);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#setReadOnly(boolean)
	 */
	@Override
	public void setReadOnly(boolean readOnly) {
		super.setReadOnly(readOnly);
		SWTControlUtil.setEnabled(verbosity, !readOnly);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode#setupData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void setupData(IPropertiesContainer data) {
		initializeWidgets(data);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode#extractData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void extractData(IPropertiesContainer data) {
		updateAttributes(data);
	}
}
