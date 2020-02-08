/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.internal.services;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.tcf.te.launch.core.persistence.launchcontext.LaunchContextsPersistenceDelegate;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.tcf.launch.core.interfaces.ILaunchTypes;

/**
 * Debug service launches listener implementation
 */
public class DebugServicesLaunchesListener implements ILaunchesListener2 {

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchesListener#launchesRemoved(org.eclipse.debug.core.ILaunch[])
	 */
	@Override
	public void launchesRemoved(ILaunch[] launches) {
		firePropertyChange(launches);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchesListener#launchesAdded(org.eclipse.debug.core.ILaunch[])
	 */
	@Override
	public void launchesAdded(ILaunch[] launches) {
		firePropertyChange(launches);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchesListener#launchesChanged(org.eclipse.debug.core.ILaunch[])
	 */
	@Override
	public void launchesChanged(ILaunch[] launches) {
//		firePropertyChange(launches);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchesListener2#launchesTerminated(org.eclipse.debug.core.ILaunch[])
	 */
	@Override
	public void launchesTerminated(ILaunch[] launches) {
		firePropertyChange(launches);
	}

	/**
	 * Fires a property change event for the watched context if one of the given
	 * launches matches.
	 *
	 * @param launches The launches. Must not be <code>null</code>.
	 * @param unregister <code>True</code> to unregister the listener if a matching launch is found, <code>false</code> otherwise.
	 */
	protected void firePropertyChange(ILaunch[] launches) {
		Assert.isNotNull(launches);

		for (ILaunch launch : launches) {
			try {
				if (launch.getLaunchConfiguration() != null && launch.getLaunchConfiguration().getType() != null
						&& launch.getLaunchConfiguration().getType().getIdentifier() != null
						&& launch.getLaunchConfiguration().getType().getIdentifier().equals(ILaunchTypes.ATTACH)) {
					IModelNode[] contexts = LaunchContextsPersistenceDelegate.getLaunchContexts(launch.getLaunchConfiguration());
					if (contexts != null && contexts.length == 1 && contexts[0] != null) {
						contexts[0].fireChangeEvent("dbgLaunchedState", null, null); //$NON-NLS-1$
					}
				}
			} catch (CoreException e) {
				if (Platform.inDebugMode()) e.printStackTrace();
			}
		}
	}

}
