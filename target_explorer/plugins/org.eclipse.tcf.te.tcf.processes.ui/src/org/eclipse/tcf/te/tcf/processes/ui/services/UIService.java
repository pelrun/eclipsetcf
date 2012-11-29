/**
 * UIService.java
 * Created on Nov 15, 2012
 *
 * Copyright (c) 2012 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.tcf.processes.ui.services;

import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.services.interfaces.IUIService;
import org.eclipse.tcf.te.tcf.processes.ui.handler.PropertiesHandlerDelegate;
import org.eclipse.tcf.te.ui.interfaces.handler.IPropertiesHandlerDelegate;

/**
 * UI service implementation.
 */
public class UIService extends AbstractService implements IUIService {
	private final IPropertiesHandlerDelegate propertiesHandlerDelegate = new PropertiesHandlerDelegate();

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IUIService#getDelegate(java.lang.Object, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <V> V getDelegate(Object context, Class<? extends V> clazz) {

		if (IPropertiesHandlerDelegate.class.isAssignableFrom(clazz)) {
			return (V) propertiesHandlerDelegate;
		}

		return null;
	}

}
