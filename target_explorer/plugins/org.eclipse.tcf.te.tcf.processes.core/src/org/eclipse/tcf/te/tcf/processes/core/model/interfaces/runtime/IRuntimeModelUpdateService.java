/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime;

import org.eclipse.tcf.te.runtime.model.interfaces.IContainerModelNode;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelUpdateService;

/**
 * Interface to be implemented by processes runtime model update services.
 */
public interface IRuntimeModelUpdateService extends IModelUpdateService {

	/**
	 * Merge the child tree of the given source container with the child tree
	 * of the given destination container.
	 *
	 * @param dst The destination container. Must not be <code>null</code>.
	 * @param src The source container. Must not be <code>null</code>.
	 *
	 * @return <code>True</code> if the destination container changed, <code>false</code> otherwise.
	 */
	public void updateChildren(IContainerModelNode dst, IContainerModelNode src);
}
