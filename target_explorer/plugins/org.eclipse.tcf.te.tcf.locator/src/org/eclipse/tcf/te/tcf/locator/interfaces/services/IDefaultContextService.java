/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.interfaces.services;

import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Interface to implement by services providing a default context for others.
 * <p>
 * The context type of the service is {@link IPeerNode}.
 */
public interface IDefaultContextService extends IService {

	/**
	 * Default context filter.
	 * <p>
	 * To be provided to the service methods to narrow the list of possible contexts.
	 */
	public static interface IContextFilter {

		/**
		 * Check if the given peer model node is matching the filter or not.
		 * <p>
		 * If the filter is matched, the peer model node is added to the list
		 * of possible contexts.
		 *
		 * @param peerNode The peer model node. Must not be <code>null</code>.
		 * @return <code>true</code> if the given peer model node is a possible candidate.
		 */
		public boolean select(IPeerNode peerNode);
	}

	/**
	 * Return a list of possible candidates matching the given filter.
	 * <p>
	 * If a selection is given and the filter applies, the selection will be added first to the
	 * resulting array of candidates. Otherwise, if a default selection was set using {@link #setDefaultContext(IPeerNode)},
	 * and the filter applies, the default selection will added first to the resulting array of
	 * candidates.
	 * <p>
	 * Service implementations may implement additional heuristics to determine candidates matching
	 * the given filter.
	 *
	 * @param selection The selection or <code>null</code>.
	 * @param filter The context filter or <code>null</code>
	 *
	 * @return An array of peer model nodes or an empty array.
	 */
	public IPeerNode[] getCandidates(Object selection, IContextFilter filter);

	/**
	 * Sets the given peer model node as new default context.
	 * <p>
	 * If the given peer model node is <code>null</code>, the default context is reseted.
	 *
	 * @param peerNode The peer model node or <code>null</code>.
	 */
	public void setDefaultContext(IPeerNode peerNode);

	/**
	 * Returns the default context matching the given context filter.
	 *
	 * @param filter The context filter or <code>null</code>
	 * @return The default context if set and the filter applies, <code>null</code> otherwise.
	 */
	public IPeerNode getDefaultContext(IContextFilter filter);
}
