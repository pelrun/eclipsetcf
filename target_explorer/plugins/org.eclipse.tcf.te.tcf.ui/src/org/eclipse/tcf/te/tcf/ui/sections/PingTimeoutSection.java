/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.sections;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.controls.validator.NumberVerifyListener;
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
	/* default */ Text verbosity;
	/* default */ Label verbosityLabel;

	private final int defaultPingTimeout;

	/**
	 * Constructor.
	 *
	 * @param form The parent managed form. Must not be <code>null</code>.
	 * @param parent The parent composite. Must not be <code>null</code>.
	 */
	public PingTimeoutSection(IManagedForm form, Composite parent) {
		this(form, parent, 2);
	}

	public PingTimeoutSection(IManagedForm form, Composite parent, int defaultPingTimeout) {
		super(form, parent, SWT.NONE);
		this.defaultPingTimeout = defaultPingTimeout >= 0 ? defaultPingTimeout : 2;
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

		verbosity = new Text(panel, SWT.BORDER);
		toolkit.adapt(verbosity, true, true);
		verbosity.addVerifyListener(new NumberVerifyListener(0, -1));
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
			int timeout = attributes.getIntProperty(IPeerProperties.PROP_PING_TIMEOUT);
			if (timeout >= 0 && timeout != defaultPingTimeout) {
				SWTControlUtil.setText(verbosity, Integer.toString(timeout));
			}
			else {
				SWTControlUtil.setText(verbosity, Integer.toString(defaultPingTimeout));
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
			int timeout = -1;
			try {
				timeout = Integer.decode(value).intValue();
			} catch (NumberFormatException e) { /* ignored on purpose */ }

			attributes.setProperty(IPeerProperties.PROP_PING_TIMEOUT, timeout != -1 && timeout != defaultPingTimeout ? value : Integer.toString(defaultPingTimeout));
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
