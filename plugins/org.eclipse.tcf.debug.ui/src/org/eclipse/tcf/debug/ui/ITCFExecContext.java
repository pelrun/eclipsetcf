/*******************************************************************************
 * Copyright (c) 2012 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.debug.ui;

import org.eclipse.tcf.services.IMemory;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.util.TCFDataCache;

/**
 *
 * @noimplement
 */
public interface ITCFExecContext extends ITCFObject {

    /**
     * Get memory space properties cache.
     * @return The memory space properties cache.
     */
    TCFDataCache<IMemory.MemoryContext> getMemoryContext();

    /**
     * Get run control properties cache.
     * @return The run control properties cache.
     */
    TCFDataCache<IRunControl.RunControlContext> getRunContext();

    /**
     * Get context registers cache.
     * @return The context registers cache.
     */
    ITCFChildren getRegisters();

    /**
     * Get context stack frames cache.
     * @return The context stack frames cache.
     */
    ITCFChildren getStackTrace();

    /**
     * Get context children cache.
     * @return The context children cache.
     */
    ITCFChildren getChildren();
}
