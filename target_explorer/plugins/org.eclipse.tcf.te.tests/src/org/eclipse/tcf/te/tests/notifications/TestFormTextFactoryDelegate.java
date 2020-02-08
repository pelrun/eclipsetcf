/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.notifications;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.tests.activator.UIPlugin;
import org.eclipse.tcf.te.ui.notifications.delegates.DefaultFormTextFactoryDelegate;

/**
 * Test notification form text factory delegate implementation.
 */
public class TestFormTextFactoryDelegate extends DefaultFormTextFactoryDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.notifications.delegates.DefaultFormTextFactoryDelegate#getImage(java.lang.String)
	 */
	@Override
	protected Image getImage(String key) {
		Assert.isNotNull(key);
	    return UIPlugin.getImage(key);
	}
}
