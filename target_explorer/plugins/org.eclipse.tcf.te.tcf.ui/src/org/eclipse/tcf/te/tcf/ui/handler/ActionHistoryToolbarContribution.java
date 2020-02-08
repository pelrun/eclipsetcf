/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.handler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Event;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.persistence.history.HistoryManager;
import org.eclipse.tcf.te.runtime.persistence.utils.DataHelper;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IDelegateService;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepAttributes;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.ui.handler.images.ActionHistoryImageDescriptor;
import org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate;
import org.eclipse.tcf.te.ui.jface.images.AbstractImageDescriptor;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

public class ActionHistoryToolbarContribution extends CompoundContributionItem implements IWorkbenchContribution {

    IServiceLocator serviceLocator;

    @Override
    public void initialize(IServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @Override
    public void dispose() {
    	super.dispose();
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

	@Override
	protected IContributionItem[] getContributionItems() {
		List<IContributionItem> items = new ArrayList<IContributionItem>();
		final IPeerNode peerNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
		if (peerNode != null) {
			boolean enabled = peerNode.getConnectState() == IConnectable.STATE_CONNECTED;

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

			String[] entries = HistoryManager.getInstance().getHistory(IStepAttributes.PROP_LAST_RUN_HISTORY_ID + "@" + peerNode.getPeerId()); //$NON-NLS-1$
			if (entries != null && entries.length > 0) {
				int count = 0;
				for (final String entry : entries) {
					if (count >= 5) {
						break;
					}
					IPropertiesContainer decoded = DataHelper.decodePropertiesContainer(entry);
					String stepGroupId = decoded.getStringProperty(IStepAttributes.ATTR_STEP_GROUP_ID);
					if (stepGroupId != null && delegates.containsKey(stepGroupId)) {
						count++;
						final IDefaultContextToolbarDelegate delegate = delegates.get(stepGroupId);
						String label = "&" + count + " " + delegate.getLabel(peerNode, entry); //$NON-NLS-1$ //$NON-NLS-2$
						AbstractImageDescriptor descriptor = new ActionHistoryImageDescriptor(
										UIPlugin.getDefault().getImageRegistry(),
										delegate.getImage(peerNode, entry),
										delegate.validate(peerNode, entry));
						UIPlugin.getSharedImage(descriptor);
						ImageDescriptor imageDescriptor = UIPlugin.getImageDescriptor(descriptor.getDecriptorKey());
						if (count == 1) {
							IContributionItem item = new CommandContributionItem(
											new CommandContributionItemParameter(
															serviceLocator,
															null,
															"org.eclipse.tcf.te.tcf.ui.toolbar.command.historyLast", //$NON-NLS-1$
															null,
															imageDescriptor,
															null,
															null,
															label,
															null,
															null,
															0, null, false));
							items.add(item);
						}
						else {
							IAction action = new Action(label) {
								@Override
								public void runWithEvent(Event event) {
									delegate.execute(peerNode, entry, false);
								}
							};
							action.setImageDescriptor(imageDescriptor);
							action.setEnabled(enabled);
							IContributionItem item = new ActionContributionItem(action);
							items.add(item);
						}
					}
				}
			}
		}
		return items.toArray(new IContributionItem[items.size()]);
	}

}
