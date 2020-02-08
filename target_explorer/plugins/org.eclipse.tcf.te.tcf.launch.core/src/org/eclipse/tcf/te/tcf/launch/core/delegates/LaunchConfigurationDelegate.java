/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.core.delegates;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.launch.core.lm.interfaces.ICommonLaunchAttributes;
import org.eclipse.tcf.util.TCFTask;
import org.osgi.framework.Bundle;

/**
 * Default tcf launch configuration delegate implementation.
 * <p>
 * The launch configuration delegate implements the bridge between the native Eclipse launch
 * configuration framework and the stepper engine. The delegate is standard for all
 * launch configurations which supports extensible and modularized launching.
 * <p>
 * <b>Implementation Details</b>:<br>
 * <ul>
 * <li>The launch configuration delegate signals the completion of the launch sequence via
 * the custom {@link ILaunch} attribute {@link ICommonLaunchAttributes#ILAUNCH_ATTRIBUTE_LAUNCH_SEQUENCE_COMPLETED}.</li>
 * <li>The launch configuration delegates enforces the removal of the launch from the Eclipse
 * debug platforms launch manager if the progress monitor is set to canceled or an status with
 * severity {@link IStatus#CANCEL} had been thrown by the stepper implementation.</li>
 * <li>The launch configuration delegate creates launches of type {@link Launch}.</li>
 * </ul>
 */
public class LaunchConfigurationDelegate extends org.eclipse.tcf.te.launch.core.delegates.LaunchConfigurationDelegate {

	static Boolean is_headless;
	static boolean ui_activation_done;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.core.delegates.LaunchConfigurationDelegate#getLaunch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
	 */
	@Override
	public ILaunch getLaunch(final ILaunchConfiguration configuration, final String mode) throws CoreException {
		return new TCFTask<ILaunch>() {
			int cnt;
			@Override
			public void run() {
				// see also TCFLaunchDelegate.getLaunch()
				if (is_headless == null) {
					Bundle b = Platform.getBundle("org.eclipse.ui.workbench"); //$NON-NLS-1$
					is_headless = new Boolean(b == null || b.getState() != Bundle.ACTIVE);
				}

				if (!Boolean.TRUE.equals(is_headless) && !ui_activation_done) {
					/* Make sure UI bundle is activated and is listening for launch events */
					try {
						Bundle bundle = Platform.getBundle("org.eclipse.tcf.debug.ui"); //$NON-NLS-1$
						bundle.start(Bundle.START_TRANSIENT);
					}
					catch (Throwable x) {
						Protocol.log("TCF debugger UI startup error", x); //$NON-NLS-1$
					}
					ui_activation_done = true;
				}
				// Need to delay at least one dispatch cycle to work around
				// a possible racing between thread that calls getLaunch() and
				// the process of activation of other TCF plug-ins.
				if (cnt++ < 2) {
					Protocol.invokeLater(this);
				}
				else {
					done(new Launch(configuration, mode));
				}
			}
		}.getE();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.core.delegates.LaunchConfigurationDelegate#onLaunchFinished(org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IStatus)
	 */
	@Override
	protected void onLaunchFinished(ILaunch launch, IStatus status) {
		super.onLaunchFinished(launch, status);
		if (launch instanceof Launch) {
			if (((Launch)launch).getCallback() != null) {
				((Launch)launch).getCallback().done(launch, status);
			}
		}
	}
}
