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

import org.eclipse.tcf.services.IStackTrace;
import org.eclipse.tcf.util.TCFDataCache;

/**
 * ITCFStackFrame is an interface that is implemented by TCF debug model elements
 * that represent a function call stack frame on a remote target.
 * A visual element in a debugger view can be adapted to this interface -
 * if the element represents a stack frame.
 *
 * @noimplement
 */
public interface ITCFStackFrame {

    /**
     * Get frame properties cache.
     * @return The frame properties cache.
     */
    TCFDataCache<IStackTrace.StackTraceContext> getStackTraceContext();

    /**
     * Get frame registers cache.
     * @return The frame registers cache.
     */
    ITCFChildren getRegisters();
}
