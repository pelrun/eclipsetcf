/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.ui.controls.common;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl;
import org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode;

/**
 * ComboSelectionControl
 */
public class ComboSelectionControl extends BaseEditBrowseTextControl implements IDataExchangeNode {

	/**
	 * Constructor.
	 * @param parentPage
	 */
	public ComboSelectionControl(IDialogPage parentPage) {
		super(parentPage);
		setHasHistory(true);
		setReadOnly(true);
		setIsGroup(false);
		setHideBrowseButton(true);
	}

	protected String getPropertiesKey() {
		return "Selection"; //$NON-NLS-1$
	}

	protected void setSelectedText(String text) {
		setEditFieldControlText(text);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode#setupData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void setupData(IPropertiesContainer data) {
		setSelectedText(data.getStringProperty(getPropertiesKey()));
	}

	protected String getSelectedText() {
		String selection = getEditFieldControlText().trim();
		return selection.length() > 0 ? selection : null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode#extractData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void extractData(IPropertiesContainer data) {
		data.setProperty(getPropertiesKey(), getSelectedText());
	}

	public boolean checkDataChanged(IPropertiesContainer data) {
		String newValue = getSelectedText();
		if (newValue == null) {
			String oldValue = data.getStringProperty(getPropertiesKey());
			return oldValue != null && !"".equals(oldValue.trim()); //$NON-NLS-1$
		}
		return !data.isProperty(getPropertiesKey(), newValue);
	}
}
