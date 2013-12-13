/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.core.delegates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.tcf.internal.debug.model.TCFLaunch;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IPathMap;
import org.eclipse.tcf.services.IPathMap.DoneSet;
import org.eclipse.tcf.services.IPathMap.PathMapRule;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.core.interfaces.IPathMapGeneratorService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Default tcf launch implementation.
 * <p>
 * The launch can be adapted to {@link IPropertiesContainer} to exchange user defined data
 * between the launch steps.
 */
public final class Launch extends TCFLaunch {

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

	/**
	 * Attach the tcf debugger to the given peer model node.
	 *
	 * @param node The peer model node. Must not be <code>null</code>.
	 */
	public void attachDebugger(IPeerNode node) {
		Assert.isNotNull(node);

		final String name = node.getPeer().getName();

		// The debugger is using it's own channel as the implementation
		// calls for channel.terminate(...) directly
		Map<String, Boolean> flags = new HashMap<String, Boolean>();
		flags.put(IChannelManager.FLAG_FORCE_NEW, Boolean.TRUE);
		Tcf.getChannelManager().openChannel(node.getPeer(), flags, new IChannelManager.DoneOpenChannel() {
			@Override
			public void doneOpenChannel(Throwable error, IChannel channel) {
				if (error == null && channel != null) {
					launchTCF(getLaunchMode(), name, channel);
				}
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.debug.model.TCFLaunch#readCustomPathMapConfiguration(org.eclipse.tcf.protocol.IChannel, org.eclipse.debug.core.ILaunchConfiguration, java.util.List)
	 */
	@Override
	protected void readCustomPathMapConfiguration(IChannel channel, ILaunchConfiguration cfg, List<PathMapRule> host_path_map) {
		Assert.isNotNull(channel);
		Assert.isNotNull(cfg);
		Assert.isNotNull(host_path_map);

        IPathMapGeneratorService generator = ServiceManager.getInstance().getService(channel.getRemotePeer(), IPathMapGeneratorService.class);
        if (generator != null) {
        	PathMapRule[] generatedRules = generator.getPathMap(channel.getRemotePeer());
        	if (generatedRules != null && generatedRules.length > 0) {
        		host_path_map.addAll(Arrays.asList(generatedRules));
        	}
        }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.debug.model.TCFLaunch#applyPathMap(org.eclipse.tcf.protocol.IChannel, org.eclipse.tcf.services.IPathMap.PathMapRule[], org.eclipse.tcf.services.IPathMap.DoneSet)
	 */
	@Override
	protected void applyPathMap(final IChannel channel, final PathMapRule[] map, final DoneSet done) {
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
					public void doneGet(IToken token, Exception error, PathMapRule[] oldMap) {
						// Merge the maps to a new list
						List<PathMapRule> rules = new ArrayList<PathMapRule>();

						if (oldMap != null && oldMap.length > 0) {
							for (PathMapRule rule : oldMap) {
								if (rule.getID() == null || !rule.getID().startsWith(clientID)) {
									rules.add(rule);
								}
							}
						}

						rules.addAll(Arrays.asList(map));

						// Determine if the map has changed
						boolean changed = oldMap != null ? oldMap.length != rules.size() : !rules.isEmpty();
						if (!changed && !rules.isEmpty()) {
							// Make a copy of new map and remove all rules listed
							// by the old map. If not empty at the end, the new map
							// is different from the old map.
							List<PathMapRule> copy = new ArrayList<PathMapRule>(rules);
							for (PathMapRule rule : oldMap) {
								Iterator<PathMapRule> iter = copy.iterator();
								while (iter.hasNext()) {
									PathMapRule r = iter.next();
									if (r.equals(rule)) {
										iter.remove();
										break;
									}
								}
							}

							changed = !copy.isEmpty();
						}

						// If the path map has changed, apply the map
						if (changed) {
							svc.set(rules.toArray(new PathMapRule[rules.size()]), done);
						} else {
							done.doneSet(token, null);
						}
					}
				});
			} else {
				done.doneSet(null, null);
			}
		} else {
			super.applyPathMap(channel, map, done);
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
}
