/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.processes.core.model.runtime.RuntimeModel;

/**
 * Processes service model manager implementation.
 */
public class ModelManager {
	// Reference to the runtime models
	/* default */ static final Map<String, IRuntimeModel> runtimeModels = new HashMap<String, IRuntimeModel>();

	/**
	 * Returns the runtime model instance for the given peer model
	 * <p>
	 * If not yet initialized, a new runtime model will be initialized before returning.
	 *
	 * @param peerNode The peer model instance. Must not be <code>null</code>.
	 * @return The runtime model.
	 */
	public static IRuntimeModel getRuntimeModel(final IPeerNode peerNode) {
		Assert.isNotNull(peerNode);

		// The result reference holder
		final AtomicReference<IRuntimeModel> runtimeModel = new AtomicReference<IRuntimeModel>();

		// Create the runnable to execute
		Runnable runnable = new Runnable() {
			@Override
            public void run() {
				Assert.isTrue(Protocol.isDispatchThread());

				// Get the peer id
				String id = peerNode.getPeerId();
				// Lookup the runtime model instance
				IRuntimeModel candidate = runtimeModels.get(id);
				// Initialize a new runtime model instance if necessary
				if (candidate == null) {
					candidate = initializeRuntimeModel(peerNode);
					if (candidate != null) runtimeModels.put(id, candidate);
				}
				// Store to the result reference holder
				runtimeModel.set(candidate);
			}
		};

		// Execute the runnable
		if (Protocol.isDispatchThread()) runnable.run();
		else Protocol.invokeAndWait(runnable);

		return runtimeModel.get();
	}

	/**
	 * Initialize the runtime model.
	 * <p>
	 * Must be called within the TCF dispatch thread.
	 *
	 * @param peerNode The peer model instance. Must not be <code>null</code>.
	 * @return The runtime model.
	 */
	protected static IRuntimeModel initializeRuntimeModel(IPeerNode peerNode) {
		Assert.isTrue(Protocol.isDispatchThread());
		IRuntimeModel runtimeModel = new RuntimeModel(peerNode);
		return runtimeModel;
	}

	/**
	 * Dispose the runtime model.
	 *
	 * @param peerNode The peer model instance. Must not be <code>null</code>.
	 */
	public static void disposeRuntimeModel(final IPeerNode peerNode) {
		Assert.isNotNull(peerNode);

		Runnable runnable = new Runnable() {
			@Override
            public void run() {
				Assert.isTrue(Protocol.isDispatchThread());

				// Get the peer id
				String id = peerNode.getPeerId();
				// Lookup the runtime model instance
				IRuntimeModel candidate = runtimeModels.remove(id);
				// Dispose it
				if (candidate != null) candidate.dispose();
			}
		};

		if (Protocol.isDispatchThread()) runnable.run();
		else Protocol.invokeAndWait(runnable);
	}
}
