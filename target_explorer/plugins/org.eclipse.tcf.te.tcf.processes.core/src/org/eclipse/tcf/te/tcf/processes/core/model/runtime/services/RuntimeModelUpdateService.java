/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.runtime.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IProcesses;
import org.eclipse.tcf.services.ISysMonitor;
import org.eclipse.tcf.te.runtime.model.interfaces.IContainerModelNode;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx.QueryState;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx.QueryType;
import org.eclipse.tcf.te.runtime.services.ServiceUtils;
import org.eclipse.tcf.te.tcf.core.model.services.AbstractModelService;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNodeProperties;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModelRefreshService;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModelUpdateService;

/**
 * Runtime model update service implementation.
 */
public class RuntimeModelUpdateService extends AbstractModelService<IRuntimeModel> implements IRuntimeModelUpdateService {

	/**
	 * Constructor.
	 *
	 * @param model The parent model. Must not be <code>null</code>.
	 */
	public RuntimeModelUpdateService(IRuntimeModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelUpdateService#add(org.eclipse.tcf.te.runtime.model.interfaces.IModelNode)
	 */
	@Override
    public void add(IModelNode node) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(node);
		getModel().add(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelUpdateService#remove(org.eclipse.tcf.te.runtime.model.interfaces.IModelNode)
	 */
	@Override
    public void remove(IModelNode node) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(node);
		Assert.isNotNull(node.getParent());
		node.getParent().remove(node, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModelUpdateService#updateChildren(org.eclipse.tcf.te.runtime.model.interfaces.IContainerModelNode, org.eclipse.tcf.te.runtime.model.interfaces.IContainerModelNode)
	 */
	@Override
	public void updateChildren(IContainerModelNode dst, IContainerModelNode src) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(dst);
		Assert.isNotNull(src);

		boolean dstNodeChanged = __updateChildren(dst, src);

		// Fire a properties changed event if the destination node changed
		if (dstNodeChanged) {
			dst.fireChangeEvent(IContainerModelNode.NOTIFY_CHANGED, null, dst.getProperties());
		}
	}

	/**
	 * Lookup a node with the given id in the given list.
	 *
	 * @param id The node id. Must not be <code>null</code>.
	 * @param list The list. Must not be <code>null</code>.
	 *
	 * @return The matching process context node or <code>null</code> if not found.
	 */
	/* default */ IProcessContextNode findInList(String id, List<IProcessContextNode> list) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(id);
		Assert.isNotNull(list);

		IProcessContextNode node = null;

		for (IProcessContextNode candidate : list) {
			if (id.equals(candidate.getStringProperty(IProcessContextNodeProperties.PROPERTY_ID))) {
				node = candidate;
				break;
			}
		}

		return node;
	}

	/**
	 * Update the child tree of the destination container from the given source container.
	 *
	 * @param dst The destination container. Must not be <code>null</code>.
	 * @param src The source container. Must not be <code>null</code>.
	 *
	 * @return <code>True</code> if the destination container changed, <code>false</code> otherwise.
	 */
	/* default */ boolean __updateChildren(IContainerModelNode dst, IContainerModelNode src) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(dst);
		Assert.isNotNull(src);

		boolean dstNodeChanged = false;

		// Get the asynchronous refreshable contexts of the nodes
		IAsyncRefreshableCtx dstRefreshable = (IAsyncRefreshableCtx)dst.getAdapter(IAsyncRefreshableCtx.class);
		Assert.isNotNull(dstRefreshable);
		IAsyncRefreshableCtx srcRefreshable = (IAsyncRefreshableCtx)src.getAdapter(IAsyncRefreshableCtx.class);
		Assert.isNotNull(srcRefreshable);

		// Synchronize the refreshable states
		Assert.isTrue(srcRefreshable.getQueryState(QueryType.CONTEXT) != QueryState.IN_PROGRESS, "Context query of node '" + src.getName() + "' in progress while updating model."); //$NON-NLS-1$ //$NON-NLS-2$
		if (srcRefreshable.getQueryState(QueryType.CONTEXT) == QueryState.DONE && dstRefreshable.getQueryState(QueryType.CONTEXT) != QueryState.DONE) {
			dstRefreshable.setQueryState(QueryType.CONTEXT, QueryState.DONE);
			dstNodeChanged |= true;
		}
		Assert.isTrue(srcRefreshable.getQueryState(QueryType.CHILD_LIST) != QueryState.IN_PROGRESS, "Child list query of node '" + src.getName() + "' in progress while updating model."); //$NON-NLS-1$ //$NON-NLS-2$
		if (srcRefreshable.getQueryState(QueryType.CHILD_LIST) == QueryState.DONE && dstRefreshable.getQueryState(QueryType.CHILD_LIST) != QueryState.DONE) {
			dstRefreshable.setQueryState(QueryType.CHILD_LIST, QueryState.DONE);
			dstNodeChanged |= true;
		}

		// If the refreshable state of the source container is PENDING, than we are done here
		if (srcRefreshable.getQueryState(QueryType.CHILD_LIST) == QueryState.PENDING) {
			return dstNodeChanged;
		}

		// Get the list of old children (update node instances where possible)
		final List<IProcessContextNode> oldChildren = dst.getChildren(IProcessContextNode.class);

		// Disable notifications while updating the child list
		boolean eventEnablementChanged = dst.setChangeEventsEnabled(false);

		// Get the list of new children
		final List<IProcessContextNode> newChildren = src.getChildren(IProcessContextNode.class);

		// Loop the list of new children and lookup a matching node in the list of old children
		for (IProcessContextNode candidate : newChildren) {
			String id = candidate.getStringProperty(IProcessContextNodeProperties.PROPERTY_ID);
			if (id == null) continue;
			// If the context node got invalid while refreshing the tree, skip the
			// context. The context will be removed from the tree as a result, if
			// the context had been added to the tree before. If the context was not
			// in the tree before, it will not be added at all.
			if (candidate.isProperty(IProcessContextNodeProperties.PROPERTY_INVALID_CTX, true)) continue;
			// Find the old process context node
			IProcessContextNode oldNode = findInList(id, oldChildren);
			if (oldNode != null) {
				// Remove the old node from the old children list
				oldChildren.remove(oldNode);
				// Update the properties of the old node from the new node
				dstNodeChanged |= __updateProperties(oldNode, candidate);
				// If the child list of the new node is valid, update the child list
				IAsyncRefreshableCtx refreshable = (IAsyncRefreshableCtx)candidate.getAdapter(IAsyncRefreshableCtx.class);
				Assert.isNotNull(refreshable);
				if (refreshable.getQueryState(QueryType.CONTEXT) == QueryState.DONE) {
					// Update the child tree of the old node from the new node
					dstNodeChanged |= __updateChildren(oldNode, candidate);
				}
			} else {
				if (candidate.getParent() == null) {
					// Parent not set -> Add the new child node
					dstNodeChanged |= dst.add(candidate);
				} else {
					// Parent set -> Create a copy of the new node
					IProcessContextNode copy = getModel().getFactory().newInstance(IProcessContextNode.class);
					__updateProperties(copy, candidate);
					IAsyncRefreshableCtx refreshable = (IAsyncRefreshableCtx)candidate.getAdapter(IAsyncRefreshableCtx.class);
					Assert.isNotNull(refreshable);
					if (refreshable.getQueryState(QueryType.CONTEXT) == QueryState.DONE) {
						__updateChildren(copy, candidate);
					}
					// Add the copy of the new node
					dstNodeChanged |= dst.add(copy);
				}
			}
		}

		// If there are remaining old children, remove them (non-recursive)
		for (IProcessContextNode oldChild : oldChildren) {
			dstNodeChanged |= dst.remove(oldChild, false);
		}

		// Re-enable the change events
		if (eventEnablementChanged) dst.setChangeEventsEnabled(true);

		return dstNodeChanged;
	}

