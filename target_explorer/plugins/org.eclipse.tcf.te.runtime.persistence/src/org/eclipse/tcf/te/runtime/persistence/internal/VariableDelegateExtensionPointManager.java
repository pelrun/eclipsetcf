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
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IVariableDelegate;


/**
 * VariableDelegateExtensionPointManager
 */
public class VariableDelegateExtensionPointManager extends AbstractExtensionPointManager<IVariableDelegate> {

	/*
	 * Thread save singleton instance creation.
	 */
	private static class LazyInstance {
		public static VariableDelegateExtensionPointManager instance = new VariableDelegateExtensionPointManager();
	}

	/**
	 * Constructor.
	 */
	VariableDelegateExtensionPointManager() {
		super();
	}

	/**
	 * Returns the singleton instance of the extension point manager.
	 */
	public static VariableDelegateExtensionPointManager getInstance() {
		return LazyInstance.instance;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.extensions.AbstractExtensionPointManager#getExtensionPointId()
	 */
	@Override
	protected String getExtensionPointId() {
		return "org.eclipse.tcf.te.runtime.persistence.variableDelegates"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.extensions.AbstractExtensionPointManager#getConfigurationElementName()
	 */
	@Override
	protected String getConfigurationElementName() {
		return "delegate"; //$NON-NLS-1$
	}

	/**
	 * Returns the bound variable delegates for the given persistence delegate.
	 *
	 * @param persistenceDelegate The persistence delegate.
	 * @return The list of bound variable delegates or an empty array.
	 */
	public IVariableDelegate[] getDelegates(IPersistenceDelegate persistenceDelegate) {
		List<IVariableDelegate> contributions = new ArrayList<IVariableDelegate>();
		Collection<ExecutableExtensionProxy<IVariableDelegate>> delegates = getExtensions().values();
		for (ExecutableExtensionProxy<IVariableDelegate> delegate : delegates) {
			String id = delegate.getConfigurationElement().getAttribute("delegateId");  //$NON-NLS-1$
			if (id == null || id.equals(persistenceDelegate.getId())) {
				IVariableDelegate instance = delegate.getInstance();
				if (instance != null && !contributions.contains(instance)) {
					contributions.add(instance);
				}
			}
		}

		return contributions.toArray(new IVariableDelegate[contributions.size()]);
	}
}
