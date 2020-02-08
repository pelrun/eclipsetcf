/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.persistence.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.tcf.te.runtime.extensions.AbstractExtensionPointManager;
import org.eclipse.tcf.te.runtime.extensions.ExecutableExtensionProxy;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IVariableProvider;


/**
 * VariableProviderExtensionPointManager
 */
public class VariableProviderExtensionPointManager extends AbstractExtensionPointManager<IVariableProvider> {

	/*
	 * Thread save singleton instance creation.
	 */
	private static class LazyInstance {
		public static VariableProviderExtensionPointManager instance = new VariableProviderExtensionPointManager();
	}

	/**
	 * Constructor.
	 */
	VariableProviderExtensionPointManager() {
		super();
	}

	/**
	 * Returns the singleton instance of the extension point manager.
	 */
	public static VariableProviderExtensionPointManager getInstance() {
		return LazyInstance.instance;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.extensions.AbstractExtensionPointManager#getExtensionPointId()
	 */
	@Override
	protected String getExtensionPointId() {
		return "org.eclipse.tcf.te.runtime.persistence.variableProviders"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.extensions.AbstractExtensionPointManager#getConfigurationElementName()
	 */
	@Override
	protected String getConfigurationElementName() {
		return "provider"; //$NON-NLS-1$
	}

	/**
	 * Returns all variable providers.
	 *
	 * @return The list of variable providers or an empty array.
	 */
	public IVariableProvider[] getProviders() {
		List<IVariableProvider> contributions = new ArrayList<IVariableProvider>();
		Collection<ExecutableExtensionProxy<IVariableProvider>> delegates = getExtensions().values();
		for (ExecutableExtensionProxy<IVariableProvider> delegate : delegates) {
			IVariableProvider instance = delegate.getInstance();
			if (instance != null && !contributions.contains(instance)) {
				contributions.add(instance);
			}
		}
		return contributions.toArray(new IVariableProvider[contributions.size()]);
	}
}
