/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.interfaces;

/**
 * This interface must be implemented by callers of the executors utilities
 * <code>wait</code> method.
 * <p>
 * The call to <code>isConditionFulfilled</code> must return <code>true</code> only
 * if the desired condition, the caller want's to wait for, has been completely fulfilled!
 */
public interface IConditionTester {
	/**
	 * Returns <code>true</code> if the desired condition, the caller want's to wait
	 * for, has been completely fulfilled.
	 */
	public boolean isConditionFulfilled();

	/**
	 * Condition tester clean up. This method is called before any waiter method will
	 * be finally left.
	 */
	public void cleanup();
}
