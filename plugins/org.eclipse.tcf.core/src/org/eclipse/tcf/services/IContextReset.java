/*******************************************************************************
 * Copyright (c) 2019.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *******************************************************************************/
package org.eclipse.tcf.services;

import java.util.Collection;
import java.util.Map;

import org.eclipse.tcf.protocol.IService;
import org.eclipse.tcf.protocol.IToken;

/**
 * TCF Context Reset service interface.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.7
 */
public interface IContextReset extends IService {

    /**
     * This service name, as it appears on the wire - a TCF name of the service.
     */
    static final String NAME = "ContextReset";

    /** The name of the reset type, String. */
    static final String CAPABILITY_TYPE = "Type";

    /** Brief description of the reset type, String. */
    static final String CAPABILITY_DESCRIPTION = "Description";

    /**
     * Report context reset service capabilities to clients so they can adjust
     * to different implementations of the service.
     *
     * @param ctx - a context ID.
     * @param done - command result call back object.
     * @return - pending command handle.
     */
    IToken getCapabilities(String context_id, DoneGetCapabilities done);

    /**
     * Call back interface for 'getCapabilities' command.
     */
    interface DoneGetCapabilities {
        /**
         * Called when 'getCapabilities' command is done.
         *
         * @param token - command handle.
         * @param error - error object or null.
         * @param capabilities - context reset service capabilities description.
         */
        void doneGetCapabilities(IToken token, Exception error, Collection<Map<String,Object>> capabilities);
    }

    /**
     * If true, context gets suspended after reset, Boolean.
     */
    static final String PARAM_SUSPEND = "Suspend";

    /**
     * Reset a specified context.
     *
     * @param context_id - a context ID, usually one returned by Run Control or Memory services.
     * @param type - name of the reset type.
     * @param params - parameters to control the context reset.
     * @param done - command result call back object.
     * @return - pending command handle.
     */
    IToken reset(String context_id, String reset_type, Map<String,Object> params, DoneReset done);

    /**
     * Call back interface for 'reset' command.
     */
    interface DoneReset {
        /**
         * Called when reset is done.
         *
         * @param token - command handle.
         * @param error - error object or null.
         */
        void doneReset(IToken token, Exception error);
    }
}
