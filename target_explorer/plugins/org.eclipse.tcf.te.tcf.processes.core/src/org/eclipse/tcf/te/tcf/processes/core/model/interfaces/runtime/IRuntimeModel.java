/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime;

import org.eclipse.tcf.te.tcf.core.model.interfaces.IModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider;


/**
 * A model dealing with Process contexts at runtime.
 * <p>
 * The context represented by the runtime model are reflecting the current state of an active
 * Processes service instance. Therefore, the runtime model is 1:1 associated with a TCF agent
 * providing the Processes service.
 * <p>
 * All model access must happen in the TCF dispatch thread.
 */
public interface IRuntimeModel extends IModel, IPeerNodeProvider {

	/**
	 * Set the auto-refresh interval in seconds.
	 * <p>
	 * <b>Note:</b> If the interval is set to 0, than auto-refresh is disabled.
	 *
	 * @param interval The auto-refresh interval in seconds.
	 */
	public void setAutoRefreshInterval(int interval);

	/**
	 * Returns the auto-refresh interval in seconds.
	 *
	 * @param The auto-refresh interval in seconds.
	 */
	public int getAutoRefreshInterval();
}
