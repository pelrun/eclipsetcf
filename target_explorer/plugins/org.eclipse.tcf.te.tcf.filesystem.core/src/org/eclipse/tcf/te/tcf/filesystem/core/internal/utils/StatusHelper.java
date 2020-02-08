/*******************************************************************************
 * Copyright (c) 2015, 2016 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.utils;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IErrorReport;
import org.eclipse.tcf.te.tcf.filesystem.core.activator.CorePlugin;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

public class StatusHelper {

	public static IStatus createStatus(String msg, Throwable e) {
		if (e != null) {
			String msg2 = e.getLocalizedMessage();

			// Try to find a translation for the error code
			int errorCode = -1;
			if (e instanceof IErrorReport) {
				errorCode = ((IErrorReport) e).getErrorCode();
			}
			String errorMessageKey = NLS.bind(Messages.FileSystem_ErrorMessage_Errno_Base, Integer.valueOf(errorCode));
			if (errorCode != -1 && Messages.hasString(errorMessageKey)) {
				msg2 = Messages.getString(errorMessageKey);
			}

			if (msg2 != null) {
				msg = msg == null ? msg2 : msg + ": " + msg2; //$NON-NLS-1$
			}
		}
		return new Status(IStatus.ERROR, CorePlugin.getUniqueIdentifier(), msg, e);
	}

}
