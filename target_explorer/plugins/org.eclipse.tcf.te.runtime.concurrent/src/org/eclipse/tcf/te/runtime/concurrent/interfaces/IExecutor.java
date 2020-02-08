/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.concurrent.interfaces;

import java.util.concurrent.Executor;

import org.eclipse.tcf.te.runtime.interfaces.extensions.IExecutableExtension;

/**
 * Execution interface declaration.
 */
public interface IExecutor extends Executor, IExecutableExtension {

}
