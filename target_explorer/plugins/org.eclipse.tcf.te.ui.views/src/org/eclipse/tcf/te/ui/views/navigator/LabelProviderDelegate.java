/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.navigator;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.ui.views.interfaces.ICategory;
import org.eclipse.tcf.te.ui.views.navigator.nodes.NewWizardNode;

/**
 * Category label provider delegate implementation.
 */
public class LabelProviderDelegate extends LabelProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		if (element instanceof ICategory) {
			return ((ICategory)element).getLabel();
		}
		if (element instanceof NewWizardNode) {
			return ((NewWizardNode)element).getLabel();
		}

	    return super.getText(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		if (element instanceof ICategory) {
			return ((ICategory)element).getImage();
		}
		if (element instanceof NewWizardNode) {
			return ((NewWizardNode)element).getImage();
		}
	    return super.getImage(element);
	}
}
