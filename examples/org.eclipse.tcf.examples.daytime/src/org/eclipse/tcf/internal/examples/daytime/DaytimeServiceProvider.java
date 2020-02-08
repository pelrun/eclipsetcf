/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.examples.daytime;

import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IService;
import org.eclipse.tcf.protocol.IServiceProvider;

/**
 * Daytime service provider implementation.
 */
public class DaytimeServiceProvider implements IServiceProvider {

    /* (non-Javadoc)
     * @see org.eclipse.tcf.protocol.IServiceProvider#getLocalService(org.eclipse.tcf.protocol.IChannel)
     */
    public IService[] getLocalService(IChannel channel) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.protocol.IServiceProvider#getServiceProxy(org.eclipse.tcf.protocol.IChannel, java.lang.String)
     */
    public IService getServiceProxy(IChannel channel, String service_name) {
        if (IDaytimeService.NAME.equals(service_name)) {
            return new DaytimeServiceProxy(channel);
    }
    return null;
    }

}
