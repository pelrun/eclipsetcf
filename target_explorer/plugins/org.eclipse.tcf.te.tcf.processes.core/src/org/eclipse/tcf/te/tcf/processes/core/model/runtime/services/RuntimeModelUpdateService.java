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

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelUpdateService;
import org.eclipse.tcf.te.tcf.core.model.services.AbstractModelService;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;

/**
 * Runtime model update service implementation.
 */
public class RuntimeModelUpdateService extends AbstractModelService<IRuntimeModel> implements IModelUpdateService {

	/**
	 * Constructor.
	 *
	 * @param model The parent model. Must not be <code>null</code>.
	 */
	public RuntimeModelUpdateService(IRuntimeModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.core.model.interfaces.services.IModelUpdateService#add(org.eclipse.tcf.te.runtime.model.interfaces.IModelNode)
	 */
	@Override
    public void add(IModelNode node) {
		Assert.isNotNull(node);
		getModel().add(node);
	}

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.core.model.interfaces.services.IModelUpdateService#remove(org.eclipse.tcf.te.runtime.model.interfaces.IModelNode)
	 */
	@Override
    public void remove(IModelNode node) {
		Assert.isNotNull(node);
		getModel().remove(node, false);
	}

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.core.model.interfaces.services.IModelUpdateService#update(org.eclipse.tcf.te.runtime.model.interfaces.IModelNode, org.eclipse.tcf.te.runtime.model.interfaces.IModelNode)
	 */
	@Override
	public void update(IModelNode dst, IModelNode src) {
		Assert.isNotNull(dst);
		Assert.isNotNull(src);

		boolean eventEnablementChanged = dst.setChangeEventsEnabled(false);
		boolean dstNodeChanged = false;

		for (String key : src.getProperties().keySet()) {
			dstNodeChanged |= dst.setProperty(key, src.getProperty(key));
		}

		// Re-enable the change events
		if (eventEnablementChanged) dst.setChangeEventsEnabled(true);

		// Fire a properties changed event if the destination node changed
		if (dstNodeChanged) {
			dst.fireChangeEvent("properties", null, dst.getProperties()); //$NON-NLS-1$
		}
	}
}
