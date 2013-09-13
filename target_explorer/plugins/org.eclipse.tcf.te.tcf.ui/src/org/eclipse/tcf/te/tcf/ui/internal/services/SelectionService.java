/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal.services;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.persistence.history.HistoryManager;
import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerType;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModelProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ISelectionService;
import org.eclipse.tcf.te.tcf.locator.model.Model;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;

/**
 * Selection service implementation.
 */
public class SelectionService extends AbstractService implements ISelectionService {

	/**
	 * Part id: System Manager view
	 */
	private static final String PART_ID_TE_VIEW = "org.eclipse.tcf.te.ui.views.View"; //$NON-NLS-1$

	/**
	 * Constructor.
	 */
	public SelectionService() {
	}

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.core.interfaces.services.ISelectionService#getCandidates(java.lang.Object, com.windriver.te.tcf.core.interfaces.services.ISelectionService.ISelectionFilter)
	 */
	@Override
	public IPeerModel[] getCandidates(Object currentSelection, ISelectionFilter filter) {
		List<IPeerModel> candidates = new ArrayList<IPeerModel>();

		// add given selection first
		if (currentSelection instanceof IStructuredSelection) {
			addCandidates((IStructuredSelection)currentSelection, filter, candidates);
		}

		// add default selection
		addCandidates(getDefaultSelections(filter), filter, candidates);

		// add active editor
		addCandidates(getEditorSelection(), filter, candidates);

		// add system management selection
		addCandidates(getPartSelection(PART_ID_TE_VIEW), filter, candidates);

		return candidates.toArray(new IPeerModel[candidates.size()]);
	}

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.core.interfaces.services.ISelectionService#setDefaultSelection(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel)
	 */
	@Override
	public void setDefaultSelection(final IPeerModel peerModel) {
		if (peerModel != null) {
			HistoryManager.getInstance().add(getClass().getName(), peerModel.getPeerId());
			EventManager.getInstance().fireEvent(new ChangeEvent(this, ChangeEvent.ID_ADDED, peerModel, peerModel));

			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window != null && window.getActivePage() != null) {
				IViewPart view = window.getActivePage().findView(PART_ID_TE_VIEW);
				if (view instanceof CommonNavigator) {
					((CommonNavigator)view).getCommonViewer().refresh();
				}
			}

			final AtomicReference<String> type = new AtomicReference<String>();
			Protocol.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					type.set(peerModel.getPeer().getAttributes().get((IPeerModelProperties.PROP_TYPE)));
				}
			});
			HistoryManager.getInstance().add(type.get() != null ? type.get() : IPeerType.TYPE_GENERIC, peerModel.getPeerId());
		}
		else {
			HistoryManager.getInstance().clear(getClass().getName());
			EventManager.getInstance().fireEvent(new ChangeEvent(this, ChangeEvent.ID_REMOVED, null, null));
		}
	}

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.core.interfaces.services.ISelectionService#getDefaultSelection(com.windriver.te.tcf.core.interfaces.services.ISelectionService.ISelectionFilter)
	 */
	@Override
	public IPeerModel getDefaultSelection(ISelectionFilter filter) {
		for (String peerId : HistoryManager.getInstance().getHistory(getClass().getName())) {
			IPeerModel peerModel = addCandidate(getPeerModel(peerId), filter, null);
			if (peerModel != null) {
				return peerModel;
			}
		}

		return null;
	}


	private IPeerModel addCandidate(IPeerModel peerModel, ISelectionFilter filter, List<IPeerModel> candidates) {
		if (peerModel != null && (filter == null || filter.select(peerModel))) {
			if (candidates != null && !candidates.contains(peerModel)) {
				candidates.add(peerModel);
			}
			return peerModel;
		}

		return null;
	}

	private void addCandidates(IStructuredSelection selection, ISelectionFilter filter, List<IPeerModel> candidates) {
		if (selection != null) {
			Iterator<Object> it = selection.iterator();
			while (it.hasNext()) {
				addCandidate((IPeerModel)Platform.getAdapterManager().getAdapter(it.next(), IPeerModel.class), filter, candidates);
			}
		}
	}

	private void addCandidates(IPeerModel[] peerModels, ISelectionFilter filter, List<IPeerModel> candidates) {
		for (IPeerModel peerModel : peerModels) {
			addCandidate(peerModel, filter, candidates);
		}
	}

	private IPeerModel[] getDefaultSelections(ISelectionFilter filter) {
		List<IPeerModel> candidates = new ArrayList<IPeerModel>();

		for (String peerId : HistoryManager.getInstance().getHistory(getClass().getName())) {
			addCandidate(getPeerModel(peerId), filter, candidates);
		}

		return candidates.toArray(new IPeerModel[candidates.size()]);
	}

	private IPeerModel getPeerModel(final String peerId) {
		if (peerId != null) {
			final AtomicReference<IPeerModel> peerModel = new AtomicReference<IPeerModel>();

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					ILocatorModel model = Model.getModel();
					Assert.isNotNull(model);
					peerModel.set(model.getService(ILocatorModelLookupService.class).lkupPeerModelById(peerId));
				}
			};

			if (Protocol.isDispatchThread()) {
				runnable.run();
			}
			else {
				Protocol.invokeAndWait(runnable);
			}

			return peerModel.get();
		}

		return null;
	}

	private IStructuredSelection getEditorSelection() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null && window.getActivePage() != null && window.getActivePage().getActiveEditor() != null) {
			return new StructuredSelection(window.getActivePage().getActiveEditor().getEditorInput());
		}
		return null;
	}

	private IStructuredSelection getPartSelection(String partId) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null && window.getActivePage() != null) {
			ISelection sel = null;
			if (partId != null) {
				sel = window.getActivePage().getSelection(partId);
			}
			else {
				sel = window.getActivePage().getSelection();
			}

			if (sel instanceof IStructuredSelection) {
				return (IStructuredSelection)sel;
			}
		}
		return null;
	}
}
