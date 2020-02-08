/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.handler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.ui.handler.AbstractCommandHandler;

/**
 * Peer node command handler implementation.
 */
public abstract class AbstractPeerNodeCommandHandler extends AbstractCommandHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IStructuredSelection selection = getSelection(event);

		List<IPeerNode> peerNodes = getPeerNodes(selection);

		return internalExecute (event, selection, peerNodes);
	}

	/**
	 * @param selection
	 * @return
	 */
    protected List<IPeerNode> getPeerNodes(IStructuredSelection selection) {
    	List<IPeerNode> peerNodes = new ArrayList<IPeerNode>();

		Iterator<Object> it = selection.iterator();
		while (it.hasNext()) {
			Object element = it.next();
			IPeerNode peerNode = getPeerNode(element);
			if (peerNode != null && !peerNodes.contains(peerNode)) {
				peerNodes.add(peerNode);
			}
		}

	    return peerNodes;
    }

	protected IPeerNode getPeerNode(Object element) {
		IPeerNode peerNode = null;
		if (element instanceof IPeerNode) {
			peerNode = (IPeerNode)element;
		}
		else if (element instanceof IAdaptable) {
			peerNode = (IPeerNode)((IAdaptable)element).getAdapter(IPeerNode.class);
		}
		if (peerNode == null) {
			peerNode = (IPeerNode)Platform.getAdapterManager().getAdapter(element, IPeerNode.class);
		}

		return peerNode;
	}

	/**
	 * @param event
	 * @param selection
	 * @param peerNodes
	 * @return
	 */
    protected abstract Object internalExecute(ExecutionEvent event, IStructuredSelection selection, List<IPeerNode> peerNodes);
}
