/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.services;

import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.services.interfaces.IDelegateService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.processes.ui.internal.delegates.DefaultContextToolbarDelegate;
import org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate;

/**
 * Delegate service implementation.
 */
public class DelegateService extends AbstractService implements IDelegateService {

	final private IDefaultContextToolbarDelegate toolbarDelegate = new DefaultContextToolbarDelegate();

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IDelegateService#getDelegate(java.lang.Object, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <V> V getDelegate(Object context, Class<? extends V> clazz) {

		if (context instanceof IPeerNode) {
			if (IDefaultContextToolbarDelegate.class.isAssignableFrom(clazz)) {
				return (V) toolbarDelegate;
			}
			return null;
		}

		return null;
	}

}
