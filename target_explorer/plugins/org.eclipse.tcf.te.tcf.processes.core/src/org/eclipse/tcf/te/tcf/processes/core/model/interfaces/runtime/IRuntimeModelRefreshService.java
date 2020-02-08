/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime;

import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelRefreshService;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;

/**
 * Interface to be implemented by processes runtime model refresh services.
 */
public interface IRuntimeModelRefreshService extends IModelRefreshService {

	/**
	 * Interface to be implemented by processes runtime model refresh service delegates.
	 */
	public static interface IDelegate {

		/**
		 * Called from the processes runtime model refresh service after having queried the
		 * system monitor and the process context objects. Can be used to initialize the node
		 * type.
		 * <p>
		 * This method is invoked before {@link #postRefreshContext(IChannel, IProcessContextNode, ICallback)}.
		 * <p>
		 * <b>Note:</b> The method must be called in the TCF event dispatch thread.
		 *
		 * @param parentContextId The parent context id or <code>null</code> for the root context.
		 * @param node The process context node. Must not be <code>null</code>.
		 */
		public void setNodeType(String parentContextId, IProcessContextNode node);

		/**
		 * Called from the processes runtime model refresh service after having queried the
		 * system monitor and the process context objects. Can be used to initiate additional
		 * context node specific queries.
		 * <p>
		 * This method is invoked after {@link #setNodeType(IProcessContextNode)}.
		 * <p>
		 * <b>Note:</b> The method must be called in the TCF event dispatch thread.
		 *
		 * @param channel An open channel. Must not be <code>null</code>.
		 * @param node The process context node. Must not be <code>null</code>.
		 * @param callback The callback to invoke once the operation is completed. Must not be <code>null</code>.
		 */
		public void postRefreshContext(IChannel channel, IProcessContextNode node, ICallback callback);

		/**
		 * Returns the list of property names the delegate may add to a given process
		 * context node while executing {@link #postRefreshContext(IChannel, IProcessContextNode, ICallback)}.
		 *
		 * @return The list of managed property name or <code>null</code>.
		 */
		public String[] getManagedPropertyNames();
	}

	/**
	 * Refresh the content of the model from the top to the given maximum depth.
	 *
	 * @param callback The callback to invoke once the refresh operation finished, or <code>null</code>.
	 * @param depth Maximum depth or <code>-1</code> for full depth.
	 */
	public void refresh(ICallback callback, int depth);

	/**
	 * Auto refresh the content of the model from the top. It search for
	 * all nodes with query state "done" and refresh them one by one.
	 *
	 * @param callback The callback to invoke once the refresh operation finished, or <code>null</code>.
	 */
	public void autoRefresh(ICallback callback);
}
