/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.debug.test.services;

import org.eclipse.tcf.debug.test.util.ICache;

/**
 *
 */
public interface IWaitForEventCache<V> extends ICache<V> {

    /**
     * Resets the event cache so that it will be called again upon the next event.
     */
    public void reset();
}
