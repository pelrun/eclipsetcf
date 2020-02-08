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

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IWindowsFileAttributes;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.adapters.FSTreeNodeAdapterFactory.FSTreeNodePeerNodeProvider;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider;
import org.eclipse.tcf.te.tcf.ui.tabbed.BaseTitledSection;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertyConstants;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * The property section for compress and encrypt attributes of a file/folder on Windows.
 */
public class WindowsAttributesCESection extends BaseTitledSection {

	// The original node.
	protected IFSTreeNode node;

	// The check box to display the compress attribute.
	protected Button compressButton;
	// The check box to display the encrypt attributes.
	protected Button encryptButton;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#createControls(org.eclipse.swt.widgets.Composite, org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage)
	 */
	@Override
	public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);

		compressButton = getWidgetFactory().createButton(composite, Messages.AdvancedAttributesDialog_Compress, SWT.CHECK);
		FormData data = new FormData();
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		data.top = new FormAttachment(0, ITabbedPropertyConstants.VSPACE);
		compressButton.setLayoutData(data);

		encryptButton = getWidgetFactory().createButton(composite, Messages.AdvancedAttributesDialog_Compress, SWT.CHECK);
		data = new FormData();
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		data.top = new FormAttachment(compressButton, ITabbedPropertyConstants.VSPACE);
		encryptButton.setLayoutData(data);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#updateData(org.eclipse.tcf.te.ui.interfaces.IPropertyChangeProvider)
	 */
	@Override
    protected void updateInput(IPeerNodeProvider input) {
        Assert.isTrue(input instanceof FSTreeNodePeerNodeProvider);
        this.node = ((FSTreeNodePeerNodeProvider)input).getFSTreeNode();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#refresh()
	 */
	@Override
	public void refresh() {
		SWTControlUtil.setSelection(compressButton, node != null &&
						node.getWin32Attr(IWindowsFileAttributes.FILE_ATTRIBUTE_COMPRESSED));
		SWTControlUtil.setSelection(encryptButton,	node != null &&
						node.getWin32Attr(IWindowsFileAttributes.FILE_ATTRIBUTE_ENCRYPTED));
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#getText()
	 */
	@Override
	protected String getText() {
		return Messages.AdvancedAttributesDialog_CompressEncrypt;
	}
}
