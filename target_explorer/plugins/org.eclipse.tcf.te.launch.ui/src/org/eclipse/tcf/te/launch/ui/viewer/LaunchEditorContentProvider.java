/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.ui.viewer;


/**
 * Launch tree content provider implementation.
 */
public class LaunchEditorContentProvider extends LaunchNavigatorContentProvider {

	/**
	 * Constructor.
	 */
	public LaunchEditorContentProvider() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.launch.ui.controls.LaunchNavigatorContentProvider#isRootNodeVisible()
	 */
	@Override
	protected boolean isRootNodeVisible() {
		return false;
	}
}