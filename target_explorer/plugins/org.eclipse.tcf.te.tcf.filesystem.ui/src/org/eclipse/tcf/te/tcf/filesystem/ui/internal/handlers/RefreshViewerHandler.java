/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.filesystem.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * The action handler to refresh the whole file system tree.
 */
public class RefreshViewerHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorInput editorInput = HandlerUtil.getActiveEditorInputChecked(event);
		IPeerNode peer = (IPeerNode) editorInput.getAdapter(IPeerNode.class);
		if (peer != null) {
			IRuntimeModel rtm = ModelManager.getRuntimeModel(peer);
			if (rtm != null) {
				rtm.getRoot().operationRefresh(true).runInJob(null);
			}
		}
		return null;
	}
}
