/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal.services;

import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.services.interfaces.IMenuService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;

/**
 * Menu service implementation.
 */
public class MenuService extends AbstractService implements IMenuService {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IMenuService#isVisible(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isVisible(Object context, String contributionID) {

		if (context instanceof ILocatorNode && contributionID.endsWith("NewActionProvider")) { //$NON-NLS-1$
			return false;
		}

		if (contributionID.endsWith("GoIntoActionProvider")) { //$NON-NLS-1$
			return false;
		}

		return true;
	}

}
