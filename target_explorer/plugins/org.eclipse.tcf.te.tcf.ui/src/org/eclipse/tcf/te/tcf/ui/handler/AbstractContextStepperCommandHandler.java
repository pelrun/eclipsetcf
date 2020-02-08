/*******************************************************************************
 * Copyright (c) 2013, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.handler;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.persistence.history.HistoryManager;
import org.eclipse.tcf.te.runtime.persistence.utils.DataHelper;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService;
import org.eclipse.tcf.te.tcf.core.interfaces.IContextDataProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.locator.services.selection.RuntimeServiceContextFilter;
import org.eclipse.tcf.te.tcf.locator.utils.PeerNodeDataHelper;
import org.eclipse.tcf.te.ui.handler.AbstractStepperCommandHandler;
import org.eclipse.tcf.te.ui.interfaces.IDataExchangeDialog;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Abstract stepper command handler for load/unload.
 */
public abstract class AbstractContextStepperCommandHandler extends AbstractStepperCommandHandler {

	protected void calculateSelectionData(ExecutionEvent event, IPropertiesContainer data) {

		setContextList(event, data);

		List<IStructuredSelection> selections = getSelections(event);

		for (IStructuredSelection selection : selections) {
			if (selection != null) {
				addSelection(selection, data);
			}
		}
	}

	protected abstract String[] getServices();

	protected abstract void addSelection(IStructuredSelection selection, IPropertiesContainer data);

	protected boolean doShowDisconnectedContexts() {
		return false;
	}

	protected IPropertiesContainer getDataFromHistory(ExecutionEvent event) {
		IPeerNode context = getContext(event);
		if (context != null) {
			IService[] stepperOpServices = ServiceManager.getInstance().getServices(context, IStepperOperationService.class, false);
			for (IService stepperOpService : stepperOpServices) {
				if (stepperOpService instanceof IStepperOperationService && ((IStepperOperationService)stepperOpService).isHandledOperation(context, operation)) {
					String groupId = ((IStepperOperationService)stepperOpService).getStepGroupId(context, operation);
					if (groupId != null) {
						String historyId = getHistoryId(event);
						if (historyId == null) {
							historyId = groupId + "@" + context.getPeerId(); //$NON-NLS-1$
						}
						String history = HistoryManager.getInstance().getFirst(historyId);
						if (history != null) {
							return DataHelper.decodePropertiesContainer(history);
						}
					}
				}
			}
		}

		return getDefaultData();
	}

	protected String getHistoryId(ExecutionEvent event) {
		return null;
	}

	protected IPropertiesContainer getDefaultData() {
		return new PropertiesContainer();
	}

	protected IPeerNode getContext(ExecutionEvent event) {
		IDefaultContextService selService = ServiceManager.getInstance().getService(IDefaultContextService.class);
		if (selService != null) {
			IPeerNode[] peerModels = selService.getCandidates(getSelection(event),
							new RuntimeServiceContextFilter(getServices(), doShowDisconnectedContexts()));
			if (peerModels.length > 0) {
				return peerModels[0];
			}
		}
		return null;
	}

	protected void setContextList(ExecutionEvent event, IPropertiesContainer data) {
		IPeerNode context = getContext(event);
		if (context != null) {
			data.setProperty(IContextDataProperties.PROPERTY_CONTEXT_LIST, PeerNodeDataHelper.encodeContextList(new IPeerNode[]{context}));
		}
	}

	protected String getContextName(IPropertiesContainer data) {
		String encoded = data.getStringProperty(IContextDataProperties.PROPERTY_CONTEXT_LIST);
		IPeerNode[] contexts = PeerNodeDataHelper.decodeContextList(encoded);
		if (contexts != null && contexts.length > 0) {
			return contexts[0].getName();
		}
		return null;
	}

	protected abstract IDataExchangeDialog getDialog(ExecutionEvent event, IPropertiesContainer data);

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.handler.AbstractStepperCommandHandler#getData(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected final IPropertiesContainer getData(ExecutionEvent event) {

		IPropertiesContainer data = getDataFromHistory(event);
		calculateSelectionData(event, data);

		IDataExchangeDialog dialog = getDialog(event, data);

		if (dialog != null) {
			dialog.setupData(data);
			if (dialog.open() == Window.OK) {
				String contextList = data.getStringProperty(IContextDataProperties.PROPERTY_CONTEXT_LIST);
				data = new PropertiesContainer();
				dialog.extractData(data);
				if (!data.containsKey(IContextDataProperties.PROPERTY_CONTEXT_LIST)) {
					data.setProperty(IContextDataProperties.PROPERTY_CONTEXT_LIST, contextList);
				}
				return data;
			}
			return null;
		}

		return data;
	}


	protected List<IStructuredSelection> getSelections(ExecutionEvent event) {
		List<IStructuredSelection> selections = new ArrayList<IStructuredSelection>();
		String partId = HandlerUtil.getActivePartId(event);
		selections.add(getSelection(event));
		if (!PART_ID_PROJECT_VIEW.equals(partId)) {
			selections.add(getPartSelection(PART_ID_PROJECT_VIEW));
		}
		if (!IUIConstants.ID_EXPLORER.equals(partId)) {
			selections.add(getEditorInputSelection());
			selections.add(getPartSelection(IUIConstants.ID_EXPLORER));
		}

		return selections;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.handler.AbstractStepperCommandHandler#getContext(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	protected Object getContext(IPropertiesContainer data) {
		IPeerNode[] contexts = PeerNodeDataHelper.decodeContextList(data.getStringProperty(IContextDataProperties.PROPERTY_CONTEXT_LIST));
		data.setProperty(IContextDataProperties.PROPERTY_CONTEXT_LIST, null);
		return contexts.length == 1 ? contexts[0] : null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.handler.AbstractStepperCommandHandler#cleanupData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	protected IPropertiesContainer cleanupData(IPropertiesContainer data) {
		data = super.cleanupData(data);
		data.setProperty(IContextDataProperties.PROPERTY_CONTEXT_LIST, null);
		return data;
	}
}
