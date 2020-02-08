/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.internal;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.activator.CoreBundleActivator;


/**
 * Class loaded by the TCF core framework when the framework is fired up. The static
 * constructor of the class will trigger whatever is necessary in this case.
 * <p>
 * <b>Note:</b> This will effectively trigger {@link CoreBundleActivator#start(org.osgi.framework.BundleContext)}
 * to be called.
 */
public class Startup {
	// Atomic boolean to store the started state of the TCF core framework
	/* default */ final static AtomicBoolean STARTED = new AtomicBoolean(false);

	static {
		setStarted(true);
	}

	/**
	 * Set the core framework started state to the given state.
	 *
	 * @param started <code>True</code> when the framework is started, <code>false</code> otherwise.
	 */
	public static final void setStarted(boolean started) {
		STARTED.set(started);

		// Start/Stop should be called in the TCF protocol dispatch thread
		if (Protocol.getEventQueue() != null) {
			// Catch IllegalStateException: TCF event dispatcher has shut down
			try {
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						if (STARTED.get()) Tcf.start(); else Tcf.stop();
					}
				};

				if (Protocol.isDispatchThread()) runnable.run();
				else Protocol.invokeAndWait(runnable);
			} catch (IllegalStateException e) {
				if (!STARTED.get() && "TCF event dispatcher has shut down".equals(e.getLocalizedMessage())) { //$NON-NLS-1$
					// ignore the exception on shutdown
				} else {
					// re-throw in any other case
					throw e;
				}
			}
		}
	}

	/**
	 * Returns if or if not the core framework has been started.
	 *
	 * @return <code>True</code> when the framework is started, <code>false</code> otherwise.
	 */
	public static final boolean isStarted() {
		return STARTED.get();
	}
}
