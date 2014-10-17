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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.ui.internal.ImageConsts;
import org.eclipse.tcf.te.tcf.ui.internal.preferences.IPreferenceKeys;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.tcf.te.ui.views.ViewsUtil;
import org.eclipse.tcf.te.ui.views.handler.OpenEditorHandler;
import org.eclipse.tcf.te.ui.views.navigator.DelegatingLabelProvider;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.eclipse.ui.services.IServiceLocator;

/**
 * Configurations control implementation.
 */
public class DefaultContextSelectorToolbarContribution extends WorkbenchWindowControlContribution
implements IWorkbenchContribution, IEventListener, IPeerModelListener {

	private Composite panel = null;
	private Label image = null;
	private Label text = null;
	private Button button = null;

	IServiceLocator serviceLocator = null;

	private MenuManager menuMgr = null;
	private Menu menu = null;

	private boolean clickRunning = false;

	/**
	 * Constructor.
	 */
	public DefaultContextSelectorToolbarContribution() {
		this("org.eclipse.tcf.te.tcf.ui.DefaultContextSelectorToolbarContribution"); //$NON-NLS-1$
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 */
	public DefaultContextSelectorToolbarContribution(String id) {
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
	 * @see org.eclipse.jface.action.ControlContribution#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createControl(final Composite parent) {
		panel = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 1; layout.marginWidth = 1;
		panel.setLayout(layout);

		Composite labelPanel = new Composite(panel, SWT.BORDER);
		labelPanel.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		labelPanel.setLayoutData(layoutData);
		layout = new GridLayout(3, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		labelPanel.setLayout(layout);

		image = new Label(labelPanel, SWT.NONE);
		layoutData = new GridData(SWT.LEAD, SWT.CENTER, false, true);
		layoutData.horizontalIndent = 1;
		layoutData.minimumWidth=20;
		layoutData.widthHint=20;
		image.setLayoutData(layoutData);
		image.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
			}
			@Override
			public void mouseUp(MouseEvent e) {
				onButtonClick();
			}
		});
		text = new Label(labelPanel, SWT.NONE);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, true);
		layoutData.minimumWidth = SWTControlUtil.convertWidthInCharsToPixels(text, 25);
		text.setLayoutData(layoutData);
		text.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
			}
			@Override
			public void mouseUp(MouseEvent e) {
				IPeerNode peerNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
				if (peerNode != null && (e.stateMask & SWT.MODIFIER_MASK) != 0 && (e.stateMask & SWT.CTRL) == SWT.CTRL && (e.stateMask & SWT.SHIFT) == SWT.SHIFT) {
					OpenEditorHandler.openEditorOnSelection(getWorkbenchWindow(), new StructuredSelection(peerNode));
					return;
				}
				onButtonClick();
			}
		});

		button = new Button(labelPanel, SWT.ARROW | SWT.DOWN | SWT.FLAT | SWT.NO_FOCUS);
		layoutData = new GridData(SWT.TRAIL, SWT.CENTER, false, true);
		layoutData.minimumWidth=20;
		layoutData.widthHint = 20;
		button.setLayoutData(layoutData);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onButtonClick();
			}
		});

	    EventManager.getInstance().addEventListener(this, ChangeEvent.class);
	    ModelManager.getPeerModel().addListener(this);

	    update();

		return panel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.ContributionItem#dispose()
	 */
	@Override
	public void dispose() {
	    super.dispose();

	    EventManager.getInstance().removeEventListener(this);
	    ModelManager.getPeerModel().removeListener(this);

	    image.dispose();
	    text.dispose();
	    if (menuMgr != null) menuMgr.dispose();

	    image = null;
	    text = null;
	}

	private IPeerNode[] getPeerNodesSorted() {
		IPeerNode[] peerNodes = ModelManager.getPeerModel().getPeerNodes();
		List<IPeerNode> visiblePeerNodes = new ArrayList<IPeerNode>();
		for (IPeerNode peerNode : peerNodes) {
			if (peerNode.isVisible()) {
				visiblePeerNodes.add(peerNode);
			}
        }

		Collections.sort(visiblePeerNodes, new Comparator<IPeerNode>() {
			@Override
			public int compare(IPeerNode o1, IPeerNode o2) {
				String type1 = o1.getPeerType();
				type1 = type1 != null ? type1 : ""; //$NON-NLS-1$
				String type2 = o2.getPeerType();
				type2 = type2 != null ? type2 : ""; //$NON-NLS-1$
				int typeCompare = type1.compareTo(type2);
			    return typeCompare != 0 ? typeCompare : o1.getName().toUpperCase().compareTo(o2.getName().toUpperCase());
			}
		});

		return visiblePeerNodes.toArray(new IPeerNode[visiblePeerNodes.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.ContributionItem#update()
	 */
	@Override
	public void update() {
		if (menuMgr != null) menuMgr.markDirty();
		if (image != null && text != null) {
			IPeerNode peerNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
			if (peerNode == null) {
				IPeerNode[] peerNodes = getPeerNodesSorted();
				if (peerNodes != null && peerNodes.length > 0) {
					peerNode = peerNodes[0];
					ServiceManager.getInstance().getService(IDefaultContextService.class).setDefaultContext(peerNode);
					return;
				}
			}

			if (peerNode != null) {
			    DelegatingLabelProvider labelProvider = new DelegatingLabelProvider();
				image.setImage(labelProvider.decorateImage(labelProvider.getImage(peerNode), peerNode));
				String fullName = labelProvider.getText(peerNode);
				String name = fullName;
				if (name.length() > 22 && name.length() >= 25) {
					name = name.substring(0, 22) + "..."; //$NON-NLS-1$
				}
				text.setText(name);

				image.setToolTipText(!fullName.equals(name) ? fullName : Messages.DefaultContextSelectorToolbarContribution_tooltip_button);
				text.setToolTipText(!fullName.equals(name) ? fullName : Messages.DefaultContextSelectorToolbarContribution_tooltip_button);
				button.setToolTipText(Messages.DefaultContextSelectorToolbarContribution_tooltip_button);
			}
			else {
				image.setImage(UIPlugin.getImage(ImageConsts.NEW_CONFIG));
				text.setText(Messages.DefaultContextSelectorToolbarContribution_label_new);

				image.setToolTipText(Messages.DefaultContextSelectorToolbarContribution_tooltip_new);
				text.setToolTipText(Messages.DefaultContextSelectorToolbarContribution_tooltip_new);
				button.setToolTipText(Messages.DefaultContextSelectorToolbarContribution_tooltip_new);
			}
		}
	}

	protected void onButtonClick() {
		if (!clickRunning) {
			clickRunning = true;
			IPeerNode peerNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
			if (peerNode == null) {
				openNewWizard();
			}
			else {
				createContextMenu(panel);
				if (menu != null) {
					Point point = panel.toDisplay(panel.getLocation());
					menu.setLocation(point.x, point.y + panel.getBounds().height);
					menu.setVisible(true);
				}
			}
			clickRunning = false;
		}
	}

    protected void openNewWizard() {
    	try {
    		new NewToolbarWizardHandler().execute(new ExecutionEvent());
    	}
    	catch (Exception e) {
    	}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.ContributionItem#isDynamic()
	 */
	@Override
	public boolean isDynamic() {
	    return true;
	}

	protected void createContextMenu(Composite panel) {
		if (menu == null || menuMgr == null || menuMgr.isDirty()) {
			try {
				if (menuMgr != null) menuMgr.dispose();
				menuMgr = new MenuManager();
				menuMgr.add(new GroupMarker("group.configurations")); //$NON-NLS-1$
	    		IPeerNode defaultContext = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
	    		if (defaultContext != null && defaultContext.isVisible()) {
					menuMgr.add(getAction(defaultContext));
	    		}
			    for (final IPeerNode peerNode : getPeerNodesSorted()) {
			    	if (peerNode == defaultContext || !peerNode.isVisible()) {
			    		continue;
			    	}
					menuMgr.add(getAction(peerNode));
			    }
			    menuMgr.add(new Separator("group.open")); //$NON-NLS-1$
			    menuMgr.add(new GroupMarker("group.delete")); //$NON-NLS-1$
			    menuMgr.add(new GroupMarker("group.new")); //$NON-NLS-1$
				menuMgr.add(new Separator("group.additions")); //$NON-NLS-1$
				final IMenuService service = (IMenuService)serviceLocator.getService(IMenuService.class);
				service.populateContributionManager(menuMgr, "menu:" + getId()); //$NON-NLS-1$

				if (menu != null && !menu.isDisposed()) {
					menu.setVisible(false);
					menu.dispose();
				}
				menu = menuMgr.createContextMenu(panel);
			}
			catch (Exception e) {
				menuMgr = null;
				menu = null;
			}
		}
	}

	protected IAction getAction(final IPeerNode peerNode) {
		IAction action = new Action() {
			private IPeerNode node = peerNode;
			@Override
            public void run() {
				ServiceManager.getInstance().getService(IDefaultContextService.class).setDefaultContext(node);
			}
		};
	    DelegatingLabelProvider labelProvider = new DelegatingLabelProvider();
		action.setText(labelProvider.getText(peerNode));
		Image image = labelProvider.decorateImage(labelProvider.getImage(peerNode), peerNode);
		action.setImageDescriptor(ImageDescriptor.createFromImage(image));

		return action;
	}

	/**
	 * Get the label provider for a peer model node.
	 *
	 * @param peerNode The peer model node.
	 * @return The label provider or <code>null</code>.
	 */
	protected ILabelProvider getLabelProvider(IPeerNode peerNode) {
		ILabelProvider labelProvider = (ILabelProvider)peerNode.getAdapter(ILabelProvider.class);
		if (labelProvider == null) {
			labelProvider = (ILabelProvider)Platform.getAdapterManager().loadAdapter(peerNode, ILabelProvider.class.getName());
		}
		return labelProvider;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.events.IEventListener#eventFired(java.util.EventObject)
	 */
    @Override
    public void eventFired(EventObject event) {
    	if (event instanceof ChangeEvent) {
    		ChangeEvent changeEvent = (ChangeEvent)event;
    		IPeerNode peerNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
    		boolean openEditorOnChange = UIPlugin.getScopedPreferences().getBoolean(IPreferenceKeys.PREF_OPEN_EDITOR_ON_DEFAULT_CONTEXT_CHANGE);
    		if (peerNode != null && changeEvent.getSource() instanceof IDefaultContextService) {
    			ICommandService service = (ICommandService)PlatformUI.getWorkbench().getService(ICommandService.class);
    			service.refreshElements("org.eclipse.tcf.te.ui.toolbar.command.connect", null); //$NON-NLS-1$
    			service.refreshElements("org.eclipse.tcf.te.ui.toolbar.command.disconnect", null); //$NON-NLS-1$
    			if (openEditorOnChange) {
    				ViewsUtil.openEditor(new StructuredSelection(peerNode));
    			}
    		}

    		if (changeEvent.getSource() instanceof IDefaultContextService ||
    						(changeEvent.getSource() == peerNode &&
    						(IPeerNodeProperties.PROPERTY_CONNECT_STATE.equals(changeEvent.getEventId()) ||
    										IPeerNodeProperties.PROPERTY_IS_VALID.equals(changeEvent.getEventId()) ||
    										"properties".equals(changeEvent.getEventId())))) { //$NON-NLS-1$
    			if (menuMgr != null) menuMgr.markDirty();
    			ExecutorsUtil.executeInUI(new Runnable() {
    				@Override
    				public void run() {
    					update();
    				}
    			});
    		}
    	}
    }


	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener#modelChanged(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel, org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, boolean)
	 */
    @Override
    public void modelChanged(IPeerModel model, IPeerNode peerNode, boolean added) {
    	if (menuMgr != null) menuMgr.markDirty();
		ExecutorsUtil.executeInUI(new Runnable() {
			@Override
			public void run() {
				update();
			}
		});
    }


	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener#modelDisposed(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel)
	 */
    @Override
    public void modelDisposed(IPeerModel model) {
    }
}