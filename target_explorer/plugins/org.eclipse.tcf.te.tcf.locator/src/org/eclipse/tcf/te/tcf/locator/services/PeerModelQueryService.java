/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.services;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
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
	public String[] queryLocalServices(final IPeerNode node) {
		Assert.isNotNull(node);

		final AtomicReference<String> services = new AtomicReference<String>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				services.set(node.getStringProperty(IPeerNodeProperties.PROPERTY_LOCAL_SERVICES));
			}
		});

		if (services.get() != null) {
			String[] local = services.get().split(",\\s*"); //$NON-NLS-1$
			return local;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelQueryService#queryRemoteServices(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode)
	 */
	@Override
	public String[] queryRemoteServices(final IPeerNode node) {
		Assert.isNotNull(node);

		final AtomicReference<String> services = new AtomicReference<String>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				services.set(node.getStringProperty(IPeerNodeProperties.PROPERTY_REMOTE_SERVICES));
			}
		});

		if (services.get() != null) {
			String[] remote = services.get().split(",\\s*"); //$NON-NLS-1$
			return remote;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelQueryService#queryOfflineServices(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode)
	 */
	@Override
    public String[] queryOfflineServices(final IPeerNode node) {
		Assert.isNotNull(node);

		final AtomicReference<String> services = new AtomicReference<String>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				services.set(node.getPeer().getAttributes().get(IPeerProperties.PROP_OFFLINE_SERVICES));
			}
		});

		if (services.get() != null) {
			String[] offline = services.get().split(",\\s*"); //$NON-NLS-1$
			return offline;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelQueryService#hasLocalService(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, java.lang.String[])
	 */
	@Override
	public boolean hasLocalService(IPeerNode node, String... service) {
		String[] localServices = queryLocalServices(node);
		if (localServices == null || localServices.length == 0) {
			return false;
		}
		List<String> local = Arrays.asList(localServices);
		for (String s : service) {
	        if (!local.contains(s)) {
	        	return false;
	        }
        }
	    return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelQueryService#hasRemoteService(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, java.lang.String[])
	 */
	@Override
	public boolean hasRemoteService(IPeerNode node, String... service) {
		String[] remoteServices = queryRemoteServices(node);
		if (remoteServices == null || remoteServices.length == 0) {
			return false;
		}
		List<String> remote = Arrays.asList(remoteServices);
		if (remote == null || remote.isEmpty()) {
			return false;
		}
		for (String s : service) {
	        if (!remote.contains(s)) {
	        	return false;
	        }
        }
	    return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelQueryService#hasOfflineService(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, java.lang.String[])
	 */
	@Override
	public boolean hasOfflineService(IPeerNode node, String... service) {
		String[] services = queryRemoteServices(node);
		if (services == null) {
			services = queryOfflineServices(node);
		}
		if (services == null || services.length == 0) {
			return false;
		}
		List<String> remoteOrOfflineServices = Arrays.asList(services);
		for (String s : service) {
	        if (!remoteOrOfflineServices.contains(s)) {
	        	return false;
	        }
        }
	    return true;
	}
}
