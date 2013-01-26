/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.runtime.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.runtime.model.interfaces.IContainerModelNode;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelLookupService;
import org.eclipse.tcf.te.tcf.core.model.services.AbstractModelService;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNodeProperties;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;

/**
 * Runtime model lookup service implementation.
 */
public class RuntimeModelLookupService extends AbstractModelService<IRuntimeModel> implements IModelLookupService {

	/**
	 * Constructor.
	 *
	 * @param model The parent model. Must not be <code>null</code>.
	 */
	public RuntimeModelLookupService(IRuntimeModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelLookupService#lkupModelNodeByUUID(java.util.UUID)
	 */
	@Override
	public IModelNode lkupModelNodeByUUID(UUID uuid) {
	    return getModel().find(uuid);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelLookupService#lkupModelNodesById(java.lang.String)
	 */
	@Override
	public IModelNode[] lkupModelNodesById(String id) {
		Assert.isNotNull(id);

		List<IModelNode> nodes = new ArrayList<IModelNode>();
		nodes.addAll(findInContainerByIdRecursively(getModel(), id));

		return nodes.toArray(new IModelNode[nodes.size()]);
	}

	/**
	 * Search the given container recursively and returns all nodes matching the given id.
	 *
	 * @param container The container. Must not be <code>null</code<.
	 * @param id The id to match. Must not be <code>null</code>.
	 *
	 * @return The list of matching nodes, or an empty list.
	 */
	private List<IModelNode> findInContainerByIdRecursively(IContainerModelNode container, String id) {
		Assert.isNotNull(container);
		Assert.isNotNull(id);

		List<IModelNode> nodes = new ArrayList<IModelNode>();
		List<IModelNode> candidates = container.getChildren(IModelNode.class);
		for (IModelNode candidate : candidates) {
			if (id.equals(candidate.getStringProperty(IProcessContextNodeProperties.PROPERTY_ID)) && !nodes.contains(candidate)) {
				nodes.add(candidate);
			}
			if (candidate instanceof IContainerModelNode) nodes.addAll(findInContainerByIdRecursively((IContainerModelNode)candidate, id));
		}

		return nodes;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelLookupService#lkupModelNodesByName(java.lang.String)
	 */
	@Override
    public IModelNode[] lkupModelNodesByName(String name) {
		Assert.isNotNull(name);

		List<IModelNode> nodes = new ArrayList<IModelNode>();
		nodes.addAll(findInContainerByNameRecursively(getModel(), name));

		return nodes.toArray(new IModelNode[nodes.size()]);
	}

	/**
	 * Search the given container recursively and returns all nodes matching the given name.
	 *
	 * @param container The container. Must not be <code>null</code<.
	 * @param name The name to match. Must not be <code>null</code>.
	 *
	 * @return The list of matching nodes, or an empty list.
	 */
	private List<IModelNode> findInContainerByNameRecursively(IContainerModelNode container, String name) {
		Assert.isNotNull(container);
		Assert.isNotNull(name);

		List<IModelNode> nodes = new ArrayList<IModelNode>();
		List<IModelNode> candidates = container.getChildren(IModelNode.class);
		for (IModelNode candidate : candidates) {
			if (name.equals(candidate.getName()) && !nodes.contains(candidate)) {
				nodes.add(candidate);
			}
			if (candidate instanceof IContainerModelNode) nodes.addAll(findInContainerByNameRecursively((IContainerModelNode)candidate, name));
		}

		return nodes;
	}
}
