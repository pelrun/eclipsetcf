/*******************************************************************************
 * Copyright (c) 2014, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.internal.channelmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IStreams;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepAttributes;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService;
import org.eclipse.tcf.te.runtime.stepper.job.StepperJob;
import org.eclipse.tcf.te.runtime.stepper.utils.StepperHelper;
import org.eclipse.tcf.te.tcf.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.core.events.ChannelEvent;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.core.interfaces.IStepperServiceOperations;
import org.eclipse.tcf.te.tcf.core.interfaces.steps.ITcfStepAttributes;
import org.eclipse.tcf.te.tcf.core.interfaces.tracing.ITraceIds;
import org.eclipse.tcf.te.tcf.core.internal.channelmanager.steps.ShutdownValueAddStep;
import org.eclipse.tcf.te.tcf.core.nls.Messages;
import org.eclipse.tcf.te.tcf.core.va.ValueAddManager;
import org.eclipse.tcf.te.tcf.core.va.interfaces.IValueAdd;

/**
 * Channel manager implementation.
 */
public class ChannelManager extends PlatformObject implements IChannelManager {
	// The map of reference counters per channel
	/* default */ final Map<IChannel, AtomicInteger> refCounters = new HashMap<IChannel, AtomicInteger>();
	// The map of channels per peer id
	/* default */ final Map<String, IChannel> channels = new HashMap<String, IChannel>();
	// The map of pending open channel callback's per peer id
	/* default */ final Map<String, List<DoneOpenChannel>> pendingDones = new HashMap<String, List<DoneOpenChannel>>();
	// The list of channels opened via "forceNew" flag (needed to handle the close channel correctly)
	/* default */ final List<IChannel> forcedChannels = new ArrayList<IChannel>();
	// The map of flags used for opening a forced channel per channel
	/* default */ final Map<IChannel, Map<String, Boolean>> forcedChannelFlags = new HashMap<IChannel, Map<String, Boolean>>();
	// The map of stream listener proxies per channel
	/* default */ final Map<IChannel, List<StreamListenerProxy>> streamProxies = new HashMap<IChannel, List<StreamListenerProxy>>();
	// The map of scheduled "open channel" stepper jobs per peer id
	/* default */ final Map<String, StepperJob> pendingOpenChannel = new HashMap<String, StepperJob>();
	// The map of scheduled "close channel" stepper jobs per channel
	/* default */ final Map<IChannel, StepperJob> pendingCloseChannel = new HashMap<IChannel, StepperJob>();
	// The map of channel to peer associations
	/* default */ final Map<IChannel, IPeer> c2p = new HashMap<IChannel, IPeer>();

