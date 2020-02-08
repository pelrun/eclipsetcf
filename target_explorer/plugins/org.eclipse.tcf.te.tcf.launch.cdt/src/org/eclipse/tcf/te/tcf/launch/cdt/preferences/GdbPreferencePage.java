/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.cdt.preferences;

import java.util.regex.Pattern;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.tcf.te.runtime.preferences.ScopedEclipsePreferences;
import org.eclipse.tcf.te.tcf.launch.cdt.activator.Activator;
import org.eclipse.tcf.te.tcf.launch.cdt.nls.Messages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * GDB launcher preference page implementation.
 */
public class GdbPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	// References to the field editors
	private StringFieldEditor portList;
	private StringFieldEditor mappedToPortList;
	private StringFieldEditor portListAttach;
	private StringFieldEditor mappedToPortListAttach;


	// The preference store used internally for the field editors
	private IPreferenceStore store;

	/**
	 * Port list field editor implementation.
	 */
	private abstract class PortListFieldEditor extends StringFieldEditor {
		private Pattern valid = Pattern.compile("[1-9][0-9,]*"); //$NON-NLS-1$

		/**
		 * Constructor.
		 *
		 * @param name The name of the preference this field editor works on.
		 * @param labelText The label text.
		 * @param parent the parent of the field editor's control
		 */
		public PortListFieldEditor(String name, String labelText, Composite parent) {
			super(name, labelText, parent);
			this.setEmptyStringAllowed(true);
			this.setErrorMessage(NLS.bind(Messages.GdbPreferencePage_portList_error, getErrorLabel()));
		}

		/**
		 * Returns the label of the port list for the error message.
		 */
		protected abstract String getErrorLabel();

		/* (non-Javadoc)
		 * @see org.eclipse.jface.preference.StringFieldEditor#doCheckState()
		 */
		@Override
		protected boolean doCheckState() {
			if ("".equals(getStringValue())) { //$NON-NLS-1$
				return isEmptyStringAllowed();
			} else if (valid.matcher(getStringValue()).matches()) {
				return true;
			}
			return false;
		}
	}

	/**
	 * Constructor
	 */
	public GdbPreferencePage() {
    	super(FieldEditorPreferencePage.GRID);

    	// The internal preference store never needs saving
    	store = new PreferenceStore() {
    		@Override
    		public boolean needsSaving() {
    		    return false;
    		}
    	};
	}


	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
		((GridLayout)parent.getLayout()).makeColumnsEqualWidth = false;

		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0; layout.marginWidth = 0;
		panel.setLayout(layout);
		panel.setFont(parent.getFont());

		Label label = new Label(panel, SWT.HORIZONTAL);
		label.setText(Messages.GdbPreferencePage_label);
		GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		label.setLayoutData(layoutData);

		Group appLaunchGroup = new Group(panel, SWT.NONE);
		appLaunchGroup.setText(Messages.GdbPreferencePage_appLaunchGroup_label);
		appLaunchGroup.setLayout(new GridLayout());
		appLaunchGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite panel2 = new Composite(appLaunchGroup, SWT.NONE);
		panel2.setLayout(new GridLayout());
		panel2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		portList = new PortListFieldEditor("portList", Messages.GdbPreferencePage_portList_label, panel2) { //$NON-NLS-1$
			@Override
			protected String getErrorLabel() {
			    return Messages.GdbPreferencePage_portList_error_portList;
			}
		};
		addField(portList);

		mappedToPortList = new PortListFieldEditor("mappedToPortList", Messages.GdbPreferencePage_mappedToPortList_label, panel2) { //$NON-NLS-1$
			@Override
			protected String getErrorLabel() {
			    return Messages.GdbPreferencePage_portList_error_mappedToPortList;
			}
		};
		addField(mappedToPortList);

		Group attachLaunchGroup = new Group(panel, SWT.NONE);
		attachLaunchGroup.setText(Messages.GdbPreferencePage_attachLaunchGroup_label);
		attachLaunchGroup.setLayout(new GridLayout());
		attachLaunchGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite panel3 = new Composite(attachLaunchGroup, SWT.NONE);
		panel3.setLayout(new GridLayout());
		panel3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		portListAttach = new PortListFieldEditor("portListAttach", Messages.GdbPreferencePage_portList_label, panel3) { //$NON-NLS-1$
			@Override
			protected String getErrorLabel() {
			    return Messages.GdbPreferencePage_portList_error_portList;
			}
		};
		addField(portListAttach);

		mappedToPortListAttach = new PortListFieldEditor("mappedToPortListAttach", Messages.GdbPreferencePage_mappedToPortList_label, panel3) { //$NON-NLS-1$
			@Override
			protected String getErrorLabel() {
			    return Messages.GdbPreferencePage_portList_error_mappedToPortList;
			}
		};
		addField(mappedToPortListAttach);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#getPreferenceStore()
	 */
	@Override
	public IPreferenceStore getPreferenceStore() {
	    return store;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#initialize()
	 */
	@Override
	protected void initialize() {
        ScopedEclipsePreferences prefs = Activator.getScopedPreferences();

        String port = prefs.getString(IPreferenceKeys.PREF_GDBSERVER_PORT);
        String portAlternatives = prefs.getString(IPreferenceKeys.PREF_GDBSERVER_PORT_ALTERNATIVES);

        String portList = ""; //$NON-NLS-1$
        if (port != null && !"".equals(port)) portList += port.trim(); //$NON-NLS-1$
        if (portAlternatives != null && !"".equals(portAlternatives)) portList += "," + portAlternatives.trim(); //$NON-NLS-1$ //$NON-NLS-2$

		store.setDefault("portList", portList); //$NON-NLS-1$
		store.setValue("portList", portList); //$NON-NLS-1$

        String mappedTo = prefs.getString(IPreferenceKeys.PREF_GDBSERVER_PORT_MAPPED_TO);
        String mappedToAlternatives = prefs.getString(IPreferenceKeys.PREF_GDBSERVER_PORT_MAPPED_TO_ALTERNATIVES);

        String mappedToList = ""; //$NON-NLS-1$
        if (mappedTo != null && !"".equals(mappedTo)) mappedToList += mappedTo.trim(); //$NON-NLS-1$
        if (mappedToAlternatives != null && !"".equals(mappedToAlternatives)) mappedToList += "," + mappedToAlternatives.trim(); //$NON-NLS-1$ //$NON-NLS-2$

		store.setDefault("mappedToPortList", mappedToList); //$NON-NLS-1$
		store.setValue("mappedToPortList", mappedToList); //$NON-NLS-1$

        port = prefs.getString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH);
        portAlternatives = prefs.getString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_ALTERNATIVES);

        portList = ""; //$NON-NLS-1$
        if (port != null && !"".equals(port)) portList += port.trim(); //$NON-NLS-1$
        if (portAlternatives != null && !"".equals(portAlternatives)) portList += "," + portAlternatives.trim(); //$NON-NLS-1$ //$NON-NLS-2$

		store.setDefault("portListAttach", portList); //$NON-NLS-1$
		store.setValue("portListAttach", portList); //$NON-NLS-1$

        mappedTo = prefs.getString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_MAPPED_TO);
        mappedToAlternatives = prefs.getString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_MAPPED_TO_ALTERNATIVES);

        mappedToList = ""; //$NON-NLS-1$
        if (mappedTo != null && !"".equals(mappedTo)) mappedToList += mappedTo.trim(); //$NON-NLS-1$
        if (mappedToAlternatives != null && !"".equals(mappedToAlternatives)) mappedToList += "," + mappedToAlternatives.trim(); //$NON-NLS-1$ //$NON-NLS-2$

		store.setDefault("mappedToPortListAttach", mappedToList); //$NON-NLS-1$
		store.setValue("mappedToPortListAttach", mappedToList); //$NON-NLS-1$

		// Load values into field editors
	    super.initialize();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
	    boolean success = super.performOk();

	    if (success) {
	        ScopedEclipsePreferences prefs = Activator.getScopedPreferences();

	        String portList = store.getString("portList"); //$NON-NLS-1$

	        if ("".equals(portList.trim())) { //$NON-NLS-1$
	        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT, null);
	        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_ALTERNATIVES, null);
	        } else {
	        	int idx = portList.indexOf(',');
	        	if (idx == -1) {
		        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT, portList.trim());
		        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_ALTERNATIVES, null);
	        	} else {
		        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT, portList.substring(0, idx).trim());
		        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_ALTERNATIVES, portList.length() > (idx + 1) ? portList.substring(idx + 1) : null);
	        	}
	        }

	        portList = store.getString("mappedToPortList"); //$NON-NLS-1$

	        if ("".equals(portList.trim())) { //$NON-NLS-1$
	        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_MAPPED_TO, null);
	        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_MAPPED_TO_ALTERNATIVES, null);
	        } else {
	        	int idx = portList.indexOf(',');
	        	if (idx == -1) {
		        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_MAPPED_TO, portList.trim());
		        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_MAPPED_TO_ALTERNATIVES, null);
	        	} else {
		        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_MAPPED_TO, portList.substring(0, idx).trim());
		        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_MAPPED_TO_ALTERNATIVES, portList.length() > (idx + 1) ? portList.substring(idx + 1) : null);
	        	}
	        }

	        portList = store.getString("portListAttach"); //$NON-NLS-1$

	        if ("".equals(portList.trim())) { //$NON-NLS-1$
	        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH, null);
	        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_ALTERNATIVES, null);
	        } else {
	        	int idx = portList.indexOf(',');
	        	if (idx == -1) {
		        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH, portList.trim());
		        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_ALTERNATIVES, null);
	        	} else {
		        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH, portList.substring(0, idx).trim());
		        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_ALTERNATIVES, portList.length() > (idx + 1) ? portList.substring(idx + 1) : null);
	        	}
	        }

	        portList = store.getString("mappedToPortListAttach"); //$NON-NLS-1$

	        if ("".equals(portList.trim())) { //$NON-NLS-1$
	        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_MAPPED_TO, null);
	        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_MAPPED_TO_ALTERNATIVES, null);
	        } else {
	        	int idx = portList.indexOf(',');
	        	if (idx == -1) {
		        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_MAPPED_TO, portList.trim());
		        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_MAPPED_TO_ALTERNATIVES, null);
	        	} else {
		        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_MAPPED_TO, portList.substring(0, idx).trim());
		        	prefs.putString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_MAPPED_TO_ALTERNATIVES, portList.length() > (idx + 1) ? portList.substring(idx + 1) : null);
	        	}
	        }
	    }

	    return success;
	}

}
