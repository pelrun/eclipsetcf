/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.panels;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService;
import org.eclipse.tcf.te.runtime.services.interfaces.constants.IPropertiesAccessServiceConstants;
import org.eclipse.tcf.te.ui.controls.BaseDialogPageControl;
import org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.tcf.te.ui.terminals.activator.UIPlugin;
import org.eclipse.tcf.te.ui.terminals.interfaces.IConfigurationPanel;
import org.eclipse.tcf.te.ui.terminals.nls.Messages;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.ide.IDEEncoding;

/**
 * Abstract terminal configuration panel implementation.
 */
public abstract class AbstractConfigurationPanel extends AbstractWizardConfigurationPanel implements IConfigurationPanel {
	private static final String LAST_HOST_TAG = "lastHost";//$NON-NLS-1$
	private static final String HOSTS_TAG = "hosts";//$NON-NLS-1$

	// The sub-controls
	/* default */ Combo hostCombo;
	private Button deleteHostButton;
	private Combo encodingCombo;

	// The selection
	private ISelection selection;
	// A map containing the settings per host
	protected final Map<String, Map<String, String>> hostSettingsMap = new HashMap<String, Map<String, String>>();

	/**
	 * Constructor.
	 *
	 * @param parentControl The parent control. Must not be <code>null</code>!
	 */
	public AbstractConfigurationPanel(BaseDialogPageControl parentControl) {
		super(parentControl);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.IConfigurationPanel#setSelection(org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void setSelection(ISelection selection) {
		this.selection = selection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.IConfigurationPanel#getSelection()
	 */
	@Override
	public ISelection getSelection() {
		return selection;
	}

	/**
	 * Returns the host name or IP from the current selection.
	 *
	 * @return The host name or IP, or <code>null</code>.
	 */
	public String getSelectionHost() {
		ISelection selection = getSelection();
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object element = ((IStructuredSelection) selection).getFirstElement();
			IPropertiesAccessService service = ServiceManager.getInstance().getService(element, IPropertiesAccessService.class);
			if (service != null) {
				Map<String, String> props = service.getTargetAddress(element);
				if (props != null && props.containsKey(IPropertiesAccessServiceConstants.PROP_ADDRESS)) {
					return props.get(IPropertiesAccessServiceConstants.PROP_ADDRESS);
				}
			}
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel#doRestoreWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
	public void doRestoreWidgetValues(IDialogSettings settings, String idPrefix) {

		String[] hosts = settings.getArray(HOSTS_TAG);
		if (hosts != null) {
			for (int i = 0; i < hosts.length; i++) {
				String hostEntry = hosts[i];
				String[] hostString = hostEntry.split("\\|");//$NON-NLS-1$
				String hostName = hostString[0];
				if (hostString.length == 2) {
					HashMap<String, String> attr = deSerialize(hostString[1]);
					hostSettingsMap.put(hostName, attr);
				}
				else {
					hostSettingsMap.put(hostName, new HashMap<String, String>());
				}
			}
		}

		if (!isWithoutSelection()) {
			String host = getSelectionHost();
			if (host != null) {
				fillSettingsForHost(host);
			}
		}
		else {
			if (hostCombo != null) {
				fillHostCombo();
				String lastHost = settings.get(LAST_HOST_TAG);
				if (lastHost != null) {
					int index = hostCombo.indexOf(lastHost);
					if (index != -1) {
						hostCombo.select(index);
					}
					else {
						hostCombo.select(0);
					}
				}
				else {
					hostCombo.select(0);
				}
				fillSettingsForHost(hostCombo.getText());
			}
		}
	}

	protected HashMap<String, String> deSerialize(String hostString) {
		HashMap<String, String> attr = new HashMap<String, String>();

		if (hostString.length() != 0) {
			String[] hostAttrs = hostString.split("\\:");//$NON-NLS-1$
			for (int j = 0; j < hostAttrs.length; j = j + 2) {
				String key = hostAttrs[j];
				String value = hostAttrs[j + 1];
				attr.put(key, value);
			}
		}
		return attr;
	}

	protected void serialize(Map<String, String> hostEntry, StringBuffer hostString) {
		if (hostEntry.keySet().size() != 0) {
			Iterator<Entry<String, String>> nextHostAttr = hostEntry.entrySet().iterator();
			while (nextHostAttr.hasNext()) {
				Entry<String, String> entry = nextHostAttr.next();
				String attrKey = entry.getKey();
				String attrValue = entry.getValue();
				hostString.append(attrKey + ":" + attrValue + ":");//$NON-NLS-1$ //$NON-NLS-2$
			}
			hostString.deleteCharAt(hostString.length() - 1);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel#doSaveWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
	public void doSaveWidgetValues(IDialogSettings settings, String idPrefix) {
		Iterator<String> nextHost = hostSettingsMap.keySet().iterator();
		String[] hosts = new String[hostSettingsMap.keySet().size()];
		int i = 0;
		while (nextHost.hasNext()) {
			StringBuffer hostString = new StringBuffer();
			String host = nextHost.next();
			hostString.append(host + "|");//$NON-NLS-1$
			Map<String, String> hostEntry = hostSettingsMap.get(host);
			serialize(hostEntry, hostString);
			hosts[i] = hostString.toString();
			i = i + 1;
		}
		settings.put(HOSTS_TAG, hosts);
		if (isWithoutSelection()) {
			if (hostCombo != null) {
				String host = getHostFromSettings();
				if (host != null) settings.put(LAST_HOST_TAG, host);
			}
		}
	}

	protected abstract void saveSettingsForHost(boolean add);

	protected abstract void fillSettingsForHost(String host);

	protected abstract String getHostFromSettings();

	protected void removeSecurePassword(String host) {
		// noop by default
	}

	protected String getHostFromCombo() {
		if (hostCombo != null) {
			return hostCombo.getText();
		}
		return null;
	}

	protected void removeSettingsForHost(String host) {
		if (hostSettingsMap.containsKey(host)) {
			hostSettingsMap.remove(host);
		}
	}

	private List<String> getHostList() {
		List<String> hostList = new ArrayList<String>();
		hostList.addAll(hostSettingsMap.keySet());
		return hostList;
	}

	/**
	 * Fill the host combo with the stored per host setting names.
	 */
	protected void fillHostCombo() {
		if (hostCombo != null) {
			hostCombo.removeAll();
			List<String> hostList = getHostList();
			Collections.sort(hostList);
			Iterator<String> nextHost = hostList.iterator();
			while (nextHost.hasNext()) {
				String host = nextHost.next();
				hostCombo.add(host);
			}
			if (hostList.size() <= 1) {
				hostCombo.setEnabled(false);
			}
			else {
				hostCombo.setEnabled(true);

			}
			if (deleteHostButton != null) {
				if (hostList.size() == 0) {
					deleteHostButton.setEnabled(false);
				}
				else {
					deleteHostButton.setEnabled(true);
				}
			}
		}
	}

	public boolean isWithoutSelection() {
		ISelection selection = getSelection();
		if (selection == null) {
			return true;
		}
		if (selection instanceof IStructuredSelection && selection.isEmpty()) {
			return true;
		}
		return false;
	}

	public boolean isWithHostList() {
		return true;
	}

	/**
	 * Create the host selection combo.
	 *
	 * @param parent The parent composite. Must not be <code>null</code>.
	 * @param separator If <code>true</code>, a separator will be added after the controls.
	 */
	protected void createHostsUI(Composite parent, boolean separator) {
		Assert.isNotNull(parent);

		if (isWithoutSelection() && isWithHostList()) {
			Composite comboComposite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout(3, false);
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			comboComposite.setLayout(layout);
			comboComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			Label label = new Label(comboComposite, SWT.HORIZONTAL);
			label.setText(Messages.AbstractConfigurationPanel_hosts);

			hostCombo = new Combo(comboComposite, SWT.READ_ONLY);
			hostCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			hostCombo.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					String host = SWTControlUtil.getText(hostCombo);
					fillSettingsForHost(host);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});

			deleteHostButton = new Button(comboComposite, SWT.NONE);
			// deleteHostButton.setText(Messages.AbstractConfigurationPanel_delete);

			ISharedImages workbenchImages = UIPlugin.getDefault().getWorkbench().getSharedImages();
			deleteHostButton.setImage(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE).createImage());

			deleteHostButton.setToolTipText(Messages.AbstractConfigurationPanel_deleteButtonTooltip);
			deleteHostButton.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					String host = getHostFromCombo();
					if (host != null && host.length() != 0) {
						removeSettingsForHost(host);
						removeSecurePassword(host);
						fillHostCombo();
						SWTControlUtil.select(hostCombo, 0);
						host = getHostFromCombo();
						if (host != null && host.length() != 0) {
							fillSettingsForHost(host);
						}
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});

			if (separator) {
				Label sep = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
				sep.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			}
		}
	}

