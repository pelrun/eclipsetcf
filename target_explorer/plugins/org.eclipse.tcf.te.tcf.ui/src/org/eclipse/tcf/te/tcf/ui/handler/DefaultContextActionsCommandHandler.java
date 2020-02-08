/*******************************************************************************
 * Copyright (c) 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.ContributionManager;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.QuickMenuCreator;
import org.eclipse.ui.menus.IMenuService;

public class DefaultContextActionsCommandHandler extends AbstractHandler {

	protected static final String DEFAULT_CONTEXT_ACTIONS_URI = "org.eclipse.tcf.te.tcf.ui.DefaultContextActionsToolbarContribution"; //$NON-NLS-1$

	protected Point menuLocation = null;

	private final QuickMenuCreator fMenuCreator = new QuickMenuCreator() {
		@SuppressWarnings("cast")
		@Override
		protected void fillMenu(IMenuManager menu) {
			if (!(menu instanceof ContributionManager)) {
				return;
			}
			IMenuService service = (IMenuService) PlatformUI.getWorkbench()
					.getService(IMenuService.class);

			menu.add(new Separator("group.connect")); //$NON-NLS-1$
			menu.add(new Separator("group.launch")); //$NON-NLS-1$
			menu.add(new Separator("group.launch.rundebug")); //$NON-NLS-1$
			menu.add(new Separator("group.history")); //$NON-NLS-1$
			menu.add(new Separator("group.additions")); //$NON-NLS-1$
			service.populateContributionManager((ContributionManager) menu, "menu:" + DEFAULT_CONTEXT_ACTIONS_URI); //$NON-NLS-1$
			for (IContributionItem item : menu.getItems()) {
	            item.update();
            }
		}

		@Override
		protected Point computeMenuLocation(Control focus) {
			if (menuLocation != null) {
				return menuLocation;
			}
			return super.computeMenuLocation(focus);
		}
	};

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Get the position of the ToolItem
		if (event.getTrigger() instanceof Event) {
			Event eventTrigger = (Event) event.getTrigger();
			if (eventTrigger.widget instanceof ToolItem) {
				ToolItem toolItem = (ToolItem) eventTrigger.widget;
				Rectangle bounds = toolItem.getBounds();
				if ( bounds != null ) {
					menuLocation = toolItem.getParent().toDisplay(bounds.x, bounds.y + bounds.height);
				}
			}
		}

		fMenuCreator.createMenu();
		return null;
	}

}
