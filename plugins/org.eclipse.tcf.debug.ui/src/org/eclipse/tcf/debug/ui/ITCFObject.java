/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.debug.ui;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.tcf.protocol.IChannel;

/**
 * ITCFObject is an interface that is implemented by all TCF debug model elements.
 * A visual element in a debugger view can be adapted to this interface -
 * if the element represents a remote TCF object.
 * Clients can get communication channel and ID of the object,
 * and use them to access the object through TCF service interfaces.
 *
 * @noimplement
 */
public interface ITCFObject extends IAdaptable {

    /**
     * Get TCF ID of the object.
     * @return TCF ID
     */
    String getID();

    /**
     * Get IChannel of the debug model that owns this object.
     * @return IChannel object
     */
    IChannel getChannel();

    /**
     * Get TCF debug model that owns this object.
     * @return ITCFModel interface
     */
    ITCFModel getModel();

    /**
     * Get parent object.
     * @return parent object or null if the object is a root
     */
    ITCFObject getParent();
}