	/**
	 * Create the encoding selection combo.
	 *
	 * @param parent The parent composite. Must not be <code>null</code>.
	 * @param separator If <code>true</code>, a separator will be added before the controls.
	 */
	protected void createEncodingUI(Composite parent, boolean separator) {
		Assert.isNotNull(parent);

		if (separator) {
			Label sep = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
			sep.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

		Composite panel = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0; layout.marginWidth = 0;
		panel.setLayout(layout);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Label label = new Label(panel, SWT.HORIZONTAL);
		label.setText(Messages.AbstractConfigurationPanel_encoding);

		encodingCombo = new Combo(panel, SWT.READ_ONLY);
		encodingCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		fillEncodingCombo();
	}

	/**
	 * Fill the encoding combo.
	 */
	protected void fillEncodingCombo() {
		if (encodingCombo != null) {
			List<String> encodings = new ArrayList<String>();

			// Some hard-coded encodings
			encodings.add("Default (ISO-8859-1)"); //$NON-NLS-1$
			encodings.add("UTF-8"); //$NON-NLS-1$

			// The default Eclipse encoding (configured in the preferences)
			String eclipseEncoding = IDEEncoding.getResourceEncoding();
			if (eclipseEncoding != null && !encodings.contains(eclipseEncoding)) encodings.add(eclipseEncoding);

			// The default host (Java VM) encoding
			String hostEncoding = Charset.defaultCharset().displayName();
			if (!encodings.contains(hostEncoding)) encodings.add(hostEncoding);

			SWTControlUtil.setItems(encodingCombo, encodings.toArray(new String[encodings.size()]));
			SWTControlUtil.select(encodingCombo, 0);
		}
	}

	/**
	 * Select the encoding.
	 *
	 * @param encoding The encoding. Must not be <code>null</code>.
	 */
	protected void setEncoding(String encoding) {
		Assert.isNotNull(encoding);

		int index = SWTControlUtil.indexOf(encodingCombo, encoding);
		if (index != -1) SWTControlUtil.select(encodingCombo, index);
		else SWTControlUtil.setText(encodingCombo, encoding);
	}

	/**
	 * Returns the selected encoding.
	 *
	 * @return The selected encoding or <code>null</code>.
	 */
	protected String getEncoding() {
		String encoding = SWTControlUtil.getText(encodingCombo);
		return encoding != null && encoding.startsWith("Default") ? null : encoding; //$NON-NLS-1$
	}

	/**
	 * Returns if or if not the selected encoding is supported.
	 *
	 * @return <code>True</code> if the selected encoding is supported.
	 */
	protected boolean isEncodingValid() {
		try {
			String encoding = getEncoding();
			return Charset.isSupported(encoding != null ? encoding : "ISO-8859-1"); //$NON-NLS-1$
		} catch (IllegalCharsetNameException e) {
			return false;
		}
	}
}
