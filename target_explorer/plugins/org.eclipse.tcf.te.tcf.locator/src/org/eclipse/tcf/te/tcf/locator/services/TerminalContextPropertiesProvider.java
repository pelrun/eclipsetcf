/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.services;

import java.util.Map;

import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalContextPropertiesProvider;

/**
 * Terminal context properties provider implementation.
 */
public class TerminalContextPropertiesProvider implements ITerminalContextPropertiesProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.core.terminals.interfaces.ITerminalContextPropertiesProvider#getTargetAddress(java.lang.Object)
	 */
	@Override
	public Map<String, String> getTargetAddress(Object context) {
		IPropertiesAccessService service = ServiceManager.getInstance().getService(context, IPropertiesAccessService.class);
		if (service != null) {
			return service.getTargetAddress(context);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.core.terminals.interfaces.ITerminalContextPropertiesProvider#getProperty(java.lang.Object, java.lang.String)
	 */
	@Override
	public Object getProperty(Object context, String key) {
		IPropertiesAccessService service = ServiceManager.getInstance().getService(context, IPropertiesAccessService.class);
		if (service != null) {
			return service.getProperty(context, key);
		}
		return null;
	}

}
