/*******************************************************************************
 * Copyright (c) 2014, 2015 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.remote.core.activator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * The activator class controls the plug-in life cycle
 */
public class CoreBundleActivator implements BundleActivator {

	private static final String PLUGIN_ID = "org.eclipes.tcf.te.tcf.remote.core"; //$NON-NLS-1$
	// The bundle context
	private static BundleContext context;
	// The shared instance
	private static CoreBundleActivator plugin;

	/**
	 * Returns the bundle context
	 *
	 * @return the bundle context
	 */
	public static BundleContext getContext() {
		return context;
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static CoreBundleActivator getDefault() {
		return plugin;
	}

	/**
	 * Convenience method which returns the unique identifier of this plugin.
	 */
	public static String getUniqueIdentifier() {
		return PLUGIN_ID;
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		context = bundleContext;
		plugin = this;
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		context = null;
		plugin = null;
	}

	public static <T> T getService(Class<T> service) {
		ServiceReference<T> ref = context.getServiceReference(service);
		return ref != null ? context.getService(ref) : null;
	}

	public static void logError(String msg, Throwable th) {
		Platform.getLog(Platform.getBundle(PLUGIN_ID)).log(new Status(IStatus.ERROR, PLUGIN_ID, msg, th));
    }

}
