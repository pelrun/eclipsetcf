/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.utils;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.te.tcf.filesystem.core.activator.CorePlugin;

public class StatusHelper {

	public static IStatus createStatus(String msg, Throwable e) {
		if (e != null) {
			String msg2= e.getLocalizedMessage();
			if (msg2 != null) {
				msg = msg == null ? msg2 : msg + ": " + msg2; //$NON-NLS-1$
			}
		}
		return new Status(IStatus.ERROR, CorePlugin.getUniqueIdentifier(), msg, e);
	}

}
