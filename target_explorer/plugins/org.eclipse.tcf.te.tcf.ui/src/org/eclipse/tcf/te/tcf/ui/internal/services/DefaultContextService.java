/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
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
import org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.ActivityManagerEvent;
import org.eclipse.ui.activities.IActivityManager;
import org.eclipse.ui.activities.IActivityManagerListener;

/**
 * Default context service implementation.
 */
public class DefaultContextService extends AbstractService implements IDefaultContextService, IPeerModelListener, IActivityManagerListener {

	/**
	 * Part id: System Management view
	 */
	private static final String PART_ID_TE_VIEW = "org.eclipse.tcf.te.ui.views.View"; //$NON-NLS-1$

	/**
	 * Constructor.
	 */
	public DefaultContextService() {
		ModelManager.getPeerModel().addListener(this);
		IActivityManager manager = PlatformUI.getWorkbench().getActivitySupport().getActivityManager();
		manager.addActivityManagerListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService#getCandidates(java.lang.Object, org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService.IContextFilter)
	 */
	@Override
	public IPeerNode[] getCandidates(Object currentSelection, IContextFilter filter) {
		List<IPeerNode> candidates = new ArrayList<IPeerNode>();

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

		return candidates.toArray(new IPeerNode[candidates.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService#setDefaultContext(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode)
	 */
	@Override
	public void setDefaultContext(final IPeerNode peerNode) {
		if (peerNode != null) {
			HistoryManager.getInstance().add(getClass().getName(), peerNode.getPeerId());
			EventManager.getInstance().fireEvent(new ChangeEvent(this, ChangeEvent.ID_ADDED, peerNode, peerNode));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService#getDefaultContext(org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService.IContextFilter)
	 */
	@Override
	public IPeerNode getDefaultContext(IContextFilter filter) {
		for (String peerId : HistoryManager.getInstance().getHistory(getClass().getName())) {
			IPeerNode peerNode = addCandidate(getPeerNode(peerId), filter, null);
			if (peerNode != null) {
				return peerNode;
			}
		}

		return null;
	}


	private IPeerNode addCandidate(IPeerNode peerNode, IContextFilter filter, List<IPeerNode> candidates) {
		if (peerNode != null && peerNode.isVisible() && (filter == null || filter.select(peerNode))) {
			if (candidates != null && !candidates.contains(peerNode)) {
				candidates.add(peerNode);
			}
			return peerNode;
		}

		return null;
	}

	private void addCandidates(IStructuredSelection selection, IContextFilter filter, List<IPeerNode> candidates) {
		if (selection != null) {
			Iterator<Object> it = selection.iterator();
			while (it.hasNext()) {
				addCandidate((IPeerNode)Platform.getAdapterManager().getAdapter(it.next(), IPeerNode.class), filter, candidates);
			}
		}
	}

	private void addCandidates(IPeerNode[] peerModels, IContextFilter filter, List<IPeerNode> candidates) {
		for (IPeerNode peerNode : peerModels) {
			addCandidate(peerNode, filter, candidates);
		}
	}

	private IPeerNode[] getDefaultSelections(IContextFilter filter) {
		List<IPeerNode> candidates = new ArrayList<IPeerNode>();

		for (String peerId : HistoryManager.getInstance().getHistory(getClass().getName())) {
			addCandidate(getPeerNode(peerId), filter, candidates);
		}

		return candidates.toArray(new IPeerNode[candidates.size()]);
	}

	private IPeerNode getPeerNode(final String peerId) {
		if (peerId != null) {
			final AtomicReference<IPeerNode> peerNode = new AtomicReference<IPeerNode>();

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					IPeerModel model = ModelManager.getPeerModel();
					Assert.isNotNull(model);
					peerNode.set(model.getService(IPeerModelLookupService.class).lkupPeerModelById(peerId));
				}
			};

			if (Protocol.isDispatchThread()) {
				runnable.run();
			}
			else {
				Protocol.invokeAndWait(runnable);
			}

			return peerNode.get();
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

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener#modelChanged(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel, org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, boolean)
	 */
	@Override
	public void modelChanged(IPeerModel model, IPeerNode peerNode, boolean added) {
		if (!added) {
			IPeerNode defaultContext = getDefaultContext(null);
			EventManager.getInstance().fireEvent(new ChangeEvent(this, ChangeEvent.ID_CHANGED, defaultContext, defaultContext));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener#modelDisposed(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel)
	 */
	@Override
	public void modelDisposed(IPeerModel model) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.activities.IActivityManagerListener#activityManagerChanged(org.eclipse.ui.activities.ActivityManagerEvent)
	 */
    @Override
    public void activityManagerChanged(ActivityManagerEvent activityManagerEvent) {
    	if (activityManagerEvent.haveEnabledActivityIdsChanged()) {
			IPeerNode defaultContext = getDefaultContext(null);
			EventManager.getInstance().fireEvent(new ChangeEvent(this, ChangeEvent.ID_CHANGED, defaultContext, defaultContext));
    	}
    }
}
