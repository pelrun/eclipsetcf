/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.services;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelQueryService;

/**
 * Default peer model peer node query service implementation.
 */
public class PeerModelQueryService extends AbstractPeerModelService implements IPeerModelQueryService {

	/**
	 * Constructor.
	 *
	 * @param parentModel The parent peer model instance. Must not be <code>null</code>.
	 */
	public PeerModelQueryService(IPeerModel parentModel) {
		super(parentModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelQueryService#queryLocalServices(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode)
	 */
	@Override
	public String queryLocalServices(final IPeerNode node) {
		Assert.isNotNull(node);

		final AtomicReference<String> services = new AtomicReference<String>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				services.set(node.getStringProperty(IPeerNodeProperties.PROP_LOCAL_SERVICES));
			}
		});

		return services.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelQueryService#queryRemoteServices(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode)
	 */
	@Override
	public String queryRemoteServices(final IPeerNode node) {
		Assert.isNotNull(node);

		final AtomicReference<String> services = new AtomicReference<String>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				services.set(node.getStringProperty(IPeerNodeProperties.PROP_REMOTE_SERVICES));
			}
		});

		return services.get();
	}
}
