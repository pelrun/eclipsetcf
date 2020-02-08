/*******************************************************************************
 * Copyright (c) 2007, 2013 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import org.eclipse.debug.core.IRequest;
import org.eclipse.tcf.debug.ui.ITCFPresentationProvider;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.protocol.Protocol;

/**
 * TCFRunnable is a wrapper for IRequest.
 * It implements Runnable interface and is used by TCFModel to handle the request.
 */
public abstract class TCFRunnable implements Runnable {

    private final IRequest request;
    private final Iterable<ITCFPresentationProvider> listeners;

    protected boolean done;

    public TCFRunnable(TCFModel model, IRequest request) {
        this.request = request;
        listeners = model.view_request_listeners;
        if (Protocol.isDispatchThread()) {
            if (listeners != null) {
                for (ITCFPresentationProvider l : listeners) {
                    try {
                        if (!l.updateStarted(TCFRunnable.this.request)) return;
                    }
                    catch (Throwable x) {
                        Activator.log("Unhandled exception in a presentation provider", x);
                    }
                }
            }
            run();
            return;
        }
        if (listeners == null) {
            Protocol.invokeLater(this);
            return;
        }
        Protocol.invokeLater(new Runnable() {
            @Override
            public void run() {
                assert !done;
                for (ITCFPresentationProvider l : listeners) {
                    try {
                        if (!l.updateStarted(TCFRunnable.this.request)) return;
                    }
                    catch (Throwable x) {
                        Activator.log("Unhandled exception in a presentation provider", x);
                    }
                }
                TCFRunnable.this.run();
            }
        });
    }

    public void done() {
        assert !done;
        done = true;
        if (listeners != null) {
            for (ITCFPresentationProvider l : listeners) {
                try {
                    if (!l.updateComplete(request)) return;
                }
                catch (Throwable x) {
                    Activator.log("Unhandled exception in a presentation provider", x);
                }
            }
        }
        // Don't call Display.asyncExec: display thread can be blocked waiting for the request.
        // For example, display thread is blocked for action state update requests.
        // Calling back into Eclipse on TCF thread is dangerous too - if Eclipse blocks TCF thread
        // we can get deadlocked. Might need a new thread (or Job) to make this call safe.
        request.done();
    }
}
