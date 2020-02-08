/*******************************************************************************
 * Copyright (c) 2013, 2014 MontaVista Software, LLC. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Anna Dushistova (MontaVista) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.cdt.activator;

import org.eclipse.tcf.te.runtime.preferences.ScopedEclipsePreferences;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The shared instance
	private static Activator plugin;
	// The scoped preferences instance
	private static volatile ScopedEclipsePreferences scopedPreferences;

	private boolean isTEInitialized = false;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Convenience method which returns the unique identifier of this plugin.
	 */
	public static String getUniqueIdentifier() {
		if (getDefault() != null && getDefault().getBundle() != null) {
			return getDefault().getBundle().getSymbolicName();
		}
		return "org.eclipse.tcf.te.tcf.launch.cdt"; //$NON-NLS-1$
	}

	/**
	 * Return the scoped preferences for this plug-in.
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
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
    public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}


	public void initializeTE() {
		if(!isTEInitialized ){
		IPeerNode[] peers = ModelManager.getPeerModel().getPeerNodes();
		if (peers.length == 0) {
			// Sleep shortly
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
			}
		}
		isTEInitialized = true;
		}
	}

}
