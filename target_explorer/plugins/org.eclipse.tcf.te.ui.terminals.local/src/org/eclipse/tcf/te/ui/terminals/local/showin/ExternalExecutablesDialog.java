/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.local.showin;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl;
import org.eclipse.tcf.te.ui.controls.common.NameControl;
import org.eclipse.tcf.te.ui.controls.file.FileSelectionControl;
import org.eclipse.tcf.te.ui.controls.validator.FileNameValidator;
import org.eclipse.tcf.te.ui.controls.validator.RegexValidator;
import org.eclipse.tcf.te.ui.controls.validator.Validator;
import org.eclipse.tcf.te.ui.jface.dialogs.CustomTrayDialog;
import org.eclipse.tcf.te.ui.jface.interfaces.IValidatingContainer;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.tcf.te.ui.terminals.local.help.IContextHelpIds;
import org.eclipse.tcf.te.ui.terminals.local.nls.Messages;
import org.eclipse.tcf.te.ui.terminals.local.showin.interfaces.IExternalExecutablesProperties;

/**
 * External executables dialog implementation.
 */
public class ExternalExecutablesDialog extends CustomTrayDialog implements IValidatingContainer {
	private final boolean edit;
	private NameControl name;
	private FileSelectionControl path;
	private BaseEditBrowseTextControl args;
	private FileSelectionControl icon;

	private Map<String, Object> executableData;

