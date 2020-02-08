/*******************************************************************************
 * Copyright (c) 2014, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.processes.core.steps;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.iterators.AbstractPeerNodeStepGroupIterator;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.IProcessContextItem;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.IProcessesDataProperties;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.steps.IProcessesStepAttributes;
import org.eclipse.tcf.te.tcf.processes.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModelRefreshService;
import org.eclipse.tcf.te.tcf.processes.core.util.ProcessDataHelper;

/**
 * Step group iterator for attach contexts.
 */
public class AttachContextsIterator extends AbstractPeerNodeStepGroupIterator {

	final List<IProcessContextNode> items = new ArrayList<IProcessContextNode>();

	/**
	 * Constructor.
	 */
	public AttachContextsIterator() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupIterator#initialize(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void initialize(IStepContext context, final IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		super.initialize(context, data, fullQualifiedId, monitor);
		final IPeerNode peerNode = getActivePeerModelContext(context, data, fullQualifiedId);

		final Callback cb = new Callback();
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				IRuntimeModelRefreshService service = ModelManager.getRuntimeModel(peerNode).getService(IRuntimeModelRefreshService.class);
				service.refresh(cb, -1);
			}
		});
		ExecutorsUtil.waitAndExecute(0, cb.getDoneConditionTester(monitor));

		for (IProcessContextItem item : ProcessDataHelper.decodeProcessContextItems(data.getStringProperty(IProcessesDataProperties.PROPERTY_CONTEXT_LIST))) {
			for (IProcessContextNode node : ProcessDataHelper.getProcessContextNodes(peerNode, item)) {
   				items.add(node);
			}
		}
		setIterations(items.size());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.iterators.AbstractStepGroupIterator#internalNext(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void internalNext(IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		final IProcessContextNode item = items.get(getIteration());
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				StepperAttributeUtil.setProperty(IProcessesStepAttributes.ATTR_PROCESS_CONTEXT_NODE, fullQualifiedId, data, item);
				StepperAttributeUtil.setProperty(IProcessesStepAttributes.ATTR_PROCESS_CONTEXT, fullQualifiedId, data, item.getProcessContext());
			}
		});
	}
}
