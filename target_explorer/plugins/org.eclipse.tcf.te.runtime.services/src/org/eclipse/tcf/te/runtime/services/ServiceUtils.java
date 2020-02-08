/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.runtime.services;

import org.eclipse.tcf.te.runtime.services.interfaces.IDelegateService;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.services.interfaces.IUIService;

/**
 * ServiceUtils
 */
public final class ServiceUtils {

	public static <V extends Object> V getUIServiceDelegate(Object context, Object delegateContext, Class<? extends V> delegateClass) {
		IService[] services = ServiceManager.getInstance().getServices(context, IUIService.class, false);
		for (IService service : services) {
	        V delegate = ((IUIService)service).getDelegate(delegateContext, delegateClass);
	        if (delegate != null) {
	        	return delegate;
	        }
        }

		return null;
	}

	public static <V extends Object> V getDelegateServiceDelegate(Object context, Object delegateContext, Class<? extends V> delegateClass) {
		IService[] services = ServiceManager.getInstance().getServices(context, IDelegateService.class, false);
		for (IService service : services) {
	        V delegate = ((IDelegateService)service).getDelegate(delegateContext, delegateClass);
	        if (delegate != null) {
	        	return delegate;
	        }
        }

		return null;
	}

}
