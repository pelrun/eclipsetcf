/*******************************************************************************
 * Copyright (c) 2012, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.core.delegates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.tcf.internal.debug.model.TCFLaunch;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IPathMap;
import org.eclipse.tcf.services.IPathMap.PathMapRule;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.core.interfaces.IPathMapGeneratorService;
import org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService;
import org.eclipse.tcf.te.tcf.launch.core.internal.services.PathMapService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Default tcf launch implementation.
 * <p>
 * The launch can be adapted to {@link IPropertiesContainer} to exchange user defined data
 * between the launch steps.
 */
public final class Launch extends TCFLaunch {

	private ICallback callback = null;

	private boolean manualDisconnected = false;

	/**
	 * Non-notifying properties container used for data exchange between the steps.
	 */
	private final IPropertiesContainer properties = new PropertiesContainer() {
		@Override
		public Object getAdapter(Class adapter) {
			if (ILaunch.class.equals(adapter)) {
				return Launch.this;
			}
			return super.getAdapter(adapter);
		}
	};

	/**
	 * Constructor.
	 *
	 * @param configuration The launch configuration that was launched.
	 * @param mode The launch mode.
	 */
	public Launch(ILaunchConfiguration configuration, String mode) {
		super(configuration, mode);
	}

	public void setCallback(ICallback callback) {
		this.callback = callback;
	}

	public ICallback getCallback() {
		return callback;
	}

	/**
	 * Attach the tcf debugger to the given peer model node.
	 *
	 * @param node The peer model node. Must not be <code>null</code>.
	 */
	public void attachDebugger(IPeerNode node, final ICallback callback) {
		Assert.isNotNull(node);
		Assert.isNotNull(callback);

		manualDisconnected = false;

		// Remember the peer node
		properties.setProperty("node", node); //$NON-NLS-1$
		// Determine the peer name to pass on to the TCF launch
		final String name = node.getName();

		// The debugger is using it's own channel as the implementation
		// calls for channel.terminate(...) directly
		Map<String, Boolean> flags = new HashMap<String, Boolean>();
		flags.put(IChannelManager.FLAG_FORCE_NEW, Boolean.TRUE);
		Tcf.getChannelManager().openChannel(node.getPeer(), flags, new IChannelManager.DoneOpenChannel() {
			@Override
			public void doneOpenChannel(Throwable error, IChannel channel) {
				if (error == null && channel != null) {
					LaunchListener listener = new LaunchListener() {
						@Override
						public void onProcessStreamError(TCFLaunch launch, String process_id, int stream_id, Exception error, int lost_size) {
						}
						@Override
						public void onProcessOutput(TCFLaunch launch, String process_id, int stream_id, byte[] data) {
						}
						@Override
						public void onDisconnected(TCFLaunch launch) {
							callback.done(Launch.this, StatusHelper.getStatus(Launch.this.getError()));
							removeListener(this);
						}
						@Override
						public void onCreated(TCFLaunch launch) {
						}
						@Override
						public void onConnected(TCFLaunch launch) {
							callback.done(Launch.this, Status.OK_STATUS);
							removeListener(this);
						}
					};
					addListener(listener);
					launchTCF(getLaunchMode(), name, channel);
				}
				else {
					callback.done(Launch.this, StatusHelper.getStatus(error));
				}
			}
		});
	}

	public boolean isManualDisconnected() {
		return manualDisconnected;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.debug.model.TCFLaunch#disconnect()
	 */
	@Override
	public void disconnect() throws DebugException {
		manualDisconnected = true;
	    super.disconnect();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.debug.model.TCFLaunch#getPeerName(org.eclipse.tcf.protocol.IPeer)
	 */
	@Override
	protected String getPeerName(IPeer peer) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(peer);

		IPeerNode node = (IPeerNode)properties.getProperty("node"); //$NON-NLS-1$

	    return node != null ? node.getName() : super.getPeerName(peer);
	}

	private Collection<PathMapRule> readCustomPathMapConfiguration() {
		IPeerNode node = (IPeerNode)properties.getProperty("node"); //$NON-NLS-1$
		IPeer peer = node != null ? node.getPeer() : getChannel().getRemotePeer();
		ArrayList<PathMapRule> list = new ArrayList<PathMapRule>();

        IPathMapGeneratorService generator = ServiceManager.getInstance().getService(peer, IPathMapGeneratorService.class);
        if (generator != null) {
        	PathMapRule[] generatedRules = generator.getPathMap(peer);
        	if (generatedRules != null && generatedRules.length > 0) {
        		for (PathMapRule rule : generatedRules) list.add(rule);
        	}
        }
        return list;
	}

	@Override
	protected void applyPathMap(final Runnable done) {

		final List<PathMapRule> configuredMap = getHostPathMap();
		configuredMap.addAll(readCustomPathMapConfiguration());

        int cnt = 0;
        String id = getClientID();
        for (IPathMap.PathMapRule r : configuredMap) r.getProperties().put(IPathMap.PROP_ID, id + "/" + cnt++);

		// Get the client ID
		final String clientID = getClientID();
		// If we have a client ID, we can identify path map rules set by other clients
		// and leave them alone. Otherwise, just set the path map.
		if (clientID != null) {
	        final IPathMap svc = getService(IPathMap.class);
			if (svc != null) {
				// Get the old path maps first. Keep path map rules not coming from us
				svc.get(new IPathMap.DoneGet() {
					@Override
					public void doneGet(IToken token, Exception error, PathMapRule[] map) {
						// Merge the path maps
						List<PathMapRule> rules = PathMapService.mergePathMaps(clientID, map,
										configuredMap.toArray(new IPathMap.PathMapRule[configuredMap.size()]));

						// If the merged path map differs from the agent side path map, apply the map
						if (PathMapService.isDifferent(rules, map)) {
							// Apply the path map
							PathMapService.set(rules, svc, false, new IPathMap.DoneSet() {
								@Override
								public void doneSet(IToken token, Exception error) {
					                if (error != null) getChannel().terminate(error);
					                else if (done != null) done.run();
								}
							});
						} else if (done != null) {
							done.run();
						}
					}
				});

			} else if (done != null) {
				done.run();
			}
		} else {
			super.applyPathMap(done);
		}
	}

    /* (non-Javadoc)
	 * @see org.eclipse.debug.core.Launch#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (IPropertiesContainer.class.equals(adapter)) {
			return properties;
		}

		// Must force adapters to be loaded: (Defect WIND00243348, and Eclipse bug 197664).
		Platform.getAdapterManager().loadAdapter(this, adapter.getName());

		return super.getAdapter(adapter);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.debug.model.TCFLaunch#launchConfigurationChanged(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void launchConfigurationChanged(ILaunchConfiguration cfg) {
		super.launchConfigurationChanged(cfg);

		// Apply Path Map changes
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (getChannel() != null) {
					final IPeer peer = getPeer();
					if (peer != null) {
						final IPathMapService service = ServiceManager.getInstance().getService(peer, IPathMapService.class);
						if (service != null) {
							ExecutorsUtil.execute(new Runnable() {
								@Override
								public void run() {
									service.applyPathMap(peer, false, true, new Callback() {
										@Override
										protected void internalDone(Object caller, IStatus status) {
										}
									});
								}
							});
						}
					}
				}
			}
		});
	}
}
