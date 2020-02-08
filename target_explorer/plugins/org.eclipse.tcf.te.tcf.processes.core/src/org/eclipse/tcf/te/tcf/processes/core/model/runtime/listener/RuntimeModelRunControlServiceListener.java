/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.runtime.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.services.IRunControl.RunControlContext;
import org.eclipse.tcf.services.IRunControl.RunControlListener;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelLookupService;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelRefreshService;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;

/**
 * Run control service runtime model service listener implementation.
 */
public class RuntimeModelRunControlServiceListener implements RunControlListener {
	// Reference to the parent model
	private final IRuntimeModel model;

	/**
     * Constructor.
     */
    public RuntimeModelRunControlServiceListener(IRuntimeModel model) {
    	Assert.isNotNull(model);
    	this.model = model;
    }

	/**
	 * Returns the parent runtime model.
	 *
	 * @return The parent runtime model.
	 */
	public final IRuntimeModel getModel() {
		return model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IRunControl.RunControlListener#contextAdded(org.eclipse.tcf.services.IRunControl.RunControlContext[])
	 */
	@Override
	public void contextAdded(RunControlContext[] contexts) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IRunControl.RunControlListener#contextChanged(org.eclipse.tcf.services.IRunControl.RunControlContext[])
	 */
	@Override
	public void contextChanged(RunControlContext[] contexts) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IRunControl.RunControlListener#contextRemoved(java.lang.String[])
	 */
	@Override
	public void contextRemoved(String[] context_ids) {
		// If a run control context is removed from run control (detach),
		// and the context is known to our model, refresh the context as
		// we do not get events from the process service if the attach state changed.
		if (context_ids != null && context_ids.length > 0) {
			IModelLookupService lkupService = model.getService(IModelLookupService.class);
			IModelRefreshService refreshService = model.getService(IModelRefreshService.class);

			// If we get a context ID like "P2.274154640", use the first part of the ID to
			// refresh the parent context.
			List<String> parentContextIDs = new ArrayList<String>();

			for (String contextID : context_ids) {
				if (contextID == null || "".equals(contextID.trim())) continue; //$NON-NLS-1$

				IModelNode[] candidates = lkupService.lkupModelNodesById(contextID);
				if (candidates != null && candidates.length > 0) {
					for (IModelNode node : candidates) {
						refreshService.refresh(node, null);
					}
				}

				String[] parts = contextID.split("\\."); //$NON-NLS-1$
				if (parts.length > 1) {
					// Look for candidates for all parts. If found, than add the ID
					// to the parent list
					for (String partID : parts) {
						candidates = lkupService.lkupModelNodesById(contextID);
						if (candidates != null && candidates.length > 0) {
							if (!parentContextIDs.contains(partID)) {
								parentContextIDs.add(partID);
							}
						}
					}
				}
			}

			// Refresh all determined parent contexts
			for (String parentContextID : parentContextIDs) {
				IModelNode[] candidates = lkupService.lkupModelNodesById(parentContextID);
				if (candidates != null && candidates.length > 0) {
					for (IModelNode node : candidates) {
						refreshService.refresh(node, null);
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IRunControl.RunControlListener#contextSuspended(java.lang.String, java.lang.String, java.lang.String, java.util.Map)
	 */
	@Override
	public void contextSuspended(String context, String pc, String reason, Map<String, Object> params) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IRunControl.RunControlListener#contextResumed(java.lang.String)
	 */
	@Override
	public void contextResumed(String context) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IRunControl.RunControlListener#containerSuspended(java.lang.String, java.lang.String, java.lang.String, java.util.Map, java.lang.String[])
	 */
	@Override
	public void containerSuspended(String context, String pc, String reason, Map<String, Object> params, String[] suspended_ids) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IRunControl.RunControlListener#containerResumed(java.lang.String[])
	 */
	@Override
	public void containerResumed(String[] context_ids) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IRunControl.RunControlListener#contextException(java.lang.String, java.lang.String)
	 */
	@Override
	public void contextException(String context, String msg) {
	}

}
