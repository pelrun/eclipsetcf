/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.processes.core.persistence;

import java.util.List;

import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.IProcessContextItem;

/**
 * ProcessContextItem
 */
public class ProcessContextItem extends PropertiesContainer implements IProcessContextItem {

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
	public String[] getPath() {
		Object path = getProperty(PROPERTY_PATH);
		if (path instanceof String[]) {
			return (String[])path;
		}
		if (path instanceof List<?>) {
			return ((List<?>)path).toArray(new String[((List<?>)path).size()]);
		}
		return null;
	}

}
