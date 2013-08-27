/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.core.internal.services;

import java.util.ArrayList;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate;
import org.eclipse.tcf.services.IPathMap.PathMapRule;
import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService;

/**
 * Path map service implementation.
 */
public class PathMapService extends AbstractService implements IPathMapService {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService#getPathMap(java.lang.Object)
	 */
	@Override
	public PathMapRule[] getPathMap(Object context) {
		Assert.isNotNull(context);

		PathMapRule[] rules = null;

		// Get the launch configuration for that peer model
		ILaunchConfiguration config = (ILaunchConfiguration) Platform.getAdapterManager().getAdapter(context, ILaunchConfiguration.class);
		if (config == null) {
			config = (ILaunchConfiguration) Platform.getAdapterManager().loadAdapter(context, "org.eclipse.debug.core.ILaunchConfiguration"); //$NON-NLS-1$
		}

		if (config != null) {
			try {
				String path_map_cfg = config.getAttribute(TCFLaunchDelegate.ATTR_PATH_MAP, ""); //$NON-NLS-1$
				ArrayList<org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.PathMapRule> map = TCFLaunchDelegate.parsePathMapAttribute(path_map_cfg);
				if (map != null && !map.isEmpty()) {
					rules = map.toArray(new PathMapRule[map.size()]);
				}
			} catch (CoreException e) { /* ignored on purpose */ }
		}

		return rules;
	}

}
