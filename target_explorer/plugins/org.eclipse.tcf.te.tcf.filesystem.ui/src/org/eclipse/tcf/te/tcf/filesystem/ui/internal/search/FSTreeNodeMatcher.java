/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.search;

import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.ui.interfaces.ISearchMatcher;
import org.eclipse.tcf.te.ui.utils.StringMatcher;
/**
 * The ISearchMatcher implementation for IFSTreeNode.
 */
public class FSTreeNodeMatcher implements ISearchMatcher {
	// Whether it is case sensitive
	private boolean fCaseSensitive;
	// Whether it is precise matching.
	private boolean fMatchPrecise;
	// The string matcher used for matching.
	private StringMatcher fStringMatcher;
	// The current selected target simulator index.
	private int fTargetType;
	// The current target names.
	private String fTargetName;
	// The flag if system files should be included
	private boolean fIncludeSystem;
	// The flag if hidden files should be included
	private boolean fIncludeHidden;

	/**
	 * Constructor with different option parameters.
	 *
	 * @param caseSensitive Option of case sensitive
	 * @param matchPrecise Option of precise matching
	 * @param targetType Option of the target simulator
	 * @param targetName Option of the target name
	 * @param includeSystem Option if system files be included
	 * @param includeHidden Option if hidden files be included
	 */
	public FSTreeNodeMatcher(boolean caseSensitive, boolean matchPrecise,
					int targetType, String targetName, boolean includeSystem, boolean includeHidden) {
		fCaseSensitive = caseSensitive;
		fTargetName = targetName;
		fMatchPrecise = matchPrecise;
		if (!fMatchPrecise) {
			fStringMatcher = new StringMatcher(fTargetName, !fCaseSensitive, false);
		}
		fTargetType = targetType;
		fIncludeSystem = includeSystem;
		fIncludeHidden = includeHidden;
	}

	@Override
	public boolean match(Object context) {
		if (context instanceof IFSTreeNode) {
			IFSTreeNode node = (IFSTreeNode) context;
			if (fTargetType == 1 && !node.isFile() || fTargetType == 2 && !node.isDirectory())
				return false;
			if (!fIncludeSystem && node.isSystemFile())
				return false;
			if (!fIncludeHidden && node.isHidden())
				return false;

			String text = node.getName();
			if (text != null) {
				if (fMatchPrecise) {
					if (fCaseSensitive)
						return text.equals(fTargetName);
					return text.equalsIgnoreCase(fTargetName);
				}
				return fStringMatcher.match(text);
			}
		}
		return false;
	}
}
