/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.handler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.IProcessContextItem;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.IProcessesDataProperties;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.util.ProcessDataHelper;
import org.eclipse.tcf.te.tcf.processes.ui.internal.dialogs.AttachContextSelectionDialog;
import org.eclipse.tcf.te.tcf.ui.handler.AbstractContextStepperCommandHandler;
import org.eclipse.tcf.te.ui.interfaces.IDataExchangeDialog;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Stepper command handler vor attach.
 */
public class AttachContextHandler extends AbstractContextStepperCommandHandler {

	public static final String RUNTIME_SERVICE_PROCESSES = "Processes"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.handler.AbstractCommandHandler#getDialogSettingsSectionName()
	 */
	@Override
	protected String getDialogSettingsSectionName() {
		return AttachContextHandler.class.getName();
	}

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.ui.handler.AbstractContextStepperCommandHandler#getServices()
	 */
	@Override
	protected String[] getServices() {
		return new String[]{RUNTIME_SERVICE_PROCESSES};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.handler.AbstractContextStepperCommandHandler#getDialog(org.eclipse.core.commands.ExecutionEvent, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	protected IDataExchangeDialog getDialog(ExecutionEvent event, IPropertiesContainer data) {
		if (!data.containsKey(IProcessesDataProperties.PROPERTY_CONTEXT_LIST)) {
			return new AttachContextSelectionDialog(HandlerUtil.getActiveShell(event), null);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.handler.AbstractContextStepperCommandHandler#getSelections(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected List<IStructuredSelection> getSelections(ExecutionEvent event) {
		List<IStructuredSelection> selections = new ArrayList<IStructuredSelection>();
		ISelection sel = HandlerUtil.getCurrentSelection(event);
		if (sel instanceof IStructuredSelection) {
			selections.add((IStructuredSelection)sel);
		}

		return selections;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.handler.AbstractContextStepperCommandHandler#addSelection(org.eclipse.jface.viewers.IStructuredSelection, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	protected void addSelection(IStructuredSelection selection, IPropertiesContainer data) {
		final List<IProcessContextItem> items = new ArrayList<IProcessContextItem>();
		if (selection != null) {
			Iterator<Object> it = selection.iterator();
			while (it.hasNext()) {
				final Object element = it.next();
				if (element instanceof IProcessContextNode) {
    				IProcessContextItem item = ProcessDataHelper.getProcessContextItem((IProcessContextNode)element);
    				if (item != null && !items.contains(item)) {
    					items.add(item);
    				}
				}
			}
		}

		if (!items.isEmpty()) {
			data.setProperty(IProcessesDataProperties.PROPERTY_CONTEXT_LIST, ProcessDataHelper.encodeProcessContextItems(items.toArray(new IProcessContextItem[items.size()])));
		}
	}
}
