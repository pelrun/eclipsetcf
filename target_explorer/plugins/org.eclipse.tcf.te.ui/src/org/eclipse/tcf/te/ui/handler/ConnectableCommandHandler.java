/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.handler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.core.utils.ConnectStateHelper;
import org.eclipse.tcf.te.ui.jface.dialogs.OptionalMessageDialog;
import org.eclipse.tcf.te.ui.nls.Messages;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Connectable command handler implementation.
 */
public class ConnectableCommandHandler extends AbstractCommandHandler implements IExecutableExtension {

	protected static final String PARAM_ACTION = "action"; //$NON-NLS-1$
	protected static final String PARAM_HANDLE_DIRTY = "handleDirty"; //$NON-NLS-1$

	protected int action = IConnectable.STATE_UNKNOWN;
	protected boolean handleDirty = false;

	public int getAction() {
		return action;
	}

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.ui.handler.AbstractAgentCommandHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(event);

		List<IConnectable> connectables = getConnectables(selection);

		if (handleDirty) {
			for (IEditorReference ref : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences()) {
				if (ref.isDirty()) {
					try {
						IConnectable connectable = getConnectable(ref.getEditorInput());
						if (connectable != null && connectables.contains(connectable)) {
							int result = OptionalMessageDialog.openYesNoCancelDialog(
											HandlerUtil.getActiveShell(event),
											Messages.AbstractEditorCommandHandler_saveDialog_title,
											NLS.bind(Messages.AbstractEditorCommandHandler_saveDialog_message, ref.getTitle()),
											null, null);
							switch (result) {
							case IDialogConstants.YES_ID:
								ref.getEditor(true).doSave(null);
								break;
							case IDialogConstants.CANCEL_ID:
								return null;
							}
						}
					}
					catch (Exception e) {
					}
				}
            }
		}

		return internalExecute(event, selection, connectables);
	}

	public Object internalExecute(ExecutionEvent event, IStructuredSelection selection, List<IConnectable> connectables) {
		for (IConnectable connectable : connectables) {
			if (connectable.isConnectStateChangeAllowed(action)) {
				connectable.changeConnectState(action, null, null);
			}
		}

		return null;
	}

	/**
	 * Get the connectables out of the selection.
	 * @param selection The selection
	 * @return The connectables within the given selection.
	 */
	protected List<IConnectable> getConnectables(IStructuredSelection selection) {
		List<IConnectable> connectables = new ArrayList<IConnectable>();

		Iterator<Object> it = selection.iterator();
		while (it.hasNext()) {
			Object element = it.next();
			IConnectable connectable = getConnectable(element);
			if (connectable != null && !connectables.contains(connectable)) {
				connectables.add(connectable);
			}
		}

		return connectables;
	}

	protected IConnectable getConnectable(Object element) {
		IConnectable connectable = null;
		if (element instanceof IConnectable) {
			connectable = (IConnectable)element;
		}
		else if (element instanceof IAdaptable) {
			connectable = (IConnectable)((IAdaptable)element).getAdapter(IConnectable.class);
		}
		if (connectable == null) {
			connectable = (IConnectable)Platform.getAdapterManager().getAdapter(element, IConnectable.class);
		}

		return connectable;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		if (data instanceof Map) {
			Map<?,?> dataMap = (Map<?,?>)data;
			if (dataMap.get(PARAM_ACTION) instanceof String) {
				String stateStr = dataMap.get(PARAM_ACTION).toString().trim();
				this.action = ConnectStateHelper.getConnectAction(stateStr);
			}
			if (dataMap.get(PARAM_HANDLE_DIRTY) instanceof String) {
				String value = dataMap.get(PARAM_HANDLE_DIRTY).toString().trim();
				this.handleDirty = Boolean.parseBoolean(value);
			}
		}
	}
}
