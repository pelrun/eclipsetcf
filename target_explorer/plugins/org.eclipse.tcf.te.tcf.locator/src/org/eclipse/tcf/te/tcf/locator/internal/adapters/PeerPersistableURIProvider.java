/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.internal.adapters;

import java.io.File;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.adapters.ModelNodePersistableURIProvider;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistableNodeProperties;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider;
import org.eclipse.tcf.te.tcf.locator.model.ModelLocationUtil;
import org.osgi.framework.Version;

/**
 * Persistable implementation handling peer attributes.
 */
public class PeerPersistableURIProvider extends ModelNodePersistableURIProvider {

	/**
	 * Determine the peer from the given context object.
	 *
	 * @param context The context object or <code>null</code>.
	 * @return The peer or <code>null</code>.
	 */
	private IPeer getPeer(Object context) {
		IPeer peer = null;

		if (context instanceof IPeer) {
			peer = (IPeer)context;
		}
		else if (context instanceof IPeerNode) {
			peer = ((IPeerNode)context).getPeer();
		}
		else if (context instanceof IPeerNodeProvider) {
			peer = ((IPeerNodeProvider)context).getPeerNode().getPeer();
		}

		return peer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistableURIProvider#getURI(java.lang.Object)
	 */
	@Override
	public URI getURI(final Object context) {
		Assert.isNotNull(context);

		URI uri = null;
		final IPeer peer = getPeer(context);

		if (peer != null) {
			// Get the URI the peer model has been created from
			final AtomicReference<URI> nodeURI = new AtomicReference<URI>();
			final AtomicReference<Version> version = new AtomicReference<Version>();
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					String value = peer.getAttributes().get(IPersistableNodeProperties.PROPERTY_URI);
					if (value != null && !"".equals(value.trim())) { //$NON-NLS-1$
						nodeURI.set(URI.create(value.trim()));
					}
					value = peer.getAttributes().get(IPeerProperties.PROP_VERSION);
					version.set(value != null ? new Version(value.trim()) : null);
				}
			};
			if (Protocol.isDispatchThread()) {
				runnable.run();
			}
			else {
				Protocol.invokeAndWait(runnable);
			}

			if (nodeURI.get() != null) {
				uri = nodeURI.get();
			}

			if (uri == null) {
				String baseName = peer.getName();
				if (baseName == null) {
					baseName = peer.getID();
				}
				String name = makeValidFileSystemName(baseName);
				// Get the URI from the name
				uri = getRoot().append(name + ".peer").toFile().toURI(); //$NON-NLS-1$
				try {
					File file = new File(uri.normalize());
					int i = 0;
					while (file.exists()) {
						name = makeValidFileSystemName(baseName +
										(version.get() != null ? "_" + version.get().toString() : "") +  //$NON-NLS-1$ //$NON-NLS-2$
										(i > 0 ? " (" + i + ")": "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						uri = getRoot().append(name + ".peer").toFile().toURI(); //$NON-NLS-1$
						file = new File(uri.normalize());
						i++;
					}
				}
				catch (Exception e) {
				}
			}
		}

		return uri;
	}

	/**
	 * Returns the root location of the peers storage.
	 *
	 * @return The root location or <code>null</code> if it cannot be determined.
	 */
	@Override
	public IPath getRoot() {
		return ModelLocationUtil.getStaticPeersRootLocation();
	}
}
