/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.editor;

import java.util.EventObject;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.tcf.te.launch.ui.editor.AbstractLaunchTabContainerEditorPage;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.runtime.persistence.PersistenceManager;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService;
import org.eclipse.tcf.te.tcf.launch.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

/**
 * TCF launch configuration tab container page implementation.
 */
public abstract class AbstractTcfLaunchTabContainerEditorPage extends AbstractLaunchTabContainerEditorPage implements ILaunchConfigurationListener {

	protected ILaunchConfigurationListener launchConfigListener = null;

	protected static final String PROP_LAUNCH_CONFIG_WC = "launchConfigWorkingCopy.transient.silent"; //$NON-NLS-1$
	protected static final String PROP_ORIGINAL_LAUNCH_CONFIG_ATTRIBUTES = "launchConfigAttributes.transient.silent"; //$NON-NLS-1$

	/**
	 * Get the peer model from the editor input.
	 *
	 * @param input The editor input.
	 * @return The peer model.
	 */
	public IPeerNode getPeerModel(Object input) {
		return (IPeerNode) ((IAdaptable) input).getAdapter(IPeerNode.class);
	}

	private boolean isAutoSave() {
		boolean autoSave = !UIPlugin.getDefault().getPreferenceStore().getBoolean("NoLaunchEditorTabAutoSave"); //$NON-NLS-1$
		return autoSave;
	}

