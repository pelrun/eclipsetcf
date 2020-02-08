/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.controls.validator;


/**
 * Validator using regular expression.
 */
public class RegexValidator extends Validator {

	/**
	 * Attribute to check if the text to validate does not match the given regex.
	 */
	public static final int ATTR_NOT_REGEX = 2;
	// next attribute should start with 2^2

	// keys for info messages
	public static final String INFO_MISSING_VALUE = "RegexValidator_Information_MissingValue"; //$NON-NLS-1$

	// keys for error messages
	public static final String ERROR_INVALID_VALUE = "RegexValidator_Error_InvalidValue"; //$NON-NLS-1$

	// arguments
	private String regex;

	/**
	 * Constructor.
	 * @param attributes
	 */
	public RegexValidator(int attributes, String regex) {
		super(attributes);
		this.regex = regex;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.validator.Validator#isValid(java.lang.String)
	 */
	@Override
	public boolean isValid(String newText) {
		init();

		// info message when value is empty and mandatory
		if (newText == null || newText.trim().length() == 0) {
			if (isMandatory()) {
				setMessage(getMessageText(INFO_MISSING_VALUE), getMessageTextType(INFO_MISSING_VALUE, INFORMATION));
				return false;
			}
			return true;
		}

		boolean match = newText.matches(regex);
		if ((!isAttribute(ATTR_NOT_REGEX) && !match) || (isAttribute(ATTR_NOT_REGEX) && match)) {
			setMessage(getMessageText(ERROR_INVALID_VALUE), getMessageTextType(ERROR_INVALID_VALUE, ERROR));
			return getMessageType() != ERROR;
		}

		return true;
	}

	/**
	 * Returns the regular expression.
	 * @return
	 */
	protected String getRegularExpression() {
		return regex;
	}

	/**
	 * Set the regular expression.
	 * @param regex
	 */
	protected void setRegularExpression(String regex) {
		if (regex != null && regex.length() > 0) {
			this.regex = regex;
		}
		else {
			this.regex = ".*"; //$NON-NLS-1$
		}
	}
}
