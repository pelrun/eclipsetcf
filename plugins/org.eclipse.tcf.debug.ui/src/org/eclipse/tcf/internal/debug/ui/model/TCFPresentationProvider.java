/*******************************************************************************
 * Copyright (c) 2012, 2013 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import java.util.ArrayList;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tcf.debug.ui.ITCFPresentationProvider;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.osgi.framework.Bundle;

/**
 * TCF clients can implement ITCFPresentationProvider to participate in presentation of TCF debug model.
 * The implementation is registered through extension point: org.eclipse.tcf.debug.ui.presentation_provider
 */
public class TCFPresentationProvider {

    private static ArrayList<ITCFPresentationProvider> providers;

    public static Iterable<ITCFPresentationProvider> getPresentationProviders() {
        if (providers == null) {
            ArrayList<ITCFPresentationProvider> list = new ArrayList<ITCFPresentationProvider>();
            try {
                IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(
                        Activator.PLUGIN_ID, "presentation_provider"); //$NON-NLS-1$
                IExtension[] extensions = point.getExtensions();
                for (int i = 0; i < extensions.length; i++) {
                    try {
                        Bundle bundle = Platform.getBundle(extensions[i].getContributor().getName());
                        bundle.start(Bundle.START_TRANSIENT);
                        IConfigurationElement[] e = extensions[i].getConfigurationElements();
                        for (int j = 0; j < e.length; j++) {
                            String nm = e[j].getName();
                            if (nm.equals("class")) { //$NON-NLS-1$
                                Class<?> c = bundle.loadClass(e[j].getAttribute("name")); //$NON-NLS-1$
                                list.add((ITCFPresentationProvider)c.newInstance());
                            }
                        }
                    }
                    catch (Throwable x) {
                        Activator.log("Cannot access presentation provider extension points", x);
                    }
                }
            }
            catch (Exception x) {
                Activator.log("Cannot access presentation provider extension points", x);
            }
            providers = list;
        }
        return providers;
    }
}
