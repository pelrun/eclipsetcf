/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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
