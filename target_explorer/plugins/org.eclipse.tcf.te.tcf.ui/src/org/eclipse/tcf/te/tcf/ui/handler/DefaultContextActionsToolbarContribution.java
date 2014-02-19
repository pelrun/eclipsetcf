/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.ui.handler;

import java.util.EventObject;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.ui.internal.ImageConsts;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.views.handler.ShowInSystemManagementHandler;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.eclipse.ui.services.IServiceLocator;

/**
 * DefaultContextActionsToolbarContribution
 */
public class DefaultContextActionsToolbarContribution extends WorkbenchWindowControlContribution
implements IWorkbenchContribution, IEventListener {

	ToolBar toolbar = null;
	ToolItem item = null;

	IServiceLocator serviceLocator = null;

	private MenuManager menuMgr = null;
	private Menu menu = null;

	private boolean clickRunning = false;

	/**
	 * Constructor.
	 */
	public DefaultContextActionsToolbarContribution() {
		this("org.eclipse.tcf.te.tcf.ui.DefaultContextActionsToolbarContribution"); //$NON-NLS-1$
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 */
	public DefaultContextActionsToolbarContribution(String id) {
		super(id);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.menus.IWorkbenchContribution#initialize(org.eclipse.ui.services.IServiceLocator)
	 */
	@Override
	public void initialize(IServiceLocator serviceLocator) {
		this.serviceLocator = serviceLocator;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.ContributionItem#isDynamic()
	 */
	@Override
	public boolean isDynamic() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.ControlContribution#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createControl(Composite parent) {
		toolbar = new ToolBar(parent, SWT.FLAT);
		item = new ToolItem(toolbar, SWT.DROP_DOWN);
		item.setImage(UIPlugin.getImage(ImageConsts.SYSTEM_MGNT_VIEW));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IPeerNode peerNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
				if (peerNode != null && (e.stateMask & SWT.MODIFIER_MASK) != 0 && (e.stateMask & SWT.CTRL) == SWT.CTRL && (e.stateMask & SWT.SHIFT) == SWT.SHIFT) {
					ShowInSystemManagementHandler.setAndCheckSelection(IUIConstants.ID_EXPLORER, peerNode);
					return;
				}
				onButtonClick();
			}
		});


		EventManager.getInstance().addEventListener(this, ChangeEvent.class);

		update();

		return toolbar;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.ContributionItem#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();

		EventManager.getInstance().removeEventListener(this);

		item.dispose();
		toolbar.dispose();

		if (menu != null && !menu.isDisposed()) {
			menu.setVisible(false);
			menu.dispose();
			menu = null;
		}
		if (menuMgr != null) {
			menuMgr.dispose();
			menuMgr = null;
		}
	}

	protected void onButtonClick() {
		if (!clickRunning) {
			clickRunning = true;
			update();
			createContextMenu(toolbar);
			if (menu != null) {
				Point point = toolbar.toDisplay(toolbar.getLocation());
				menu.setLocation(point.x, point.y + toolbar.getBounds().height);
				menu.setVisible(true);
			}
			clickRunning = false;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.ContributionItem#update()
	 */
	@Override
	public void update() {
		if (menu != null && !menu.isDisposed()) {
			menu.setVisible(false);
			menu.dispose();
			menu = null;
		}
		if (menuMgr != null) {
			menuMgr.dispose();
		}

		if (item != null && !item.isDisposed()) {
			IPeerNode peerNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
//			item.setEnabled(peerNode != null && peerNode.getConnectState() == IConnectable.STATE_CONNECTED);

			if (peerNode == null) {
				item.setToolTipText(Messages.DefaultContextActionsToolbarContribution_tooltip_button_noContext);
				item.setEnabled(false);
			}
			else {
				item.setToolTipText(Messages.DefaultContextActionsToolbarContribution_tooltip_button);
				item.setEnabled(true);
			}
//			else if (item.isEnabled()) {
//				item.setToolTipText(Messages.DefaultContextActionsToolbarContribution_tooltip_button);
//			}
//			else {
//				item.setToolTipText(Messages.DefaultContextActionsToolbarContribution_tooltip_button_disabled);
//			}
		}
	}

	protected void createContextMenu(Composite panel) {
		try {
			menuMgr = new MenuManager();
			menuMgr.add(new Separator("group.connect")); //$NON-NLS-1$
			menuMgr.add(new Separator("group.launch")); //$NON-NLS-1$
			menuMgr.add(new Separator("group.launch.rundebug")); //$NON-NLS-1$
			menuMgr.add(new Separator("group.additions")); //$NON-NLS-1$
			final IMenuService service = (IMenuService) serviceLocator.getService(IMenuService.class);
			service.populateContributionManager(menuMgr, "menu:" + getId()); //$NON-NLS-1$
			menu = menuMgr.createContextMenu(panel);
		}
		catch (Exception e) {
			if (Platform.inDebugMode()) {
				Platform.getLog(UIPlugin.getDefault().getBundle()).log(StatusHelper.getStatus(e));
			}
			menuMgr = null;
			menu = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.events.IEventListener#eventFired(java.util.EventObject)
	 */
	@Override
	public void eventFired(EventObject event) {
		if (event instanceof ChangeEvent) {
			ChangeEvent changeEvent = (ChangeEvent)event;
			IPeerNode peerNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
			if (changeEvent.getSource() instanceof IDefaultContextService ||
				(changeEvent.getSource() == peerNode &&
				(IPeerNodeProperties.PROP_CONNECT_STATE.equals(changeEvent.getEventId()) || "properties".equals(changeEvent.getEventId())))) { //$NON-NLS-1$
				ExecutorsUtil.executeInUI(new Runnable() {
					@Override
					public void run() {
						update();
					}
				});
			}
		}
	}
}
