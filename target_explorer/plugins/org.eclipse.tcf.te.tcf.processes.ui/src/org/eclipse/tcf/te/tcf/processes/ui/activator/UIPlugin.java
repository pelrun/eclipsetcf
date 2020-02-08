/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.activator;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.tcf.processes.ui.internal.ImageConsts;
import org.eclipse.tcf.te.ui.jface.images.AbstractImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class UIPlugin extends AbstractUIPlugin {
	// The shared instance
	private static UIPlugin plugin;

	/**
	 * The constructor
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
		return "org.eclipse.tcf.te.tcf.processes.ui"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
    public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
    public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#initializeImageRegistry(org.eclipse.jface.resource.ImageRegistry)
	 */
	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		URL url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "process.png"); //$NON-NLS-1$
		registry.put(ImageConsts.OBJ_Process, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "thread.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.OBJ_Thread, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "process_root.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.OBJ_Process_Root, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "process_polling.png"); //$NON-NLS-1$
		registry.put(ImageConsts.PM_POLLING, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "attach.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.ATTACH, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "detach.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.DETACH, ImageDescriptor.createFromURL(url));
	}

	/**
	 * Loads the image registered under the specified key from the image
	 * registry and returns the <code>Image</code> object instance.
	 *
	 * @param key The key the image is registered with.
	 * @return The <code>Image</code> object instance or <code>null</code>.
	 */
	public static Image getImage(String key) {
		Image image = getDefault().getImageRegistry().get(key);
		if (image == null) {
			image = org.eclipse.tcf.te.ui.activator.UIPlugin.getImage(key);
		}
		return image;
	}

	/**
	 * Loads the image registered under the specified key from the image
	 * registry and returns the <code>ImageDescriptor</code> object instance.
	 *
	 * @param key The key the image is registered with.
	 * @return The <code>ImageDescriptor</code> object instance or <code>null</code>.
	 */
	public static ImageDescriptor getImageDescriptor(String key) {
		ImageDescriptor descriptor = getDefault().getImageRegistry().getDescriptor(key);
		if (descriptor == null) {
			descriptor = org.eclipse.tcf.te.ui.activator.UIPlugin.getImageDescriptor(key);
		}
		return descriptor;
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
