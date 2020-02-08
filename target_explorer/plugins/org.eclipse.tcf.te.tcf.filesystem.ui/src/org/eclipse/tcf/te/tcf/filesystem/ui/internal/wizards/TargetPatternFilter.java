/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.wizards;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.ui.views.navigator.DelegatingLabelProvider;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * A class that handles filtering wizard node items based on a supplied matching
 * string and keywords
 * <p>
 * This class is copied and adapted from <code>org.eclipse.ui.internal.dialogs.WizardPatternFilter</code>.
 */
public class TargetPatternFilter extends PatternFilter {
    private DelegatingLabelProvider targetLabelProvider = new DelegatingLabelProvider();
	/**
	 * Create a new instance of a WizardPatternFilter
	 */
	public TargetPatternFilter() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.internal.dialogs.PatternFilter#isElementSelectable(java.lang.Object)
	 */
	@Override
    public boolean isElementSelectable(Object element) {
		return element instanceof IPeerNode;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.internal.dialogs.PatternFilter#isElementMatch(org.eclipse.jface.viewers.Viewer, java.lang.Object)
	 */
	@Override
    protected boolean isLeafMatch(Viewer viewer, Object element) {
		if ( element instanceof IPeerNode) {
			String text = targetLabelProvider.getText(element);
			if (wordMatches(text)) {
				return true;
			}
		}
		return false;
	}
}
