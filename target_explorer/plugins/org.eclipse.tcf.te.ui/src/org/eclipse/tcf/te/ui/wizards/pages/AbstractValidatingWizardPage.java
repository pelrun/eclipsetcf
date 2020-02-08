/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.wizards.pages;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.tcf.te.ui.jface.interfaces.IValidatingContainer;

/**
 * An abstract validating wizard page implementation.
 * <p>
 * This wizard page implementation is adding utility methods for handling page validation.
 */
public abstract class AbstractValidatingWizardPage extends AbstractWizardPage implements IValidatingContainer {
	// A used to detect if a validation process is already running.
	// If set to true, validate() should return immediately.
	private boolean validationInProgress = false;

	/**
	 * Constructor.
	 *
	 * @param pageName The page name. Must not be <code>null</code>.
	 */
	public AbstractValidatingWizardPage(String pageName) {
		super(pageName);
	}

	/**
	 * Constructor.
	 *
	 * @param pageName The page name. Must not be <code>null</code>.
	 * @param title The wizard page title or <code>null</code>.
	 * @param titleImage The wizard page title image or <code>null</code>.
	 */
	public AbstractValidatingWizardPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	/**
	 * Set the validation in progress action.
	 *
	 * @param action <code>True</code> to mark validation in progress, <code>false</code> otherwise.
	 */
	public final boolean setValidationInProgress(boolean state) {
		boolean changed = false;
		// Apply only if really changed
		if (validationInProgress != state) {
			// Set the new value
			validationInProgress = state;
			onValidationInProgressChanged(validationInProgress);
			changed = true;
		}
		return changed;
	}

	/**
	 * Called from {@link #setValidationInProgress(boolean)} if the value
	 * of the corresponding flag changed. Subclasses may overwrite this
	 * method if additional custom steps shall be executed.
	 * <p>
	 * The default implementation is doing nothing.
	 *
	 * @param newValue The new value of the validation in progress flag. Same as calling {@link #isValidationInProgress()}.
	 */
	protected void onValidationInProgressChanged(boolean newValue) {
	}

	/**
	 * Returns if the current validation in progress action.
	 *
	 * @return <code>True</code> to mark validation in progress, <code>false</code> otherwise.
	 */
	public final boolean isValidationInProgress() {
		return validationInProgress;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.interfaces.IValidatingContainer#validate()
	 */
	@Override
	public final void validate() {
		if (isValidationInProgress())  return;

		ValidationResult result = doValidate();
		if (result != null) {
			setMessage(result.getMessage(), result.getMessageType());
			setPageComplete(result.isValid());
		}
		else {
			setMessage(null, IMessageProvider.NONE);
			setPageComplete(true);
		}

		setValidationInProgress(false);
	}

	/**
	 * Do the validation.
	 * @return The validation result or <code>null</code>.
	 */
	protected abstract ValidationResult doValidate();
}
