/*******************************************************************************
 * Copyright (c) 2016 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.launch;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.tcf.debug.ITCFLaunchProjectBuilder;
import org.eclipse.tcf.internal.debug.Activator;
import org.osgi.framework.Bundle;

/**
 * TCF implements building of a project before launching a debug session.
 * Default build logic might not work properly for some project type, e.g. CDT projects.
 * TCF clients can implement ITCFLaunchProjectBuilder to override default launch behavior.
 */
public class TCFLaunchProjectBuilder {

    public static ITCFLaunchProjectBuilder getLaunchProjectBuilder(ILaunchConfiguration configuration) {
        try {
            String project = configuration.getAttribute(TCFLaunchDelegate.ATTR_PROJECT_NAME, "");
            if (project.length() == 0) return null;
            IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(Activator.PLUGIN_ID, "launch_project_builder");
            if (point == null) return null;
            IExtension[] extensions = point.getExtensions();
            for (IExtension extension : extensions) {
                try {
                    Bundle bundle = Platform.getBundle(extension.getNamespaceIdentifier());
                    bundle.start(Bundle.START_TRANSIENT);
                    IConfigurationElement[] e = extension.getConfigurationElements();
                    for (int j = 0; j < e.length; j++) {
                        String nm = e[j].getName();
                        if (nm.equals("class")) { //$NON-NLS-1$
                            Class<?> c = bundle.loadClass(e[j].getAttribute("name")); //$NON-NLS-1$
                            ITCFLaunchProjectBuilder builder = (ITCFLaunchProjectBuilder)c.newInstance();
                            if (builder.isSupportedProject(project)) return builder;
                        }
                    }
                }
                catch (Throwable x) {
                    Activator.log("Cannot access launch project builder extension points", x);
                }
            }
        }
        catch (Exception x) {
            Activator.log("Cannot access launch project builder extension points", x);
        }
        return null;
    }
}
