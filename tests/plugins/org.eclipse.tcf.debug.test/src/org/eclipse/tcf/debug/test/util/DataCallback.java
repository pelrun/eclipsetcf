/*******************************************************************************
 * Copyright (c) 2006, 2012 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.debug.test.util;



/**
 * Copied and apdapted from org.eclipse.cdt.dsf.concurrent.
 *
 * Request monitor that allows data to be returned to the request initiator.
 *
 * @param V The type of the data object that this monitor handles.
 *
 * @since 1.0
 */
public class DataCallback<V> extends Callback {

    /** Data object reference */
    private V fData;

    public DataCallback() {
        this(null);
    }

    public DataCallback(Callback parentCallback) {
        super(parentCallback);
    }

    /**
     * Sets the data object to specified value.  To be called by the
     * asynchronous method implementor.
     * @param data Data value to set.
     */
    public synchronized void setData(V data) { fData = data; }

    /**
     * Returns the data value, null if not set.
     */
    public synchronized V getData() { return fData; }

    public void done(V data, Throwable error) {
        setData(data);
        done(error);
    }

    @Override
    public String toString() {
        if (getData() != null) {
            return getData().toString();
        } else {
            return super.toString();
        }
    }
}
