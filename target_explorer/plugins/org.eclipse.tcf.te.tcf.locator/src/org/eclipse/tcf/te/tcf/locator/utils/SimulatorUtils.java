/**
 * SimulatorUtils.java
 * Created on Jan 22, 2013
 *
 * Copyright (c) 2013 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.tcf.locator.utils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;

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
				String value = peerNode.getPeer().getAttributes().get(IPeerNodeProperties.PROP_SIM_ENABLED);
				if (value != null) {
					isEnabled.set(Boolean.parseBoolean(value));
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

		final AtomicBoolean isEnabled = new AtomicBoolean(false);
		final AtomicReference<String> type = new AtomicReference<String>();
		final AtomicReference<String> properties = new AtomicReference<String>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				String value = peerNode.getPeer().getAttributes().get(IPeerNodeProperties.PROP_SIM_ENABLED);
				if (value != null) {
					isEnabled.set(Boolean.parseBoolean(value));
				}

				type.set(peerNode.getPeer().getAttributes().get(IPeerNodeProperties.PROP_SIM_TYPE));
				properties.set(peerNode.getPeer().getAttributes().get(IPeerNodeProperties.PROP_SIM_PROPERTIES));
			}
		};

		if (Protocol.isDispatchThread()) runnable.run();
		else Protocol.invokeAndWait(runnable);

		Result result = null;

		if (isEnabled.get()) {
			IService[] services = ServiceManager.getInstance().getServices(peerNode, ISimulatorService.class, false);
			for (IService service : services) {
				Assert.isTrue(service instanceof ISimulatorService);
				// Get the UI service which is associated with the simulator service
				String id = service.getId();
				if (id != null && id.equals(type.get())) {
					result = new Result();
					result.service = (ISimulatorService)service;
					result.id = id;
					result.settings = properties.get();
					break;
				}
			}
		}
		return result;
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
								callback.setResult(new Boolean(status.isOK()));
								callback.done(caller, status);
							}
						}, monitor);
					} else {
						callback.setResult(Boolean.FALSE);
						callback.done(this, Status.OK_STATUS);
					}
				}
			}, monitor);
		} else {
			callback.setResult(Boolean.FALSE);
			callback.done(null, Status.OK_STATUS);
		}
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
		if (result != null && result.service != null) {
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
						callback.done(null, Status.OK_STATUS);
					}
				}
			}, monitor);
		} else {
			callback.done(null, Status.OK_STATUS);
		}
	}
}
