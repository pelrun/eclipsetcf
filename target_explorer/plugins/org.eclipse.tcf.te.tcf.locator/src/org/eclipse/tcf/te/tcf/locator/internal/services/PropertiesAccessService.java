/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.internal.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.services.interfaces.constants.IPropertiesAccessServiceConstants;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Peer model properties access service implementation.
 */
public class PropertiesAccessService extends org.eclipse.tcf.te.tcf.core.model.services.PropertiesAccessService {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService#getTargetAddress(java.lang.Object)
	 */
	@Override
	public Map<String, String> getTargetAddress(Object context) {
		final Map<String, String> result = new HashMap<String, String>();

		final IPeer peer;
		if (context instanceof IPeer) {
			peer = (IPeer)context;
		}
		else if (context instanceof IPeerNode) {
			peer = ((IPeerNode)context).getPeer();
		}
		else {
			peer = null;
		}

		if (peer != null) {
			Protocol.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					Map<String, String> attributes = peer.getAttributes();

					String value = attributes.get(IPeer.ATTR_NAME);
					if (value != null && !"".equals(value.trim())) { //$NON-NLS-1$
						result.put(IPropertiesAccessServiceConstants.PROP_NAME, value);
					}
					value = attributes.get(IPeer.ATTR_IP_HOST);
					if (value != null && !"".equals(value.trim())) { //$NON-NLS-1$
						result.put(IPropertiesAccessServiceConstants.PROP_ADDRESS, value);
					}

					value = attributes.get(IPeer.ATTR_IP_PORT);
					if (value != null && !"".equals(value.trim())) { //$NON-NLS-1$
						result.put(IPropertiesAccessServiceConstants.PROP_PORT, value);
					}

					value = attributes.get(IPeer.ATTR_TRANSPORT_NAME);
					if (value != null && !"".equals(value.trim())) { //$NON-NLS-1$
						result.put(IPropertiesAccessServiceConstants.PROP_TRANSPORT_NAME, value);
					}
				}
			});
		}

		return !result.isEmpty() ? Collections.unmodifiableMap(result) : null;
	}
}
