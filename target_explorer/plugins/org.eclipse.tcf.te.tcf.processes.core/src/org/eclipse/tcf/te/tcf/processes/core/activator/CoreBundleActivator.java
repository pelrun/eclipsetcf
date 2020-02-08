/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.activator;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.preferences.ScopedEclipsePreferences;
import org.eclipse.tcf.te.runtime.tracing.TraceHandler;
import org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.processes.core.model.listener.ModelListener;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class CoreBundleActivator extends Plugin {
	// The bundle context
	private static BundleContext context;
	// The scoped preferences instance
	private static volatile ScopedEclipsePreferences scopedPreferences;
	// The trace handler instance
	private static volatile TraceHandler traceHandler;
	// The locator model listener instance
	/* default */ IPeerModelListener listener;

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
		if (getContext() != null && getContext().getBundle() != null) {
			return getContext().getBundle().getSymbolicName();
		}
		return "org.eclipse.tcf.te.tcf.processes.core"; //$NON-NLS-1$
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

	/**
	 * Returns the bundles trace handler.
	 *
	 * @return The bundles trace handler.
	 */
	public static TraceHandler getTraceHandler() {
		if (traceHandler == null) {
			traceHandler = new TraceHandler(getUniqueIdentifier());
		}
		return traceHandler;
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		CoreBundleActivator.context = bundleContext;
		plugin = this;

		// Create the model listener instance
		listener = new ModelListener();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if (listener == null) return;
				// Register the model listener with the locator model
				ModelManager.getPeerModel().addListener(listener);
			}
		};

		Protocol.invokeLater(runnable);
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		CoreBundleActivator.context = null;
		plugin = null;
		scopedPreferences = null;
		traceHandler = null;

		// Remove the model listener from the locator model
		if (listener != null) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					IPeerModel model = ModelManager.getPeerModel(true);
					if (model != null) model.removeListener(listener);
					listener = null;
				}
			};
			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);
		}
	}

}
