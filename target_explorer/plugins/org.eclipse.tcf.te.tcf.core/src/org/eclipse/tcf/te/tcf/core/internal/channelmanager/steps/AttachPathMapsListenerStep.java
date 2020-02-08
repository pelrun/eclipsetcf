/*******************************************************************************
 * Copyright (c) 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.core.internal.channelmanager.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IPathMap;
import org.eclipse.tcf.services.IPathMap.PathMapRule;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.tcf.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService;
import org.eclipse.tcf.te.tcf.core.interfaces.steps.ITcfStepAttributes;
import org.eclipse.tcf.te.tcf.core.steps.AbstractPeerStep;

public class AttachPathMapsListenerStep extends AbstractPeerStep {
	public String PATH_MAP_PROP_SHARED = "Shared"; //$NON-NLS-1$

	@Override
	public void validateExecute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(context);
		Assert.isNotNull(data);
		Assert.isNotNull(fullQualifiedId);
		Assert.isNotNull(monitor);

		IChannel channel = (IChannel)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data);
		if (channel == null || channel.getState() != IChannel.STATE_OPEN) {
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "Channel to target not available or closed.")); //$NON-NLS-1$
		}
	}

	@Override
	public void execute(final IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, IProgressMonitor monitor, final ICallback callback) {
		Assert.isNotNull(context);
		Assert.isNotNull(data);
		Assert.isNotNull(fullQualifiedId);
		Assert.isNotNull(monitor);
		Assert.isNotNull(callback);

		final IChannel channel = (IChannel)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data);
		Assert.isNotNull(channel);
		final IPeer peer = getActivePeerContext(context, data, fullQualifiedId);
		Assert.isNotNull(peer);

		if (IChannel.STATE_OPEN == channel.getState()) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					final IPathMap svc = channel.getRemoteService(IPathMap.class);
					if (svc != null) {
						svc.addListener(new IPathMap.PathMapListener() {
							@Override
							public void changed() {
								svc.get(new IPathMap.DoneGet() {
									@Override
									public void doneGet(IToken token, Exception error, final PathMapRule[] map) {
										Thread th = new Thread(new Runnable() {
											@Override
											public void run() {
												final IPathMapService service = ServiceManager.getInstance().getService(peer, IPathMapService.class);
												if (service != null) {
													// Update shared path map rules comparing existing and new ones
													PathMapRule[] existingRulesArray = service.getSharedPathMapRules(peer);
													List<PathMapRule> existingRules;
													if (existingRulesArray != null) {
														existingRules = Arrays.asList(existingRulesArray);
													} else {
														existingRules = new ArrayList<IPathMap.PathMapRule>();
													}
													List<PathMapRule> newRules;
													if (map != null) {
														newRules = Arrays.asList(map);
													} else {
														newRules = new ArrayList<IPathMap.PathMapRule>();
													}
													List<PathMapRule> diffRules = new ArrayList<IPathMap.PathMapRule>();

													// Remove old shared path maps
													for (PathMapRule rule:existingRules) {
														if (!existsPathMapRuleInList(rule, newRules) &&
																!diffRules.contains(rule)) {
															diffRules.add(rule);
														}
													}
													if (diffRules.size() > 0) {
														service.removePathMap(peer, diffRules.toArray(new IPathMap.PathMapRule[0]));
														diffRules.clear();
													}

													// Add new shared path maps
													for (PathMapRule rule:newRules) {
														if (Boolean.parseBoolean((String)rule.getProperties().get(PATH_MAP_PROP_SHARED )) &&
																!existsPathMapRuleInList(rule, existingRules) &&
																!diffRules.contains(rule)) {
															diffRules.add(rule);
														}
													}
													if (diffRules.size() > 0) {
														service.addSharedPathMapRules(peer, diffRules.toArray(new IPathMap.PathMapRule[0]));
														diffRules.clear();
													}
												}
											}
										});
										th.start();
									}
								});

							}
						});
						callback(data, fullQualifiedId, callback, Status.OK_STATUS, null);
					}
				}
			};
			Protocol.invokeLater(runnable);
		}
	}

	protected boolean existsPathMapRuleInList(PathMapRule rule, List<PathMapRule> rules) {
		for (PathMapRule r:rules) {
			if (r.getSource().equals(rule.getSource()) &&
						r.getDestination().equals(rule.getDestination())) {
				return true;
			}
		}
		return false;
	}
}
