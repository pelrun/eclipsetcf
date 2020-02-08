/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.ui.views.interfaces;

import org.eclipse.ui.IEditorInput;

/**
 * IEditorSaveAsAdapter
 */
public interface IEditorSaveAsAdapter {

	/**
	 * Used by the editor to check whether saveAs is allowed or not.
	 * @param input The editor input to check.
	 * @return <code>true</code> if saveAs is supported.
	 */
	public boolean isSaveAsAllowed(IEditorInput input);

	/**
	 * Used by the editor to save the input under a new name.
	 * @param input The editpr input to duplicate.
	 * @return The new Object.
	 */
	public Object doSaveAs(IEditorInput input);

}
