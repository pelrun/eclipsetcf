/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.model.services;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.model.interfaces.IContainerModelNode;
import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService;

/**
 * Process context node properties access service implementation.
 */
public class PropertiesAccessService extends AbstractService implements IPropertiesAccessService {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService#getTargetAddress(java.lang.Object)
	 */
	@Override
	public Map<String, String> getTargetAddress(Object context) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService#getProperty(java.lang.Object, java.lang.String)
	 */
	@Override
	public Object getProperty(final Object context, final String key) {
		Assert.isNotNull(context);
		Assert.isNotNull(key);

		final AtomicReference<Object> value = new AtomicReference<Object>();
		if (context instanceof IPropertiesContainer) {
			final IPropertiesContainer node = (IPropertiesContainer) context;

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					Object val = node.getProperty(key);
					value.set(val);
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);
		}

		return value.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService#setProperty(java.lang.Object, java.lang.String, java.lang.Object)
	 */
	@Override
	public boolean setProperty(final Object context, final String key, final Object value) {
		Assert.isNotNull(context);
		Assert.isNotNull(key);

		final AtomicBoolean result = new AtomicBoolean();
		if (context instanceof IPropertiesContainer) {
			final IPropertiesContainer node = (IPropertiesContainer) context;

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					result.set(node.setProperty(key, value));
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);
		}

		return result.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService#isProperty(java.lang.Object, java.lang.String, java.lang.Object)
	 */
	@Override
	public boolean isProperty(final Object context, final String key, final Object value) {
		Assert.isNotNull(context);
		Assert.isNotNull(key);

		final AtomicBoolean result = new AtomicBoolean();
		if (context instanceof IPropertiesContainer) {
			final IPropertiesContainer node = (IPropertiesContainer) context;

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					result.set(node.isProperty(key, value));
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);
		}

		return result.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(final Object context) {
		Assert.isNotNull(context);

		final AtomicReference<Object> value = new AtomicReference<Object>();
		if (context instanceof IContainerModelNode) {
			final IContainerModelNode node = (IContainerModelNode) context;

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					value.set(node.getParent());
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);
		}

		return value.get();
	}
}
