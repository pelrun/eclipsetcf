/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.activator;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.callback.AsyncCallbackCollector;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.interfaces.IConditionTester;
import org.eclipse.tcf.te.runtime.preferences.ScopedEclipsePreferences;
import org.eclipse.tcf.te.runtime.utils.ProgressHelper;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.editor.EditorPeerModelListener;
import org.eclipse.tcf.te.tcf.ui.internal.ImageConsts;
import org.eclipse.tcf.te.ui.jface.images.AbstractImageDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 */
public class UIPlugin extends AbstractUIPlugin {
	// The shared instance
	private static UIPlugin plugin;
	// The scoped preferences instance
	private static volatile ScopedEclipsePreferences scopedPreferences;
	// The workbench listener instance
	private IWorkbenchListener listener;
	// Reference to the workbench listener
	/* default */ final ListenerList listeners = new ListenerList();
	// The peer model listener instance
	/* default */ IPeerModelListener peerModelListener = null;

	/**
	 * Constructor.
	 */
	public UIPlugin() {
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static UIPlugin getDefault() {
		return plugin;
	}

	/**
	 * Convenience method which returns the unique identifier of this plugin.
	 */
	public static String getUniqueIdentifier() {
		if (getDefault() != null && getDefault().getBundle() != null) {
			return getDefault().getBundle().getSymbolicName();
		}
		return "org.eclipse.tcf.te.tcf.ui"; //$NON-NLS-1$
	}

	/**
	 * Return the scoped preferences for this plugin.
	 */
	public static ScopedEclipsePreferences getScopedPreferences() {
		if (scopedPreferences == null) {
			scopedPreferences = new ScopedEclipsePreferences(getUniqueIdentifier());
		}
		return scopedPreferences;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		// Create and register the workbench listener instance
		listener = new IWorkbenchListener() {

			@Override
			public boolean preShutdown(IWorkbench workbench, boolean forced) {
				boolean proceedShutdown = true;

				// If there are workbench listener registered here, than
				// invoke them now before closing all the channels.
				Object[] candidates = listeners.getListeners();
				for (Object listener : candidates) {
					if (!(listener instanceof IWorkbenchListener)) continue;
					proceedShutdown &= ((IWorkbenchListener)listener).preShutdown(workbench, forced);
					if (!proceedShutdown && !forced) break;
				}

				if (proceedShutdown || forced) {
					final IPeerModel model = ModelManager.getPeerModel(true);
					if (model != null) {
						final List<IPeerNode> peerNodes = new ArrayList<IPeerNode>();
						for (IPeerNode peerNode : model.getPeerNodes()) {
							if (peerNode.isConnectStateChangeActionAllowed(IConnectable.ACTION_DISCONNECT)) {
								peerNodes.add(peerNode);
							}
						}

						if (!peerNodes.isEmpty()) {
							IRunnableWithProgress dialogRunnable = new IRunnableWithProgress() {
								@Override
								public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
									ProgressHelper.setTaskName(monitor, "Disconnecting Connections..."); //$NON-NLS-1$

									final AsyncCallbackCollector collector = new AsyncCallbackCollector();

									Protocol.invokeAndWait(new Runnable() {
										@Override
										public void run() {
											// Loop them and check if disconnect is available
											for (IPeerNode peerNode : peerNodes) {
												if (peerNode.isConnectStateChangeActionAllowed(IConnectable.ACTION_DISCONNECT)) {
													peerNode.changeConnectState(IConnectable.ACTION_DISCONNECT, new AsyncCallbackCollector.SimpleCollectorCallback(collector), null);
												}
											}

											collector.initDone();
										}
									});

									ExecutorsUtil.waitAndExecute(0, new IConditionTester() {
										@Override
										public boolean isConditionFulfilled() {
											return collector.getConditionTester().isConditionFulfilled() || (monitor != null && monitor.isCanceled());
										}
										@Override
										public void cleanup() {
										}
									});
								}

							};

							ProgressMonitorDialog dialog = new ProgressMonitorDialog(workbench.getActiveWorkbenchWindow().getShell());
							try {
								dialog.run(true, true, dialogRunnable);
							}
							catch (Exception e) {
							}
						}
					}

					// Close all channels now
					Tcf.getChannelManager().closeAll(!Protocol.isDispatchThread());
				}

				return proceedShutdown;
			}

			@Override
			public void postShutdown(IWorkbench workbench) {
				// If there are workbench listener registered here, than invoke them now.
				Object[] candidates = listeners.getListeners();
				for (Object listener : candidates) {
					if (!(listener instanceof IWorkbenchListener)) continue;
					((IWorkbenchListener)listener).postShutdown(workbench);
				}
			}
		};
		PlatformUI.getWorkbench().addWorkbenchListener(listener);
		peerModelListener = new EditorPeerModelListener();
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				ModelManager.getPeerModel().addListener(peerModelListener);
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		if (listener != null) {
			PlatformUI.getWorkbench().removeWorkbenchListener(listener);
			listener = null;
		}
		if (peerModelListener != null) {
			ModelManager.getPeerModel().removeListener(peerModelListener);
			peerModelListener = null;
		}

		plugin = null;
		scopedPreferences = null;
		super.stop(context);
	}

