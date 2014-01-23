/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.locator.internal.services;

import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.services.interfaces.IDelegateService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeValidationDelegate;
import org.eclipse.tcf.te.tcf.locator.internal.delegates.PeerNodeValidationDelegate;

/**
 * PeerNodeDelegateService
 */
public class PeerNodeDelegateService extends AbstractService implements IDelegateService {

	private IPeerNodeValidationDelegate validationDelegate = new PeerNodeValidationDelegate();

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IDelegateService#getDelegate(java.lang.Object, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
    @Override
	public <V> V getDelegate(Object context, Class<? extends V> clazz) {
		if (IPeerNodeValidationDelegate.class.isAssignableFrom(clazz)) {
			return (V) validationDelegate;
		}
		return null;
	}

}