	/**
	 * Update the destination node properties from the given source node.
	 * <p>
	 * <b>Note:</b> This method does not update the child tree. Use {@link #updateChildren(IContainerModelNode, IContainerModelNode)}
	 * for updating the child tree.
	 *
	 * @param dst The destination node. Must not be <code>null</code>.
	 * @param src The source node. Must not be <code>null</code>.
	 *
	 * @return <code>True</code> if the properties of the destination node changed, <code>false</code> otherwise.
	 */
	/* default */ boolean __updateProperties(IProcessContextNode dst, IProcessContextNode src) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(dst);
		Assert.isNotNull(src);

		boolean eventEnablementChanged = dst.setChangeEventsEnabled(false);
		boolean dstNodeChanged = false;

		// Update the properties of the destination node from the source node
		for (String key : src.getProperties().keySet()) {
			dstNodeChanged |= dst.setProperty(key, src.getProperty(key));
		}

		// Make sure that old properties are removed from the destination node.
		// Collect the list of property names to check for removal
		List<String> managedPropertyNames = new ArrayList<String>();
		managedPropertyNames.add(IProcessContextNodeProperties.PROPERTY_ID);
		managedPropertyNames.add(IProcessContextNodeProperties.PROPERTY_NAME);

		// Determine if a delegate is registered
		IRuntimeModelRefreshService.IDelegate delegate = ServiceUtils.getDelegateServiceDelegate(dst, dst, IRuntimeModelRefreshService.IDelegate.class);

