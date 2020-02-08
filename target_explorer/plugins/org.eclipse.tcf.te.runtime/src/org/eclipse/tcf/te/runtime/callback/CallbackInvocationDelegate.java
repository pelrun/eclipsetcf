/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.callback;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.runtime.callback.AsyncCallbackCollector.ICallbackInvocationDelegate;

/**
 * Default callback invocation delegate implementation.
 * <p>
 * The callback will be invoked in the calling thread. No thread switch is implied.
 */
public class CallbackInvocationDelegate implements ICallbackInvocationDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.callback.AsyncCallbackCollector.ICallbackInvocationDelegate#invoke(java.lang.Runnable)
	 */
	@Override
	public void invoke(Runnable runnable) {
		Assert.isNotNull(runnable);
		runnable.run();
	}

}
