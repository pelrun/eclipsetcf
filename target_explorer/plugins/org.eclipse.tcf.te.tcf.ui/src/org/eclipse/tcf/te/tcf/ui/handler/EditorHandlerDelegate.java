/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.ui.handler;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.ui.interfaces.handler.IEditorHandlerDelegate;
import org.eclipse.tcf.te.ui.views.editor.EditorInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

/**
 * EditorHandlerDelegate
 */
public class EditorHandlerDelegate implements IEditorHandlerDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.handler.IEditorHandlerDelegate#getEditorInput(java.lang.Object)
	 */
	@Override
	public IEditorInput getEditorInput(Object element) {
		IPeerNode model = (IPeerNode)Platform.getAdapterManager().getAdapter(element, IPeerNode.class);
		if (model == null && element instanceof IAdaptable) {
			model = (IPeerNode)((IAdaptable)element).getAdapter(IPeerNode.class);
		}
		return new EditorInput(model != null ? model : element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.handler.IEditorHandlerDelegate#postOpenEditor(org.eclipse.ui.IEditorPart, java.lang.Object)
	 */
	@Override
	public void postOpenEditor(IEditorPart editor, Object element) {
	}
}
