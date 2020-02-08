/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.jface.interfaces;

import org.eclipse.jface.dialogs.IMessageProvider;


/**
 * Interface to be implemented by container managing the validation
 * of contained validatable sub elements.
 */
public interface IValidatingContainer {

	public static class ValidationResult implements IMessageProvider {

		private String message = null;
		private int type = -1;
		private boolean valid = true;

		/**
		 * Set the result from the given message provider.
		 * If the provider is a ValidationResult, the valid state is used too.
		 * If the provider is <code>null</code>, the message is set to <code>null</code> and the type to IMessageProvider.NONE.
		 * The new result is set or not in the same way as setResult(type,message) or setResult(valid,type,message) works.
		 * @param provider The message provider or <code>null</code>.
		 */
		public void setResult(IMessageProvider provider) {
			if (provider instanceof ValidationResult) {
				setResult(provider.getMessage(), provider.getMessageType(), ((ValidationResult)provider).isValid());
			}
			else if (provider != null) {
				setResult(provider.getMessage(), provider.getMessageType());
			}
			else {
				setResult(null, NONE);
			}
		}

		/**
		 * Set the message type and text,
		 * if type > actual type.
		 * @param message The message text.
		 * @param type The message type.
		 */
		public void setResult(String message, int type) {
			if (this.type < type) {
				this.message = message;
				this.type = type;
			}
		}

		/**
		 * Set the validation result, message type and text,
		 * if type > actual type or !valid.
		 * @param message The message text.
		 * @param type The message type.
		 * @param valid The validation result.
		 */
		public void setResult(String message, int type, boolean valid) {
			if (this.type < type || (!valid && this.valid)) {
				this.message = message;
				this.type = type;
				if (this.valid) {
					this.valid = valid;
				}
			}
		}

		/**
		 * Set the message text.
		 * @param message The message text.
		 */
		public void setMessage(String message) {
			this.message = message;
		}

		/**
		 * Set the message type.
		 * @param type The message type.
		 */
		public void setMessageType(int type) {
			this.type = type;
		}

		/**
		 * Set the validation result.
		 * A validation can be valid even when message and message type are set.
		 * @param valid The validation result.
		 */
		public void setValid(boolean valid) {
			this.valid = valid;
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
         * Return <code>true</code> if the result can be seen as valid.
         */
        public boolean isValid() {
        	return valid;
        }
	}

	/**
	 * Validates the container status.
	 * <p>
	 * If necessary, set the corresponding messages and message types to signal when some sub
	 * elements of the container needs user attention.
	 */
	public void validate();

	public void setMessage(String message, int messageType);
}
