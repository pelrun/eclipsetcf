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

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Simulator related utilities.
 */
public final class SimulatorUtils {

	/**
	 * Result of getSimulatorService.
	 */
	public static class Result {
		public ISimulatorService service;
		public String id;
		public String settings;
	}

	/**
	 * Returns if or if the given peer model has the simulator enabled or not.
	 *
	 * @param peerNode The peer model node. Must not be <code>null</code>.
	 * @return <code>True</code> if the simulator is enabled, <code>false</code> otherwise.
	 */
	public static boolean isSimulatorEnabled(final IPeerNode peerNode) {
		Assert.isNotNull(peerNode);

		final AtomicBoolean isEnabled = new AtomicBoolean(false);

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				String subType = peerNode.getPeer().getAttributes().get(IPeerProperties.PROP_SUBTYPE);
				if (subType != null) {
					isEnabled.set(subType.equals(IPeerProperties.SUBTYPE_SIM));
				}
			}
		};

		if (Protocol.isDispatchThread()) runnable.run();
		else Protocol.invokeAndWait(runnable);

		return isEnabled.get();
	}

	/**
	 * Returns the simulator service and the settings for the simulator launch.
	 * If no simulator service is configured in the peer
	 * or the configured service is not available, <code>null</code> will be returned.
	 *
	 * @param peerNode The peer model node. Must not be <code>null</code>.
	 * @return The {@link Result} containing the simulator service and the settings or <code>null</code>.
	 */
	public static Result getSimulatorService(final IPeerNode peerNode) {
		Assert.isNotNull(peerNode);

		Result result = null;

		IPeer peer = peerNode.getPeer();
		if (peer != null) {
			if (isSimulatorEnabled(peerNode)) {
				String type = peer.getAttributes().get(IPeerProperties.PROP_SIM_TYPE);
				IService[] services = ServiceManager.getInstance().getServices(peerNode, ISimulatorService.class, false);
				for (IService service : services) {
					Assert.isTrue(service instanceof ISimulatorService);
					// Get the UI service which is associated with the simulator service
					String id = service.getId();
					if (id != null && id.equals(type)) {
						result = new Result();
						result.service = (ISimulatorService)service;
						result.id = id;
						result.settings = peer.getAttributes().get(IPeerProperties.PROP_SIM_PROPERTIES);
						break;
					}
				}
			}
		}

		return result;
	}

	/**
	 * Returns the simulator service and the settings for the simulator launch.
	 * If no simulator service is configured in the peer
	 * or the configured service is not available, <code>null</code> will be returned.
	 *
	 * @param peerNode The peer model node. Must not be <code>null</code>.
	 * @return The {@link Result} containing the simulator service and the settings or <code>null</code>.
	 */
	public static ISimulatorService getSimulatorService(final Object context, final String type) {
		Assert.isNotNull(context);

		IService[] services = ServiceManager.getInstance().getServices(context, ISimulatorService.class, false);
		for (IService service : services) {
			Assert.isTrue(service instanceof ISimulatorService);
			// Get the UI service which is associated with the simulator service
			String id = service.getId();
			if (id != null && id.equals(type)) {
				return (ISimulatorService)service;
			}
		}

		return null;
	}

	/**
	 * Starts the simulator if the simulator launch is enabled for the given peer
	 * model node and the configured simulator service type is available. In any
	 * other cases, the given callback is invoked immediately.
	 *
	 * @param peerNode The peer model node. Must not be <code>null</code>.
	 * @param monitor The progress monitor.
	 * @param callback The callback to invoke if finished. Must not be <code>null</code>.
	 */
	public static void start(final IPeerNode peerNode, final IProgressMonitor monitor, final ICallback callback) {
		Assert.isNotNull(peerNode);
		Assert.isNotNull(callback);

		// Determine if we have to start a simulator first
		final Result result = getSimulatorService(peerNode);
		if (result != null && result.service != null) {
			// Check if the simulator is already running
			result.service.isRunning(peerNode, result.settings, new Callback() {
				@Override
				protected void internalDone(Object caller, IStatus status) {
					Object cbResult = getResult();
					if (cbResult instanceof Boolean && !((Boolean)cbResult).booleanValue()) {
						// Start the simulator
						result.service.start(peerNode, result.settings, new Callback() {
							@Override
							protected void internalDone(Object caller, IStatus status) {
								setUsedRunningSimulator(peerNode, false);
								callback.setResult(new Boolean(status.isOK()));
								callback.done(caller, status);
							}
						}, monitor);
					} else {
						// Try to use running simulator
						result.service.useRunning(peerNode, result.settings, new Callback() {
							@Override
							protected void internalDone(Object caller, IStatus status) {
								setUsedRunningSimulator(peerNode, true);
								callback.setResult(new Boolean(status.isOK()));
								callback.done(caller, status);
							}
						}, monitor);
					}
				}
			}, monitor);
		} else {
			setUsedRunningSimulator(peerNode, false);
			callback.setResult(Boolean.FALSE);
			callback.done(null, Status.OK_STATUS);
		}
	}

	protected static void setUsedRunningSimulator(final IPeerNode peerNode, final boolean usedRunning) {
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				peerNode.setProperty(SimulatorUtils.class.getSimpleName() + ".usedRunning", usedRunning ? new Boolean(usedRunning) : null); //$NON-NLS-1$
			}
		});
	}

	protected static boolean getUsedRunningSimulator(final IPeerNode peerNode) {
		final AtomicBoolean usedRunning = new AtomicBoolean(false);
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				usedRunning.set(peerNode.getBooleanProperty(SimulatorUtils.class.getSimpleName() + ".usedRunning")); //$NON-NLS-1$
			}
		});
		return usedRunning.get();
	}

	/**
	 * Stops the simulator if the simulator launch is enabled for the given peer
	 * model node and the configured simulator service type is available. In any
	 * other cases, the given callback is invoked immediately.
	 *
	 * @param peerNode The peer model node. Must not be <code>null</code>.
	 * @param monitor The progress monitor.
	 * @param callback The callback to invoke if finished. Must not be <code>null</code>.
	 */
	public static void stop(final IPeerNode peerNode, final IProgressMonitor monitor, final ICallback callback) {
		Assert.isNotNull(peerNode);
		Assert.isNotNull(callback);

		// Get the associated simulator service
		final Result result = getSimulatorService(peerNode);
		if (result != null && result.service != null && !getUsedRunningSimulator(peerNode)) {
			setUsedRunningSimulator(peerNode, false);
			// Determine if the simulator is at all running
			result.service.isRunning(peerNode, result.settings, new Callback() {
				@Override
				protected void internalDone(Object caller, IStatus status) {
					Object cbResult = getResult();
					if (cbResult instanceof Boolean && ((Boolean)cbResult).booleanValue()) {
						// Stop the simulator
						result.service.stop(peerNode, result.settings, new Callback(callback) {
							@Override
							protected void internalDone(Object caller, IStatus status) {
								callback.done(caller, status);
							}
						}, monitor);
					} else {
						result.service.cleanup(peerNode, result.settings);
						callback.done(null, Status.OK_STATUS);
					}
				}
			}, monitor);
		} else {
			setUsedRunningSimulator(peerNode, false);
			callback.done(null, Status.OK_STATUS);
		}
	}
}
