/*********************************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * William Chen (Wind River)	- [345384]Provide property pages for remote file system nodes
 *                                [361322]Minor improvements to the properties dialog of a file.
 *********************************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.properties;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNodeWorkingCopy;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations.UiExecutor;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * The general information page of a file's properties dialog.
 */
public class GeneralInformationPage extends PropertyPage {
	// The formatter for the size of a file.
	private static final DecimalFormat SIZE_FORMAT = new DecimalFormat();
	// The original node.
	IFSTreeNode node;
	// Cloned node for modification.
	IFSTreeNodeWorkingCopy fWorkingCopy;
	// The button of "Read-Only"
	Button btnReadOnly;
	// The button of "Hidden"
	Button btnHidden;
	// The button of "Permissions"
	Button[] btnPermissions;

	/**
	 * Create a horizontal separator between field sections.
	 *
	 * @param parent
	 *            The parent composite of the separator.
	 */
	protected void createSeparator(Composite parent) {
		Label label = new Label(parent, SWT.SEPARATOR | SWT.SHADOW_ETCHED_IN | SWT.HORIZONTAL);
		GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		data.horizontalSpan = 2;
		label.setLayoutData(data);
	}

	/**
	 * Create a field displaying the a specific value with a specific label.
	 *
	 * @param text
	 *            The label text for the field.
	 * @param value
	 *            The value to be displayed.
	 * @param parent
	 *            The parent composite of the field.
	 */
	protected void createField(String text, String value, Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalAlignment = SWT.LEFT;
		data.verticalAlignment = SWT.TOP;
		label.setLayoutData(data);
		Text txt = new Text(parent, SWT.WRAP | SWT.READ_ONLY);
		data = new GridData();
		data.verticalAlignment = SWT.TOP;
		data.widthHint = 300;
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = GridData.FILL;
		txt.setLayoutData(data);
		txt.setBackground(txt.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		txt.setText(value);
	}

	/**
	 * Get the string of the file size using using the formatter, SIZE_FORMAT.
	 *
	 * @param size
	 *            The size of the file to be formatted.
	 * @return The string in the format of SIZE_FORMAT.
	 */
	protected String getSizeText(long size) {
		return NLS.bind(Messages.GeneralInformationPage_FileSizeInfo, SIZE_FORMAT.format(size / 1024), SIZE_FORMAT.format(size));
	}

	/**
	 * Get the string of the specific time using the formatter, DATE_FORMAT.
	 *
	 * @param time
	 *            The time to be formatted.
	 * @return The string in the format of DATE_FORMAT.
	 */
	protected String getDateText(long time) {
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
		return dateFormat.format(new Date(time));
	}

	/**
	 * Create the attributes section for a Windows file/folder.
	 *
	 * @param parent
	 *            The parent composite on which it is created.
	 */
	protected void createAttributesSection(Composite parent) {
		// Attributes
		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.GeneralInformationPage_Attributes);
		GridData data = new GridData();
		data.horizontalAlignment = SWT.LEFT;
		label.setLayoutData(data);

		Composite attr = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(3, true);
		layout.marginHeight = 0;
		attr.setLayout(layout);
		// Read-only
		btnReadOnly = new Button(attr, SWT.CHECK);
		btnReadOnly.setText(Messages.GeneralInformationPage_ReadOnly);
		// Only the owner can edit this property
		btnReadOnly.setEnabled(node.isAgentOwner());
		btnReadOnly.addSelectionListener(new SelectionAdapter(){
			@Override
            public void widgetSelected(SelectionEvent e) {
				if(btnReadOnly.getSelection()!=fWorkingCopy.isReadOnly()){
					fWorkingCopy.setReadOnly(btnReadOnly.getSelection());
				}
            }
		});
		// Hidden
		btnHidden = new Button(attr, SWT.CHECK);
		btnHidden.setText(Messages.GeneralInformationPage_Hidden);
		// Only the owner can edit this property
		btnHidden.setEnabled(node.isAgentOwner());
		btnHidden.addSelectionListener(new SelectionAdapter(){
			@Override
            public void widgetSelected(SelectionEvent e) {
				Button btnHidden = (Button) e.getSource();
				fWorkingCopy.setHidden(btnHidden.getSelection());
            }
		});
		// Advanced Attributes
		Button btnAdvanced = new Button(attr, SWT.PUSH);
		btnAdvanced.setText(Messages.GeneralInformationPage_Advanced);
		btnAdvanced.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showAdvancedAttributes();
			}
		});
		// Update the attribute values.
		updateAttributes();
	}

	/**
	 * Update the value of attributes section.
	 */
	private void updateAttributes() {
		btnReadOnly.setSelection(fWorkingCopy.isReadOnly());
		btnHidden.setSelection(fWorkingCopy.isHidden());
	}

	/**
	 * Show the advanced attributes dialog for the specified file/folder.
	 */
	void showAdvancedAttributes() {
		AdvancedAttributesDialog dialog = new AdvancedAttributesDialog(this.getShell(), fWorkingCopy.createWorkingCopy());
		if (dialog.open() == Window.OK) {
			IFSTreeNodeWorkingCopy result = dialog.getResult();
			UiExecutor.execute(result.operationCommit());
		}
	}

	/**
	 * Create the permissions section for a Unix/Linux file/folder.
	 *
	 * @param parent
	 *            The parent composite on which it is created.
	 */
	protected void createPermissionsSection(Composite parent) {
		GridLayout gridLayout;
		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.GeneralInformationPage_PermissionText);
		GridData data = new GridData();
		data.horizontalAlignment = SWT.LEFT;
		data.verticalAlignment = SWT.TOP;
		label.setLayoutData(data);
		Composite perms = new Composite(parent, SWT.NONE);
		gridLayout = new GridLayout(2, false);
		gridLayout.marginHeight = 0;
		perms.setLayout(gridLayout);
		btnPermissions = new Button[9];
		createPermissionGroup(perms, 0,
				Messages.PermissionsGroup_UserPermissions);
		createPermissionGroup(perms, 3,
				Messages.PermissionsGroup_GroupPermissions);
		createPermissionGroup(perms, 6,
				Messages.PermissionsGroup_OtherPermissions);
		// Update the permission values.
		updatePermissions();
	}

	/**
	 * Create a permission group for a role, such as a user, a group or others.
	 *
	 * @param parent
	 *            The parent composite.
	 * @param bit
	 *            The permission bit index.
	 * @param header
	 *            The group's header label.
	 */
	protected void createPermissionGroup(Composite parent, int bit, String header) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(header);
		GridData data = new GridData();
		data.horizontalAlignment = SWT.LEFT;
		label.setLayoutData(data);
		Composite group = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(3, true);
		layout.marginHeight = 0;
		group.setLayout(layout);
		createPermissionButton(Messages.PermissionsGroup_Readable, bit, group);
		createPermissionButton(Messages.PermissionsGroup_Writable, bit + 1, group);
		createPermissionButton(Messages.PermissionsGroup_Executable, bit + 2, group);
	}

	/**
	 * Create a check-box field for a single permission item.
	 *
	 * @param label
	 *            The label of the permission.
	 * @param index
	 *            The index of current permission bit mask index.
	 * @param parent
	 *            The parent to hold the check-box field.
	 */
	private void createPermissionButton(String label, final int index, Composite parent) {
		btnPermissions[index] = new Button(parent, SWT.CHECK);
		btnPermissions[index].setText(label);
		// Only the owner can edit its permission.
		btnPermissions[index].setEnabled(node.isAgentOwner());
		btnPermissions[index].addSelectionListener(new SelectionAdapter(){
			@Override
            public void widgetSelected(SelectionEvent e) {
				int bit = 1 << (8 - index);
				fWorkingCopy.setPermission(bit, btnPermissions[index].getSelection());
            }
		});
	}

	/**
	 * Update the value of permissions section.
	 */
	private void updatePermissions(){
		for (int i = 0; i < 9; i++) {
			final int bit = 1 << (8 - i);
			btnPermissions[i].setSelection(fWorkingCopy.getPermission(bit));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
    protected void performDefaults() {
		fWorkingCopy = node.createWorkingCopy();
		if (node.isWindowsNode()) {
			updateAttributes();
		} else {
			updatePermissions();
		}
	    super.performDefaults();
    }

	@Override
    public boolean performOk() {
		if (hasAttrsChanged()) {
			UiExecutor.execute(fWorkingCopy.operationCommit());
		}
		return getErrorMessage() == null;
    }

	/**
	 * If the attributes has been changed.
	 * @return If the attributes has been changed.
	 */
	private boolean hasAttrsChanged() {
		return fWorkingCopy.isDirty();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		IAdaptable element = getElement();
		Assert.isTrue(element instanceof IFSTreeNode);

		node = (IFSTreeNode) element;
		fWorkingCopy = node.createWorkingCopy();
		Composite page = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(2, false);
		page.setLayout(gridLayout);
		// Field "Name"
		createField(Messages.GeneralInformationPage_Name, fWorkingCopy.getName(), page);
		// Field "Type"
		String fileType = fWorkingCopy.getFileTypeLabel();
		createField(Messages.GeneralInformationPage_Type, fileType, page);
		// Field "Location"

		String location = fWorkingCopy.isFileSystem() || fWorkingCopy.isRootDirectory() ?
						Messages.GeneralInformationPage_Computer : fWorkingCopy.getLocation();
		createField(Messages.GeneralInformationPage_Location, location, page);
		// Field "Size"
		if (fWorkingCopy.isFile()) {
			long size = fWorkingCopy.getSize();
			createField(Messages.GeneralInformationPage_Size, size > 0 ? getSizeText(size) : "", page); //$NON-NLS-1$
		}
		// Field "Modified"
		long lm = fWorkingCopy.getModificationTime();
		createField(Messages.GeneralInformationPage_Modified, lm != 0 ? getDateText(lm) : "", page); //$NON-NLS-1$
		// Field "Accessed"
		if (fWorkingCopy.isFile()) {
			long at = fWorkingCopy.getAccessTime();
			createField(Messages.GeneralInformationPage_Accessed, at != 0 ? getDateText(at) : "", page); //$NON-NLS-1$
		}
		createSeparator(page);
		if (fWorkingCopy.isWindowsNode()) {
			createAttributesSection(page);
		} else {
			createPermissionsSection(page);
		}
		return page;
	}
}
