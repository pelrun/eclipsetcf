/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.notifications.internal.factory;

import java.util.Collection;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.runtime.extensions.AbstractExtensionPointManager;
import org.eclipse.tcf.te.runtime.extensions.ExecutableExtensionProxy;
import org.eclipse.tcf.te.ui.notifications.delegates.DefaultFormTextFactoryDelegate;
import org.eclipse.tcf.te.ui.notifications.interfaces.IFormTextFactoryDelegate;


/**
 * Notification form text factory delegate extension point manager implementation.
 */
public class FactoryDelegateManager extends AbstractExtensionPointManager<IFormTextFactoryDelegate> {
	private final IFormTextFactoryDelegate defaultDelegate = new DefaultFormTextFactoryDelegate();

	/*
	 * Thread save singleton instance creation.
	 */
	private static class LazyInstance {
		public static FactoryDelegateManager instance = new FactoryDelegateManager();
	}

	/**
	 * Constructor.
	 */
	FactoryDelegateManager() {
		super();
	}

	/**
	 * Returns the singleton instance of the notification form text factory delegate manager.
	 */
	public static FactoryDelegateManager getInstance() {
		return LazyInstance.instance;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.extensions.AbstractExtensionPointManager#getExtensionPointId()
	 */
	@Override
	protected String getExtensionPointId() {
		return "org.eclipse.tcf.te.ui.notifications.factoryDelegates"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.extensions.AbstractExtensionPointManager#getConfigurationElementName()
	 */
	@Override
	protected String getConfigurationElementName() {
		return "delegate"; //$NON-NLS-1$
	}

	/**
	 * Returns the notification form text factory delegate matching the given id.
	 *
	 * @param id The notification form text factory delegate id. Must not be <code>null</code>.
	 * @return The notification form text factory delegate or <code>null</code>.
	 */
	public IFormTextFactoryDelegate getFactoryDelegate(String id) {
		Assert.isNotNull(id);

		IFormTextFactoryDelegate delegate = null;

		Collection<ExecutableExtensionProxy<IFormTextFactoryDelegate>> delegates = getExtensions().values();
		for (ExecutableExtensionProxy<IFormTextFactoryDelegate> candidate : delegates) {
			if (id.equals(candidate.getId())) {
				delegate = candidate.getInstance();
				break;
			}
		}

		return delegate;
	}

	/**
	 * Returns the default notification form text factory delegate.
	 *
	 * @return The default notification form text factory delegate.
	 */
	public IFormTextFactoryDelegate getDefaultFactoryDelegate() {
		return defaultDelegate;
	}
}
