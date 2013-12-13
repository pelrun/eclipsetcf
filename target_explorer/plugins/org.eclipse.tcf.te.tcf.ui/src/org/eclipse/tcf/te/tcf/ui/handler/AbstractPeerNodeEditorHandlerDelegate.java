/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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

/**
 * AbstractPeerNodeEditorHandlerDelegate
 */
public abstract class AbstractPeerNodeEditorHandlerDelegate implements IEditorHandlerDelegate {

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
}
