/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.processes.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.runtime.persistence.PersistenceManager;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.IProcessContextItem;
import org.eclipse.tcf.te.tcf.processes.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModelLookupService;
import org.eclipse.tcf.te.tcf.processes.core.persistence.ProcessContextItem;

/**
 * Process data helper for en/decoding.
 */
public class ProcessDataHelper {

	public static final String encodeProcessContextItems(IProcessContextItem[] items) {
		try {
			if (items != null && items.length > 0) {
				IPersistenceDelegate delegate = PersistenceManager.getInstance().getDelegate(IProcessContextItem.class, String.class);
				return (String)delegate.writeList(items, String.class);
			}
		}
		catch (Exception e) {
		}
		return null;
	}

	public static final IProcessContextItem[] decodeProcessContextItems(String encoded) {
		if (encoded != null && encoded.trim().length() > 0) {
			try {
				IPersistenceDelegate delegate = PersistenceManager.getInstance().getDelegate(IProcessContextItem.class, String.class);
				Object[] input = delegate.readList(IProcessContextItem.class, encoded);
				List<IProcessContextItem> items = new ArrayList<IProcessContextItem>();
				for (Object object : input) {
					if (object instanceof IProcessContextItem) {
						items.add((IProcessContextItem)object);
					}
				}
				return items.toArray(new IProcessContextItem[items.size()]);
			}
			catch (Exception e) {
			}
		}
		return new IProcessContextItem[0];
	}

	public static final IProcessContextItem getProcessContextItem(final IProcessContextNode node) {
		final AtomicReference<IProcessContextItem> ctxItem = new AtomicReference<IProcessContextItem>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				if (node.getProcessContext() != null) {
					Boolean value = node.getProcessContext().getProperties().containsKey("CanAttach") ?  //$NON-NLS-1$
									(Boolean)node.getProcessContext().getProperties().get("CanAttach") : Boolean.TRUE; //$NON-NLS-1$
					if (value != null && value.booleanValue()) {
						IProcessContextItem item = new ProcessContextItem();
						item.setProperty(IProcessContextItem.PROPERTY_ID, node.getProcessContext().getID());
						item.setProperty(IProcessContextItem.PROPERTY_NAME, node.getProcessContext().getName());
						item.setProperty(IProcessContextItem.PROPERTY_PATH, getProcessContextNodePath(node));
						ctxItem.set(item);
					}
				}
			}
		});
		return ctxItem.get();
	}

	protected static final String getProcessContextNodePath(IProcessContextNode node) {
		String path = null;
		while (node.getParent() instanceof IProcessContextNode) {
			node = (IProcessContextNode)node.getParent();
			path = node.getProcessContext().getName() + (path != null ? IProcessContextItem.PATH_SEPARATOR + path : ""); //$NON-NLS-1$
		}
		return path;
	}

	public static final IProcessContextNode[] getProcessContextNodes(final IPeerNode peerNode, final IProcessContextItem item) {
		final List<IProcessContextNode> nodes = new ArrayList<IProcessContextNode>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IRuntimeModelLookupService lkup = ModelManager.getRuntimeModel(peerNode).getService(IRuntimeModelLookupService.class);
				IModelNode[] modelNodes = null;
				if (item.getId() != null) {
					modelNodes = lkup.lkupModelNodesById(item.getId());
				}
				if (modelNodes == null || modelNodes.length == 0) {
					modelNodes = lkup.lkupModelNodesByName(item.getName());
				}
				if (modelNodes != null) {
					for (IModelNode node : modelNodes) {
		                if (node instanceof IProcessContextNode && isValid((IProcessContextNode)node, item)) {
		    				nodes.add((IProcessContextNode)node);
		                }
	                }
				}
			}
		});
		return nodes.toArray(new IProcessContextNode[nodes.size()]);
	}

	protected static final boolean isValid(IProcessContextNode node, IProcessContextItem item) {
		if (item.getName() != null && node.getProcessContext().getName().equals(item.getName())) {
			String itemPath = item.getPath();
			String nodePath = getProcessContextNodePath(node);
			return (itemPath == null && nodePath == null) || (itemPath != null && itemPath.equals(nodePath));
		}
		return false;
	}
}
