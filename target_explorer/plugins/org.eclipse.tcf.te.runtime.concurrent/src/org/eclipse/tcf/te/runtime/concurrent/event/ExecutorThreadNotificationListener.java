/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.concurrent.event;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventFireDelegate;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;

/**
 * Abstract notification listener implementation executing the
 * notifications within the shared executor thread.
 */
public abstract class ExecutorThreadNotificationListener extends PlatformObject implements IEventListener, IEventFireDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.events.IEventFireDelegate#fire(java.lang.Runnable)
	 */
	@Override
	public final void fire(Runnable runnable) {
		// Force notification into the executor thread.
		//
		// Note: The executor thread is not identical with the display thread!
		//       Use ExecutorsUtil.executeInUI(runnable) to execute the runnable
		//       within the display thread.
		ExecutorsUtil.execute(runnable);
	}
}
