/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.controls.validator;

/**
 * Validator checking for empty strings.
 */
public class TextValidator extends Validator {

	public static final String INFO_MISSING_NAME = "TextValidator_Information_MissingName"; //$NON-NLS-1$

	/**
	 * Constructor
	 *
	 * @param attributes The validator attributes.
	 */
	public TextValidator(int attributes) {
		super(attributes);

	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.validator.Validator#isValid(java.lang.String)
	 */
	@Override
	public boolean isValid(String newText) {
		init();

		if (newText == null || newText.trim().length() == 0) {
			if (isAttribute(ATTR_MANDATORY)) {
				setMessage(getMessageText(INFO_MISSING_NAME), getMessageTextType(INFO_MISSING_NAME, INFORMATION));
				return false;
			}
		}

		return getMessageType() != ERROR;
	}

}
