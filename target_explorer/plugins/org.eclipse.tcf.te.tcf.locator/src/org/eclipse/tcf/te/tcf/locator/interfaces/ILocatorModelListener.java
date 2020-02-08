/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.interfaces;

import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;

/**
 * Interface for clients to implement that wishes to listen to changes to the locator model.
 */
public interface ILocatorModelListener {

	/**
	 * Invoked if a locator node is added or removed to/from the locator model.
	 *
	 * @param model The changed locator model.
	 * @param locatorNode The added/removed locator node.
	 * @param added <code>True</code> if the locator node got added, <code>false</code> if it got removed.
	 */
	public void modelChanged(ILocatorModel model, ILocatorNode locatorNode, boolean added);

	/**
	 * Invoked if the locator model is disposed.
	 *
	 * @param model The disposed locator model.
	 */
	public void modelDisposed(ILocatorModel model);
}
