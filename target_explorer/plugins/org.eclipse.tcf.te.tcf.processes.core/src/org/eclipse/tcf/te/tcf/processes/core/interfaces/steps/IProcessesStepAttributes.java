/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.interfaces.steps;


/**
 * Defines the launch configuration attribute id's to access the launch step contexts.
 */
public interface IProcessesStepAttributes {

	/**
	 * Define the prefix used by all other attribute id's as prefix.
	 */
	public static final String ATTR_PREFIX = "org.eclipse.tcf.te.tcf.processes.core"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute: The process image.
	 */
	public static final String ATTR_PROCESS_IMAGE = ATTR_PREFIX + ".process_image"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute: The process arguments.
	 */
	public static final String ATTR_PROCESS_ARGUMENTS = ATTR_PREFIX + ".process_arguments"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute: The process library path.
	 */
	public static final String ATTR_PROCESS_LIBRARY_PATH = ATTR_PREFIX + ".process_library_path"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute: Flag if or if not to stop at process entry.
	 */
	public static final String ATTR_STOP_AT_ENTRY = ATTR_PREFIX + ".process_stop_at_entry"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute: Flag if or if not to stop at main.
	 */
	public static final String ATTR_STOP_AT_MAIN = ATTR_PREFIX + ".process_stop_at_main"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute: Flag if or if not to attach the process.
	 */
	public static final String ATTR_ATTACH = ATTR_PREFIX + ".process_attach"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute: Flag if or if not to attach process children.
	 */
	public static final String ATTR_ATTACH_CHILDREN = ATTR_PREFIX + ".process_attach_children"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute: Flag if or if not to redirect the output to the console.
	 */
	public static final String ATTR_OUTPUT_CONSOLE = ATTR_PREFIX + ".process_output_console"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute: The file name to redirect the output.
	 */
	public static final String ATTR_OUTPUT_FILE = ATTR_PREFIX + ".process_output_file"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute: The file name to redirect the output.
	 */
	public static final String ATTR_ADDITIONAL_PARAMETERS = ATTR_PREFIX + ".process_additional_parameters"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute (internal use): The process context object.
	 */
	public static final String ATTR_PROCESS_CONTEXT = ATTR_PREFIX + ".process_context"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute (internal use): The process context node object.
	 */
	public static final String ATTR_PROCESS_CONTEXT_NODE = ATTR_PREFIX + ".process_context_node"; //$NON-NLS-1$
}
