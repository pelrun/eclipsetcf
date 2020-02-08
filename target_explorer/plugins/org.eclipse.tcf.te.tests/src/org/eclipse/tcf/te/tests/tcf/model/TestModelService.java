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

import org.eclipse.tcf.te.tcf.core.model.interfaces.IModel;
import org.eclipse.tcf.te.tcf.core.model.services.AbstractModelService;

/**
 * Test model service implementation
 */
public class TestModelService extends AbstractModelService<IModel> {

	/**
	 * Constructor.
	 *
	 * @param model The parent model. Must not be <code>null</code>.
	 */
	public TestModelService(IModel model) {
		super(model);

	}

}
