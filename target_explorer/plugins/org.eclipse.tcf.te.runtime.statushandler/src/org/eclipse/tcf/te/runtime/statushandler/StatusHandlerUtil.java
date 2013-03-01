/**
 * StatusHandlerUtil.java
 * Created on May 21, 2012
 *
 * Copyright (c) 2012 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.runtime.statushandler;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.statushandler.activator.CoreBundleActivator;
import org.eclipse.tcf.te.runtime.statushandler.interfaces.IStatusHandler;
import org.eclipse.tcf.te.runtime.statushandler.interfaces.IStatusHandlerConstants;

/**
 * Status handler utility implementations.
 */
public final class StatusHandlerUtil {

	/**
	 * Handle the given status for the given context.
	 *
	 * @param status The status. Must not be <code>null</code>.
	 * @param context The context. Must not be <code>null</code>.
	 * @param template The message template or <code>null</code>.
	 * @param title The dialog title or <code>null</code>.
	 * @param contextHelpId The context help id or <code>null</code>.
	 * @param caller The caller or <code>null</code>.
	 * @param callback The callback or <code>null</code>.
	 */
	public static void handleStatus(IStatus status, Object context, String template, String title, String contextHelpId, Object caller, ICallback callback) {
		Assert.isNotNull(status);
		Assert.isNotNull(context);

		IStatusHandler[] handlers = StatusHandlerManager.getInstance().getHandler(context);
		if (handlers.length > 0) {
			IPropertiesContainer data = new PropertiesContainer();

			if (title != null) data.setProperty(IStatusHandlerConstants.PROPERTY_TITLE, title);
			if (contextHelpId != null) data.setProperty(IStatusHandlerConstants.PROPERTY_CONTEXT_HELP_ID, contextHelpId);
			if (caller != null) data.setProperty(IStatusHandlerConstants.PROPERTY_CALLER, caller);

			updateMessage(status, template);

			handlers[0].handleStatus(status, data, callback);
		} else {
			Platform.getLog(CoreBundleActivator.getContext().getBundle()).log(status);
			callback.done(caller, status);
		}
	}

	private static void updateMessage(IStatus status, String template) {
		Assert.isNotNull(status);

		StringBuilder message = new StringBuilder();
		String msg = status.getMessage();

		if (msg != null && msg.contains("Error text:")) { //$NON-NLS-1$
			StringTokenizer tokenizer = new StringTokenizer(msg, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreElements()) {
				String token = tokenizer.nextToken();
				if (token.trim().startsWith("Error text:")) { //$NON-NLS-1$
					token = token.replaceAll("Error text:", " "); //$NON-NLS-1$ //$NON-NLS-2$
					message.append(token.trim());
					break;
				}
			}
		} else if (msg != null) {
			message.append(msg.trim());
		}

		// If the status is associated with an exception, the exception message may contain additional
		// detailed information. Append it to the message
		if (status.getException() != null && status.getException().getLocalizedMessage() != null
				&& !status.getException().getLocalizedMessage().contains(message.toString())) {
			message.append("\n\n"); //$NON-NLS-1$
			message.append(status.getException().getLocalizedMessage());
		}

		// Construct the final message string
		String fullMsg = null;
		if (message.length() > 0) fullMsg = message.toString().trim();

		// Apply the template if any
		if (template != null) fullMsg = NLS.bind(template, fullMsg != null ? fullMsg : ""); //$NON-NLS-1$

		if (fullMsg != null) {
			// Normalize any possible "\r\n"
			fullMsg = fullMsg.replaceAll("\r\n", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			try {
	            final Field f = status.getClass().getDeclaredField("message"); //$NON-NLS-1$
				AccessController.doPrivileged(new PrivilegedAction<Object>() {
					@Override
					public Object run() {
						f.setAccessible(true);
						return null;
					}
				});
				f.set(status, fullMsg);
            }
            catch (Exception e) {}
		}
	}
}
