/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.notifications;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.runtime.events.NotifyEvent;
import org.eclipse.tcf.te.ui.notifications.interfaces.IFormTextFactoryDelegate;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Test notification form text factory delegate implementation.
 */
public class TestFormTextFactoryDelegate implements IFormTextFactoryDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.notifications.interfaces.IFormTextFactoryDelegate#populateFormText(org.eclipse.ui.forms.widgets.FormToolkit, org.eclipse.ui.forms.widgets.FormText, org.eclipse.tcf.te.runtime.events.NotifyEvent)
	 */
	@Override
	public void populateFormText(FormToolkit toolkit, FormText widget, NotifyEvent event) {
		Assert.isNotNull(toolkit);
		Assert.isNotNull(widget);
		Assert.isNotNull(event);
	}
}
