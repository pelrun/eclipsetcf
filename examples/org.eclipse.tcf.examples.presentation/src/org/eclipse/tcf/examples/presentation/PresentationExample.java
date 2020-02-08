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
package org.eclipse.tcf.examples.presentation;

import org.eclipse.debug.core.IRequest;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.tcf.debug.ui.ITCFModel;
import org.eclipse.tcf.debug.ui.ITCFPresentationProvider;

/**
 * This example demonstrates how to provide icons in the Registers view.
 * Using this approach, clients can alter all aspects of TCF debug model presentation.
 */
@SuppressWarnings("restriction")
public class PresentationExample implements ITCFPresentationProvider {

    @Override
    public boolean onModelCreated(ITCFModel model) {
        /* true means we want to listen for UI requests on this model */
        return true;
    }

    @Override
    public void onModelDisposed(ITCFModel model) {
    }

    @Override
    public boolean updateStarted(IRequest request) {
        /* true means we want the model to start handling of this request */
        return true;
    }

    @Override
    public boolean updateComplete(IRequest request) {
        if (request instanceof ILabelUpdate) {
            ILabelUpdate update = (ILabelUpdate)request;
            String id = update.getPresentationContext().getId();
            if (IDebugUIConstants.ID_REGISTER_VIEW.equals(id)) {
                /* This request is for label in the Registers view.
                 * Let's override the default icon with something else.
                 */
                ImageDescriptor image = DebugUITools.getImageDescriptor(
                        IDebugUIConstants.IMG_OBJS_VARIABLE);
                update.setImageDescriptor(image, 0);
            }
        }
        /* true means we want the model to finish handling of this request */
        return true;
    }
}
