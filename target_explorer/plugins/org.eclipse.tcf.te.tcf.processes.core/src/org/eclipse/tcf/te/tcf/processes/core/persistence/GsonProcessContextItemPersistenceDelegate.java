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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.tcf.te.runtime.persistence.delegates.GsonMapPersistenceDelegate;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.IProcessContextItem;

/**
 * Process context item to string delegate implementation.
 */
public class GsonProcessContextItemPersistenceDelegate extends GsonMapPersistenceDelegate {

	/**
	 * Constructor.
	 */
	public GsonProcessContextItemPersistenceDelegate() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate#getPersistedClass(java.lang.Object)
	 */
	@Override
	public Class<?> getPersistedClass(Object context) {
		return IProcessContextItem.class;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.AbstractPropertiesPersistenceDelegate#toMap(java.lang.Object)
	 */
	@Override
	protected Map<String, Object> toMap(final Object context) throws IOException {
		IProcessContextItem item = getProcessContextItem(context);
		if (item != null) {
			return super.toMap(item.getProperties());
		}

		return new HashMap<String, Object>();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.AbstractPropertiesPersistenceDelegate#fromMap(java.util.Map, java.lang.Object)
	 */
	@Override
	protected Object fromMap(Map<String, Object> map, Object context) throws IOException {
		IProcessContextItem item = new ProcessContextItem();
		item.setProperties(map);
		return item;
	}

	/**
	 * Get a process context item from the given context.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @return The process context item or <code>null</code>.
	 */
	protected IProcessContextItem getProcessContextItem(Object context) {
		IProcessContextItem item = null;

		if (context instanceof IProcessContextItem) {
			item = (IProcessContextItem)context;
		}

		return item;
	}
}
