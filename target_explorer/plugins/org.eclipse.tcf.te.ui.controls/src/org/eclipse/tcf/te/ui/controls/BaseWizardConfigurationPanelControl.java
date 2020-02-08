/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.controls;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel;
import org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel;
import org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode;
import org.eclipse.tcf.te.ui.interfaces.data.IUpdatable;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Base control to deal with wizard or property page controls
 * which should share the same UI space.
 */
public class BaseWizardConfigurationPanelControl extends BaseDialogPageControl {
	private final Map<String, IWizardConfigurationPanel> configurationPanels = new Hashtable<String, IWizardConfigurationPanel>();

	private boolean isGroup;

	private Composite panel;
	private StackLayout panelLayout;

	private String activeConfigurationPanelKey = null;
	private IWizardConfigurationPanel activeConfigurationPanel = null;

	private final AbstractWizardConfigurationPanel EMPTY_PANEL;

	/**
	 * An empty configuration panel implementation.
	 */
	private static final class EmptySettingsPanel extends AbstractWizardConfigurationPanel {

		/**
	     * Constructor.
	     *
		 * @param parentControl The parent control. Must not be <code>null</code>!
	     */
	    public EmptySettingsPanel(BaseDialogPageControl parentControl) {
		    super(parentControl);
	    }

		/* (non-Javadoc)
	     * @see org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel#setupPanel(org.eclipse.swt.widgets.Composite, org.eclipse.tcf.te.ui.controls.interfaces.FormToolkit)
	     */
        @Override
	    public void setupPanel(Composite parent, FormToolkit toolkit) {
	    	Composite panel = new Composite(parent, SWT.NONE);
	    	panel.setLayout(new GridLayout());
	    	panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

	    	panel.setBackground(parent.getBackground());

	    	setControl(panel);
	    }

	    /* (non-Javadoc)
	     * @see org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel#dataChanged(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.swt.events.TypedEvent)
	     */
	    @Override
	    public boolean dataChanged(IPropertiesContainer data, TypedEvent e) {
	        return false;
	    }

	    /* (non-Javadoc)
	     * @see org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel#isValid()
	     */
	    @Override
	    public boolean isValid() {
	        return false;
	    }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.BaseControl#dispose()
	 */
	@Override
	public void dispose() {
		EMPTY_PANEL.dispose();
	    super.dispose();
	}

	/**
	 * Constructor.
	 *
	 * @param parentPage The parent dialog page this control is embedded in.
	 *                   Might be <code>null</code> if the control is not associated with a page.
	 */
	public BaseWizardConfigurationPanelControl(IDialogPage parentPage) {
		super(parentPage);
		EMPTY_PANEL = new EmptySettingsPanel(this);
		clear();
		setPanelIsGroup(false);
	}

	/**
	 * Sets if or if not the controls panel is a <code>Group</code>.
	 *
	 * @param isGroup <code>True</code> if the controls panel is a group, <code>false</code> otherwise.
	 */
	public void setPanelIsGroup(boolean isGroup) {
		this.isGroup = isGroup;
	}

	/**
	 * Returns if or if not the controls panel is a <code>Group</code>.
	 *
	 * @return <code>True</code> if the controls panel is a group, <code>false</code> otherwise.
	 */
	public boolean isPanelIsGroup() {
		return isGroup;
	}

	/**
	 * Returns the controls panel.
	 *
	 * @return The controls panel or <code>null</code>.
	 */
	public Composite getPanel() {
		return panel;
	}

	/**
	 * Returns the label text to set for the group (if the panel is a group).
	 *
	 * @return The label text to apply or <code>null</code>.
	 */
	public String getGroupLabel() {
		return null;
	}

	/**
	 * To be called from the embedding control to setup the controls UI elements.
	 *
	 * @param parent The parent control. Must not be <code>null</code>!
	 * @param toolkit The form toolkit. Must not be <code>null</code>.
	 */
	public void setupPanel(Composite parent, String[] configurationPanelKeys, FormToolkit toolkit) {
		Assert.isNotNull(parent);
		Assert.isNotNull(toolkit);

		setFormToolkit(toolkit);

		if (isPanelIsGroup()) {
			panel = new Group(parent, SWT.NONE);
			if (getGroupLabel() != null) ((Group)panel).setText(getGroupLabel());
		} else {
			panel = new Composite(parent, SWT.NONE);
		}
		Assert.isNotNull(panel);
		panel.setFont(parent.getFont());
		panel.setBackground(parent.getBackground());

		panelLayout = new StackLayout();
		panel.setLayout(panelLayout);

		setupConfigurationPanels(panel, configurationPanelKeys, toolkit);
		EMPTY_PANEL.setupPanel(panel, toolkit);
	}

	/**
	 * Removes all configuration panels.
	 */
	public void clear() {
		configurationPanels.clear();
	}

	/**
	 * Returns a unsorted list of all registered wizard configuration
	 * panel id's.
	 *
	 * @return A list of registered wizard configuration panel id's.
	 */
	public String[] getConfigurationPanelIds() {
		return configurationPanels.keySet().toArray(new String[configurationPanels.keySet().size()]);
	}

	/**
	 * Returns the wizard configuration panel instance registered for the given configuration panel key.
	 *
	 * @param key The key to get the wizard configuration panel for. Must not be <code>null</code>!
	 * @return The wizard configuration panel instance or an empty configuration panel if the key is unknown.
	 */
	public IWizardConfigurationPanel getConfigurationPanel(String key) {
		IWizardConfigurationPanel panel = key != null ? configurationPanels.get(key) : null;
		return panel != null ? panel : EMPTY_PANEL;
	}

