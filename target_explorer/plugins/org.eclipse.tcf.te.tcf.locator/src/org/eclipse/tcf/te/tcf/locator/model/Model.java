/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.model;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.nodes.PeerModel;


/**
 * Helper class to instantiate and initialize the TCF locator model.
 */
public final class Model {
	// Reference to the locator model
	/* default */ static volatile IPeerModel peerModel;

	/**
	 * Returns the shared locator model instance.
	 * <p>
	 * If the shared locator model instance has not been yet initialized,
	 * the method does initialize the shared locator model instance.
	 *
	 * @return The shared locator model.
	 */
	public static IPeerModel getModel() {
		return getModel(false);
	}

	/**
	 * Returns the shared locator model instance.
	 * <p>
	 * If the shared locator model instance has not been yet initialized,
	 * and <code>shutdown</code> is <code>false</code>, the method does
	 * initialize the shared locator model instance.
	 *
	 * @param shutdown <code>True</code> if the method is called during shutdown and
	 *                 the model should not be initialized if not done anyway. <code>
	 *                 false</code> in any other case.
	 *
	 * @return The shared locator model.
	 */
	public static IPeerModel getModel(boolean shutdown) {
		// Access to the locator model must happen in the TCF dispatch thread
		if (peerModel == null && !shutdown) {
			if (Protocol.isDispatchThread()) {
				initialize();
			} else {
				Protocol.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						initialize();
					}
				});
			}
		}
		return peerModel;
	}

	/**
	 * Initialize the root node. Must be called within the TCF dispatch thread.
	 */
	protected static void initialize() {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		// If locator model is set in the mean while, initialize got
		// called twice. Return immediately in this case.
		if (peerModel != null) return;

		// Create the model instance
		peerModel = new PeerModel();
		// Refresh the model right away
		peerModel.getService(IPeerModelRefreshService.class).refresh(null);
		// Start the scanner
		peerModel.startScanner(5000, 120000);
	}

	/**
	 * Dispose the root node.
	 */
	public static void dispose() {
		if (peerModel == null) return;

		// Access to the locator model must happen in the TCF dispatch thread
		if (Protocol.isDispatchThread()) {
			peerModel.dispose();
		} else {
			Protocol.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					peerModel.dispose();
				}
			});
		}

		peerModel = null;
	}

}
