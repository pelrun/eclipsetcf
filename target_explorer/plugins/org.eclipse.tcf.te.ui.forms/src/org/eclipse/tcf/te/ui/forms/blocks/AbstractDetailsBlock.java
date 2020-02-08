/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.forms.blocks;

import org.eclipse.tcf.te.ui.jface.interfaces.IValidatable;
import org.eclipse.tcf.te.ui.jface.interfaces.IValidatingContainer;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IDetailsPage;

/**
 * Abstract details block implementation
 */
public abstract class AbstractDetailsBlock extends AbstractFormPart implements IDetailsPage, IValidatable {

	private String message = null;
	private int type = -1;

	/**
	 * Set the message.
	 * @param message The message.
	 * @param type The message type.
	 */
	protected void setMessage(String message, int type) {
		this.message = message;
		this.type = type;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IMessageProvider#getMessage()
	 */
	@Override
	public String getMessage() {
		return message;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IMessageProvider#getMessageType()
	 */
	@Override
	public int getMessageType() {
		return type;
	}

	/**
	 * Validate the parent validating container.
	 */
	protected void validate() {
		if (getManagedForm() != null && getManagedForm().getContainer() instanceof IValidatingContainer) {
			((IValidatingContainer)getManagedForm().getContainer()).validate();
		}
	}
}
