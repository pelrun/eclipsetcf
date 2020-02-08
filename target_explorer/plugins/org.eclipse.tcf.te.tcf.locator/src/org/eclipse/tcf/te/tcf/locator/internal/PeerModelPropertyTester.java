/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tcf.internal.core.RemotePeer;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;

/**
 * Locator model property tester.
 */
@SuppressWarnings("restriction")
public class PeerModelPropertyTester extends PropertyTester {

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, final String property, final Object[] args, final Object expectedValue) {
		if (receiver instanceof IPeer) {
			receiver = Platform.getAdapterManager().getAdapter(receiver, IPeerNode.class);
		}
		if (receiver instanceof ILocatorNode) {
			receiver = Platform.getAdapterManager().getAdapter(((ILocatorNode)receiver).getPeer(), IPeerNode.class);
		}
		// The receiver is expected to be a peer model node or a peer
		if (receiver instanceof IPeerNode) {
			final IPeerNode peerNode = (IPeerNode)receiver;
			final AtomicBoolean result = new AtomicBoolean();

			if ("isValid".equals(property) || //$NON-NLS-1$
				"hasOfflineService".equals(property) || //$NON-NLS-1$
				"hasRemoteService".equals(property) || //$NON-NLS-1$
				"hasLocalService".equals(property)) { //$NON-NLS-1$
				Protocol.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						result.set(testPeerModel(peerNode, property, args, expectedValue));
					}
				});
			}
			else {
				result.set(testPeer(peerNode.getPeer(), property, args, expectedValue));
			}

			return result.get();
		}
		return false;
	}

	/**
	 * Test the specific peer model node properties.
	 *
	 * @param node The model node. Must not be <code>null</code>.
	 * @param property The property to test.
	 * @param args The property arguments.
	 * @param expectedValue The expected value.
	 *
	 * @return <code>True</code> if the property to test has the expected value, <code>false</code>
	 *         otherwise.
	 */
	protected boolean testPeer(IPeer peer, String property, Object[] args, Object expectedValue) {
		Assert.isNotNull(peer);

		if ("name".equals(property)) { //$NON-NLS-1$
			if (peer.getName() != null && peer.getName().equals(expectedValue)) {
				return true;
			}
		}

		if ("nameRegex".equals(property) && expectedValue instanceof String) { //$NON-NLS-1$
			if (peer.getName() != null && peer.getName().matches((String)expectedValue)) {
				return true;
			}
		}

		if ("osName".equals(property)) { //$NON-NLS-1$
			if (peer.getOSName() != null && peer.getOSName().equals(expectedValue)) {
				return true;
			}
		}

		if ("osNameRegex".equals(property) && expectedValue instanceof String) { //$NON-NLS-1$
			if (peer.getOSName() != null && peer.getOSName().matches((String)expectedValue)) {
				return true;
			}
		}

		if ("isProxy".equals(property)) { //$NON-NLS-1$
			boolean isProxy = peer.getAttributes().containsKey("Proxy"); //$NON-NLS-1$
			if (expectedValue instanceof Boolean) {
				return ((Boolean) expectedValue).booleanValue() == isProxy;
			}
		}

		if ("isValueAdd".equals(property)) { //$NON-NLS-1$
			String value = peer.getAttributes().get("ValueAdd"); //$NON-NLS-1$
			boolean isValueAdd = value != null && ("1".equals(value.trim()) || Boolean.parseBoolean(value.trim())); //$NON-NLS-1$
			if (expectedValue instanceof Boolean) {
				return ((Boolean) expectedValue).booleanValue() == isValueAdd;
			}
		}

		if ("isOfType".equals(property)) { //$NON-NLS-1$
			String value = peer.getAttributes().get(IPeerProperties.PROP_TYPE);
			if (expectedValue instanceof String) {
				return value != null ? ((String)expectedValue).equals(value) : ((String)expectedValue).equalsIgnoreCase("null"); //$NON-NLS-1$
			}
		}

		if ("isOfSubType".equals(property)) { //$NON-NLS-1$
			String value = peer.getAttributes().get(IPeerProperties.PROP_SUBTYPE);
			if (expectedValue instanceof String) {
				return value != null ? ((String)expectedValue).equals(value) : ((String)expectedValue).equalsIgnoreCase("null"); //$NON-NLS-1$
			}
		}

		if ("containsPlatform".equals(property)) { //$NON-NLS-1$
			String value = peer.getAttributes().get(IPeerProperties.PROP_PLATFORMS);
			if (value != null) {
				String[] platforms = value.split("\\s*,\\s*"); //$NON-NLS-1$
				for (String platform : platforms) {
					if (platform.equalsIgnoreCase(expectedValue.toString())) {
						return true;
					}
				}
			}
		}

		if ("hasAttribute".equals(property)) { //$NON-NLS-1$
			String name = args != null && args.length > 0 ? (String)args[0] : null;
			boolean hasAttribute = name != null && !"".equals(name) ? peer.getAttributes().containsKey(name) : false; //$NON-NLS-1$
			if (expectedValue instanceof Boolean) {
				return ((Boolean) expectedValue).booleanValue() == hasAttribute;
			}
		}

		if ("isRemotePeer".equals(property)) { //$NON-NLS-1$
            boolean isRemotePeer = peer instanceof RemotePeer;
			if (expectedValue instanceof Boolean) {
				return ((Boolean) expectedValue).booleanValue() == isRemotePeer;
			}
		}

		if ("isAttribute".equals(property)) { //$NON-NLS-1$
			String name = args != null && args.length > 0 ? (String)args[0] : null;
			String value = name != null && !"".equals(name) ? peer.getAttributes().get(name) : null; //$NON-NLS-1$
			if (expectedValue != null) {
				return expectedValue.toString().equals(value);
			}
		}

		return false;
	}

	/**
	 * Test the specific peer model node properties.
	 *
	 * @param node The model node. Must not be <code>null</code>.
	 * @param property The property to test.
	 * @param args The property arguments.
	 * @param expectedValue The expected value.
	 *
	 * @return <code>True</code> if the property to test has the expected value, <code>false</code>
	 *         otherwise.
	 */
	protected boolean testPeerModel(IPeerNode peerNode, String property, Object[] args, Object expectedValue) {
		Assert.isNotNull(peerNode);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if ("isValid".equals(property)) { //$NON-NLS-1$
			boolean isValid = peerNode.isValid();
			if (expectedValue instanceof Boolean) {
				return ((Boolean) expectedValue).booleanValue() == isValid;
			}
		}

		if ("hasOfflineService".equals(property)) { //$NON-NLS-1$
			String offlineServices = peerNode.getPeer().getAttributes().get(IPeerProperties.PROP_OFFLINE_SERVICES);
			String remoteServices = peerNode.getStringProperty(IPeerNodeProperties.PROPERTY_REMOTE_SERVICES);
			List<String> offline = offlineServices != null ? Arrays.asList(offlineServices.split(",\\s*")) : Collections.EMPTY_LIST; //$NON-NLS-1$
			List<String> remote = remoteServices != null ? Arrays.asList(remoteServices.split(",\\s*")) : null; //$NON-NLS-1$
			boolean hasOfflineService = (remote == null) ? offline.contains(expectedValue) : remote.contains(expectedValue);
			if (expectedValue instanceof Boolean) {
				return ((Boolean)expectedValue).booleanValue() == hasOfflineService;
			}
			return hasOfflineService;
		}

		if ("hasRemoteService".equals(property)) { //$NON-NLS-1$
			String remoteServices = peerNode.getStringProperty(IPeerNodeProperties.PROPERTY_REMOTE_SERVICES);
			List<String> remote = remoteServices != null ? Arrays.asList(remoteServices.split(",\\s*")) : Collections.EMPTY_LIST; //$NON-NLS-1$
			boolean hasRemoteService = remote.contains(expectedValue);
			if (expectedValue instanceof Boolean) {
				return ((Boolean)expectedValue).booleanValue() == hasRemoteService;
			}
			return hasRemoteService;
		}

		if ("hasLocalService".equals(property)) { //$NON-NLS-1$
			String localServices = peerNode.getStringProperty(IPeerNodeProperties.PROPERTY_LOCAL_SERVICES);
			List<String> remote = localServices != null ? Arrays.asList(localServices.split(",\\s*")) : Collections.EMPTY_LIST; //$NON-NLS-1$
			boolean hasLocalService = remote.contains(expectedValue);
			if (expectedValue instanceof Boolean) {
				return ((Boolean)expectedValue).booleanValue() == hasLocalService;
			}
			return hasLocalService;
		}

		return false;
	}
}