		if (delegate == null && getModel().getService(IRuntimeModelRefreshService.class) instanceof RuntimeModelRefreshService) {
			delegate = ((RuntimeModelRefreshService)getModel().getService(IRuntimeModelRefreshService.class)).defaultDelegate;
		}
		if (delegate != null) {
			String[] candidates = delegate.getManagedPropertyNames();
			if (candidates != null) managedPropertyNames.addAll(Arrays.asList(candidates));
		}

		// Clean up the destination node
		for (String managedPropertyName : managedPropertyNames) {
			if (src.isProperty(managedPropertyName, null)) {
				dstNodeChanged |= dst.setProperty(managedPropertyName, null);
			}
		}

		// Update the system monitor context object (if necessary)
		ISysMonitor.SysMonitorContext s1 = dst.getSysMonitorContext();
		ISysMonitor.SysMonitorContext s2 = src.getSysMonitorContext();
		if ((s1 == null && s2 != null) || (s1 != null && s2 == null) || (s1 != null && !s1.equals(s2))) {
			dst.setSysMonitorContext(src.getSysMonitorContext());
			dstNodeChanged |= true;
		}

		// Update the process context object (if necessary)
		IProcesses.ProcessContext p1 = dst.getProcessContext();
		IProcesses.ProcessContext p2 = src.getProcessContext();
		if ((p1 == null && p2 != null) || (p1 != null && p2 == null) || (p1 != null && !p1.equals(p2))) {
			dst.setProcessContext(src.getProcessContext());
			dstNodeChanged |= true;
		}

		// Update the node type (if necessary)
		if (dst.getType() != src.getType()) {
			dst.setType(src.getType());
			dstNodeChanged |= true;
		}

		// Re-enable the change events
		if (eventEnablementChanged) dst.setChangeEventsEnabled(true);

		return dstNodeChanged;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelUpdateService#update(org.eclipse.tcf.te.runtime.model.interfaces.IModelNode, org.eclipse.tcf.te.runtime.model.interfaces.IModelNode)
	 */
	@Override
	public void update(IModelNode dst, IModelNode src) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(dst);
		Assert.isNotNull(src);

		// The nodes to update must be process context nodes
		if (!(dst instanceof IProcessContextNode) || !(src instanceof IProcessContextNode)) {
			return;
		}

		// Update the nodes only if the id's are matching
		String dstContextId = dst.getStringProperty(IProcessContextNodeProperties.PROPERTY_ID);
		String srcContextId = src.getStringProperty(IProcessContextNodeProperties.PROPERTY_ID);
		if ((dstContextId == null && srcContextId != null) || (dstContextId != null && srcContextId == null) || (dstContextId != null && !dstContextId.equals(srcContextId))) {
			return;
		}

		boolean dstNodeChanged = __updateProperties((IProcessContextNode)dst, (IProcessContextNode)src);
		if (dst instanceof IContainerModelNode && src instanceof IContainerModelNode) {
			dstNodeChanged |= __updateChildren((IContainerModelNode)dst, (IContainerModelNode)src);
		}

		// Fire a properties changed event if the destination node changed
		if (dstNodeChanged) {
			dst.fireChangeEvent(IContainerModelNode.NOTIFY_CHANGED, null, null);
		}
	}
}
