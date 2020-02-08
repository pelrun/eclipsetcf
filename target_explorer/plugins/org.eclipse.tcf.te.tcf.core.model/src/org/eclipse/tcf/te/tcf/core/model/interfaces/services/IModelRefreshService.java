/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.model.interfaces.services;

import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;

/**
 * Common interface to be implemented by a model refresh service.
 * <p>
 * <b>Note:</b> The refresh service API is designed to support asynchronous refresh operations. The
 * implementer of the service may however implement synchronous behavior. In any case, the method
 * callbacks must be invoked if given.
 */
public interface IModelRefreshService extends IModelService {

	/**
	 * Refresh the content of the model from the top.
	 *
	 * @param callback The callback to invoke once the refresh operation finished, or <code>null</code>.
	 */
	public void refresh(ICallback callback);

	/**
	 * Full refresh the given node.
	 *
	 * @param node The node. Must not be <code>null</code>.
	 * @param callback The callback to invoke once the refresh operation finished, or <code>null</code>.
	 */
	public void refresh(IModelNode node, ICallback callback);
}
