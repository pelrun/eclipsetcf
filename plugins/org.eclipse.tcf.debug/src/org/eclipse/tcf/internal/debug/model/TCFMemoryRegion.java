/*******************************************************************************
 * Copyright (c) 2010, 2013 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.model;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.services.IMemoryMap;

/**
 * A comparable extension of TCFMemoryRegion.
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public class TCFMemoryRegion extends org.eclipse.tcf.util.TCFMemoryRegion implements Comparable<TCFMemoryRegion> {

    public final BigInteger addr;
    public final BigInteger size;

    public TCFMemoryRegion(Map<String,Object> props) {
        super(props);
        this.addr = JSON.toBigInteger((Number)props.get(IMemoryMap.PROP_ADDRESS));
        this.size = JSON.toBigInteger((Number)props.get(IMemoryMap.PROP_SIZE));
    }

    public int compareTo(TCFMemoryRegion r) {
        if (addr != r.addr) {
            if (addr == null) return -1;
            if (r.addr == null) return +1;
            int n = addr.compareTo(r.addr);
            if (n != 0) return n;
        }
        if (size != r.size) {
            if (size == null) return -1;
            if (r.size == null) return +1;
            int n = size.compareTo(r.size);
            if (n != 0) return n;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TCFMemoryRegion)) return false;
        TCFMemoryRegion r = (TCFMemoryRegion)o;
        if (compareTo(r) != 0) return false;
        Map<String,Object> x = getProperties();
        Map<String,Object> y = r.getProperties();
        if (x.size() != y.size()) return false;
        Iterator<Entry<String,Object>> i = x.entrySet().iterator();
        while (i.hasNext()) {
            Entry<String,Object> e = i.next();
            String key = e.getKey();
            if (key != null) {
                if (key.equals(IMemoryMap.PROP_ADDRESS)) continue;
                if (key.equals(IMemoryMap.PROP_SIZE)) continue;
            }
            Object val = e.getValue();
            if (val == null) {
                if (y.get(key) != null || !y.containsKey(key)) return false;
            }
            else {
                if (!val.equals(y.get(key))) return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (addr == null) return 0;
        return addr.hashCode();
    }
}
