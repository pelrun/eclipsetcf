/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.stepper.context;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;

/**
 * Abstract step context implementation.
 */
public abstract class AbstractStepContext extends PlatformObject implements IStepContext {

	private final Object contextObject;

	/**
	 * Constructor.
	 *
	 * @param contextObject The context object. Must not be <code>null</code>.
	 */
	public AbstractStepContext(Object contextObject) {
		super();
		Assert.isNotNull(contextObject);
		this.contextObject = contextObject;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext#getSecondaryId()
	 */
	@Override
	public String getSecondaryId() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext#getName()
	 */
	@Override
	public String getName() {
		return getId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext#getContextObject()
	 */
	@Override
	public Object getContextObject() {
	    return contextObject;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext#getInfo(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public String getInfo(IPropertiesContainer data) {
		return getName() + "(" + getId() + (getSecondaryId() != null ? ":" + getSecondaryId() : "") + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
}
