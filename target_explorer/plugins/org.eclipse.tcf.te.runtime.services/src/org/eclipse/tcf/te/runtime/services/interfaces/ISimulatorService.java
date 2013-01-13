/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.services.interfaces;

/**
 * Simulator service.
 * <p>
 * Allows to start/stop external simulators.
 * <p>
 * Simulator instance related UI parts, like configuration panels, are retrieved
 * by clients via the {@link IUIService}.
 */
public interface ISimulatorService extends IService {

}
