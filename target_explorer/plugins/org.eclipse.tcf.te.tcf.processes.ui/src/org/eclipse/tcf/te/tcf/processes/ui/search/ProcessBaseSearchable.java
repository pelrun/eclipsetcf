/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.search;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.tcf.te.ui.interfaces.ISearchMatcher;
import org.eclipse.tcf.te.ui.utils.AbstractSearchable;

/**
 * The base searchable that provides common methods for its subclasses.
 *
 * @see ProcessStateSearchable
 * @see ProcessUserSearchable
 */
public abstract class ProcessBaseSearchable extends AbstractSearchable implements ISearchMatcher {

	/**
	 * Create a group with the specified title.
	 *
	 * @param parent The parent where the group is to be created.
	 * @return The group composite.
	 */
	protected Composite createGroup(Composite parent) {
		Assert.isNotNull(parent);

		Group group = new Group(parent, SWT.NONE);
		group.setText(getGroupTitle());
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setBackground(parent.getBackground());

		return group;
	}

	/**
	 * Returns the group title.
	 *
	 * @return The group title.
	 */
	protected abstract String getGroupTitle();

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#getMatcher()
	 */
	@Override
	public ISearchMatcher getMatcher() {
		return this;
	}
}
