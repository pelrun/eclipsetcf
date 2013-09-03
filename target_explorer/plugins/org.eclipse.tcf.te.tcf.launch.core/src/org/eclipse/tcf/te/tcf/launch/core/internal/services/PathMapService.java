/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.core.internal.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IPathMap;
import org.eclipse.tcf.services.IPathMap.PathMapRule;
import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.core.interfaces.IPathMapGeneratorService;
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
    	Assert.isTrue(!Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(context);

		PathMapRule[] rules = null;
		List<PathMapRule> rulesList = new ArrayList<PathMapRule>();

		// Get the launch configuration for that peer model
		ILaunchConfiguration config = (ILaunchConfiguration) Platform.getAdapterManager().getAdapter(context, ILaunchConfiguration.class);
		if (config == null) {
			config = (ILaunchConfiguration) Platform.getAdapterManager().loadAdapter(context, "org.eclipse.debug.core.ILaunchConfiguration"); //$NON-NLS-1$
		}

		if (config != null) {
			try {

				String path_map_cfg = config.getAttribute(org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.ATTR_PATH_MAP, ""); //$NON-NLS-1$
				rulesList.addAll(org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.parsePathMapAttribute(path_map_cfg));

				path_map_cfg = config.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, ""); //$NON-NLS-1$
				rulesList.addAll(org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.parseSourceLocatorMemento(path_map_cfg));
			} catch (CoreException e) { /* ignored on purpose */ }
		}

        IPathMapGeneratorService generator = ServiceManager.getInstance().getService(context, IPathMapGeneratorService.class);
        if (generator != null) {
        	PathMapRule[] generatedRules = generator.getPathMap(context);
        	if (generatedRules != null && generatedRules.length > 0) {
        		rulesList.addAll(Arrays.asList(generatedRules));
        	}
        }

		if (!rulesList.isEmpty()) {
	        int cnt = 0;
	        String id = getClientID();
	        for (PathMapRule r : rulesList) r.getProperties().put(IPathMap.PROP_ID, id + "/" + cnt++); //$NON-NLS-1$
			rules = rulesList.toArray(new PathMapRule[rulesList.size()]);
		}

		return rules;
	}

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService#getClientID()
     */
    @SuppressWarnings("restriction")
    @Override
    public String getClientID() {
        return org.eclipse.tcf.internal.debug.Activator.getClientID();
    }
}
