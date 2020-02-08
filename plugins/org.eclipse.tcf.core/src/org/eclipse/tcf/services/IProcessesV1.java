/*******************************************************************************
 * Copyright (c) 2007-2019 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.services;

import java.util.Map;

import org.eclipse.tcf.protocol.IToken;

/**
 * Extension of Processes service.
 * It provides new "start" command that supports additional parameters.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IProcessesV1 extends IProcesses {

    /**
     * This service name, as it appears on the wire - a TCF name of the service.
     */
    static final String NAME = "ProcessesV1";

    /* Process start parameters */

    /**
     * Process start parameter:
     * Boolean, attach the debugger to the process.
     */
    static final String START_ATTACH = "Attach";

    /**
     * Process start parameter:
     * Boolean, auto-attach process children.
     */
    static final String START_ATTACH_CHILDREN = "AttachChildren";

    /**
     * Process start parameter:
     * Boolean, stop at process entry.
     */
    static final String START_STOP_AT_ENTRY = "StopAtEntry";

    /**
     * Process start parameter:
     * Boolean, stop at main().
     */
    static final String START_STOP_AT_MAIN = "StopAtMain";

    /**
     * Process start parameter:
     * Boolean, Use pseudo-terminal for the process standard I/O.
     */
    static final String START_USE_TERMINAL = "UseTerminal";

    /**
     * Process start parameter:
     * Bit set of signals that should not be intercepted by the debugger.
     * @since 1.2
     */
    static final String START_SIG_DONT_STOP = "SigDontStop";

    /**
     * Process start parameter:
     * Bit set of signals that should not be delivered to the process.
     * @since 1.2
     */
    static final String START_SIG_DONT_PASS = "SigDontPass";

    /**
     * Start a new process on remote machine.
     * @param directory - initial value of working directory for the process.
     * @param file - process image file.
     * @param command_line - command line arguments for the process.
     * Note: the service does NOT add image file name as first argument for the process.
     * If a client wants first parameter to be the file name, it should add it itself.
     * @param environment - map of environment variables for the process,
     * if null then default set of environment variables will be used.
     * @param params - additional process start parameters, see START_*.
     * @param done - call back interface called when operation is completed.
     * @return pending command handle, can be used to cancel the command.
     */
    IToken start(String directory, String file,
            String[] command_line, Map<String,String> environment,
            Map<String,Object> params, DoneStart done);

    /**
     * Set the process TTY widow size.
     * Applicable only when the process is started with START_USE_TERMINAL.
     * @param id - process context ID.
     * @param col - number of columns.
     * @param row - number of rows.
     * @param done - call back interface called when operation is completed.
     * @return pending command handle, can be used to cancel the command.
     * @since 1.2
     */
    IToken setWinSize(String id, int col, int row, DoneCommand done);

    /**
     * Client call back interface for getCapabilities().
     * @since 1.2
     */
    interface DoneGetCapabilities {
        /**
         * Called when the capability retrieval is done.
         *
         * @param error The error description if the operation failed, <code>null</code> if succeeded.
         * @param properties The global processes service or context specific capabilities.
         */
        public void doneGetCapabilities(IToken token, Exception error, Map<String,Object> properties);
    }

    /**
     * The command reports the ProcessesV1 service capabilities to clients so they can adjust
     * to different implementations of the service. When called with a null ("") context
     * ID the global capabilities are returned, otherwise context specific capabilities
     * are returned.
     *
     * @param id The context ID or <code>null</code>.
     * @param done The call back interface called when the operation is completed. Must not be <code>null</code>.
     * @since 1.2
     */
    public IToken getCapabilities(String id, DoneGetCapabilities done);
}
