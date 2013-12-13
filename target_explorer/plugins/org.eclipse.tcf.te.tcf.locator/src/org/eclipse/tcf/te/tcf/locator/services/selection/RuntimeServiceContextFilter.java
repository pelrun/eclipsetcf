/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.services.selection;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;

/**
 * Default context service filter implementation to filter peer model nodes depending on their
 * runtime and offline services.
 */
public class RuntimeServiceContextFilter implements IDefaultContextService.IContextFilter {

	protected final String[] serviceNames;
	protected final boolean useDisconnectedContexts;

	/**
	 * Constructor.
	 * @param serviceName The service name to check.
	 * @param useDisconnectedContexts <code>true</code> if disconnected targets should be selected too.
	 */
	public RuntimeServiceContextFilter(String serviceName, boolean useDisconnectedContexts) {
		this.serviceNames = new String[]{serviceName};
		this.useDisconnectedContexts = useDisconnectedContexts;
	}

	/**
	 * Constructor.
	 * @param serviceNames The list of service names to check.
	 * @param useDisconnectedContexts <code>true</code> if disconnected targets should be selected too.
	 */
	public RuntimeServiceContextFilter(String[] serviceNames, boolean useDisconnectedContexts) {
		this.serviceNames = serviceNames;
		this.useDisconnectedContexts = useDisconnectedContexts;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService.IContextFilter#select(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode)
	 */
	@Override
	public boolean select(final IPeerNode peerNode) {
		Assert.isNotNull(peerNode);

		final IPeer peer = peerNode.getPeer();
		final AtomicBoolean result = new AtomicBoolean(false);
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				List<String> list;
				String services = peerNode.getStringProperty(IPeerNodeProperties.PROP_REMOTE_SERVICES);
				if (services != null) {
					list = Arrays.asList(services.split(",\\s*")); //$NON-NLS-1$
					boolean containsAll = true;
					for (String serviceName : serviceNames) {
						containsAll &= list.contains(serviceName);
						if (!containsAll) {
							break;
						}
					}
					result.set(containsAll);
					return;
				}

				if (useDisconnectedContexts) {
					services = peer.getAttributes().get(IPeerNodeProperties.PROP_OFFLINE_SERVICES);
					list = services != null ? Arrays.asList(services.split(",\\s*")) : Collections.EMPTY_LIST; //$NON-NLS-1$
					boolean containsAll = true;
					for (String serviceName : serviceNames) {
						containsAll &= list.contains(serviceName);
						if (!containsAll) {
							break;
						}
					}
					result.set(containsAll);
				}
			}
		});
		return result.get();
	}

}
