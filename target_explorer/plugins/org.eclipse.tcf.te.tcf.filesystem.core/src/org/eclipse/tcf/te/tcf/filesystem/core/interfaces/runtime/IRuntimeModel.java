/*******************************************************************************
 * Copyright (c) 2014, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime;

import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.te.tcf.core.model.interfaces.IModel;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IResultOperation;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider;


/**
 * A model dealing with the filesystem at runtime.
 */
public interface IRuntimeModel extends IModel, IPeerNodeProvider {

	/**
	 * Returns the channel of this runtime model
	 */
	public IChannel getChannel();

	/**
	 * Returns the root node of this model.
	 */
    public IFSTreeNode getRoot();

    /**
     * Returns an operation for restoring nodes from a path
     */
	public IResultOperation<IFSTreeNode> operationRestoreFromPath(String path);
}
