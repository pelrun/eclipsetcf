/*******************************************************************************
 * Copyright (c) 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.handler;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.ui.activator.UIPlugin;
import org.eclipse.ui.menus.ExtensionContributionFactory;
import org.eclipse.ui.menus.IContributionRoot;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.services.IServiceLocator;

public class DefaultContextActionsMenuContribution extends ExtensionContributionFactory {

	@SuppressWarnings("cast")
	@Override
	public void createContributionItems(IServiceLocator serviceLocator, IContributionRoot additions) {
		try {
			MenuManager menuMgr = new MenuManager();
			menuMgr.add(new Separator("group.connect")); //$NON-NLS-1$
			menuMgr.add(new Separator("group.launch")); //$NON-NLS-1$
			menuMgr.add(new Separator("group.launch.rundebug")); //$NON-NLS-1$
			menuMgr.add(new Separator("group.history")); //$NON-NLS-1$
			menuMgr.add(new Separator("group.additions")); //$NON-NLS-1$
			IMenuService service = (IMenuService) serviceLocator.getService(IMenuService.class);
			service.populateContributionManager(menuMgr, "menu:" + getLocation()); //$NON-NLS-1$
			for (IContributionItem item : menuMgr.getItems()) {
				additions.addContributionItem(item, null);
	            item.update();
            }
		}
		catch (Exception e) {
			if (Platform.inDebugMode()) {
				Platform.getLog(UIPlugin.getDefault().getBundle()).log(StatusHelper.getStatus(e));
			}
		}
	}
}
