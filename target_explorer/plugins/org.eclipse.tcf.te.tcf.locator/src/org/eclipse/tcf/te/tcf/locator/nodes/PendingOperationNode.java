/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.nodes;

import org.eclipse.tcf.te.runtime.model.PendingOperationModelNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPendingOperationNode;

/**
 * An immutable model node to visualize a pending operation.
 */
public class PendingOperationNode extends PendingOperationModelNode implements IPendingOperationNode {

	/**
	 * Constructor.
	 */
	public PendingOperationNode() {
		super();
	}
}
