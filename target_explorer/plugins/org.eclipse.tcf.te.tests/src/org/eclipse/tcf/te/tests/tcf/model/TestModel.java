/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf.model;

import org.eclipse.tcf.te.runtime.model.ContainerModelNode;
import org.eclipse.tcf.te.runtime.model.factory.Factory;
import org.eclipse.tcf.te.runtime.model.interfaces.factory.IFactory;
import org.eclipse.tcf.te.tcf.core.model.interfaces.IModel;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelService;

/**
 * Test TCF model implementation.
 */
public class TestModel extends ContainerModelNode implements IModel {
	// Flag to mark the model disposed
	private boolean disposed;
	// Reference to the model node factory
	private IFactory factory = null;

	// The test model service
	private final TestModelService service = new TestModelService(this);

	/**
     * Constructor.
     */
    public TestModel() {
    	disposed = false;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.model.interfaces.IModel#dispose()
	 */
    @Override
    public void dispose() {
    	disposed = true;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.model.interfaces.IModel#isDisposed()
	 */
    @Override
    public boolean isDisposed() {
        return disposed;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.model.interfaces.IModel#getService(java.lang.Class)
	 */
    @SuppressWarnings("unchecked")
    @Override
    public <V extends IModelService> V getService(Class<V> serviceInterface) {
		return (V)getAdapter(serviceInterface);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (TestModelService.class.equals(adapter)) {
			return service;
		}
		return super.getAdapter(adapter);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.model.interfaces.IModel#setFactory(org.eclipse.tcf.te.runtime.model.interfaces.factory.IFactory)
	 */
    @Override
    public void setFactory(IFactory factory) {
		this.factory = factory;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.model.interfaces.IModel#getFactory()
	 */
    @Override
    public IFactory getFactory() {
		return factory != null ? factory : Factory.getInstance();
    }

}