/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.internal.services;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;

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
		if (context instanceof IProcessContextNode) {
			final IProcessContextNode node = (IProcessContextNode) context;

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
		if (context instanceof IPeerModel) {
			final IProcessContextNode node = (IProcessContextNode) context;

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
		if (context instanceof IPeerModel) {
			final IProcessContextNode node = (IProcessContextNode) context;

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
		if (context instanceof IProcessContextNode) {
			final IProcessContextNode node = (IProcessContextNode) context;

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
