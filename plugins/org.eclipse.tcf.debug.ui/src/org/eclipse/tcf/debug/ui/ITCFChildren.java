/*******************************************************************************
 * Copyright (c) 2012 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.debug.ui;

import java.util.Map;

/**
 * ITCFChildren represents a special type of TCF data cache that is
 * used to cache a list of children of a model object.
 */
public interface ITCFChildren {

    /**
     * @return true if cache contains up-to-date data or error.
     */
    boolean isValid();

    /**
     * If the cache is not valid, initiate data retrieval and
     * add a client call-back to cache wait list.
     * Client call-backs are activated when cache state changes.
     * Call-backs are removed from waiting list after that.
     * It is responsibility of clients to check if the state change was one they are waiting for.
     * If the cache is valid do nothing and return true.
     * @param cb - a call-back object
     * @return true if the cache is already valid
     */
    boolean validate(Runnable cb);

    /**
     * Get the cached data - the list of children objects.
     * @return the list of children.
     * Note: It is prohibited to call this method when the cache is not valid.
     */
    Map<String,ITCFObject> getChildren();

    /**
     * @return error object if data retrieval ended with an error,
     * or null if retrieval was successful.
     * Note: It is prohibited to call this method when the cache is not valid.
     */
    Throwable getError();
}
