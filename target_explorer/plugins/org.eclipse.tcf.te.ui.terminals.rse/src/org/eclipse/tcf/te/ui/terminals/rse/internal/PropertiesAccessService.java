/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.rse.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rse.core.model.IHost;
import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService;
import org.eclipse.tcf.te.runtime.services.interfaces.constants.IPropertiesAccessServiceConstants;

/**
 * Properties access service implementation.
 */
public class PropertiesAccessService extends AbstractService implements IPropertiesAccessService {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService#getTargetAddress(java.lang.Object)
	 */
	@Override
	public Map<String, String> getTargetAddress(Object context) {
		if (context instanceof IHost) {
			IHost host = (IHost) context;

			Map<String, String> props = new HashMap<String, String>();
			props.put(IPropertiesAccessServiceConstants.PROP_ADDRESS, host.getHostName());
			props.put(IPropertiesAccessServiceConstants.PROP_NAME, host.getName());

			return props;
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService#getProperty(java.lang.Object, java.lang.String)
	 */
	@Override
	public Object getProperty(Object context, String key) {
		if (context instanceof IHost) {
			IHost host = (IHost) context;

			if (IPropertiesAccessServiceConstants.PROP_DEFAULT_USER.equals(key)) {
				String user = host.getDefaultUserId();
				if (user != null && !"".equals(user.trim())) { //$NON-NLS-1$
					return user;
				}
			}

			if (IPropertiesAccessServiceConstants.PROP_DEFAULT_ENCODING.equals(key)) {
				String encoding = host.getDefaultEncoding(true);
				if (encoding != null && !"".equals(encoding)) { //$NON-NLS-1$
					return encoding;
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService#setProperty(java.lang.Object, java.lang.String, java.lang.Object)
	 */
	@Override
	public boolean setProperty(Object context, String key, Object value) {
		// Changing the properties via this API is not supported.
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService#isProperty(java.lang.Object, java.lang.String, java.lang.Object)
	 */
	@Override
	public boolean isProperty(Object context, String key, Object value) {
		if (context instanceof IHost) {
			IHost host = (IHost) context;

			if (IPropertiesAccessServiceConstants.PROP_DEFAULT_USER.equals(key)) {
				return value == null ? host.getDefaultUserId() == null : value.equals(host.getDefaultUserId());
			}
			if (IPropertiesAccessServiceConstants.PROP_DEFAULT_ENCODING.equals(key)) {
				return value == null ? host.getDefaultEncoding(true) == null : value.equals(host.getDefaultEncoding(true));
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object context) {
		return null;
	}

}
