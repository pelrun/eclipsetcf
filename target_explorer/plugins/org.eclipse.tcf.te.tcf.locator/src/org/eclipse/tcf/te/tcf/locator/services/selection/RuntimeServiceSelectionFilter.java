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

import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModelProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ISelectionService;

/**
 * Filter for ISelectionService to filter peer model nodes depending on their runtime and offline services.
 */
public class RuntimeServiceSelectionFilter implements ISelectionService.ISelectionFilter {

	protected final String[] serviceNames;
	protected final boolean useDisconnectedContexts;

	/**
	 * Constructor.
	 * @param serviceName The service name to check.
	 * @param useDisconnectedContexts <code>true</code> if disconnected targets should be selected too.
	 */
	public RuntimeServiceSelectionFilter(String serviceName, boolean useDisconnectedContexts) {
		this.serviceNames = new String[]{serviceName};
		this.useDisconnectedContexts = useDisconnectedContexts;
	}

	/**
	 * Constructor.
	 * @param serviceNames The list of service names to check.
	 * @param useDisconnectedContexts <code>true</code> if disconnected targets should be selected too.
	 */
	public RuntimeServiceSelectionFilter(String[] serviceNames, boolean useDisconnectedContexts) {
		this.serviceNames = serviceNames;
		this.useDisconnectedContexts = useDisconnectedContexts;
	}

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.core.interfaces.services.ISelectionService.ISelectionFilter#select(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel)
	 */
	@Override
	public boolean select(final IPeerModel peerModel) {
		if (peerModel != null) {
			final IPeer peer = peerModel.getPeer();
			final AtomicBoolean result = new AtomicBoolean(false);
			Protocol.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					List<String> list;
					String services = peerModel.getStringProperty(IPeerModelProperties.PROP_REMOTE_SERVICES);
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
						services = peer.getAttributes().get(IPeerModelProperties.PROP_OFFLINE_SERVICES);
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
		return false;
	}

}
