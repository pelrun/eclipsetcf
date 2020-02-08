/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.activator;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.url.TcfURLConnection;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.url.TcfURLStreamHandlerService;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.PersistenceManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

/**
 * The activator class of the core file system plugin.
 */
public class CorePlugin extends Plugin {

	private static final String PREFKEY_REVEAL_ON_CONNECT = "revealOnConnect"; //$NON-NLS-1$

	private static final String PLUGIN_ID = "org.eclipse.tcf.te.tcf.filesystem.core"; //$NON-NLS-1$

	// The bundle context of this plugin.
	private static BundleContext context;
	// The shared instance of this plug-in.
	private static CorePlugin plugin;
	// The service registration for the "tcf" URL stream handler.
	private ServiceRegistration<?> regURLStreamHandlerService;

	private Set<String> fRevealOnConnect;

	/**
	 * Get the bundle context of this plugin.
	 * @return The bundle context object.
	 */
	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
    public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		CorePlugin.context = bundleContext;
		plugin = this;
		// Register the "tcf" URL stream handler service.
		Hashtable<String, String[]> properties = new Hashtable<String, String[]>();
		properties.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] { TcfURLConnection.PROTOCOL_SCHEMA });
		regURLStreamHandlerService = context.registerService(URLStreamHandlerService.class.getName(), new TcfURLStreamHandlerService(), properties);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
    public void stop(BundleContext bundleContext) throws Exception {
		if (PersistenceManager.needsDisposal()) PersistenceManager.getInstance().dispose();
		if (regURLStreamHandlerService != null) {
			// When URL stream handler service is unregistered, any URL related operation will be invalid.
			regURLStreamHandlerService.unregister();
			regURLStreamHandlerService = null;
		}
		CorePlugin.context = null;
		plugin = null;
		super.stop(bundleContext);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static CorePlugin getDefault() {
		return plugin;
	}

	/**
	 * Convenience method which returns the unique identifier of this plugin.
	 */
	public static String getUniqueIdentifier() {
		return PLUGIN_ID;
	}

	public static void logError(String msg, Throwable cause) {
		getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, msg, cause));
	}


	public boolean addToRevealOnConnect(String location) {
		if (unsafeGetRevealOnConnect().add(location)) {
			storeRevealOnConnect();
			return true;
		}
		return false;
	}

	public boolean removeFromRevealOnConnect(String location) {
		if (unsafeGetRevealOnConnect().remove(location)) {
			storeRevealOnConnect();
			return true;
		}
		return false;
	}

	public boolean isRevealOnConnect(String location) {
		return unsafeGetRevealOnConnect().contains(location);
	}

	public Set<String> getRevealOnConnect() {
		return new HashSet<String>(unsafeGetRevealOnConnect());
	}

	private void storeRevealOnConnect() {
		if (fRevealOnConnect == null)
			return;

		StringBuilder buf = new StringBuilder();
		for (String reveal : fRevealOnConnect) {
			if (buf.length() > 0)
				buf.append("\0"); //$NON-NLS-1$
			buf.append(reveal);
		}
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		node.put(PREFKEY_REVEAL_ON_CONNECT, buf.toString());
		try {
			node.flush();
		} catch (BackingStoreException e) {
		}
	}

	private Set<String> unsafeGetRevealOnConnect() {
		if (fRevealOnConnect == null) {
			HashSet<String> favorites = new HashSet<String>();
			String favs = Platform.getPreferencesService().getString(PLUGIN_ID, PREFKEY_REVEAL_ON_CONNECT, "", null); //$NON-NLS-1$
			for (String fav : favs.split("\0")) { //$NON-NLS-1$
				if (fav.length() > 0)
					favorites.add(fav);
			}
			fRevealOnConnect = favorites;
		}
		return fRevealOnConnect;
	}
}
