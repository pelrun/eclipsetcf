/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.services;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistableNodeProperties;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IURIPersistenceService;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.ServiceUtils;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.core.peers.Peer;
import org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelMigrationDelegate;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelUpdateService;
import org.eclipse.tcf.te.tcf.locator.model.ModelLocationUtil;
import org.eclipse.tcf.te.tcf.locator.nodes.PeerNode;
import org.osgi.framework.Version;


/**
 * Default peer model refresh service implementation.
 */
public class PeerModelRefreshService extends AbstractPeerModelService implements IPeerModelRefreshService {

	/**
	 * Constructor.
	 *
	 * @param parentModel The parent peer model instance. Must not be <code>null</code>.
	 */
	public PeerModelRefreshService(IPeerModel parentModel) {
		super(parentModel);
	}

	/**
	 * Asynchronously invoke the callback within the TCF dispatch thread.
	 *
	 * @param callback The callback to invoke or <code>null</code>.
	 */
	protected final void invokeCallback(final ICallback callback) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (callback != null) {
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					callback.done(PeerModelRefreshService.this, Status.OK_STATUS);
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelRefreshService#refresh(org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void refresh(final ICallback callback) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		// Get the parent peer model
		IPeerModel model = getPeerModel();

		// If the parent model is already disposed, the service will drop out immediately
		if (model.isDisposed()) {
			invokeCallback(callback);
			return;
		}

		// If the TCF framework isn't initialized yet, the service will drop out immediately
		if (!Tcf.isRunning()) {
			invokeCallback(callback);
			return;
		}

		// Get the list of old children (update node instances where possible)
		final List<IPeerNode> oldChildren = new ArrayList<IPeerNode>(Arrays.asList(model.getPeerNodes()));

		// Refresh the static peer definitions
		refreshStaticPeers(oldChildren, model);

		// Invoke the callback
		invokeCallback(callback);
	}

	/**
	 * Process the given map of peers and update the given peer model.
	 *
	 * @param peers The map of peers to process. Must not be <code>null</code>.
	 * @param oldChildren The list of old children. Must not be <code>null</code>.
	 * @param model The peer model. Must not be <code>null</code>.
	 */
	protected void processPeers(Map<String, IPeer> peers, List<IPeerNode> oldChildren, IPeerModel model) {
		Assert.isNotNull(peers);
		Assert.isNotNull(oldChildren);
		Assert.isNotNull(model);

		for (Entry<String, IPeer> entry : peers.entrySet()) {
			// Get the peer instance for the current peer id
			IPeer peer = entry.getValue();
			// Try to find an existing peer node first
			IPeerNode peerNode = model.getService(IPeerModelLookupService.class).lkupPeerModelById(entry.getKey());
			// And create a new one if we cannot find it
			if (peerNode == null) {
				peerNode = new PeerNode(model, peer);
				model.getService(IPeerModelUpdateService.class).add(peerNode);
			}
			else {
				oldChildren.remove(peerNode);
			}

			if (peerNode.getPeer() != peer) {
				peerNode.setProperty(IPeerNodeProperties.PROPERTY_INSTANCE, peer);
			}
		}

		if (!oldChildren.isEmpty()) {
			for (IPeerNode oldPeerNode : oldChildren) {
				model.getService(IPeerModelUpdateService.class).remove(oldPeerNode);
            }
		}
	}

