/*******************************************************************************
 * Copyright (c) 2014, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;

/**
 * Common utils
 */
public final class CommonUtils {

	public static String getType(IPeerNode peerNode) {
		if (peerNode != null) {
			return peerNode.getPeerType();
		}
		return null;
	}

	public static String getType(IPeer peer) {
		if (peer != null) {
			return peer.getAttributes().get(IPeerProperties.PROP_TYPE);
		}
		return null;
	}

	public static String getType(IPropertiesContainer props) {
		return props != null ? props.getStringProperty(IPeerProperties.PROP_TYPE) : null;
	}

	public static String getSubType(IPeerNode peerNode) {
		IPeer peer = peerNode.getPeer();
		return getSubType(peer);
	}

	public static String getSubType(IPeer peer) {
		Map<String, String> attrs = peer.getAttributes();
		String subType = attrs.get(IPeerProperties.PROP_SUBTYPE);
		if (subType == null) {
			subType = IPeerProperties.SUBTYPE_REAL;
		}
		return subType;
	}

	public static String getSubType(IPropertiesContainer props) {
		String subType = props.getStringProperty(IPeerProperties.PROP_SUBTYPE);
		if (subType == null) {
			subType = IPeerProperties.SUBTYPE_REAL;
		}
		return subType;
	}

	public static String getMode(IPeerNode peerNode) {
		IPeer peer = peerNode.getPeer();
		return getMode(peer);
	}

	public static String getMode(IPeer peer) {
		Map<String, String> attrs = peer.getAttributes();
		String mode = attrs.get(IPeerProperties.PROP_MODE);
		if (mode == null) {
			mode = IPeerProperties.MODE_RUN;
		}
		return mode;
	}

	public static String getMode(IPropertiesContainer props) {
		String mode = props.getStringProperty(IPeerProperties.PROP_MODE);
		if (mode == null) {
			mode = IPeerProperties.MODE_RUN;
		}
		return mode;
	}

	/**
	 * Get a free local port.
	 * @return The port or -1 on any errors.
	 */
	public static int getFreePort() {
		int port = -1;
		try {
			ServerSocket socket = new ServerSocket(0);
			port = socket.getLocalPort();
			socket.close();
		} catch (IOException e) { /* ignored on purpose */ }

		return port;
	}

    public static boolean setPeerError(final IPeerNode peerNode, final String error) {
		final AtomicBoolean changed = new AtomicBoolean();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				changed.set(peerNode.setProperty(IPeerNodeProperties.PROPERTY_ERROR, error));
			}
		});
		return changed.get();
	}

    public static String getPeerError(final IPeerNode peerNode) {
		final AtomicReference<String> error = new AtomicReference<String>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				error.set(peerNode.getStringProperty(IPeerNodeProperties.PROPERTY_ERROR));
			}
		});

		if (error.get() != null && error.get().trim().length() > 0) {
			return error.get();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
    public static Map<String,String> getPeerWarnings(final IPeerNode peerNode) {
		final AtomicReference<Object> warnings = new AtomicReference<Object>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				warnings.set(peerNode.getProperty(IPeerNodeProperties.PROPERTY_WARNINGS));
			}
		});

		if (warnings.get() != null && warnings.get() instanceof Map<?,?>) {
			return (Map<String,String>)warnings.get();
		}
		return null;
	}

	public static boolean setPeerWarning(final IPeerNode peerNode, final String key, final String value) {
		final AtomicBoolean changed = new AtomicBoolean();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
                @SuppressWarnings("unchecked")
                Map<String,String> warnings = (Map<String,String>)peerNode.getProperty(IPeerNodeProperties.PROPERTY_WARNINGS);
                if (warnings == null) {
                	if (value == null) {
                		return;
                	}
                	warnings = new HashMap<String,String>();
                }
            	if (value != null) {
            		changed.set(!value.equals(warnings.get(key)));
            		warnings.put(key, value);
            	}
            	else {
            		changed.set(warnings.get(key) != null);
            		warnings.remove(key);
            		if (warnings.isEmpty()) {
            			warnings = null;
            		}
            	}
            	peerNode.setChangeEventsEnabled(false);
                peerNode.setProperty(IPeerNodeProperties.PROPERTY_WARNINGS, warnings);
            	peerNode.setChangeEventsEnabled(true);
			}
		});
		peerNode.fireChangeEvent(IPeerNodeProperties.PROPERTY_WARNINGS, null, null);
		return changed.get();
	}

	@SuppressWarnings("unchecked")
	public static String getPeerWarningOrigin(final IPeerNode peerNode, final String warningKey) {
		final AtomicReference<Object> warningOrigins = new AtomicReference<Object>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				warningOrigins.set(peerNode.getProperty(IPeerNodeProperties.PROPERTY_WARNING_ORIGINS));
			}
		});

		if (warningOrigins.get() != null && warningOrigins.get() instanceof Map<?,?>) {
			return ((Map<String,String>)warningOrigins.get()).get(warningKey);
		}
		return null;
	}

	public static boolean setPeerWarningOrigin(final IPeerNode peerNode, final String warningKey, final String value) {
		final AtomicBoolean changed = new AtomicBoolean();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				@SuppressWarnings("unchecked")
				Map<String,String> warningsOrigins = (Map<String,String>)peerNode.getProperty(IPeerNodeProperties.PROPERTY_WARNING_ORIGINS);
                if (warningsOrigins == null) {
                	if (value == null) {
                		return;
                	}
                	warningsOrigins = new HashMap<String,String>();
                }
            	if (value != null) {
            		changed.set(!value.equals(warningsOrigins.get(warningKey)));
            		warningsOrigins.put(warningKey, value);
            	}
            	else {
            		changed.set(warningsOrigins.get(warningKey) != null);
            		warningsOrigins.remove(warningKey);
            		if (warningsOrigins.isEmpty()) {
            			warningsOrigins = null;
            		}
            	}
            	peerNode.setChangeEventsEnabled(false);
                peerNode.setProperty(IPeerNodeProperties.PROPERTY_WARNING_ORIGINS, warningsOrigins);
            	peerNode.setChangeEventsEnabled(true);
			}
		});
		peerNode.fireChangeEvent(IPeerNodeProperties.PROPERTY_WARNING_ORIGINS, null, null);
		return changed.get();
	}
}
