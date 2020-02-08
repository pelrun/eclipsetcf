/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.launch.ui.tabs.refprojects;

import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.tcf.te.launch.core.interfaces.IReferencedProjectItem;

/**
 * RefProjectsCheckStateProvider
 */
public class RefProjectsCheckStateProvider implements ICheckStateProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICheckStateProvider#isGrayed(java.lang.Object)
	 */
	@Override
	public boolean isGrayed(Object element) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICheckStateProvider#isChecked(java.lang.Object)
	 */
	@Override
	public boolean isChecked(Object element) {
		if (element instanceof IReferencedProjectItem) {
			IReferencedProjectItem item = (IReferencedProjectItem)element;
			return item.getBooleanProperty(IReferencedProjectItem.PROPERTY_ENABLED);
		}
		return false;
	}
}