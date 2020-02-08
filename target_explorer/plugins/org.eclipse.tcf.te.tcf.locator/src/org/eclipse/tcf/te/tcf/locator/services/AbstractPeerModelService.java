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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelService;


/**
 * Abstract peer model service base implementation.
 */
public abstract class AbstractPeerModelService extends PlatformObject implements IPeerModelService {
	// Reference to the parent locator model
	private final IPeerModel peerModel;

	/**
	 * Constructor.
	 *
	 * @param parentModel The parent peer model instance. Must not be <code>null</code>.
	 */
	public AbstractPeerModelService(IPeerModel parentModel) {
		Assert.isNotNull(parentModel);
		peerModel = parentModel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelService#getPeerModel()
	 */
	@Override
	public final IPeerModel getPeerModel() {
		return peerModel;
	}
}