	/**
     * Constructor.
     *
	 * @param shell The parent shell or <code>null</code>.
     */
	public ExternalExecutablesDialog(Shell shell, boolean edit) {
	    super(shell, IContextHelpIds.EXTERNAL_EXECUTABLES_DIALOG);
	    this.edit = edit;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTrayDialog#createDialogAreaContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createDialogAreaContent(Composite parent) {
	    super.createDialogAreaContent(parent);

	    setDialogTitle(edit ? Messages.ExternalExecutablesDialog_title_edit : Messages.ExternalExecutablesDialog_title_add);

        Composite panel = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0; layout.marginWidth = 0;
        panel.setLayout(layout);
        GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        layoutData.widthHint = SWTControlUtil.convertWidthInCharsToPixels(panel, 50);
        panel.setLayoutData(layoutData);

        name = new NameControl(null) {
        	@Override
        	protected void configureEditFieldValidator(Validator validator) {
        	    super.configureEditFieldValidator(validator);
    			validator.setMessageText(RegexValidator.INFO_MISSING_VALUE, Messages.ExternalExecutablesDialog_name_info_missingValue);
        	}
        	@Override
        	public IValidatingContainer getValidatingContainer() {
        	    return ExternalExecutablesDialog.this;
        	}
        };
        name.setParentControlIsInnerPanel(true);
        name.setupPanel(panel);

        path = new FileSelectionControl(null) {
        	@Override
        	protected Validator doCreateEditFieldValidator() {
        		return new FileNameValidator(Validator.ATTR_MANDATORY |
	                               FileNameValidator.ATTR_MUST_EXIST |
	                               FileNameValidator.ATTR_CAN_READ |
	                               FileNameValidator.ATTR_ABSOLUT);
        	}
        	@Override
        	protected void configureEditFieldValidator(Validator validator) {
        	    super.configureEditFieldValidator(validator);
    			validator.setMessageText(FileNameValidator.INFO_MISSING_FILE_NAME, Messages.ExternalExecutablesDialog_path_info_missingFilename);
    			validator.setMessageText(FileNameValidator.ERROR_MUST_EXIST, Messages.ExternalExecutablesDialog_path_error_mustExist);
    			validator.setMessageText(FileNameValidator.ERROR_INVALID_FILE_NAME, Messages.ExternalExecutablesDialog_path_error_invalidFilename);
    			validator.setMessageText(FileNameValidator.ERROR_NO_ACCESS, Messages.ExternalExecutablesDialog_path_error_noAccess);
    			validator.setMessageText(FileNameValidator.ERROR_IS_RELATIV, Messages.ExternalExecutablesDialog_path_error_isRelativ);


        	}
        	@Override
        	public IValidatingContainer getValidatingContainer() {
        	    return ExternalExecutablesDialog.this;
        	}
        };
        path.setIsGroup(false);
        path.setParentControlIsInnerPanel(true);
        path.setEditFieldLabel(Messages.ExternalExecutablesDialog_field_path);
        path.setupPanel(panel);

        args = new BaseEditBrowseTextControl(null) {
        	@Override
        	public IValidatingContainer getValidatingContainer() {
        	    return ExternalExecutablesDialog.this;
        	}
        };
        args.setEditFieldLabel(Messages.ExternalExecutablesDialog_field_args);
        args.setParentControlIsInnerPanel(true);
        args.setupPanel(panel);

        icon = new FileSelectionControl(null) {
        	@Override
        	protected Validator doCreateEditFieldValidator() {
        		return new FileNameValidator(FileNameValidator.ATTR_MUST_EXIST | FileNameValidator.ATTR_CAN_READ | FileNameValidator.ATTR_ABSOLUT);
        	}
        	@Override
        	protected void configureEditFieldValidator(Validator validator) {
        	    super.configureEditFieldValidator(validator);
    			validator.setMessageText(FileNameValidator.ERROR_MUST_EXIST, Messages.ExternalExecutablesDialog_icon_error_mustExist);
    			validator.setMessageText(FileNameValidator.ERROR_INVALID_FILE_NAME, Messages.ExternalExecutablesDialog_icon_error_invalidFilename);
    			validator.setMessageText(FileNameValidator.ERROR_NO_ACCESS, Messages.ExternalExecutablesDialog_icon_error_noAccess);
    			validator.setMessageText(FileNameValidator.ERROR_IS_RELATIV, Messages.ExternalExecutablesDialog_icon_error_isRelativ);


        	}
        	@Override
        	public IValidatingContainer getValidatingContainer() {
        	    return ExternalExecutablesDialog.this;
        	}
        };
        icon.setIsGroup(false);
        icon.setParentControlIsInnerPanel(true);
        icon.setEditFieldLabel(Messages.ExternalExecutablesDialog_field_icon);
        icon.setupPanel(panel);


        if (executableData != null) {
        	String value = (String)executableData.get(IExternalExecutablesProperties.PROP_NAME);
        	name.setEditFieldControlText(value != null && !"".equals(value.trim()) ? value : ""); //$NON-NLS-1$ //$NON-NLS-2$
        	value = (String)executableData.get(IExternalExecutablesProperties.PROP_PATH);
        	path.setEditFieldControlText(value != null && !"".equals(value.trim()) ? value : ""); //$NON-NLS-1$ //$NON-NLS-2$
        	value = (String)executableData.get(IExternalExecutablesProperties.PROP_ARGS);
        	args.setEditFieldControlText(value != null && !"".equals(value.trim()) ? value : ""); //$NON-NLS-1$ //$NON-NLS-2$
        	value = (String)executableData.get(IExternalExecutablesProperties.PROP_ICON);
        	icon.setEditFieldControlText(value != null && !"".equals(value.trim()) ? value : ""); //$NON-NLS-1$ //$NON-NLS-2$
        }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TrayDialog#createButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createButtonBar(Composite parent) {
	    Control control = super.createButtonBar(parent);
	    validate();
	    return control;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButton(org.eclipse.swt.widgets.Composite, int, java.lang.String, boolean)
	 */
	@Override
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		if (IDialogConstants.OK_ID == id && !edit) {
			label = Messages.ExternalExecutablesDialog_button_add;
		}
	    return super.createButton(parent, id, label, defaultButton);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTrayDialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		if (name != null && path != null) {
			// Extract the executable properties
			if (executableData == null) executableData = new HashMap<String, Object>();

			String value = name.getEditFieldControlText();
			if (value != null && !"".equals(value.trim())) { //$NON-NLS-1$
				executableData.put(IExternalExecutablesProperties.PROP_NAME, value);
			} else {
				executableData.remove(IExternalExecutablesProperties.PROP_NAME);
			}

			value = path.getEditFieldControlText();
			if (value != null && !"".equals(value.trim())) { //$NON-NLS-1$
				executableData.put(IExternalExecutablesProperties.PROP_PATH, value);
			} else {
				executableData.remove(IExternalExecutablesProperties.PROP_PATH);
			}

			value = args.getEditFieldControlText();
			if (value != null && !"".equals(value.trim())) { //$NON-NLS-1$
				executableData.put(IExternalExecutablesProperties.PROP_ARGS, value);
			} else {
				executableData.remove(IExternalExecutablesProperties.PROP_ARGS);
			}

			value = icon.getEditFieldControlText();
			if (value != null && !"".equals(value.trim())) { //$NON-NLS-1$
				executableData.put(IExternalExecutablesProperties.PROP_ICON, value);
			} else {
				executableData.remove(IExternalExecutablesProperties.PROP_ICON);
			}
		} else {
			executableData = null;
		}
	    super.okPressed();
	}

	@Override
	protected void cancelPressed() {
		// If the user pressed cancel, the dialog needs to return null
		executableData = null;
	    super.cancelPressed();
	}

	/**
	 * Returns the executable properties the user entered.
	 *
	 * @return The executable properties or <code>null</code>.
	 */
	public Map<String, Object> getExecutableData() {
		return executableData;
	}

	/**
	 * Set or reset the executable properties. This method has effect
	 * only if called before opening the dialog.
	 *
	 * @param data The executable properties or <code>null</code>.
	 */
	public void setExecutableData(Map<String, Object> data) {
		if (data == null) {
			executableData = data;
		} else {
			executableData = new HashMap<String, Object>(data);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.interfaces.IValidatingContainer#validate()
	 */
	@Override
	public void validate() {
		boolean valid = true;

		if (name != null) {
			valid = name.isValid();
		}

		if (path != null) {
			valid |= path.isValid();
		}

		if (args != null) {
			valid |= args.isValid();
		}

		if (icon != null) {
			valid |= icon.isValid();
		}

		SWTControlUtil.setEnabled(getButton(IDialogConstants.OK_ID), valid);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.interfaces.IValidatingContainer#setMessage(java.lang.String, int)
	 */
	@Override
    public void setMessage(String message, int messageType) {
    }
}
