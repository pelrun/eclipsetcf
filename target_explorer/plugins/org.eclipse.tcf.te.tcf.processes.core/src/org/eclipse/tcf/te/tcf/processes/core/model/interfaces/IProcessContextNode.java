/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.interfaces;

import org.eclipse.tcf.services.IProcesses;
import org.eclipse.tcf.services.ISysMonitor;
import org.eclipse.tcf.te.runtime.model.interfaces.IContainerModelNode;

/**
 * A process context node.
 */
public interface IProcessContextNode extends IContainerModelNode {
	/**
	 * Process context node types
	 */
	public enum TYPE { Unknown, Process, Thread }

	/**
	 * Set the type of the context node.
	 *
	 * @param type The context node type. Must not be <code>null</code>.
	 */
	public void setType(TYPE type);

	/**
	 * Returns the type of the context node.
	 *
	 * @return The context node type.
	 */
	public TYPE getType();

	/**
	 * Returns if or if not the context node has all information required
	 * to define the context through the Processes service.
	 *
	 * @return <code>True</code> if the context definition is complete, <code>false</code> otherwise.
	 */
	public boolean isComplete();

	/**
	 * Associated the agent side process context object.
	 *
	 * @param config The agent side process context object or <code>null</code>.
	 */
	public void setProcessContext(IProcesses.ProcessContext context);

	/**
	 * Returns the agent side process context object.
	 *
	 * @return The agent side process context object or <code>null</code>.
	 */
	public IProcesses.ProcessContext getProcessContext();

	/**
	 * Associated the agent side system monitor context object.
	 *
	 * @param config The agent side system monitor context object or <code>null</code>.
	 */
	public void setSysMonitorContext(ISysMonitor.SysMonitorContext context);

	/**
	 * Returns the agent side system monitor context object.
	 *
	 * @return The agent side system monitor context object or <code>null</code>.
	 */
	public ISysMonitor.SysMonitorContext getSysMonitorContext();
}
