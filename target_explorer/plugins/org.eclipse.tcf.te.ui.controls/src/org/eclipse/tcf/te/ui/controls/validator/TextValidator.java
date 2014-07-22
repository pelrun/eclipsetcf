/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
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
