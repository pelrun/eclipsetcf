/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.processes.core.persistence;

import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.IProcessContextItem;

/**
 * ProcessContextItem
 */
public class ProcessContextItem extends PropertiesContainer implements IProcessContextItem, Comparable<IProcessContextItem> {

	/**
	 * Constructor.
	 */
	public ProcessContextItem() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.core.interfaces.IProcessContextItem#getId()
	 */
	@Override
	public String getId() {
		return getStringProperty(PROPERTY_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.core.interfaces.IProcessContextItem#getName()
	 */
	@Override
	public String getName() {
		return getStringProperty(PROPERTY_NAME);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.core.interfaces.IProcessContextItem#getPath()
	 */
	@Override
	public String getPath() {
		return getStringProperty(PROPERTY_PATH);
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
    @Override
    public int compareTo(IProcessContextItem other) {
	    return toString().compareTo(other.toString());
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.runtime.properties.PropertiesContainer#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
    	if (obj instanceof IProcessContextItem) {
    		IProcessContextItem other = (IProcessContextItem)obj;
    		return toString().equals(other.toString());
    	}
        return super.equals(obj);
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.runtime.properties.PropertiesContainer#toString()
     */
    @Override
    public String toString() {
		String toString = getName() != null ? getName() : "unknown"; //$NON-NLS-1$
		if (getId() != null) {
			toString += " (" + getId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getPath() != null) {
			toString = getPath() + IProcessContextItem.PATH_SEPARATOR + toString;
		}
		return toString;
    }
}
