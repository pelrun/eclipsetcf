/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.core.internal.adapters;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.context.AbstractStepContext;

/**
 * Launch step context adapter implementation.
 */
public class LaunchStepContext extends AbstractStepContext {

	/**
	 * Constructor.
	 *
	 * @param launch The launch. Must not be <code>null</code>.
	 */
	public LaunchStepContext(ILaunch launch) {
		super(launch);
	}

	/**
	 * Returns the launch.
	 * @return The launch.
	 */
	public ILaunch getLaunch() {
		return (ILaunch)getContextObject();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext#getId()
	 */
	@Override
	public String getId() {
		return getLaunch().getLaunchConfiguration() != null ? getLaunch().getLaunchConfiguration().getName() : null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext#getSecondaryId()
	 */
	@Override
	public String getSecondaryId() {
		return getLaunch().getLaunchMode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext#getName()
	 */
	@Override
	public String getName() {
		return getId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext#getInfo(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public String getInfo(IPropertiesContainer data) {
		try {
			return getName() + "(" + getLaunch().getLaunchMode() + ") - " + getLaunch().getLaunchConfiguration().getType().getName(); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (CoreException e) {
		}
		return getName() + "(" + getLaunch().getLaunchMode() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(final Class adapter) {
		if (ILaunch.class.equals(adapter)) {
			return getLaunch();
		}

		if (ILaunchConfiguration.class.isAssignableFrom(adapter)) {
			return getLaunch().getLaunchConfiguration();
		}

		if (ILaunchConfigurationType.class.isAssignableFrom(adapter)) {
			try {
				return getLaunch().getLaunchConfiguration().getType();
			}
			catch (CoreException e) {
			}
		}

		return super.getAdapter(adapter);
	}
}
