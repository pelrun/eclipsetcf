/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.internal.factory;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.runtime.model.factory.AbstractFactoryDelegate;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.nodes.ProcessContextNode;

/**
 * Processes service model node factory delegate implementation.
 */
public class ModelNodeFactoryDelegate extends AbstractFactoryDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.model.interfaces.factory.IFactoryDelegate#newInstance(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
    @Override
	public <V extends IModelNode> V newInstance(Class<V> nodeInterface) {
		Assert.isNotNull(nodeInterface);

		V node = null;

		if (IProcessContextNode.class.equals(nodeInterface)) {
			node = (V)new ProcessContextNode();
		}

		return node;
	}

}
