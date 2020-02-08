/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.search;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.ui.navigator.runtime.LabelProviderDelegate;
import org.eclipse.tcf.te.ui.interfaces.ISearchMatcher;
import org.eclipse.tcf.te.ui.utils.StringMatcher;
/**
 * The ISearchMatcher implementation for a Process Tree Node.
 */
public class ProcessNodeGeneralMatcher implements ISearchMatcher {
	// Whether it is case sensitive
	private boolean fCaseSensitive;
	// The string matcher for matching.
	private StringMatcher fStringMatcher;
	// The label provider used to get a text for a process.
	private ILabelProvider labelProvider = new LabelProviderDelegate();
	// The current target names.
	private String fTargetName;

	/**
	 * Constructor with options.
	 *
	 * @param caseSensitive
	 * @param targetName
	 */
	public ProcessNodeGeneralMatcher(boolean caseSensitive, String targetName) {
		fCaseSensitive = caseSensitive;
		fTargetName = targetName;
		fStringMatcher = new StringMatcher(fTargetName, !fCaseSensitive, false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchMatcher#match(java.lang.Object)
	 */
	@Override
	public boolean match(Object context) {
		if (context == null) return false;
		if (context instanceof IProcessContextNode) {
			IProcessContextNode node = (IProcessContextNode) context;
			String text = labelProvider.getText(node);
			if (text != null) {
				return fStringMatcher.match(text);
			}
		}
		return false;
	}
}
