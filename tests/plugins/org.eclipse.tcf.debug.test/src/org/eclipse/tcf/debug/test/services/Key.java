/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.debug.test.services;


abstract class Key<V> {
    private Class<V> fCacheClass;

    public Key(Class<V> cacheClass) {
        fCacheClass = cacheClass;
    }

    abstract V createCache();

    public Class<V> getCacheClass() {
        return fCacheClass;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Key) {
            return ((Key<?>)obj).fCacheClass.equals(fCacheClass);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return fCacheClass.hashCode();
    }
}
