/*******************************************************************************
 * Copyright (c) 2008, 2011 Anyware Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Anyware Technologies  - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.protocol;

/**
 * Clients can implement this interface if they want to provide implementation of a local service or
 * remote service proxy.
 */
public interface IServiceProvider {

    /**
     * Get the local services available in the given channel
     * @param channel channel
     * @return an array of services
     */
    public IService[] getLocalService(IChannel channel);

    /**
     * Get the service o in the given channel for the given service name
     * @param channel
     * @param service_name
     * @return the service
     */
    public IService getServiceProxy(IChannel channel, String service_name);
}
