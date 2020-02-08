/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.processes.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.processes.ui.internal.dialogs.IntervalConfigDialog;
import org.eclipse.tcf.te.tcf.processes.ui.internal.preferences.PreferencesInitializer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * The handler to configure the refreshing interval in a dialog.
 */
public class ConfigRefreshIntervalHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorInput editorInput = HandlerUtil.getActiveEditorInputChecked(event);
		IPeerNode peer = (IPeerNode) editorInput.getAdapter(IPeerNode.class);
		if (peer != null) {
			Shell parent = HandlerUtil.getActiveShellChecked(event);
			IntervalConfigDialog dialog = new IntervalConfigDialog(peer, parent);
			IRuntimeModel model = ModelManager.getRuntimeModel(peer);
			int interval = model.getAutoRefreshInterval();
			dialog.setResult(interval);
			if (dialog.open() == Window.OK) {
				interval = dialog.getResult();
				model.setAutoRefreshInterval(interval);
				PreferencesInitializer.addMRUInterval(interval);
			}
		}
		return null;
	}

}
