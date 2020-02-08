/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.model.factory.AbstractFactoryDelegate2;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.nodes.PeerNode;

/**
 * Locator model node factory delegate implementation.
 */
public class ModelNodeFactoryDelegate extends AbstractFactoryDelegate2 {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.model.interfaces.factory.IFactoryDelegate#newInstance(java.lang.Class)
	 */
    @Override
	public <V extends IModelNode> V newInstance(Class<V> nodeInterface) {
		return newInstance(nodeInterface, new Object[0]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.model.interfaces.factory.IFactoryDelegate2#newInstance(java.lang.Class, java.lang.Object[])
	 */
	@SuppressWarnings("unchecked")
    @Override
	public <V extends IModelNode> V newInstance(final Class<V> nodeInterface, final Object[] args) {
		if (IPeerNode.class.equals(nodeInterface)) {
			// Peer model constructor has 2 arguments, IPeerModel and IPeer
			if (args != null && args.length == 2 && args[0] instanceof IPeerModel && args[1] instanceof IPeer) {
				final AtomicReference<V> node = new AtomicReference<V>();

				Runnable runnable = new Runnable() {
					@Override
                    public void run() {
						node.set((V) new PeerNode((IPeerModel)args[0], (IPeer)args[1]));
					}
				};
				if (Protocol.isDispatchThread()) runnable.run();
				else Protocol.invokeAndWait(runnable);

				return node.get();
			}
		}

	    return null;
	}
}
