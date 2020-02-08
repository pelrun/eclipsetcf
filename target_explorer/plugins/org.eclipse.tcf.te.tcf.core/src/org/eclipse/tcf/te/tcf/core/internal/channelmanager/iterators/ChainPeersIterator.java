/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.core.internal.channelmanager.iterators;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepAttributes;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.core.interfaces.steps.ITcfStepAttributes;
import org.eclipse.tcf.te.tcf.core.iterators.AbstractPeerStepGroupIterator;
import org.eclipse.tcf.te.tcf.core.util.persistence.PeerDataHelper;
import org.eclipse.tcf.te.tcf.core.va.ValueAddManager;
import org.eclipse.tcf.te.tcf.core.va.interfaces.IValueAdd;

/**
 * ChainPeersIterator
 */
public class ChainPeersIterator extends AbstractPeerStepGroupIterator {

	/* default */ final List<IPeer> peers = new ArrayList<IPeer>();

	/**
	 * Constructor.
	 */
	public ChainPeersIterator() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.iterators.AbstractStepGroupIterator#initialize(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void initialize(IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
	    super.initialize(context, data, fullQualifiedId, monitor);

	    final IPeer peer = getActivePeerContext(context, data, fullQualifiedId);
	    final String peerId = peer.getID();
		final boolean useValueAdds = !StepperAttributeUtil.getBooleanProperty(IChannelManager.FLAG_NO_VALUE_ADD, fullQualifiedId, data);

	    peers.clear();

	    Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if (useValueAdds) {
					IValueAdd[] valueAdds = ValueAddManager.getInstance().getValueAdd(peer);
					for (IValueAdd valueAdd : valueAdds) {
						IPeer valueAddPeer = valueAdd.getPeer(peerId);
						if (valueAddPeer != null) {
							peers.add(valueAddPeer);
						}
					}
				}

				String proxyConfiguration = peer.getAttributes().get(IPeerProperties.PROP_PROXIES);
				if (proxyConfiguration != null) {
					for (IPeer proxy : PeerDataHelper.decodePeerList(proxyConfiguration)) {
				        peers.add(proxy);
			        }
				}

				if (!StepperAttributeUtil.getBooleanProperty(ITcfStepAttributes.ATTR_SKIP_PEER_TO_CHAIN, fullQualifiedId, data)) {
					peers.add(peer);
				}
			}
		};

		if (Protocol.isDispatchThread()) runnable.run();
		else Protocol.invokeAndWait(runnable);

	    setIterations(peers.size());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.iterators.AbstractStepGroupIterator#internalNext(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void internalNext(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		StepperAttributeUtil.setProperty(IStepAttributes.ATTR_ACTIVE_CONTEXT, fullQualifiedId, data, peers.get(getIteration()));
	}
}
