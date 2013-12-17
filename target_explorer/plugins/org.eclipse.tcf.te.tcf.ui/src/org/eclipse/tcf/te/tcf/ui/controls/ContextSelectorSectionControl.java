/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.ui.controls;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.tcf.te.tcf.locator.model.Model;
import org.eclipse.tcf.te.tcf.ui.navigator.ContentProvider;
import org.eclipse.tcf.te.ui.views.controls.AbstractContextSelectorControl;
import org.eclipse.tcf.te.ui.views.navigator.DelegatingLabelProvider;
import org.eclipse.tcf.te.ui.views.sections.AbstractContextSelectorSection;

/**
 * Locator model launch context selector control.
 */
public class ContextSelectorSectionControl extends AbstractContextSelectorControl {

	protected final AbstractContextSelectorSection section;

	/**
	 * Constructor.
	 *
	 * @param section The parent context selector section. Must not be <code>null</code>.
	 * @param parentPage The parent target connection page this control is embedded in. Might be
	 *            <code>null</code> if the control is not associated with a page.
	 */
    public ContextSelectorSectionControl(AbstractContextSelectorSection section, IDialogPage parentPage) {
        super(parentPage);
        Assert.isNotNull(section);
        this.section = section;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.tabs.launchcontext.AbstractContextSelectorControl#getInitialViewerInput()
	 */
	@Override
	protected Object getInitialViewerInput() {
		return Model.getPeerModel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.controls.AbstractContextSelectorControl#onCheckStateChanged(java.lang.Object, boolean)
	 */
	@Override
	protected void onCheckStateChanged(Object element, boolean checked) {
	    super.onCheckStateChanged(element, checked);
	    section.dataChanged();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.tabs.launchcontext.AbstractContextSelectorControl#doConfigureTreeContentAndLabelProvider(org.eclipse.jface.viewers.TreeViewer)
	 */
	@Override
	protected void doConfigureTreeContentAndLabelProvider(TreeViewer viewer) {
		viewer.setContentProvider(new ContentProvider(true));
		DelegatingLabelProvider labelProvider = new DelegatingLabelProvider();
		viewer.setLabelProvider(new DecoratingLabelProvider(labelProvider, labelProvider));
	}
}