	/**
	 * Returns if or if not the given wizard configuration panel is equal to the
	 * empty configuration panel.
	 *
	 * @param panel The wizard configuration panel or <code>null</code>.
	 * @return <code>True</code> if the wizard configuration panel is equal to the empty configuration panel.
	 */
	public final boolean isEmptyConfigurationPanel(IWizardConfigurationPanel panel) {
		return EMPTY_PANEL == panel;
	}

	/**
	 * Adds the given wizard configuration panel under the given configuration panel key to the
	 * list of known panels. If the given configuration panel is <code>null</code>, any configuration
	 * panel stored under the given key is removed from the list of known panels.
	 *
	 * @param key The key to get the wizard configuration panel for. Must not be <code>null</code>!
	 * @param panel The wizard configuration panel instance or <code>null</code>.
	 */
	public void addConfigurationPanel(String key, IWizardConfigurationPanel panel) {
		if (key == null) return;
		if (panel != null) {
			configurationPanels.put(key, panel);
		} else {
			configurationPanels.remove(key);
		}
	}

	/**
	 * Setup the wizard configuration panels for being presented to the user. This method is called by the
	 * controls <code>doSetupPanel(...)</code> and initialize all possible wizard configuration panels to show.
	 * The default implementation iterates over the given list of configuration panel keys and calls
	 * <code>setupPanel(...)</code> for each of them.
	 *
	 * @param parent The parent composite to use for the wizard configuration panels. Must not be <code>null</code>!
	 * @param configurationPanelKeys The list of configuration panels to initialize. Might be <code>null</code> or empty!
	 * @param toolkit The form toolkit. Must not be <code>null</code>.
	 */
	public void setupConfigurationPanels(Composite parent, String[] configurationPanelKeys, FormToolkit toolkit) {
		Assert.isNotNull(parent);
		Assert.isNotNull(toolkit);

		if (configurationPanelKeys != null) {
			for (int i = 0; i < configurationPanelKeys.length; i++) {
				IWizardConfigurationPanel configPanel = getConfigurationPanel(configurationPanelKeys[i]);
				Assert.isNotNull(configPanel);
				configPanel.setupPanel(parent, toolkit);
			}
		}
	}

	/**
	 * Make the wizard configuration panel registered under the given configuration panel key the
	 * most top configuration panel. If no configuration panel is registered under the given key,
	 * nothing will happen.
	 *
	 * @param key The key to get the wizard configuration panel for. Must not be <code>null</code>!
	 */
	public void showConfigurationPanel(String key) {
		String activeKey = getActiveConfigurationPanelKey();
		if (key != null && key.equals(activeKey) && activeConfigurationPanel != null) {
			return;
		}
		IWizardConfigurationPanel configPanel = getActiveConfigurationPanel();
		IPropertiesContainer data = new PropertiesContainer();
		if (configPanel instanceof IDataExchangeNode) {
			((IDataExchangeNode)configPanel).extractData(data);
		}
		configPanel = getConfigurationPanel(key);
		Assert.isNotNull(configPanel);
		if (configPanel.getControl() != null) {
			activeConfigurationPanel = configPanel;
			activeConfigurationPanelKey = key;
			panelLayout.topControl = configPanel.getControl();
			panel.layout();
			if (!data.isEmpty() && configPanel instanceof IUpdatable) {
				((IUpdatable)configPanel).updateData(data);
			}
			configPanel.activate();
		}
		else {
			activeConfigurationPanelKey = key;
		}
	}

	/**
	 * Returns the currently active configuration panel.
	 *
	 * @return The active configuration panel or <code>null</code>.
	 */
	public IWizardConfigurationPanel getActiveConfigurationPanel() {
		return activeConfigurationPanel;
	}

	/**
	 * Returns the currently active configuration panel key.
	 *
	 * @return The active configuration panel key or <code>null</code>.
	 */
	public String getActiveConfigurationPanelKey() {
		return activeConfigurationPanelKey;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.BaseControl#doSaveWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
	public void doSaveWidgetValues(IDialogSettings settings, String idPrefix) {
		super.doSaveWidgetValues(settings, idPrefix);
		if (settings != null) {
			IWizardConfigurationPanel configPanel = getActiveConfigurationPanel();
			if (configPanel != null && !isEmptyConfigurationPanel(configPanel)) {
				String key = configPanel.getDialogSettingsSectionName();
				key = key != null ? key : getActiveConfigurationPanelKey();
				IDialogSettings configPanelSettings = settings.getSection(key);
				if (configPanelSettings == null) configPanelSettings = settings.addNewSection(key);
				configPanel.doSaveWidgetValues(configPanelSettings, idPrefix);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.BaseControl#doRestoreWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
	public void doRestoreWidgetValues(IDialogSettings settings, String idPrefix) {
		super.doRestoreWidgetValues(settings, idPrefix);
		if (settings != null) {
			for (String panelKey : configurationPanels.keySet()) {
				IWizardConfigurationPanel configPanel = getConfigurationPanel(panelKey);
				if (configPanel != null && !isEmptyConfigurationPanel(configPanel)) {
					String settingsKey = configPanel.getDialogSettingsSectionName();
					settingsKey = settingsKey != null ? settingsKey : panelKey;
					IDialogSettings configPanelSettings = settings.getSection(settingsKey);
					if (configPanelSettings == null) configPanelSettings = settings.addNewSection(settingsKey);
					configPanel.doRestoreWidgetValues(configPanelSettings, idPrefix);
				}
			}
		}
	}
}
