/*******************************************************************************
 * Copyright (c) 2014, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
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
import org.eclipse.tcf.te.tcf.locator.utils.CommonUtils;
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
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.eclipse.ui.services.IServiceLocator;

/**
 * Configurations control implementation.
 */
public class DefaultContextSelectorToolbarContribution extends WorkbenchWindowControlContribution
implements IWorkbenchContribution, IEventListener, IPeerModelListener, IPropertyChangeListener, IHyperlinkListener{

	private static final String WARNING_BACKGROUND_FG_COLOR_NAME = "org.eclipse.ui.themes.matchColors.toolbarWarningBackground"; //$NON-NLS-1$

	private static final int CUSTOM_TOOLTIP_TIMER_TIME = 400; // Milliseconds
	private static final int CURSOR_HEIGHT = 20; // px
	private static final int CUSTOM_TOOLTIP_MAX_WIDTH = 320; // px

	private Composite panel = null;
	private Composite labelPanel = null;
	private Label image = null;
	private Label text = null;
	private Button button = null;

	/* default */ Shell customTooltipShell = null;
	/* default */ Composite customTooltipComposite = null;
	/* default */ FormText customTooltipText = null;
	/* default */ Timer customTooltipTimer = null;

	IServiceLocator serviceLocator = null;

	private MenuManager menuMgr = null;
	private Menu menu = null;

	private boolean clickRunning = false;

	private final RGB lightYellowRgb = new RGB(255, 250, 150); // Warning background color
	private enum PanelStyle {DEFAULT,WARNING} // Color styles
	/* default */ Color warningBackgroundColor = null;

	/* default */ Boolean signatureValid;

	protected volatile boolean updatePending;

	/**
	 * Constructor.
	 */
	public DefaultContextSelectorToolbarContribution() {
		this("org.eclipse.tcf.te.tcf.ui.DefaultContextSelectorToolbarContribution"); //$NON-NLS-1$
		PlatformUI.getPreferenceStore().addPropertyChangeListener(this);
		JFaceResources.getColorRegistry().addListener(this);
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 */
	public DefaultContextSelectorToolbarContribution(String id) {
		super(id);
		PlatformUI.getPreferenceStore().addPropertyChangeListener(this);
		JFaceResources.getColorRegistry().addListener(this);
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
		layout.marginHeight = 0; layout.marginWidth = 1;
		panel.setLayout(layout);

		initThemeColors();

		labelPanel = new Composite(panel, SWT.BORDER);
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

		// Customized tooltip
		Display display = PlatformUI.getWorkbench().getDisplay();
		customTooltipShell = new Shell(display, SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
		GridLayout customTooltipShellLayout = new GridLayout();
		customTooltipShellLayout.marginWidth = 0; customTooltipShellLayout.marginHeight = 0;
		customTooltipShellLayout.verticalSpacing = 0; customTooltipShellLayout.horizontalSpacing = 0;
		customTooltipShell.setLayout(customTooltipShellLayout);
		customTooltipShell.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		customTooltipComposite = new Composite(customTooltipShell, SWT.NONE);
		customTooltipComposite.setLayout(new GridLayout());
        customTooltipComposite.setBackground(warningBackgroundColor);

        customTooltipText = new FormText(customTooltipComposite, SWT.NO_FOCUS);
        customTooltipText.setBackground(warningBackgroundColor);
        customTooltipText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        customTooltipText.addHyperlinkListener(this);
        customTooltipText.addMouseTrackListener(new MouseTrackListener() {
			@Override
			public void mouseHover(MouseEvent e) {
			}

			@Override
			public void mouseExit(MouseEvent e) {
				startTooltipTimer();
			}

			@Override
			public void mouseEnter(MouseEvent e) {
				stopTooltipTimer();
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

		text.addMouseTrackListener(new MouseTrackListener() {
			@Override
			public void mouseHover(MouseEvent e) {
				showTooltip(e);
			}

			@Override
			public void mouseExit(MouseEvent e) {
				startTooltipTimer();
			}

			@Override
			public void mouseEnter(MouseEvent e) {
			}
		});

		button = new Button(labelPanel, SWT.ARROW | SWT.DOWN | SWT.FLAT | SWT.NO_FOCUS);
		layoutData = new GridData(SWT.TRAIL, SWT.CENTER, false, true);
		layoutData.minimumWidth=20;
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

    private void initThemeColors() {
	    // Get colors from Theme preferences
		warningBackgroundColor = JFaceResources.getColorRegistry().get(WARNING_BACKGROUND_FG_COLOR_NAME);
		if (warningBackgroundColor == null) {
			JFaceResources.getColorRegistry().put(WARNING_BACKGROUND_FG_COLOR_NAME, lightYellowRgb);
			warningBackgroundColor = JFaceResources.getColorRegistry().get(WARNING_BACKGROUND_FG_COLOR_NAME);
		}
    }

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.ContributionItem#dispose()
	 */
	@Override
	public void dispose() {
	    super.dispose();

	    EventManager.getInstance().removeEventListener(this);
	    ModelManager.getPeerModel().removeListener(this);

	    customTooltipShell.dispose();
	    customTooltipText.dispose();
		customTooltipComposite.dispose();
		stopTooltipTimer();

	    image.dispose();
	    text.dispose();
	    if (menuMgr != null) menuMgr.dispose();

	    image = null;
	    text = null;
	    customTooltipShell = null;
	    customTooltipText = null;
		customTooltipComposite = null;
		customTooltipTimer = null;
	}

	protected IPeerNode[] getPeerNodesSorted() {
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
			changePanelStyle(PanelStyle.DEFAULT);

			if (peerNode != null) {
			    DelegatingLabelProvider labelProvider = new DelegatingLabelProvider();
				image.setImage(labelProvider.decorateImage(labelProvider.getImage(peerNode), peerNode));
				String fullName = labelProvider.getText(peerNode);
				String name = fullName;
				if (name.length() > 22 && name.length() >= 25) {
					name = name.substring(0, 22) + "..."; //$NON-NLS-1$
				}
				text.setText(name);

				String tooltipMessage = Messages.DefaultContextSelectorToolbarContribution_tooltip_button;
				String tooltip = !fullName.equals(name) ? fullName : tooltipMessage;
				if (!peerNode.isValid()) {
					String error = CommonUtils.getPeerError(peerNode);
					tooltip = !fullName.equals(name) ? fullName+"\n" : ""; //$NON-NLS-1$ //$NON-NLS-2$
					if (error != null) {
                        tooltip += error;
					}
					else {
						tooltip += Messages.PeerLabelProviderDelegate_description_invalid;
					}
				}
				else if (peerNode.getConnectState() == IConnectable.STATE_CONNECTED) {
					Map<String,String> warnings = CommonUtils.getPeerWarnings(peerNode);
					if (warnings != null && !warnings.isEmpty()) {
						changePanelStyle(PanelStyle.WARNING);

						tooltip = !fullName.equals(name) ? fullName : ""; //$NON-NLS-1$
						for (String warning : warnings.values()) {
							if (tooltip.trim().length() > 0) {
								tooltip += "\n"; //$NON-NLS-1$
							}
	                        tooltip += warning;
                        }
					}
				}
				else if (peerNode.getConnectState() == IConnectable.STATE_CONNECTION_LOST ||
								 peerNode.getConnectState() == IConnectable.STATE_CONNECTION_RECOVERING ||
								 peerNode.getConnectState() == IConnectable.STATE_UNKNOWN) {
					changePanelStyle(PanelStyle.WARNING);
				}

				image.setToolTipText(tooltip);
				button.setToolTipText(tooltipMessage);
			}
			else {
				image.setImage(UIPlugin.getImage(ImageConsts.NEW_CONFIG));
				text.setText(Messages.DefaultContextSelectorToolbarContribution_label_new);

				image.setToolTipText(Messages.DefaultContextSelectorToolbarContribution_tooltip_new);
				button.setToolTipText(Messages.DefaultContextSelectorToolbarContribution_tooltip_new);
			}
		}
	}

	/**
	 * Switches between themes.
	 * 	- Default: when there aren't issues with the connection.
	 *  - Warning: when there is some kind of warning. The background turns yellow.
	 * @param pStyle
	 */
	private void changePanelStyle(PanelStyle pStyle) {
		if (text != null && labelPanel != null) {
			if (pStyle.equals(PanelStyle.DEFAULT)) {
				labelPanel.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
				text.setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
			} else if (pStyle.equals(PanelStyle.WARNING)) {
				labelPanel.setBackground(warningBackgroundColor);
				text.setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
				customTooltipComposite.setBackground(warningBackgroundColor);
				customTooltipText.setBackground(warningBackgroundColor);
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
			    menuMgr.add(new GroupMarker("group.rename")); //$NON-NLS-1$
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
    		if (changeEvent.getSource() instanceof IDefaultContextService) {
    			if (openEditorOnChange && peerNode != null) {
    				ViewsUtil.openEditor(new StructuredSelection(peerNode));
    			}
    		}

    		if (changeEvent.getSource() instanceof IDefaultContextService ||
    						(changeEvent.getSource() == peerNode &&
    						(IPeerNodeProperties.PROPERTY_CONNECT_STATE.equals(changeEvent.getEventId()) ||
    										IPeerNodeProperties.PROPERTY_IS_VALID.equals(changeEvent.getEventId()) ||
    										IPeerNodeProperties.PROPERTY_WARNINGS.equals(changeEvent.getEventId()) ||
    										"properties".equals(changeEvent.getEventId())))) { //$NON-NLS-1$
    			scheduleUpdate();
    		}
    	}
    }

	private void scheduleUpdate() {
		if (updatePending) return;
		updatePending = true;
		ExecutorsUtil.executeInUI(new Runnable() {
			private boolean scheduled;
			@SuppressWarnings("synthetic-access")
			@Override
			public void run() {
				if (text == null) return;
				if (!scheduled) {
					Display.getCurrent().timerExec(200, this);
					scheduled = true;
					return;
				}
				updatePending = false;
    			ICommandService service = (ICommandService)PlatformUI.getWorkbench().getService(ICommandService.class);
    			service.refreshElements("org.eclipse.tcf.te.ui.toolbar.command.connect", null); //$NON-NLS-1$
    			service.refreshElements("org.eclipse.tcf.te.ui.toolbar.command.disconnect", null); //$NON-NLS-1$
				update();
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener#modelChanged(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel, org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, boolean)
	 */
    @Override
    public void modelChanged(IPeerModel model, IPeerNode peerNode, boolean added) {
    	scheduleUpdate();
    }


	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener#modelDisposed(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel)
	 */
    @Override
    public void modelDisposed(IPeerModel model) {
    }

	@Override
    public void propertyChange(PropertyChangeEvent event) {
		String property= event.getProperty();
		if (property.equals(WARNING_BACKGROUND_FG_COLOR_NAME)) {
			initThemeColors();
			update();
		}
    }

	@Override
	public void linkEntered(HyperlinkEvent e) {
	}

	@Override
	public void linkExited(HyperlinkEvent e) {
	}

	@Override
	public void linkActivated(HyperlinkEvent e) {
		if (e.widget instanceof FormText) {
			// Process event id
			// e.g. fixConnection=20b476ca-9804-40ae-ae58-ecebc36ffa74
			String selectedNodeId = null;
			if (e.data != null) {
				String[] splitStr = e.data.toString().split("="); //$NON-NLS-1$
				if (splitStr.length == 2) {
					selectedNodeId = splitStr[1];
				}
			}

			IPeerNode selectedNode = getPeerNodeById(selectedNodeId);
			if (selectedNode != null) {
				OpenEditorHandler.openEditorOnSelection(getWorkbenchWindow(), new StructuredSelection(selectedNode));
				stopTooltipTimer();
				hideCustomTooltip();
			}
		}
	}

	/* default */ IPeerNode getPeerNodeById(String id) {
		IPeerNode[] peerNodes = ModelManager.getPeerModel().getPeerNodes();

		if (peerNodes!=null) {
			for(IPeerNode pNode:peerNodes) {
				if (pNode.getPeerId().equals(id)) {
					return pNode;
				}
			}
		}

		return null;
	}

	/**
	 * Calculates the position where the tooltip should be, considering
	 * the current cursor position.
	 * @param e
	 * @return
	 */
	/* default */ Point calculateTooltipPosition(MouseEvent e) {
		Point p = new Point(0, 0);

		if (e.widget instanceof Control) {
	        p = ((Control) e.widget).toDisplay(e.x, e.y);
	        Point shellSize = customTooltipShell.getSize();
	        Rectangle screenBounds = ((Control) e.widget).getDisplay().getBounds();
	        // Keeps the tooltip inside the screen bounds
	        if (p.x + shellSize.x > screenBounds.width) {
	        	p.x -= (p.x + shellSize.x - screenBounds.width);
	        	if (p.x < 0) {
	        		p.x = 0;
	        	}
	        }
	        p.y += CURSOR_HEIGHT;
	    }

		return p;
	}

	/**
	 * Counts how many time the substring <code>sub</code> appears in the
	 * string <code>str</code>.
	 * @param str
	 * @param sub
	 * @return
	 */
	/* default */ int countMatches(String str, String sub) {
		int lastIndex = 0;
	    int count = 0;

		while (lastIndex != -1) {
			lastIndex = str.indexOf(sub, lastIndex);
			if (lastIndex != -1) {
				count++;
				lastIndex += sub.length();
			}
		}

		return count;
	}

	/* default */ void startTooltipTimer() {
		if (customTooltipShell != null) {
			if (customTooltipTimer!=null) {
				customTooltipTimer.cancel();
				customTooltipTimer.purge();
			}

			customTooltipTimer = new Timer();
			customTooltipTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					hideCustomTooltip();
				}
			}, CUSTOM_TOOLTIP_TIMER_TIME);
		}
	}

	/* default */ void stopTooltipTimer() {
		if (customTooltipTimer!=null) {
			customTooltipTimer.cancel();
			customTooltipTimer.purge();
		}
	}

	/* default */ void hideCustomTooltip() {
		if (customTooltipShell!=null && PlatformUI.getWorkbench()!=null && PlatformUI.getWorkbench().getDisplay()!=null) {
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			    @Override
				public void run() {
			    	if (customTooltipShell!=null) {
			    		customTooltipShell.setVisible(false);
			    	}
			    }
			});
		}
	}

	/* default */ void showTooltip(MouseEvent e) {
		if (customTooltipShell != null) {
			stopTooltipTimer();

			GC gc = new GC(customTooltipText);
			int nLines = 0; // Number of text lines
			int maxLineWidth = 0; // Width of the widest line
			StringBuilder tooltipStringBuilder = new StringBuilder();

			tooltipStringBuilder.append("<form>"); //$NON-NLS-1$
			// Get list of warnings
			IPeerNode[] peerNodes = getPeerNodesSorted();
			for(IPeerNode pNode:peerNodes) {
				Map<String,String> warningsMap = CommonUtils.getPeerWarnings(pNode);
				if (warningsMap != null) {
					Iterator<Entry<String, String>> warningsMapIterator = warningsMap.entrySet().iterator();
					List<String> currentModulesDisplayed = new ArrayList<String>(); // To avoid duplicated warnings
				    while (warningsMapIterator.hasNext()) {
				    	Entry<String, String> warningsMapEntry = warningsMapIterator.next();

				    	// Check duplicated warnings
				    	String warningOrigin = CommonUtils.getPeerWarningOrigin(pNode, warningsMapEntry.getKey());
				    	if (warningOrigin != null) {
					    	if (currentModulesDisplayed.contains(warningOrigin)) {
					    		continue;
					    	}
					    	currentModulesDisplayed.add(warningOrigin);
				    	}

				    	String warningStr = warningsMapEntry.getValue();
				    	tooltipStringBuilder.append("<p>"); //$NON-NLS-1$
				    	tooltipStringBuilder.append(warningStr);
				    	tooltipStringBuilder.append("</p>"); //$NON-NLS-1$
				    	tooltipStringBuilder.append("<p><a href=\"fixConnection="); //$NON-NLS-1$
				    	tooltipStringBuilder.append(pNode.getPeerId());
				    	tooltipStringBuilder.append("\">"); //$NON-NLS-1$
				    	tooltipStringBuilder.append(Messages.DefaultContextSelectorToolbarContribution_tooltip_warningFix);
				    	tooltipStringBuilder.append("</a></p><br/><br/>"); //$NON-NLS-1$

				    	// Calculate text lines used by the text
				    	int textWidth = gc.stringExtent(warningStr).x;
				    	nLines += (textWidth/CUSTOM_TOOLTIP_MAX_WIDTH) + 1;
				    	nLines += countMatches(warningStr, "\n"); //$NON-NLS-1$
				    	nLines += 3; // Link and separators
				    	if (textWidth > maxLineWidth) {
				    		maxLineWidth = textWidth;
				    	}
				    }
				}
			}
			tooltipStringBuilder.append("</form>"); //$NON-NLS-1$

			if (nLines > 0) {
				customTooltipText.setText(tooltipStringBuilder.toString().replaceAll("(\r\n|\n)", "<br />"), true, false); //$NON-NLS-1$ //$NON-NLS-2$

				// Resize tooltip
				if (maxLineWidth > CUSTOM_TOOLTIP_MAX_WIDTH) {
					maxLineWidth = CUSTOM_TOOLTIP_MAX_WIDTH;
				}
				int textHeightCalc = gc.stringExtent(tooltipStringBuilder.toString()).y;
				customTooltipShell.setSize(maxLineWidth, textHeightCalc*nLines);
				customTooltipComposite.setSize(maxLineWidth, textHeightCalc*nLines);
				customTooltipShell.setLocation(calculateTooltipPosition(e));
				customTooltipShell.setVisible(true);
			}
		}
	}
}