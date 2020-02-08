/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.internal.services;

import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.services.interfaces.IMenuService;
import org.eclipse.tcf.te.ui.views.interfaces.ICategory;

/**
 * Menu service implementation.
 */
public class MenuService extends AbstractService implements IMenuService {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IMenuService#isVisible(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isVisible(Object context, String contributionID) {

		if (context instanceof ICategory) {
    		if (contributionID.endsWith("menu.showIn")) { //$NON-NLS-1$
    			return false;
    		}
    		if (contributionID.endsWith("file.import") || contributionID.endsWith("file.export")) { //$NON-NLS-1$ //$NON-NLS-2$
    			return false;
    		}
    	}

	    return true;
	}

}
