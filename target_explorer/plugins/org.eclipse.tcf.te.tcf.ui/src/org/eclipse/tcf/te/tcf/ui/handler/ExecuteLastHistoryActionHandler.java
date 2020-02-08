/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.ui.handler;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.persistence.history.HistoryManager;
import org.eclipse.tcf.te.runtime.persistence.utils.DataHelper;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IDelegateService;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepAttributes;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate;
import org.eclipse.tcf.te.ui.views.handler.OpenEditorHandler;

/**
 * ExecuteLastHistoryActionHandler
 */
public class ExecuteLastHistoryActionHandler extends OpenEditorHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.handler.OpenEditorHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IPeerNode peerNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);

		IService[] services = ServiceManager.getInstance().getServices(peerNode, IDelegateService.class, false);
		Map<String, IDefaultContextToolbarDelegate> delegates = new LinkedHashMap<String, IDefaultContextToolbarDelegate>();
		for (IService service : services) {
	        if (service instanceof IDelegateService) {
	        	IDefaultContextToolbarDelegate delegate = ((IDelegateService)service).getDelegate(peerNode, IDefaultContextToolbarDelegate.class);
	        	if (delegate != null) {
        			for (String stepGroupId : delegate.getHandledStepGroupIds(peerNode)) {
        				if (!delegates.containsKey(stepGroupId)) {
        					delegates.put(stepGroupId, delegate);
        				}
                    }
	        	}
	        }
        }

		String entry = HistoryManager.getInstance().getFirst(IStepAttributes.PROP_LAST_RUN_HISTORY_ID + "@" + peerNode.getPeerId()); //$NON-NLS-1$
		if (entry != null) {
			IPropertiesContainer decoded = DataHelper.decodePropertiesContainer(entry);
			String stepGroupId = decoded.getStringProperty(IStepAttributes.ATTR_STEP_GROUP_ID);
			if (stepGroupId != null && delegates.containsKey(stepGroupId)) {
				IDefaultContextToolbarDelegate delegate = delegates.get(stepGroupId);
				delegate.execute(peerNode, entry, false);
			}
		}
	    return null;
	}
}
