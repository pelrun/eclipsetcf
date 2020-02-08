/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.interfaces.nodes;

import org.eclipse.core.runtime.IAdaptable;

/**
 * Interface to be implemented by nodes providing access to an peer
 * node object instance without being a peer node object itself.
 */
public interface IPeerNodeProvider extends IAdaptable {

	/**
	 * Returns the associated peer node object.
	 *
	 * @return The peer node object instance or <code>null</code>.
	 */
	public IPeerNode getPeerNode();
}
