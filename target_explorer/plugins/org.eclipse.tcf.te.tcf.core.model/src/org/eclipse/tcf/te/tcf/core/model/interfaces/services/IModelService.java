/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.model.interfaces.services;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.tcf.te.tcf.core.model.interfaces.IModel;


/**
 * Common interface to be implemented by all model services.
 */
public interface IModelService extends IAdaptable {

	/**
	 * Returns the parent model.
	 *
	 * @return The parent model.
	 */
	public IModel getModel();
}
