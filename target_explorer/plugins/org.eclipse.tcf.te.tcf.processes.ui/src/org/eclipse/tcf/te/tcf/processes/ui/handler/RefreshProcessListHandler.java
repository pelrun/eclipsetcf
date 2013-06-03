/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IUIService;
import org.eclipse.tcf.te.tcf.core.model.interfaces.IModel;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.processes.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.processes.ui.interfaces.IProcessMonitorMessageProviderDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
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
		final IPeerModel peer = (IPeerModel) editorInput.getAdapter(IPeerModel.class);
		if (peer != null) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					IModel model = ModelManager.getRuntimeModel(peer);
					Assert.isNotNull(model);
					model.getService(IModelRefreshService.class).refresh(null);
				}
			};
			Protocol.invokeLater(runnable);
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
				IPeerModel node = editorInput != null ? (IPeerModel) editorInput.getAdapter(IPeerModel.class) : null;

				IUIService service = ServiceManager.getInstance().getService(node, IUIService.class);
				IProcessMonitorMessageProviderDelegate delegate = service != null ? service.getDelegate(node, IProcessMonitorMessageProviderDelegate.class) : null;

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
