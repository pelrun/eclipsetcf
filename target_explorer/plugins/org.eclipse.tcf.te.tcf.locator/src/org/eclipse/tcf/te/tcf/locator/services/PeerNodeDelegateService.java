/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.locator.services;

import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.services.interfaces.IDelegateService;
import org.eclipse.tcf.te.tcf.locator.delegates.PeerNodeValidationDelegate;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * PeerNodeDelegateService
 */
public class PeerNodeDelegateService extends AbstractService implements IDelegateService {

	private IPeerNode.IDelegate validationDelegate = new PeerNodeValidationDelegate();

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IDelegateService#getDelegate(java.lang.Object, java.lang.Class)
	 */
    @SuppressWarnings("unchecked")
    @Override
    public <V extends Object> V getDelegate(Object context, Class<? extends V> clazz) {
		if (IPeerNode.IDelegate.class.isAssignableFrom(clazz)) {
			return (V) validationDelegate;
		}
		return null;
    }

}
