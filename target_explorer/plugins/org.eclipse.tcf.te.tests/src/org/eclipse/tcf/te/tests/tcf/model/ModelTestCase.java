/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf.model;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.tcf.te.tcf.core.model.interfaces.IModel;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelService;
import org.eclipse.tcf.te.tests.CoreTestCase;

/**
 * TCF model test cases.
 */
@SuppressWarnings("restriction")
public class ModelTestCase extends CoreTestCase {

	/**
	 * Provides a test suite to the caller which combines all single
	 * test bundled within this category.
	 *
	 * @return Test suite containing all test for this test category.
	 */
	public static Test getTestSuite() {
		TestSuite testSuite = new TestSuite("Test TCF model framework"); //$NON-NLS-1$

			// add ourself to the test suite
			testSuite.addTestSuite(ModelTestCase.class);

		return testSuite;
	}

	//***** BEGIN SECTION: Single test methods *****
	//NOTE: All method which represents a single test case must
	//      start with 'test'!

    public void testModel() {
		// Create the TCF test model instance
		IModel model = new TestModel();
		assertNotNull("Failed to create test model instance", model); //$NON-NLS-1$
		assertFalse("ModelManager is disposed but should not", model.isDisposed()); //$NON-NLS-1$

		// Get the test model service
		IModelService service = model.getService(TestModelService.class);
		assertNotNull("Failed to access test model service instance", service); //$NON-NLS-1$
		assertSame("Test service instance is associated with a different model instance", model, service.getModel()); //$NON-NLS-1$

		// Dispose the model
		model.dispose();
		assertTrue("ModelManager is not disposed but should", model.isDisposed()); //$NON-NLS-1$

		org.eclipse.tcf.te.tcf.core.model.activator.CoreBundleActivator.getContext();
		org.eclipse.tcf.te.tcf.core.model.activator.CoreBundleActivator.getUniqueIdentifier();
	}

	//***** END SECTION: Single test methods *****

}
