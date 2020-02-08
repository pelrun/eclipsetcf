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

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.AsyncCallbackCollector;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.services.ServiceUtils;
import org.eclipse.tcf.te.tcf.core.model.interfaces.IModel;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.processes.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.processes.ui.interfaces.IProcessMonitorUIDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

/**
 * The handler to refresh the process list.
 */
public class RefreshProcessListHandler extends AbstractHandler implements IElementUpdater {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorInput editorInput = HandlerUtil.getActiveEditorInputChecked(event);
		final IPeerNode peer = (IPeerNode) editorInput.getAdapter(IPeerNode.class);
		if (peer != null) {
			BusyIndicator.showWhile(PlatformUI.getWorkbench().getDisplay(), new Runnable() {
				@Override
				public void run() {
					final AsyncCallbackCollector collector = new AsyncCallbackCollector();
					final ICallback callback = new AsyncCallbackCollector.SimpleCollectorCallback(collector);

					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							IModel model = ModelManager.getRuntimeModel(peer);
							Assert.isNotNull(model);
							model.getService(IModelRefreshService.class).refresh(callback);
						}
					};
					Protocol.invokeLater(runnable);

					collector.initDone();

					ExecutorsUtil.waitAndExecute(0, collector.getConditionTester());
				}
			});

		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.commands.IElementUpdater#updateElement(org.eclipse.ui.menus.UIElement, java.util.Map)
	 */
	@Override
	public void updateElement(UIElement element, Map parameters) {
		IWorkbenchPartSite site = (IWorkbenchPartSite)parameters.get("org.eclipse.ui.part.IWorkbenchPartSite"); //$NON-NLS-1$
		if (site != null) {
			IWorkbenchPart part = site.getPart();
			if (part instanceof IEditorPart) {
				IEditorInput editorInput = ((IEditorPart)part).getEditorInput();
				IPeerNode peerNode = editorInput != null ? (IPeerNode) editorInput.getAdapter(IPeerNode.class) : null;
    			IProcessMonitorUIDelegate delegate = ServiceUtils.getUIServiceDelegate(peerNode, peerNode, IProcessMonitorUIDelegate.class);
				if (delegate != null) {
					String text = delegate.getMessage("RefreshProcessListHandler_updateElement_text"); //$NON-NLS-1$
					if (text != null) element.setText(text);

					String tooltip = delegate.getMessage("RefreshProcessListHandler_updateElement_tooltip"); //$NON-NLS-1$
					if (tooltip != null) element.setTooltip(tooltip);
				}
			}
		}
	}
}
