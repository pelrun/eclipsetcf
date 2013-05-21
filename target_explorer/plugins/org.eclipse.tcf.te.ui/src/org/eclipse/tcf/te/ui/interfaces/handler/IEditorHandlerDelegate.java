/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.ui.interfaces.handler;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

/**
 * IEditorHandlerDelegate
 */
public interface IEditorHandlerDelegate {

	/**
	 * Get a valid editor input for the given element.
	 * @param element The selected element.
	 * @return The editor input.
	 */
	public IEditorInput getEditorInput(Object element);

	/**
	 * Action that should be done after the editor was opened.
	 * @param element The selected element.
	 */
	public void postOpenEditor(IEditorPart editor, Object element);
}
