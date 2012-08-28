/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime;

import org.eclipse.tcf.te.tcf.core.model.interfaces.IModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModelProvider;


/**
 * A model dealing with Process contexts at runtime.
 * <p>
 * The context represented by the runtime model are reflecting the current state of an active
 * Processes service instance. Therefore, the runtime model is 1:1 associated with a TCF agent
 * providing the Processes service.
 * <p>
 * All model access must happen in the TCF dispatch thread.
 */
public interface IRuntimeModel extends IModel, IPeerModelProvider {

}