	/**
	 * Adds the given workbench listener.
	 * <p>
	 * Has not effect if the same listener is already registered.
	 *
	 * @param listener The listener. Must not be <code>null</code>.
	 */
	public void addListener(IWorkbenchListener listener) {
    	Assert.isNotNull(listener);
    	listeners.add(listener);
	}

	/**
	 * Removes the given workbench listener.
	 * <p>
	 * Has no effect if the same listener was not already registered.
	 *
	 * @param listener The listener. Must not be <code>null</code>.
	 */
	public void removeListener(IWorkbenchListener listener) {
    	Assert.isNotNull(listener);
    	listeners.remove(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#initializeImageRegistry(org.eclipse.jface.resource.ImageRegistry)
	 */
	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		URL url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "peer.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.PEER_NODE, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "peer_discovered.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.PEER_DISCOVERED, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "peer.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.PEER_STATIC, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "targets_view.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.SYSTEM_MGNT_VIEW, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "connection.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.CONNECTION, ImageDescriptor.createFromURL(url));

		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_ELCL + "newTarget_wiz.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.NEW_PEER_NODE, ImageDescriptor.createFromURL(url));

		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OVR + "busy.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.BUSY_OVR, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OVR + "gold_ovr.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.GOLD_OVR, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OVR + "warning_ovr.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.WARNING_OVR, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OVR + "green_ovr.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.GREEN_OVR, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OVR + "grey_ovr.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.GREY_OVR, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OVR + "red_ovr.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.RED_OVR, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OVR + "redX_ovr.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.RED_X_OVR, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OVR + "link_ovr.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.LINK_OVR, ImageDescriptor.createFromURL(url));

		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_ETOOL + "newConfig.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.NEW_CONFIG, ImageDescriptor.createFromURL(url));
	}

	/**
	 * Loads the image registered under the specified key from the image
	 * registry and returns the <code>Image</code> object instance.
	 *
	 * @param key The key the image is registered with.
	 * @return The <code>Image</code> object instance or <code>null</code>.
	 */
	public static Image getImage(String key) {
		return getDefault().getImageRegistry().get(key);
	}

	/**
	 * Loads the image registered under the specified key from the image
	 * registry and returns the <code>ImageDescriptor</code> object instance.
	 *
	 * @param key The key the image is registered with.
	 * @return The <code>ImageDescriptor</code> object instance or <code>null</code>.
	 */
	public static ImageDescriptor getImageDescriptor(String key) {
		return getDefault().getImageRegistry().getDescriptor(key);
	}

	/**
	 * Loads the image given by the specified image descriptor from the image
	 * registry. If the image has been loaded ones before already, the cached
	 * <code>Image</code> object instance is returned. Otherwise, the <code>
	 * Image</code> object instance will be created and cached before returned.
	 *
	 * @param descriptor The image descriptor.
	 * @return The corresponding <code>Image</code> object instance or <code>null</code>.
	 */
	public static Image getSharedImage(AbstractImageDescriptor descriptor) {
		ImageRegistry registry = getDefault().getImageRegistry();

		String imageKey = descriptor.getDecriptorKey();
		Image image = registry.get(imageKey);
		if (image == null) {
			registry.put(imageKey, descriptor);
			image = registry.get(imageKey);
		}

		return image;
	}
}
