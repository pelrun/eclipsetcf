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

import org.eclipse.debug.core.IRequest;

/**
 * TCF clients can implement ITCFPresentationProvider to participate in presentation of TCF debug model.
 * The implementation is registered through extension point: org.eclipse.tcf.debug.ui.presentation_provider
 */
public interface ITCFPresentationProvider {

    /**
     * Debug model created notification.
     * @param model - the model.
     * @return - true if the client wants to listen for updateStarted/updateComplete events on the model,
     * returning false stops ant further client notifications from that model.
     */
    boolean onModelCreated(ITCFModel model);

    /**
     * Debug model disposed notification.
     * @param model - the model.
     */
    void onModelDisposed(ITCFModel model);

    /**
     * This method is called for every presentation data request before
     * the debug model starts to fill the request with the requested data.
     * @param request - data request from the debugger UI.
     * @return - true if the client wants the debug model to continue processing the
     * request. If the client returns false, it is his responsibility to fill
     * the request and to call "IRequest.done".
     */
    boolean updateStarted(IRequest request);

    /**
     * This method is called for every presentation data request after
     * the debug model has filled the request with the requested data,
     * but before it calls "done" method of the request.
     * Clients can modify the data that is stored in the request object.
     * @param request - data request from the debugger UI.
     * @return - true if the client wants the debug model to continue processing the
     * request. If the client returns false, it is his responsibility to call "IRequest.done".
     */
    boolean updateComplete(IRequest request);
}