	/**
	 * Refresh the static peer definitions.
	 *
	 * @param oldChildren The list of old children. Must not be <code>null</code>.
	 * @param model The peer model. Must not be <code>null</code>.
	 */
	protected void refreshStaticPeers(List<IPeerNode> oldChildren, IPeerModel model) {
		Assert.isNotNull(oldChildren);
		Assert.isNotNull(model);

		// Get the root locations to lookup the static peer definitions
		File[] roots = getStaticPeerLookupDirectories();
		if (roots.length > 0) {
			// The map of peers created from the static definitions
			Map<String, IPeer> peers = new HashMap<String, IPeer>();
			// Process the root locations
			for (File root : roots) {
				// List all "*.json" and "*.peer" files within the root location
				File[] candidates = root.listFiles(new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						IPath path = new Path(pathname.getAbsolutePath());
						return path.getFileExtension() != null && path.getFileExtension().toLowerCase().equals("peer"); //$NON-NLS-1$
					}
				});
				// If there are ini files to read, process them
				if (candidates != null && candidates.length > 0) {

					for (File candidate : candidates) {
						try {
							IURIPersistenceService service = ServiceManager.getInstance().getService(IURIPersistenceService.class);
							IPeer tempPeer = (IPeer)service.read(IPeer.class, candidate.getAbsoluteFile().toURI());

							Map<String,String> attrs = new HashMap<String, String>(tempPeer.getAttributes());

							// Remember the file path within the properties
							attrs.put(IPersistableNodeProperties.PROPERTY_URI, candidate.getAbsoluteFile().toURI().toString());

							// Validate the name attribute. If not set, set
							// it to the file name without the .ini extension.
							String name = attrs.get(IPeer.ATTR_NAME);
							if (name == null || "".equals(name.trim())) { //$NON-NLS-1$
								name = new Path(candidate.getAbsolutePath()).removeFileExtension().lastSegment();
								attrs.put(IPeer.ATTR_NAME, name);
							}

							// Validate the id attribute. If not set, generate one.
							String id = attrs.get(IPeer.ATTR_ID);
							if (id == null || "".equals(id.trim()) || "USR:".equals(id.trim())) { //$NON-NLS-1$ //$NON-NLS-2$
								String transport = attrs.get(IPeer.ATTR_TRANSPORT_NAME);
								String host = attrs.get(IPeer.ATTR_IP_HOST);
								String port = attrs.get(IPeer.ATTR_IP_PORT);

								if (transport != null && host != null && !(id != null && "USR:".equals(id.trim()))) { //$NON-NLS-1$
									id = transport.trim() + ":" + host.trim(); //$NON-NLS-1$
									id += port != null ? ":" + port.trim() : ":1534"; //$NON-NLS-1$ //$NON-NLS-2$
								} else {
									id = "USR:" + System.currentTimeMillis(); //$NON-NLS-1$
									// If the key is not unique, we have to wait a little bit an try again
									while (peers.containsKey(id)) {
										try { Thread.sleep(20); } catch (InterruptedException e) { /* ignored on purpose */ }
										id = "USR:" + System.currentTimeMillis(); //$NON-NLS-1$
									}
								}
								attrs.put(IPeer.ATTR_ID, id);
							}
							// Construct the peer from the attributes
							IPeer peer = new Peer(attrs);
							IPeerModelMigrationDelegate delegate = ServiceUtils.getDelegateServiceDelegate(peer, peer, IPeerModelMigrationDelegate.class);
							if (delegate != null) {
								Version activeVersion = delegate.getVersion();
								String version = attrs.get(IPeerProperties.PROP_VERSION);
								String value = attrs.get(IPeerProperties.PROP_MIGRATED);
								boolean migrated = value != null && Boolean.parseBoolean(value);
								Version peerVersion = version != null ? new Version(version.trim()) : Version.emptyVersion;
								if (peerVersion.compareTo(activeVersion) == 0) {
									// Add the peer to the peers map
									peers.put(peer.getID(), peer);
								}
								else if (!migrated) {
									IPeer migratedPeer = delegate.migrate(peer);
									if (migratedPeer != null) {
										attrs.put(IPeerProperties.PROP_MIGRATED, Boolean.TRUE.toString());
										service.write(new Peer(attrs), null);

										attrs = new HashMap<String, String>(migratedPeer.getAttributes());
										attrs.put(IPersistableNodeProperties.PROPERTY_URI, null);
										attrs.put(IPeerProperties.PROP_VERSION, activeVersion.toString());
										peer = new Peer(attrs);
										URI uri = service.getURI(peer);
										attrs.put(IPersistableNodeProperties.PROPERTY_URI, uri.toString());
										service.write(peer, uri);
										// Add the migrated peer to the peers map
										peers.put(peer.getID(), peer);
									}
								}
							}
							else {
								// Add the peer to the peers map
								peers.put(peer.getID(), peer);
							}
						} catch (Throwable e) {
							/* ignored on purpose */
						}
					}
				}
			}

			// Process the read peers
			if (!oldChildren.isEmpty() || !peers.isEmpty()) {
				processPeers(peers, oldChildren, model);
			}
		}
	}

	/**
	 * Returns the list of root locations to lookup for static peers definitions.
	 *
	 * @return The list of root locations or an empty list.
	 */
	protected File[] getStaticPeerLookupDirectories() {
		// The list defining the root locations
		List<File> rootLocations = new ArrayList<File>();

		// always add default root location
		IPath defaultPath = ModelLocationUtil.getStaticPeersRootLocation();
		if (defaultPath != null) {
			File file = defaultPath.toFile();
			if (file.canRead() && file.isDirectory() && !rootLocations.contains(file)) {
				rootLocations.add(file);
			}
		}

		return rootLocations.toArray(new File[rootLocations.size()]);
	}
}
