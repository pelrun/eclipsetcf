/*******************************************************************************
 * Copyright (c) 2013, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.core.internal.services;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.cdt.debug.core.sourcelookup.MappingSourceContainer;
import org.eclipse.cdt.debug.internal.core.sourcelookup.MapEntrySourceContainer;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IPathMap;
import org.eclipse.tcf.services.IPathMap.PathMapRule;
import org.eclipse.tcf.te.runtime.callback.AsyncCallbackCollector;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.async.CallbackInvocationDelegate;
import org.eclipse.tcf.te.tcf.core.interfaces.IPathMapGeneratorService;
import org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService;
import org.eclipse.tcf.te.tcf.launch.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider;

/**
 * Path map service implementation.
 */
@SuppressWarnings("restriction")
public class PathMapService extends AbstractService implements IPathMapService {
	private final static String PATH_MAP_PROP_SHARED = "Shared"; //$NON-NLS-1$

	// Lock to handle multi thread access
	private final Lock lock = new ReentrantLock();

	// Contains a list of the shared Path Map rules for each context
	private final Map<String, List<IPathMap.PathMapRule>> sharedPathMapRules = new HashMap<String, List<IPathMap.PathMapRule>>();

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService#generateSourcePathMappings(java.lang.Object)
	 */
	@Override
	public void generateSourcePathMappings(Object context) {
    	Assert.isTrue(!Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(context);

		// Get the launch configuration for that peer model
		ILaunchConfiguration config = (ILaunchConfiguration) Platform.getAdapterManager().getAdapter(context, ILaunchConfiguration.class);
		if (config == null) {
			config = (ILaunchConfiguration) Platform.getAdapterManager().loadAdapter(context, "org.eclipse.debug.core.ILaunchConfiguration"); //$NON-NLS-1$
		}

		IPathMapGeneratorService generator = ServiceManager.getInstance().getService(context, IPathMapGeneratorService.class);

		if (config != null) {
			if (generator != null) {
				PathMapRule[] generatedRules = generator.getSourcePathMap(context);
				if (generatedRules != null) {
					MapEntrySourceContainer[] mappings = new MapEntrySourceContainer[generatedRules.length];
					int i = 0;
					for (PathMapRule pathMapRule : generatedRules) {
						// CDT 9.0 changes constructor of MapEntrySourceCounter
						MapEntrySourceContainer mapping = null;

						Class<MapEntrySourceContainer> clazz = MapEntrySourceContainer.class;
						try {
							Constructor<MapEntrySourceContainer> c = clazz.getConstructor(IPath.class, IPath.class);
							c.setAccessible(true);
							mapping = c.newInstance(new Path(pathMapRule.getSource()), new Path(pathMapRule.getDestination()));
						} catch (NoSuchMethodException e) {
							try {
								Constructor<MapEntrySourceContainer> c = clazz.getConstructor(String.class, IPath.class);
								c.setAccessible(true);
								mapping = c.newInstance(pathMapRule.getSource(), new Path(pathMapRule.getDestination()));
							}
							catch (Exception e2) { /* ignored on purpose */ }
						}
						catch (Exception e) { /* ignored on purpose */ }

						if (mapping != null) {
							mappings[i++] = mapping;
						}
                    }
					try {
						config = addSourceMappingToLaunchConfig(config, mappings);
					}
					catch (Exception e) {
					}
				}
			}
		}
	}

	private ILaunchConfiguration addSourceMappingToLaunchConfig(ILaunchConfiguration config, MapEntrySourceContainer[] mappings) throws CoreException {
		String memento = null;
		String type = null;

		ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
		memento = wc.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, (String) null);
		type = wc.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, (String) null);
		if (type == null) {
			type = wc.getType().getSourceLocatorId();
		}
		ISourceLocator locator = DebugPlugin.getDefault().getLaunchManager().newSourceLocator(type);
		if (locator instanceof AbstractSourceLookupDirector) {
			AbstractSourceLookupDirector director = (AbstractSourceLookupDirector) locator;
			if (memento == null) {
				director.initializeDefaults(wc);
			} else {
				director.initializeFromMemento(memento, wc);
			}

			ArrayList<ISourceContainer> containerList = new ArrayList<ISourceContainer>(Arrays.asList(director.getSourceContainers()));
			MappingSourceContainer generatedMappings = null;
			for (ISourceContainer container : containerList) {
				if (container instanceof MappingSourceContainer) {
					if (container.getName().equals(SOURCE_PATH_MAPPING_CONTAINER_NAME)) {
						generatedMappings = (MappingSourceContainer) container;
						break;
					}
				}
			}

			if (generatedMappings != null) {
				containerList.remove(generatedMappings);
			}
			generatedMappings = new MappingSourceContainer(SOURCE_PATH_MAPPING_CONTAINER_NAME);
			generatedMappings.init(director);
			containerList.add(generatedMappings);

			for (MapEntrySourceContainer mapping : mappings) {
				generatedMappings.addMapEntry(mapping);
	        }
			director.setSourceContainers(containerList.toArray(new ISourceContainer[containerList.size()]));
			wc.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, director.getMemento());
			wc.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, director.getId());
			return wc.doSave();
		}
		return config;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService#getPathMap(java.lang.Object)
	 */
    @Override
	public PathMapRule[] getPathMap(Object context) {
    	Assert.isTrue(!Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(context);

		PathMapRule[] rules = null;

		try {
			// Acquire the lock before accessing the path mappings
			lock.lock();

			generateSourcePathMappings(context);

			List<PathMapRule> rulesList = new ArrayList<PathMapRule>();

			// Get the launch configuration for that peer model
			ILaunchConfiguration config = (ILaunchConfiguration) Platform.getAdapterManager().getAdapter(context, ILaunchConfiguration.class);
			if (config == null) {
				config = (ILaunchConfiguration) Platform.getAdapterManager().loadAdapter(context, "org.eclipse.debug.core.ILaunchConfiguration"); //$NON-NLS-1$
			}

			if (config != null) {
				try {
					String path_map_cfg = config.getAttribute(org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.ATTR_PATH_MAP, ""); //$NON-NLS-1$
					rulesList.addAll(org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.parsePathMapAttribute(path_map_cfg));

					path_map_cfg = config.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, ""); //$NON-NLS-1$
					rulesList.addAll(org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.parseSourceLocatorMemento(path_map_cfg));
				} catch (CoreException e) { /* ignored on purpose */ }
			}

			IPathMapGeneratorService generator = ServiceManager.getInstance().getService(context, IPathMapGeneratorService.class);
			if (generator != null) {
				PathMapRule[] generatedRules = generator.getPathMap(context);
				if (generatedRules != null && generatedRules.length > 0) {
					rulesList.addAll(Arrays.asList(generatedRules));
				}
			}

			if (!rulesList.isEmpty()) {
				int cnt = 0;
				String id = getClientID();
				for (PathMapRule r : rulesList) {
					if (r.getProperties().get(IPathMap.PROP_ID) == null) {
						r.getProperties().put(IPathMap.PROP_ID, id + "/" + cnt++); //$NON-NLS-1$
					}
				}
				rules = rulesList.toArray(new PathMapRule[rulesList.size()]);
			}
		} finally {
			// Release the lock
			lock.unlock();
		}

		return rules;
	}

    /*
     * (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService#addSharedPathMapRules(java.lang.Object, org.eclipse.tcf.services.IPathMap.PathMapRule[])
     */
	@Override
	public void addSharedPathMapRules(Object context, PathMapRule[] rules) {
		Assert.isNotNull(context);
		Assert.isNotNull(rules);

		if (context instanceof IPeer) {
			List<IPathMap.PathMapRule> rulesToAdd = new ArrayList<IPathMap.PathMapRule>();
			for (PathMapRule rule:rules) {
				Map<String, Object> props = new LinkedHashMap<String, Object>();
				props.put(IPathMap.PROP_SOURCE, rule.getSource());
				props.put(IPathMap.PROP_DESTINATION, rule.getDestination());
				props.put(PATH_MAP_PROP_SHARED, rule.getProperties().get(PATH_MAP_PROP_SHARED));
				rulesToAdd.add(new org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.PathMapRule(props));
			}

			// Store new shared path map rules
			List<PathMapRule> currentSharedPMRules = sharedPathMapRules.get(((IPeer)context).getID());
			if (currentSharedPMRules == null) {
				currentSharedPMRules = new ArrayList<IPathMap.PathMapRule>();
			}
			currentSharedPMRules.addAll(rulesToAdd);
			sharedPathMapRules.put(((IPeer)context).getID(), currentSharedPMRules);
			addPathMap(context, rules);
		}
	}

    /*
     * (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService#addPathMap(java.lang.Object, org.eclipse.tcf.services.IPathMap.PathMapRule[])
     */
	@Override
	public void addPathMap(Object context, IPathMap.PathMapRule[] rules) {
		Assert.isTrue(!Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(context);
		Assert.isNotNull(rules);

		List<PathMapRule> rulesWithoutMatching = new ArrayList<PathMapRule>();

		try {
			// Acquire the lock before accessing the path mappings
			lock.lock();

			List<PathMapRule> rulesList = new ArrayList<PathMapRule>();

			// Get the launch configuration for that peer model
			ILaunchConfigurationWorkingCopy config = (ILaunchConfigurationWorkingCopy) Platform.getAdapterManager().getAdapter(context, ILaunchConfigurationWorkingCopy.class);
			if (config == null) {
				config = (ILaunchConfigurationWorkingCopy) Platform.getAdapterManager().loadAdapter(context, "org.eclipse.debug.core.ILaunchConfigurationWorkingCopy"); //$NON-NLS-1$
			}

			if (config != null) {
				populatePathMapRulesList(config, rulesList);

				// Find an existing path map rule for the given source and destination
				for (PathMapRule r:rules) {
					PathMapRule matchingRule = null;
					for (PathMapRule candidate : rulesList) {
						if (r.getSource().equals(candidate.getSource()) && r.getDestination().equals(candidate.getDestination())) {
							matchingRule = candidate;
							break;
						}
					}
					if (matchingRule == null) {
						rulesWithoutMatching.add(r);
					}
				}

				// Add new path map rules
				if (rulesWithoutMatching.size() > 0) {
					for (PathMapRule rule:rulesWithoutMatching) {
						Map<String, Object> props = new LinkedHashMap<String, Object>();
						props.put(IPathMap.PROP_SOURCE, rule.getSource());
						props.put(IPathMap.PROP_DESTINATION, rule.getDestination());
						props.put(PATH_MAP_PROP_SHARED, rule.getProperties().get(PATH_MAP_PROP_SHARED));
						rulesList.add(new org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.PathMapRule(props));
					}

					// Update the launch configuration
					updateLaunchConfiguration(config, rulesList);

					// Apply the path map
					applyPathMap(context, false, false, new Callback() {
						@Override
						protected void internalDone(Object caller, IStatus status) {
							if (status != null && !status.isOK() && Platform.inDebugMode()) {
								Platform.getLog(CoreBundleActivator.getContext().getBundle()).log(status);
							}
						}
					});
				}
			}
		} finally {
			// Release the lock
			lock.unlock();
		}
	}

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService#addPathMap(java.lang.Object, java.lang.String, java.lang.String)
     */
    @Override
    public PathMapRule addPathMap(Object context, String source, String destination) {
    	Map<String, Object> props = new LinkedHashMap<String, Object>();
		props.put(IPathMap.PROP_SOURCE, source);
		props.put(IPathMap.PROP_DESTINATION, destination);
		org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.PathMapRule rule = new org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.PathMapRule(props);

		addPathMap(context, new PathMapRule[] {rule});

		return rule;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService#removePathMap(java.lang.Object, org.eclipse.tcf.services.IPathMap.PathMapRule[])
     */
	@Override
	public void removePathMap(Object context, IPathMap.PathMapRule[] rules) {
		Assert.isTrue(!Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(context);
		Assert.isNotNull(rules);

		try {
			// Acquire the lock before accessing the path mappings
			lock.lock();

			List<PathMapRule> rulesList = new ArrayList<PathMapRule>();

			// Get the launch configuration for that peer model
			ILaunchConfigurationWorkingCopy config = (ILaunchConfigurationWorkingCopy) Platform.getAdapterManager().getAdapter(context, ILaunchConfigurationWorkingCopy.class);
			if (config == null) {
				config = (ILaunchConfigurationWorkingCopy) Platform.getAdapterManager().loadAdapter(context, "org.eclipse.debug.core.ILaunchConfigurationWorkingCopy"); //$NON-NLS-1$
			}

			if (config != null) {
				populatePathMapRulesList(config, rulesList);

				// If the original rule has an ID set, create a copy of the rule
				// but without the ID property
				List<PathMapRule> rulesToRemove = new ArrayList<PathMapRule>();
				for (PathMapRule rule:rules) {
					if (rule.getID() != null) {
						Map<String, Object> props = new HashMap<String, Object>(rule.getProperties());
						props.remove(IPathMap.PROP_ID);
						rule = new org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.PathMapRule(props);
					}
					rulesToRemove.add(rule);
				}

				// Remove the given rule from the list of present
				if (rulesList.removeAll(rulesToRemove)) {
					// Update the launch configuration
					updateLaunchConfiguration(config, rulesList);

					// Apply the path map
					applyPathMap(context, true, true, new Callback() {
						@Override
						protected void internalDone(Object caller, IStatus status) {
							if (status != null && !status.isOK() && Platform.inDebugMode()) {
								Platform.getLog(CoreBundleActivator.getContext().getBundle()).log(status);
							}
						}
					});
				}
			}
		} finally {
			// Release the lock
			lock.unlock();
		}
	}

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService#removePathMap(java.lang.Object, org.eclipse.tcf.services.IPathMap.PathMapRule)
     */
    @Override
    public void removePathMap(final Object context, final PathMapRule rule) {
    	removePathMap(context, new PathMapRule[]{rule});
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService#cleanSharedPathMapRules()
     */
	@Override
	public void cleanSharedPathMapRules(Object context) {
		Assert.isTrue(!Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(context);

		if (context instanceof IPeer) {
			List<PathMapRule> pathMapRulesToRemove = sharedPathMapRules.get(((IPeer)context).getID());
			if (pathMapRulesToRemove != null) {
				removePathMap(context, pathMapRulesToRemove.toArray(new IPathMap.PathMapRule[0]));
			}
			sharedPathMapRules.remove(((IPeer)context).getID());
		}
	}

    /**
     * Populate the given path map rules list from the given launch configuration.
     *
     * @param config The launch configuration. Must not be <code>null</code>.
     * @param rulesList The path map rules list. Must not be <code>null</code>.
     */
    private void populatePathMapRulesList(ILaunchConfiguration config, List<PathMapRule> rulesList) {
    	Assert.isNotNull(config);
    	Assert.isNotNull(rulesList);

		try {
			String path_map_cfg = config.getAttribute(org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.ATTR_PATH_MAP, ""); //$NON-NLS-1$
			String path_map_cfgV1 = config.getAttribute(org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.ATTR_PATH_MAP + "V1", ""); //$NON-NLS-1$ //$NON-NLS-2$

			rulesList.addAll(org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.parsePathMapAttribute(path_map_cfgV1));

	        int i = -1;
	        for (PathMapRule candidate : org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.parsePathMapAttribute(path_map_cfg)) {
	            if (rulesList.contains(candidate)) {
	                i = rulesList.indexOf(candidate);
	            } else {
	            	rulesList.add(++i, candidate);
	            }
	        }
		} catch (CoreException e) { /* ignored on purpose */ }
    }

    /**
     * Write back the given path map rules list to the given launch configuration.
     *
     * @param config The launch configuration. Must not be <code>null</code>.
     * @param rulesList The path map rules list. Must not be <code>null</code>.
     */
    private void updateLaunchConfiguration(ILaunchConfigurationWorkingCopy config, List<PathMapRule> rulesList) {
    	Assert.isNotNull(config);
    	Assert.isNotNull(rulesList);

		// Update the launch configuration
        for (PathMapRule candidate : rulesList) {
            candidate.getProperties().remove(IPathMap.PROP_ID);
        }

        StringBuilder bf = new StringBuilder();
        StringBuilder bf1 = new StringBuilder();

        for (PathMapRule candidate : rulesList) {
        	if (!(candidate instanceof org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.PathMapRule)) continue;

            boolean enabled = true;
            if (candidate.getProperties().containsKey("Enabled")) { //$NON-NLS-1$
                enabled = Boolean.parseBoolean(candidate.getProperties().get("Enabled").toString()); //$NON-NLS-1$
            }
            if (enabled) {
                candidate.getProperties().remove("Enabled"); //$NON-NLS-1$
                bf.append(candidate.toString());
            }
            bf1.append(candidate.toString());
        }

        if (bf.length() == 0) {
            config.removeAttribute(TCFLaunchDelegate.ATTR_PATH_MAP);
        } else {
            config.setAttribute(TCFLaunchDelegate.ATTR_PATH_MAP, bf.toString());
        }

        if (bf1.length() == 0) {
            config.removeAttribute(org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.ATTR_PATH_MAP + "V1"); //$NON-NLS-1$
        } else {
            config.setAttribute(org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.ATTR_PATH_MAP + "V1", bf1.toString()); //$NON-NLS-1$
        }

        try {
	        config.doSave();
        }
        catch (CoreException e) {
    		if (Platform.inDebugMode()) {
    			Platform.getLog(CoreBundleActivator.getContext().getBundle()).log(e.getStatus());
    		}
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService#applyPathMap(java.lang.Object, boolean, boolean, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
     */
    @Override
    public void applyPathMap(final Object context, final boolean force, final boolean forceEmpty, final ICallback callback) {
    	Assert.isTrue(!Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
    	Assert.isNotNull(context);
    	Assert.isNotNull(callback);

    	IPeer peer = context instanceof IPeer ? (IPeer)context : null;
    	if (peer == null && context instanceof IPeerNode) peer = ((IPeerNode)context).getPeer();
    	if (peer == null && context instanceof IPeerNodeProvider && ((IPeerNodeProvider)context).getPeerNode() != null) peer = ((IPeerNodeProvider)context).getPeerNode().getPeer();

    	// If called as part of the "open channel" step group, IChannelManager.getChannel(peer)
    	// will return null. For this case, the channel to use is passed as context directly.
    	final IChannel channel = context instanceof IChannel ? (IChannel)context : null;
    	// The peer in that case is the remote peer of the channel
    	if (peer == null && channel != null) {
    		final AtomicReference<IPeer> remotePeer = new AtomicReference<IPeer>();

    		Protocol.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					remotePeer.set(channel.getRemotePeer());
				}
			});

    		peer = remotePeer.get();
    	}

    	// Make sure that the callback is invoked in the TCF dispatch thread
    	final AsyncCallbackCollector collector = new AsyncCallbackCollector(callback, new CallbackInvocationDelegate());
    	final ICallback innerCallback = new AsyncCallbackCollector.SimpleCollectorCallback(collector);
    	collector.initDone();

    	if (peer != null) {
			final IChannel c = channel != null ? channel : Tcf.getChannelManager().getChannel(peer);
			if (c != null && IChannel.STATE_OPEN == c.getState()) {
				// Channel is open -> Have to update the path maps

				// Get the configured path mappings. This must be called from
				// outside the runnable as getPathMap(...) must be called from
				// outside of the TCF dispatch thread.
				final PathMapRule[] configuredMap = getPathMap(context instanceof IChannel ? peer : context);

				if (configuredMap != null && configuredMap.length > 0) {
					// Create the runnable which set the path map
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							final IPathMap svc = c.getRemoteService(IPathMap.class);
							if (svc != null) {
								// Get the old path maps first. Keep path map rules not coming from us
								svc.get(new IPathMap.DoneGet() {
									@Override
									public void doneGet(IToken token, Exception error, PathMapRule[] map) {
										// Merge the path maps
										List<PathMapRule> rules = mergePathMaps(getClientID(), map, configuredMap);

										// If the merged path map differs from the agent side path map, apply the map
										if (force || isDifferent(rules, map)) {
											// Apply the path map
											set(rules, svc, forceEmpty, new IPathMap.DoneSet() {
												@Override
												public void doneSet(IToken token, Exception error) {
													innerCallback.done(PathMapService.this, StatusHelper.getStatus(error));
												}
											});
										} else {
											innerCallback.done(PathMapService.this, Status.OK_STATUS);
										}
									}
								});
							} else {
								innerCallback.done(PathMapService.this, Status.OK_STATUS);
							}
						}
					};

					Protocol.invokeLater(runnable);
				} else {
					innerCallback.done(PathMapService.this, Status.OK_STATUS);
				}
			} else {
				innerCallback.done(PathMapService.this, Status.OK_STATUS);
			}
    	} else {
    		innerCallback.done(PathMapService.this, Status.OK_STATUS);
    	}
    }

    /**
     * Merge the given agent and client side path maps.
     *
     * @param clientID The current client ID. Must not be <code>null</code>.
     * @param agentSidePathMap The agent side path map or <code>null</code>.
     * @param clientSidePathMap The client side path map. Must not be <code>null</code>.
     *
     * @return The merged path map.
     */
    public static List<PathMapRule> mergePathMaps(String clientID, PathMapRule[] agentSidePathMap, PathMapRule[] clientSidePathMap) {
    	Assert.isNotNull(clientID);
    	Assert.isNotNull(clientSidePathMap);

		// Merge the maps to a new list
		List<PathMapRule> rules = new ArrayList<PathMapRule>();
		// The map of agent side path map rules
		List<PathMapRule> agentSideRules = new ArrayList<PathMapRule>();

		if (agentSidePathMap != null && agentSidePathMap.length > 0) {
			for (PathMapRule rule : agentSidePathMap) {
				if (rule.getID() == null || (!rule.getID().startsWith(clientID) && !"agent".equalsIgnoreCase(rule.getID()))) { //$NON-NLS-1$
					rules.add(rule);
				} else if ("agent".equalsIgnoreCase(rule.getID())) { //$NON-NLS-1$
					agentSideRules.add(rule);
				}
			}
		}

		for (PathMapRule rule : clientSidePathMap) {
			if (IPathMapService.PATHMAP_PROTOCOL_HOST_TO_TARGET.equals(rule.getProtocol())) continue;
			// If the configured rule matches an agent side path map rule, ignore the configured rule
			// and add the agent side rule
			boolean addRule = true;
			Map<String, Object> m1 = new HashMap<String, Object>(rule.getProperties());
			m1.remove(IPathMap.PROP_ID);
			for (PathMapRule agentSideRule : agentSideRules) {
				Map<String, Object> m2 = new HashMap<String, Object>(agentSideRule.getProperties());
				m2.remove(IPathMap.PROP_ID);
				if (m1.equals(m2)) {
					rules.add(agentSideRule);
					addRule = false;
					break;
				}
			}
			// Add the configured rule
			if (addRule) rules.add(rule);
		}

		return rules;
    }

    /**
     * Returns if or if not the given merged path map is different from the given agent
     * side path map.
     *
     * @param mergedPathMap The merged path map. Must not be <code>null</code>.
     * @param agentSidePathMap The agent side path map or <code>null</code>.
     *
     * @return <code>True</code> if the merged path map is different, <code>false</code> if not.
     */
    public static boolean isDifferent(List<PathMapRule> mergedPathMap, PathMapRule[] agentSidePathMap) {
    	Assert.isNotNull(mergedPathMap);

		boolean changed = agentSidePathMap != null ? agentSidePathMap.length != mergedPathMap.size() : !mergedPathMap.isEmpty();
		if (!changed && !mergedPathMap.isEmpty()) {
			// Make a copy of new map and remove all rules listed
			// by the old map. If not empty at the end, the new map
			// is different from the old map.
			List<PathMapRule> copy = new ArrayList<PathMapRule>(mergedPathMap);
			for (PathMapRule rule : agentSidePathMap) {
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

		return changed;
    }

    /**
     * Set the given path map.
     * <p>
     * <b>Note:</b> This method must be called from within the TCF dispatch thread.
     *
     * @param map The path map. Must not be <code>null</code>.
     * @param svc The path map service. Must not be <code>null</code>.
	 * @param forceEmpty If <code>true</code>, the path map will be set even if empty.
     * @param done The callback to invoke. Must not be <code>null</code>.
     */
    public static void set(List<PathMapRule> map, IPathMap svc, boolean forceEmpty, IPathMap.DoneSet done) {
    	Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
    	Assert.isNotNull(map);
    	Assert.isNotNull(svc);
    	Assert.isNotNull(done);

		// Get rid of the agent side rules before applying the rules for real
		Iterator<PathMapRule> iter = map.iterator();
		while (iter.hasNext()) {
			PathMapRule rule = iter.next();
			if ("agent".equalsIgnoreCase(rule.getID()) || Boolean.parseBoolean((String) rule.getProperties().get(PATH_MAP_PROP_SHARED))) { //$NON-NLS-1$
				iter.remove();
			}
		}
		// Apply the path map rules if not empty or forced
		if (!map.isEmpty() || forceEmpty) {
			svc.set(map.toArray(new PathMapRule[map.size()]), done);
		} else {
			done.doneSet(null, null);
		}
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService#getClientID()
     */
    @Override
    public String getClientID() {
        return org.eclipse.tcf.internal.debug.Activator.getClientID();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService#getSharedPathMapRules(java.lang.Object)
     */

	@Override
	public PathMapRule[] getSharedPathMapRules(Object context) {
		Assert.isNotNull(context);

		PathMapRule[] rules = null;
		try {
			// Acquire the lock before accessing the shared path mappings
			lock.lock();

			if (sharedPathMapRules != null ) {
				List<PathMapRule> sharedRules = sharedPathMapRules.get(((IPeer)context).getID());
				if (sharedRules != null && sharedRules.size() > 0) {
					rules = sharedRules.toArray(new IPathMap.PathMapRule[sharedRules.size()]);
				}
			}
		} finally {
			// Release the lock
			lock.unlock();
		}

		return rules;
	}
}