	/**
	 * Constructor
	 */
	public ChannelManager() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager#openChannel(org.eclipse.tcf.protocol.IPeer, java.util.Map, org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager.DoneOpenChannel)
	 */
	@Override
	public void openChannel(final IPeer peer, final Map<String, Boolean> flags, final DoneOpenChannel done) {
		openChannel(peer, flags, done, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager#openChannel(org.eclipse.tcf.protocol.IPeer, java.util.Map, org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager.DoneOpenChannel, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void openChannel(final IPeer peer, final Map<String, Boolean> flags, final DoneOpenChannel done, final IProgressMonitor monitor) {
		Assert.isNotNull(peer);
		Assert.isNotNull(done);

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(1, ITraceIds.TRACE_CHANNEL_MANAGER)) {
			try {
				throw new Throwable();
			} catch (Throwable e) {
				CoreBundleActivator.getTraceHandler().trace("ChannelManager#openChannel called from:", //$NON-NLS-1$
															1, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, ChannelManager.this);
				e.printStackTrace();
			}
		}

		// The client done callback must be called within the TCF event dispatch thread
		final DoneOpenChannel internalDone = new DoneOpenChannel() {

			@Override
			public void doneOpenChannel(final Throwable error, final IChannel channel) {
				// Invoke the client done callback
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						done.doneOpenChannel(error, channel);
					}
				};

				if (Protocol.isDispatchThread()) runnable.run();
				else Protocol.invokeLater(runnable);
			}
		};

		// The channel instance to return
		IChannel channel = null;

		// Get the peer id
		final String id = peer.getID();

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
			CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.ChannelManager_openChannel_message, id, flags),
														0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, ChannelManager.this);
		}

		// First thing to determine is if to open a new channel or the shared
		// channel can be used, if there is a shared channel at all.
		boolean forceNew = flags != null && flags.containsKey(IChannelManager.FLAG_FORCE_NEW) ? flags.get(IChannelManager.FLAG_FORCE_NEW).booleanValue() : false;
		final boolean noValueAdd = flags != null && flags.containsKey(IChannelManager.FLAG_NO_VALUE_ADD) ? flags.get(IChannelManager.FLAG_NO_VALUE_ADD).booleanValue() : false;
		final boolean noPathMap = flags != null && flags.containsKey(IChannelManager.FLAG_NO_PATH_MAP) ? flags.get(IChannelManager.FLAG_NO_PATH_MAP).booleanValue() : false;
		// If noValueAdd == true or noPathMap == true -> forceNew has to be true as well
		if (noValueAdd || noPathMap) forceNew = true;

		final boolean finForceNew = forceNew;

		// Query the shared channel if not forced to open a new channel
		if (!forceNew) channel = channels.get(id);

		// If a shared channel is available, check if the shared channel can be used
		if (channel != null) {
			// If the channel is still open, it's all done and the channel can be returned right away
			if (channel.getState() == IChannel.STATE_OPEN) {
				// Increase the reference count
				AtomicInteger counter = refCounters.get(channel);
				if (counter == null) {
					counter = new AtomicInteger(0);
					refCounters.put(channel, counter);
				}
				counter.incrementAndGet();

				if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
					CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.ChannelManager_openChannel_reuse_message, id, counter.toString()),
																0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, ChannelManager.this);
				}

				// Invoke the channel open done callback
				internalDone.doneOpenChannel(null, channel);
			}
			// If the channel is opening, wait for the channel to become fully opened.
			// Add the done open channel callback to the list of pending callback's.
			else if (channel.getState() == IChannel.STATE_OPENING) {
				List<DoneOpenChannel> dones = pendingDones.get(id);
				if (dones == null) {
					dones = new ArrayList<DoneOpenChannel>();
					pendingDones.put(id, dones);
				}
				Assert.isNotNull(dones);
				dones.add(internalDone);

				if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
					CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.ChannelManager_openChannel_pending_message, id, "0x" + Integer.toHexString(internalDone.hashCode())), //$NON-NLS-1$
																0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, ChannelManager.this);
				}
			}
			else {
				// Channel is not in open state -> drop the instance
				channels.remove(id);
				refCounters.remove(channel);
				c2p.remove(channel);
				channel = null;
			}
		}

		// Channel not available -> open a new one
		if (channel == null) {
			// Check if there is a pending "open channel" stepper job
			StepperJob job = !forceNew ? pendingOpenChannel.get(id) : null;
			if (job == null) {
				// No pending "open channel" stepper job -> schedule one and initiate opening the channel
				if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
					CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.ChannelManager_openChannel_new_message, id),
									0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, ChannelManager.this);
				}

				// Create the data properties container passed to the "open channel" steps
				final IPropertiesContainer data = new PropertiesContainer();
				// Set the flags to be passed to the "open channel" steps
				data.setProperty(IChannelManager.FLAG_FORCE_NEW, forceNew);
				data.setProperty(IChannelManager.FLAG_NO_VALUE_ADD, noValueAdd);
				data.setProperty(IChannelManager.FLAG_NO_PATH_MAP, noPathMap);
				// No recent action history persistence
				data.setProperty(IStepAttributes.PROP_SKIP_LAST_RUN_HISTORY, true);

				// Create the callback to be invoked once the "open channel" stepper job is completed
				final ICallback callback = new Callback() {
					@Override
					protected void internalDone(Object caller, IStatus status) {
						// Check for error
						if (status.getSeverity() == IStatus.ERROR) {
							// Extract the failure cause
							Throwable error = status.getException();

							if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
								CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.ChannelManager_openChannel_failed_message, id, error),
																			0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, ChannelManager.this);
							}

							// Job is done -> remove it from the list of pending jobs (shared channels only)
							if (!finForceNew) pendingOpenChannel.remove(id);

							// Invoke the primary "open channel" done callback
							internalDone.doneOpenChannel(error, null);

							// Invoke pending callback's
							List<DoneOpenChannel> pending = pendingDones.remove(id);
							if (pending != null && !pending.isEmpty()) {
								for (DoneOpenChannel d : pending) {
									d.doneOpenChannel(error, null);
								}
							}
						} else {
							// Get the channel
							IChannel channel = (IChannel)data.getProperty(ITcfStepAttributes.ATTR_CHANNEL);
							Assert.isNotNull(channel);
							Assert.isTrue(channel.getState() == IChannel.STATE_OPEN);

							// Store the channel
							if (!finForceNew) channels.put(id, channel);
							if (!finForceNew) refCounters.put(channel, new AtomicInteger(1));
							if (finForceNew) forcedChannels.add(channel);
							if (finForceNew) forcedChannelFlags.put(channel, flags);

							// Remember for which peer the channel got opened.
							c2p.put(channel, peer);

							// Job is done -> remove it from the list of pending jobs (shared channels only)
							if (!finForceNew) pendingOpenChannel.remove(id);

							if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
								CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.ChannelManager_openChannel_success_message, id),
																			0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, ChannelManager.this);
							}

							// Invoke the primary "open channel" done callback
							internalDone.doneOpenChannel(null, channel);

							// Invoke pending callback's (shared channels only)
							if (!finForceNew) {
								List<DoneOpenChannel> pending = pendingDones.remove(id);
								if (pending != null && !pending.isEmpty()) {
									for (DoneOpenChannel d : pending) {
										d.doneOpenChannel(null, channel);
									}
								}
							}
						}
					}
				};

				// Get the stepper operation service
				IStepperOperationService stepperOperationService = StepperHelper.getService(peer, IStepperServiceOperations.OPEN_CHANNEL);

				// Schedule the "open channel" stepper job
				IStepContext stepContext = stepperOperationService.getStepContext(peer, IStepperServiceOperations.OPEN_CHANNEL);
				String stepGroupId = stepperOperationService.getStepGroupId(peer, IStepperServiceOperations.OPEN_CHANNEL);

				if (stepGroupId != null && stepContext != null) {
					String name = stepperOperationService.getStepGroupName(peer, IStepperServiceOperations.OPEN_CHANNEL);
					boolean isCancelable = stepperOperationService.isCancelable(peer, IStepperServiceOperations.OPEN_CHANNEL);

					job = new StepperJob(name != null ? name : "", stepContext, stepperOperationService.getStepGroupData(peer, IStepperServiceOperations.OPEN_CHANNEL, data), stepGroupId, IStepperServiceOperations.OPEN_CHANNEL, isCancelable, true); //$NON-NLS-1$
					job.setJobCallback(callback);
					job.markStatusHandled();
					if (monitor != null) {
						final StepperJob finalJob = job;
						Thread thread = new Thread(new Runnable() {
							@Override
							public void run() {
								finalJob.run(monitor);
							}
						}, job.getName());
						thread.start();
					}
					else {
						job.schedule();
					}
				}

				// Remember the "open channel" stepper job until finished (shared channels only)
				if (job != null && !forceNew) {
					pendingOpenChannel.put(id, job);
				}
			} else {
				// There is a pending "open channel" stepper job -> add the open channel callback to the list
				List<DoneOpenChannel> dones = pendingDones.get(id);
				if (dones == null) {
					dones = new ArrayList<DoneOpenChannel>();
					pendingDones.put(id, dones);
				}
				Assert.isNotNull(dones);
				dones.add(internalDone);

				if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
					CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.ChannelManager_openChannel_pending_message, id, "0x" + Integer.toHexString(internalDone.hashCode())), //$NON-NLS-1$
																0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, ChannelManager.this);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager#getChannel(org.eclipse.tcf.protocol.IPeer)
	 */
	@Override
	public IChannel getChannel(final IPeer peer) {
		final AtomicReference<IChannel> channel = new AtomicReference<IChannel>();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
				channel.set(internalGetChannel(peer));
			}
		};
		if (Protocol.isDispatchThread()) runnable.run();
		else Protocol.invokeAndWait(runnable);

	    return channel.get();
	}

	/**
	 * Returns the shared channel instance for the given peer.
	 * <p>
	 * <b>Note:</b> This method must be invoked at the TCF dispatch thread.
	 *
	 * @param peer The peer. Must not be <code>null</code>.
	 * @return The channel instance or <code>null</code>.
	 */
	public IChannel internalGetChannel(IPeer peer) {
		Assert.isNotNull(peer);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		// Get the peer id
		String id = peer.getID();

		// Get the channel
		IChannel channel = channels.get(id);
		if (channel != null && !(channel.getState() == IChannel.STATE_OPEN || channel.getState() == IChannel.STATE_OPENING)) {
			// Channel is not in open state -> drop the instance
			channels.remove(id);
			refCounters.remove(channel);
			c2p.remove(channel);
			channel = null;
		}

		return channel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager#closeChannel(org.eclipse.tcf.protocol.IChannel)
	 */
	@Override
	public void closeChannel(final IChannel channel) {
		closeChannel(channel, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager#closeChannel(org.eclipse.tcf.protocol.IChannel)
	 */
	@Override
	public void closeChannel(final IChannel channel, final IProgressMonitor monitor) {
		Runnable runnable = new Runnable() {
			@Override
            public void run() {
				Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
				internalCloseChannel(channel, monitor);
			}
		};
		if (Protocol.isDispatchThread()) runnable.run();
		else Protocol.invokeLater(runnable);
	}

	/**
	 * Closes the given channel.
	 * <p>
	 * If the given channel is a reference counted channel, the channel will be closed if the reference counter
	 * reaches 0. For non reference counted channels, the channel is closed immediately.
	 * <p>
	 * <b>Note:</b> This method must be invoked at the TCF dispatch thread.
	 *
	 * @param channel The channel. Must not be <code>null</code>.
	 */
	/* default */ void internalCloseChannel(final IChannel channel, final IProgressMonitor monitor) {
		Assert.isNotNull(channel);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		// Determine the peer for the channel to close
		IPeer p = c2p.get(channel);
		final IPeer peer = p != null ? p : channel.getRemotePeer();

		// Get the id of the remote peer
		final String id = peer.getID();

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
			CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.ChannelManager_closeChannel_message, id),
														0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, ChannelManager.this);
		}

		// Determine if the given channel is a reference counted channel
		final boolean isRefCounted = !forcedChannels.contains(channel);

		// Get the reference counter (if the channel is a reference counted channel)
		AtomicInteger counter = isRefCounted ? refCounters.get(channel) : null;

		// If the counter is null or get 0 after the decrement, close the channel
		if (counter == null || counter.decrementAndGet() == 0) {
			// Check if there is a pending "close channel" stepper job
			StepperJob job = pendingCloseChannel.get(channel);
			if (job == null) {
				// No pending "close channel" stepper job -> schedule one and initiate closing the channel
				if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
					CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.ChannelManager_closeChannel_close_message, id),
																0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, ChannelManager.this);
				}

				// Create the data properties container passed to the "close channel" steps
				final IPropertiesContainer data = new PropertiesContainer();
				// Set the channel to close
				data.setProperty(ITcfStepAttributes.ATTR_CHANNEL, channel);
				// No recent action history persistence
				data.setProperty(IStepAttributes.PROP_SKIP_LAST_RUN_HISTORY, true);

				// Determine if the value-add's can be shutdown or must stay alive.
				// In case the channel to close is not reference counted, but this is a reference
				// counted channel to the same peer, and that channel is still open, the
				// value-adds must stay alive.
				if (!isRefCounted) {
					IChannel shared = channels.get(id);
					if (shared != null && (shared.getState() == IChannel.STATE_OPEN || shared.getState() == IChannel.STATE_OPENING)) {
						data.setProperty(ShutdownValueAddStep.PROP_SKIP_SHUTDOWN_STEP, true);
					}
				} else {
					// The channel is reference counted, that means it is a shared channel
					// and normally it will shutdown the value-adds if closed. However, we
					// can have not reference counted channels to the same target that is
					// using value-add's. In this case we also have to skip shutting down
					// the value-add.
					for (IChannel c : forcedChannels) {
						if (id.equals(c.getRemotePeer().getID())) {
							Map<String, Boolean> flags = forcedChannelFlags.get(c);
							boolean noValueAdd = flags != null && flags.containsKey(IChannelManager.FLAG_NO_VALUE_ADD) ? flags.get(IChannelManager.FLAG_NO_VALUE_ADD).booleanValue() : false;
							if (!noValueAdd) {
								data.setProperty(ShutdownValueAddStep.PROP_SKIP_SHUTDOWN_STEP, true);
								break;
							}
						}
					}
				}

				// Create the callback to be invoked once the "close channel" stepper job is completed
				final ICallback callback = new Callback() {
					@Override
					protected void internalDone(Object caller, IStatus status) {
						// Check for error
						if (status.getSeverity() == IStatus.ERROR) {
							// Extract the failure cause
							Throwable error = status.getException();

							if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
								CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.ChannelManager_closeChannel_failed_message, id, error),
																					 0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, ChannelManager.this);
							}

							// Job is done -> remove it from the list of pending jobs
							pendingCloseChannel.remove(channel);
						} else {
							// Job is done -> remove it from the list of pending jobs
							pendingCloseChannel.remove(channel);

							// Clean the reference counter and the channel map
							if (isRefCounted) channels.remove(id);
							if (isRefCounted) refCounters.remove(channel);
							if (!isRefCounted) forcedChannels.remove(channel);
							if (!isRefCounted) forcedChannelFlags.remove(channel);

							// Remove the channel / peer association
							c2p.remove(channel);

							if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
								CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.ChannelManager_closeChannel_closed_message, id),
																			0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, ChannelManager.this);
							}
						}

						// Log closed channels
						ChannelEvent event = new ChannelEvent(ChannelManager.this, channel, ChannelEvent.TYPE_CLOSE, null);
						EventManager.getInstance().fireEvent(event);

						// If there is no remaining shared or private channel to the target,
						// also close the log writer which is shared by all channels to the same target.
						Runnable runnable = new Runnable() {
							@Override
							public void run() {
								boolean closeWriter = internalGetChannel(peer) == null;
								if (closeWriter) {
									for (IChannel c : forcedChannels) {
										if (id.equals(c.getRemotePeer().getID()) && c.getState() != IChannel.STATE_CLOSED) {
											closeWriter = false;
											break;
										}
									}

									if (closeWriter) {
										ChannelEvent event = new ChannelEvent(ChannelManager.this, channel, ChannelEvent.TYPE_CLOSE_WRITER, null);
										EventManager.getInstance().fireEvent(event);
									}
								}
							}
						};

						if (Protocol.isDispatchThread()) runnable.run();
						else Protocol.invokeLater(runnable);
					}
				};

				// Get the stepper operation service
				IStepperOperationService stepperOperationService = StepperHelper.getService(peer, IStepperServiceOperations.CLOSE_CHANNEL);

				// Schedule the "close channel" stepper job
				IStepContext stepContext = stepperOperationService.getStepContext(peer, IStepperServiceOperations.CLOSE_CHANNEL);
				String stepGroupId = stepperOperationService.getStepGroupId(peer, IStepperServiceOperations.CLOSE_CHANNEL);

				if (stepGroupId != null && stepContext != null) {
					String name = stepperOperationService.getStepGroupName(peer, IStepperServiceOperations.CLOSE_CHANNEL);
					boolean isCancelable = stepperOperationService.isCancelable(peer, IStepperServiceOperations.CLOSE_CHANNEL);

					job = new StepperJob(name != null ? name : "", stepContext, stepperOperationService.getStepGroupData(peer, IStepperServiceOperations.CLOSE_CHANNEL, data), stepGroupId, IStepperServiceOperations.CLOSE_CHANNEL, isCancelable, true); //$NON-NLS-1$
					job.setJobCallback(callback);
					job.markStatusHandled();
					if (monitor != null) {
						final StepperJob finalJob = job;
						Thread thread = new Thread(new Runnable() {
							@Override
							public void run() {
								finalJob.run(monitor);
							}
						}, "Close channel to " + job.getName()); //$NON-NLS-1$
						thread.start();
					}
					else {
						job.schedule();
					}
				}

				// Remember the "close channel" stepper job until finished
				if (job != null) {
					pendingCloseChannel.put(channel, job);
				}
			} else {
				// There is a pending "close channel" stepper job
				if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
					CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.ChannelManager_closeChannel_pending_message, id),
																0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, ChannelManager.this);
				}
			}
		} else {
			if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
				CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.ChannelManager_closeChannel_inuse_message, id, counter.toString()),
															0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, ChannelManager.this);
			}
		}

		// Clean up the list of forced channels. Remove all channels already been closed.
		ListIterator<IChannel> iter = forcedChannels.listIterator();
		while (iter.hasNext()) {
			IChannel c = iter.next();
			if (c.getState() == IChannel.STATE_CLOSED) {
				iter.remove();
				forcedChannelFlags.remove(c);
				c2p.remove(c);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager#shutdown(org.eclipse.tcf.protocol.IPeer, boolean)
	 */
	@Override
	public void shutdown(final IPeer peer, boolean wait) {
		Runnable runnable = new Runnable() {
			@Override
            public void run() {
				Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
				internalShutdown(peer);
			}
		};

		if (Protocol.isDispatchThread()) runnable.run();
		else if (wait) Protocol.invokeAndWait(runnable);
		else Protocol.invokeLater(runnable);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager#shutdown(org.eclipse.tcf.protocol.IPeer)
	 */
	@Override
	public void shutdown(final IPeer peer) {
		shutdown(peer, false);
	}

	/**
	 * Shutdown the communication to the given peer, no matter of the current
	 * reference count. A possible associated value-add is shutdown as well.
	 *
	 * @param peer The peer. Must not be <code>null</code>.
	 */
	/* default */ void internalShutdown(IPeer peer) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(peer);

		// Get the peer id
		String id = peer.getID();

		// First, close all channels that are not reference counted
		ListIterator<IChannel> iter = forcedChannels.listIterator();
		while (iter.hasNext()) {
			IChannel c = iter.next();
			if (id.equals(c.getRemotePeer().getID())) {
				c.close();
				iter.remove();
				forcedChannelFlags.remove(c);
			}
		}

		// Get the channel
		IChannel channel = internalGetChannel(peer);
		if (channel != null) {
			// Reset the reference count (will force a channel close)
			refCounters.remove(channel);
			channels.remove(channel.getRemotePeer().getID());
			c2p.remove(channel);

			// Close the channel
			channel.close();

			// Log closed channels
			ChannelEvent event = new ChannelEvent(ChannelManager.this, channel, ChannelEvent.TYPE_CLOSE, null);
			EventManager.getInstance().fireEvent(event);

			event = new ChannelEvent(ChannelManager.this, channel, ChannelEvent.TYPE_CLOSE_WRITER, null);
			EventManager.getInstance().fireEvent(event);
		}


		try {
			IValueAdd[] valueAdds = ValueAddManager.getInstance().getValueAdd(peer);
			if (valueAdds != null) {
				for (IValueAdd valueAdd : valueAdds) {
					valueAdd.shutdown(peer.getID(), new Callback());
				}
			}
		}
		catch (Exception e) {
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager#closeAll(boolean)
	 */
	@Override
	public void closeAll(boolean wait) {
		if (wait) Assert.isTrue(!Protocol.isDispatchThread());

		Runnable runnable = new Runnable() {
			@Override
            public void run() {
				Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
				internalCloseAll();
			}
		};

		if (Protocol.isDispatchThread()) runnable.run();
		else if (wait) Protocol.invokeAndWait(runnable);
		else Protocol.invokeLater(runnable);
	}

	/**
	 * Close all open channel, no matter of the current reference count.
	 * <p>
	 * <b>Note:</b> This method must be invoked at the TCF dispatch thread.
	 */
	/* default */ void internalCloseAll() {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		IChannel[] openChannels = channels.values().toArray(new IChannel[channels.values().size()]);

		refCounters.clear();
		channels.clear();

		for (IChannel channel : openChannels) internalCloseChannel(channel, null);

		c2p.clear();
	}

	// ----- Streams handling -----

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager#subscribeStream(org.eclipse.tcf.protocol.IChannel, java.lang.String, org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager.IStreamsListener, org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager.DoneSubscribeStream)
	 */
    @Override
    public void subscribeStream(final IChannel channel, final String streamType, final IStreamsListener listener, final DoneSubscribeStream done) {
    	Assert.isNotNull(channel);
    	Assert.isNotNull(streamType);
    	Assert.isNotNull(listener);
    	Assert.isNotNull(done);

    	if (channel.getState() != IChannel.STATE_OPEN) {
    		done.doneSubscribeStream(new Exception(Messages.ChannelManager_stream_closed_message));
    		return;
    	}

    	StreamListenerProxy proxy = null;

    	// Get all the streams listener proxy instance for the given channel
    	List<StreamListenerProxy> proxies = streamProxies.get(channel);
    	// Loop the proxies and find the one for the given stream type
    	if (proxies != null) {
    		for (StreamListenerProxy candidate : proxies) {
    			if (streamType.equals(candidate.getStreamType())) {
    				proxy = candidate;
    				break;
    			}
    		}
    	}

    	// If the proxy already exist, add the listener to the proxy and return immediately
    	if (proxy != null) {
    		proxy.addListener(listener);
    		done.doneSubscribeStream(null);
    	} else {
    		// No proxy yet -> subscribe to the stream type for real and register the proxy
    		proxy = new StreamListenerProxy(channel, streamType);
    		if (proxies == null) {
    			proxies = new ArrayList<StreamListenerProxy>();
    			streamProxies.put(channel, proxies);
    		}
    		proxies.add(proxy);
    		proxy.addListener(listener);

    		IStreams service = channel.getRemoteService(IStreams.class);
    		if (service != null) {
    			final StreamListenerProxy finProxy = proxy;
    			final List<StreamListenerProxy> finProxies = proxies;

    			// Subscribe to the stream type
    			service.subscribe(streamType, proxy, new IStreams.DoneSubscribe() {
					@Override
					public void doneSubscribe(IToken token, Exception error) {
						if (error != null) {
							finProxy.removeListener(listener);
							if (finProxy.isEmpty()) finProxies.remove(finProxy);
			    			if (finProxies.isEmpty()) streamProxies.remove(channel);
						}
						done.doneSubscribeStream(error);
					}
				});
    		} else {
    			proxy.removeListener(listener);
    			if (proxy.isEmpty()) proxies.remove(proxy);
    			if (proxies.isEmpty()) streamProxies.remove(channel);
    			done.doneSubscribeStream(new Exception(Messages.ChannelManager_stream_missing_service_message));
    		}
    	}
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager#unsubscribeStream(org.eclipse.tcf.protocol.IChannel, java.lang.String, org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager.IStreamsListener, org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager.DoneUnsubscribeStream)
     */
    @Override
    public void unsubscribeStream(final IChannel channel, final String streamType, final IStreamsListener listener, final DoneUnsubscribeStream done) {
    	Assert.isNotNull(channel);
    	Assert.isNotNull(streamType);
    	Assert.isNotNull(listener);
    	Assert.isNotNull(done);

    	if (channel.getState() != IChannel.STATE_OPEN) {
    		done.doneUnsubscribeStream(new Exception(Messages.ChannelManager_stream_closed_message));
    		return;
    	}

    	StreamListenerProxy proxy = null;

    	// Get all the streams listener proxy instance for the given channel
    	List<StreamListenerProxy> proxies = streamProxies.get(channel);
    	// Loop the proxies and find the one for the given stream type
    	if (proxies != null) {
    		for (StreamListenerProxy candidate : proxies) {
    			if (streamType.equals(candidate.getStreamType())) {
    				proxy = candidate;
    				break;
    			}
    		}
    	}

    	if (proxy != null) {
    		// Remove the listener from the proxy
    		proxy.removeListener(listener);
    		// Are there remaining proxied listeners for this stream type?
    		if (proxy.isEmpty()) {
    			// Remove from proxy list
				proxies.remove(proxy);
				if (proxies.isEmpty()) streamProxies.remove(channel);
    			// Unregister the stream type
        		IStreams service = channel.getRemoteService(IStreams.class);
        		if (service != null) {
        			// Unsubscribe
        			service.unsubscribe(streamType, proxy, new IStreams.DoneUnsubscribe() {
						@Override
						public void doneUnsubscribe(IToken token, Exception error) {
							done.doneUnsubscribeStream(error);
						}
					});
        		} else {
        			done.doneUnsubscribeStream(new Exception(Messages.ChannelManager_stream_missing_service_message));
        		}
    		} else {
				done.doneUnsubscribeStream(null);
    		}
    	} else {
			done.doneUnsubscribeStream(null);
    	}
    }

}
