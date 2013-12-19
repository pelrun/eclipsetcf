/**
 * ProcessPropertiesFilter.java
 * Created on Sep 14, 2013
 *
 * Copyright (c) 2013 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.tcf.processes.ui.internal.tabbed;

import org.eclipse.core.runtime.Platform;

/**
 * The filter to filter out non process nodes.
 */
public class ProcessPropertiesFilter extends ProcessContextFilter {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IFilter#select(java.lang.Object)
	 */
	@Override
	public boolean select(Object toTest) {
		return Platform.inDebugMode() && super.select(toTest);
	}

}