	/**
	 * Get the launch configuration from the peer model.
	 *
	 * @param peerNode The peer model.
	 * @return The launch configuration.
	 */
	public static ILaunchConfigurationWorkingCopy getLaunchConfig(final IPeerNode peerNode) {
		ILaunchConfigurationWorkingCopy wc = null;
		if (peerNode != null) {
			IPropertiesAccessService service = ServiceManager.getInstance().getService(peerNode, IPropertiesAccessService.class);
			Assert.isNotNull(service);
			if (service.getProperty(peerNode, PROP_LAUNCH_CONFIG_WC) instanceof ILaunchConfigurationWorkingCopy) {
				wc = (ILaunchConfigurationWorkingCopy) service.getProperty(peerNode, PROP_LAUNCH_CONFIG_WC);
			}
			else {
				wc = (ILaunchConfigurationWorkingCopy) Platform.getAdapterManager().getAdapter(peerNode, ILaunchConfigurationWorkingCopy.class);
				if (wc == null) {
					wc = (ILaunchConfigurationWorkingCopy) Platform.getAdapterManager().loadAdapter(peerNode, "org.eclipse.debug.core.ILaunchConfigurationWorkingCopy"); //$NON-NLS-1$
				}
				Assert.isNotNull(wc);
				service.setProperty(peerNode, PROP_LAUNCH_CONFIG_WC, wc);
				IPersistenceDelegate delegate = PersistenceManager.getInstance().getDelegate(wc, String.class);
				String launchConfigAttributes = null;
				try {
					launchConfigAttributes = delegate != null ? (String) delegate.write(wc, String.class) : null;
				}
				catch (Exception e) {
					/* ignored on purpose */
				}
				service.setProperty(peerNode, PROP_ORIGINAL_LAUNCH_CONFIG_ATTRIBUTES, launchConfigAttributes);
			}
		}
		return wc;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.editor.AbstractLaunchTabContainerEditorPage#setupData(java.lang.Object)
	 */
	@Override
	public boolean setupData(Object input) {
		ILaunchConfigurationWorkingCopy wc = getLaunchConfig(getPeerModel(input));
		if (wc != null) {
			getLaunchConfigurationTab().initializeFrom(wc);
			checkLaunchConfigDirty();
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.editor.AbstractLaunchTabContainerEditorPage#extractData()
	 */
	@Override
	public boolean extractData() {
		if (isAutoSave()) {
			return false;
		}
		ILaunchConfigurationWorkingCopy wc = getLaunchConfig(getPeerModel(getEditorInput()));
		if (wc != null && checkLaunchConfigDirty()) {
			getLaunchConfigurationTab().performApply(wc);
			try {
				IPeerNode peerNode = getPeerModel(getEditorInput());
				IPropertiesAccessService service = ServiceManager.getInstance().getService(peerNode, IPropertiesAccessService.class);
				Assert.isNotNull(service);
				service.setProperty(peerNode, PROP_LAUNCH_CONFIG_WC, null);
				wc.doSave();
				onPostSave(wc);
				checkLaunchConfigDirty();
				return true;
			}
			catch (Exception e) {
			}
		}
		return false;
	}

	/**
	 * Check if the launch configuration has changed. If it has changed, the page is set dirty.
	 *
	 * @return <code>true</code> if the launch configuration has changed since last save.
	 */
	public boolean checkLaunchConfigDirty() {
		boolean dirty = false;
		IPeerNode peerNode = getPeerModel(getEditorInput());
		IPropertiesAccessService service = ServiceManager.getInstance().getService(peerNode, IPropertiesAccessService.class);
		String oldLaunchConfigAttributes = (String) service.getProperty(peerNode, PROP_ORIGINAL_LAUNCH_CONFIG_ATTRIBUTES);
		IPersistenceDelegate delegate = PersistenceManager.getInstance().getDelegate(getLaunchConfig(peerNode), String.class);
		String launchConfigAttributes = null;
		try {
			launchConfigAttributes = (String) delegate.write(getLaunchConfig(peerNode), String.class);
			dirty = !launchConfigAttributes.equals(oldLaunchConfigAttributes);
		}
		catch (Exception e) {
		}
		setDirty(dirty);
		return dirty;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.editor.AbstractLaunchTabContainerEditorPage#setDirty(boolean)
	 */
	@Override
	public void setDirty(boolean dirty) {
		if (isAutoSave()) {
			final ILaunchConfigurationWorkingCopy wc = getLaunchConfig(getPeerModel(getEditorInput()));
			if (wc != null && dirty) {
				IPeerNode peerNode = getPeerModel(getEditorInput());
				IPropertiesAccessService service = ServiceManager.getInstance().getService(peerNode, IPropertiesAccessService.class);
				service.setProperty(peerNode, PROP_LAUNCH_CONFIG_WC, null);
				try {
					wc.doSave();
					onPostSave(wc);
				}
				catch (Exception e) {
				}
			}
		}
		else {
			super.setDirty(dirty);
			ExecutorsUtil.executeInUI(new Runnable() {
				@Override
				public void run() {
					getManagedForm().dirtyStateChanged();
				}
			});
		}
	}

	/**
	 * Called once the editor page got saved.
	 *
	 * @param config The launch configuration saved. Must not be <code>null</code>.
	 */
	protected void onPostSave(ILaunchConfiguration config) {
		Assert.isNotNull(config);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.editor.AbstractLaunchTabContainerEditorPage#setActive(boolean)
	 */
	@Override
	public void setActive(boolean active) {
		super.setActive(active);
		if (active && launchConfigListener == null) {
			launchConfigListener = this;
			DebugPlugin.getDefault().getLaunchManager().addLaunchConfigurationListener(this);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.editor.AbstractLaunchTabContainerEditorPage#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		IPeerNode peerNode = getPeerModel(getEditorInput());
		IPropertiesAccessService service = ServiceManager.getInstance().getService(peerNode, IPropertiesAccessService.class);
		service.setProperty(peerNode, PROP_ORIGINAL_LAUNCH_CONFIG_ATTRIBUTES, null);
		service.setProperty(peerNode, PROP_LAUNCH_CONFIG_WC, null);
		DebugPlugin.getDefault().getLaunchManager().removeLaunchConfigurationListener(this);
		launchConfigListener = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationAdded(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void launchConfigurationAdded(ILaunchConfiguration configuration) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationRemoved(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationChanged(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void launchConfigurationChanged(ILaunchConfiguration configuration) {
		if (!(configuration instanceof ILaunchConfigurationWorkingCopy)) {
			IPeerNode peerNode = getPeerModel(getEditorInput());
			IPropertiesAccessService service = ServiceManager.getInstance().getService(peerNode, IPropertiesAccessService.class);
			ILaunchConfigurationWorkingCopy wc = (ILaunchConfigurationWorkingCopy) service.getProperty(peerNode, PROP_LAUNCH_CONFIG_WC);
			if (wc != null && configuration.getName().equals(wc.getName())) {
				service.setProperty(peerNode, PROP_ORIGINAL_LAUNCH_CONFIG_ATTRIBUTES, null);
				service.setProperty(peerNode, PROP_LAUNCH_CONFIG_WC, null);
				ExecutorsUtil.executeInUI(new Runnable() {
					@Override
					public void run() {
						setActive(isActive());
					}
				});
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractCustomFormToolkitEditorPage#doCreateLinkContribution(org.eclipse.jface.action.IToolBarManager)
	 */
	@Override
	protected IContributionItem doCreateLinkContribution(final IToolBarManager tbManager) {
		return new ControlContribution("SetAsDefaultContextLink") { //$NON-NLS-1$
			IEventListener eventListener = null;
			@Override
			public void dispose() {
				super.dispose();
				if (eventListener == null) {
					EventManager.getInstance().removeEventListener(eventListener);
				}
			}
			@Override
			protected Control createControl(Composite parent) {
				final ImageHyperlink hyperlink = new ImageHyperlink(parent, SWT.NONE);
				hyperlink.setText(Messages.AbstractCustomFormToolkitEditorPage_setAsDefault_link);
				hyperlink.setUnderlined(true);
				hyperlink.setForeground(getManagedForm().getToolkit().getHyperlinkGroup().getForeground());
				IPeerNode defaultNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
				setVisible(defaultNode == null || defaultNode != getEditorInputNode());
				hyperlink.addHyperlinkListener(new IHyperlinkListener() {
					@Override
					public void linkActivated(HyperlinkEvent e) {
						ServiceManager.getInstance().getService(IDefaultContextService.class).setDefaultContext((IPeerNode)getEditorInputNode());
					}
					@Override
					public void linkEntered(HyperlinkEvent e) {
						hyperlink.setForeground(getManagedForm().getToolkit().getHyperlinkGroup().getActiveForeground());
					}
					@Override
					public void linkExited(HyperlinkEvent e) {
						hyperlink.setForeground(getManagedForm().getToolkit().getHyperlinkGroup().getForeground());
					}
				});
	
				eventListener = new IEventListener() {
					@Override
					public void eventFired(EventObject event) {
						if (event instanceof ChangeEvent) {
							ChangeEvent changeEvent = (ChangeEvent)event;
							if (changeEvent.getSource() instanceof IDefaultContextService) {
								IPeerNode defaultNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
								setVisible(defaultNode == null || getEditorInputNode() == null || defaultNode != getEditorInputNode());
								ExecutorsUtil.executeInUI(new Runnable() {
									@Override
									public void run() {
										tbManager.update(true);
									}
								});
							}
						}
					}
				};
	
				EventManager.getInstance().addEventListener(eventListener, ChangeEvent.class);
	
				return hyperlink;
			}
		};
	}
}
