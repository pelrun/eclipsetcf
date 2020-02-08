/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.async;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.AsyncCallbackCollector;

/**
 * Asynchronous callback collector callback invocation delegate implementation.
 * <p>
 * The delegate invokes callbacks within the TCF dispatch thread.
 */
public class CallbackInvocationDelegate implements AsyncCallbackCollector.ICallbackInvocationDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.core.async.AsyncCallbackCollector.ICallbackInvocationDelegate#invoke(java.lang.Runnable)
	 */
	@Override
	public void invoke(Runnable runnable) {
		Assert.isNotNull(runnable);
		if (Protocol.isDispatchThread()) runnable.run();
		else Protocol.invokeLater(runnable);
	}
}
