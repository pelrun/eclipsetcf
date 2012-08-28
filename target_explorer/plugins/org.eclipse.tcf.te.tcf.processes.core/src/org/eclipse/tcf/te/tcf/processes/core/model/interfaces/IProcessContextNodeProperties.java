/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.interfaces;

import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;

/**
 * Process context node property constants.
 */
public interface IProcessContextNodeProperties {

	/**
	 * The context id. Used to identify the context to the "Processes" service.
	 */
	public static final String PROPERTY_ID = IModelNode.PROPERTY_ID;

	/**
	 * The context name. If set, used in the UI to represent the context node.
	 */
	public static final String PROPERTY_NAME = IModelNode.PROPERTY_NAME;
}
