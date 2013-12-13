/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.handler;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.te.ui.jface.dialogs.OptionalMessageDialog;
import org.eclipse.tcf.te.ui.nls.Messages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.EditorPart;

/**
 * Connectable command handler implementation.
 */
public abstract class AbstractEditorCommandHandler extends AbstractHandler implements IExecutableExtension {

	protected static final String PARAM_HANDLE_DIRTY = "handleDirty"; //$NON-NLS-1$

	protected boolean handleDirty = false;

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.ui.handler.AbstractAgentCommandHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		if (handleDirty(event)) {
			IWorkbenchPart part = HandlerUtil.getActivePart(event);
			if (part instanceof EditorPart) {
				if (((EditorPart)part).isDirty()) {
					int result = OptionalMessageDialog.openYesNoCancelDialog(
									HandlerUtil.getActiveShell(event),
									Messages.AbstractEditorCommandHandler_saveDialog_title,
									NLS.bind(Messages.AbstractEditorCommandHandler_saveDialog_message, ((EditorPart)part).getTitle()),
									null, null);
					switch (result) {
					case IDialogConstants.YES_ID:
						((EditorPart)part).doSave(null);
						break;
					case IDialogConstants.CANCEL_ID:
						return null;
					}
				}
			}
		}

		return internalExecute(event);
	}

	protected abstract Object internalExecute(ExecutionEvent event) throws ExecutionException;

	protected boolean handleDirty(ExecutionEvent event) {
		return handleDirty;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		if (data instanceof Map) {
			Map<?,?> dataMap = (Map<?,?>)data;
			if (dataMap.get(PARAM_HANDLE_DIRTY) instanceof String) {
				String value = dataMap.get(PARAM_HANDLE_DIRTY).toString().trim();
				this.handleDirty = Boolean.parseBoolean(value);
			}
		}
	}
}
