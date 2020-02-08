/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.statushandler.interfaces;

/**
 * Status handler constants.
 */
public interface IStatusHandlerConstants {

	/**
	 * The id of the default status handler.
	 */
	public final static String ID_DEFAUT_HANDLER = "org.eclipse.tcf.te.statushandler.default"; //$NON-NLS-1$

	/**
	 * The status to handle is a question (yes/no) (value 0x100).
	 */
	public final static int QUESTION = 0x100;

	/**
	 * The status to handle is a question (yes/no) with cancel (value 0x200).
	 */
	public final static int YES_NO_CANCEL = 0x200;

	/**
	 * Property: The title of the message dialog.
	 *           The value is expected to be a string.
	 */
	public final static String PROPERTY_TITLE = "title"; //$NON-NLS-1$

	/**
	 * Property: An string array listing the labels of the message dialog buttons.
	 *           If <code>null</code>, the default labeling, typically &quot;OK&quot;
	 *           for a single button message dialog, will be applied.
	 */
	public final static String PROPERTY_BUTTON_LABEL = "buttonLabel"; //$NON-NLS-1$

	/**
	 * Property: The context help id of the message dialog.
	 *           The value is expected to be a string.
	 */
	public final static String PROPERTY_CONTEXT_HELP_ID = "contextHelpId"; //$NON-NLS-1$

	/**
	 * Property: The preference slot id for the &quot;don't ask again&quot; checkbox.
	 *           The value is expected to be a string.
	 */
	public final static String PROPERTY_DONT_ASK_AGAIN_ID = "dontAskAgainId"; //$NON-NLS-1$

	/**
	 * Property: The caller of the status handler. The value is expected to
	 *           be the caller object or the callers class object.
	 */
	public final static String PROPERTY_CALLER = "caller"; //$NON-NLS-1$

	/**
	 * Property: The result of the status handling.
	 */
	public final static String PROPERTY_RESULT = "result"; //$NON-NLS-1$

	/**
	 * Property: The text to be shown in the Details section of the Dialog.
	 */
	public final static String PROPERTY_DETAILS_TEXT = "detailsText"; //$NON-NLS-1$

	/**
	 * Property: The index of the Details button in the Dialog.
	 */
	public final static String PROPERTY_DETAILS_BUTTON_INDEX = "detailsButtonIndex"; //$NON-NLS-1$
}
