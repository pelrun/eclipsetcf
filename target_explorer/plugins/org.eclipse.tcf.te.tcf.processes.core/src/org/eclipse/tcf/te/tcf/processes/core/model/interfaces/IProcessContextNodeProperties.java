/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
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

	/**
	 * The command line of the context. The command line is a string array.
	 */
	public static final String PROPERTY_CMD_LINE = "cmdline"; //$NON-NLS-1$

	/**
	 * The capabilities the "ProcessesV1" service provides for the given process id.
	 * <p>
	 * The property data is a <code>Map&lt;String, Object&gt;</code>.
	 */
	public static final String PROPERTY_CAPABILITIES = "capabilities"; //$NON-NLS-1$

	/**
	 * The property key base to access the parameter lists for the provided capabilities.
	 * <p>
	 * The full property key is <code>PROPERTY_PARAMETER_LIST.&lt;command&gt;</code>.
	 * <p>
	 * The property data is a <code>List&lt;Map&lt;String, Object&gt;&gt;</code>.
	 */
	public static final String PROPERTY_PARAMETER_LIST = "parameterList"; //$NON-NLS-1$

	/**
	 * Property is set if the context becomes invalid during a refresh.
	 */
	public static final String PROPERTY_INVALID_CTX = "invalidCtx"; //$NON-NLS-1$
}
