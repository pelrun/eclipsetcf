/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.tables.properties;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;

/**
 * Immutable representation of a table node.
 */
public final class NodePropertiesTableTableNode extends PlatformObject {
	/**
	 * The node name.
	 */
	public final String name;

	/**
	 * The node value.
	 */
	public final String value;

	/**
	 * Constructor.
	 *
	 * @param name The node name. Must not be <code>null</code>.
	 * @param value The node value. Must not be <code>null</code>.
	 */
	public NodePropertiesTableTableNode(String name, String value) {
		Assert.isNotNull(name);
		Assert.isNotNull(value);

		this.name = name;
		this.value = value;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
	    boolean equals = super.equals(obj);

	    if (!equals && obj instanceof NodePropertiesTableTableNode) {
	    	return name.equals(((NodePropertiesTableTableNode)obj).name);
	    }

	    return equals;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
	    return name.hashCode();
	}
}
