/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.tabbed;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;

import org.eclipse.core.runtime.Assert;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNodeWorkingCopy;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.adapters.FSTreeNodeAdapterFactory.FSTreeNodePeerNodeProvider;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider;
import org.eclipse.tcf.te.tcf.ui.tabbed.BaseTitledSection;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * The section that displays the basic information of a folder.
 */
public class BasicFolderSection extends BaseTitledSection {
	// The formatter for the size of a file.
	private static final DecimalFormat SIZE_FORMAT = new DecimalFormat();

	// The original node to be displayed and edited.
	protected IFSTreeNode node;
	// The copy used to be edited.
	protected IFSTreeNodeWorkingCopy clone;

	// The text for the name of the node.
	protected Text nameText;
	// The text for the type of the node.
	protected Text typeText;
	// The text for the location of the node.
	protected Text locationText;
	// The text for the modified time of the node.
	protected Text modifiedText;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#createControls(org.eclipse.swt.widgets.Composite, org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage)
	 */
	@Override
    public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);
		nameText = createTextField(null, Messages.GeneralInformationPage_Name);
		typeText = createTextField(nameText, Messages.GeneralInformationPage_Type);
		locationText = createWrapTextField(typeText, Messages.GeneralInformationPage_Location);
		modifiedText = createTextField(locationText, Messages.GeneralInformationPage_Modified);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#updateData(org.eclipse.tcf.te.ui.interfaces.IPropertyChangeProvider)
	 */
	@Override
    protected void updateInput(IPeerNodeProvider input) {
         Assert.isTrue(input instanceof FSTreeNodePeerNodeProvider);
         this.node = ((FSTreeNodePeerNodeProvider)input).getFSTreeNode();
         this.clone = node.createWorkingCopy();
     }

	 /*
	  * (non-Javadoc)
	  * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#refresh()
	  */
	@Override
    public void refresh() {
		SWTControlUtil.setText(nameText, clone != null ? clone.getName() : ""); //$NON-NLS-1$
		SWTControlUtil.setText(typeText, clone != null ? clone.getFileTypeLabel() : ""); //$NON-NLS-1$
		String location = clone == null || clone.isRootDirectory() ? Messages.GeneralInformationPage_Computer : clone.getLocation();
		SWTControlUtil.setText(locationText, location);
		SWTControlUtil.setText(modifiedText, clone != null && clone.getModificationTime() != 0 ? getDateText(clone.getModificationTime()) : ""); //$NON-NLS-1$
		super.refresh();
    }

	/**
	 * Get the string of the specific time using the formatter, DATE_FORMAT.
	 *
	 * @param time The time to be formatted.
	 * @return The string in the format of DATE_FORMAT.
	 */
	protected String getDateText(long time) {
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
		return dateFormat.format(new Date(time));
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

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#getText()
	 */
	@Override
    protected String getText() {
	    return Messages.BasicFolderSection_BasicInfoText;
    }
}