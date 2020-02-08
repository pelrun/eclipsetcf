/*******************************************************************************
 * Copyright (c) 2013-2019 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.services;

import java.util.Map;

import org.eclipse.tcf.protocol.IService;
import org.eclipse.tcf.protocol.IToken;

/**
 * TCF Profiler service interface.
 *
 * The service itself does not implement profiling, it manages creation/disposal of
 * profiler instances and communications between clients and profilers.
 * The service API is generic and it is supposed to support any kind of profiling and tracing.
 * A TCF agent can optionally include a profiler. The profiler would register itself with the service.
 * A client starts profiling by sending profiler configuration data for a debug context.
 * Multiple different profilers can be active at same debug context at same time.
 * If a client has started profiling, it is expected to read and process profiling data periodically.
 * Profiling data format is a contract between the profiler and its clients,
 * the service does not try to interpret the data.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IProfiler extends IService {

    /**
     * This service name, as it appears on the wire - a TCF name of the service.
     */
    static final String NAME = "Profiler";


    /* Profiler configuration parameters ----------------------------------- */

    /**
     * Profiler configuration parameter:
     * Number: size of stack traces in profiling samples,
     * 0 means no profiling,
     * 1 means no stack tracing.
     */
    static final String PARAM_FRAME_CNT = "FrameCnt";

    /**
     * Profiler configuration parameter:
     * Number: size of profiling data buffer, in samples.
     */
    static final String PARAM_MAX_SAMPLES = "MaxSamples";


    /* Profile data properties --------------------------------------------- */

    /**
     * Profile data property:
     * String: data format.
     * @since 1.2
     */
    static final String PROP_FORMAT = "Format";

    /**
     * Profile data property:
     * Number: address size in bytes.
     * Default is 4 bytes.
     */
    static final String PROP_ADDR_SIZE = "AddrSize";

    /**
     * Profile data property:
     * Sample endianess.
     * Default is little-endian.
     */
    static final String PROP_BIG_ENDIAN = "BigEndian";

    /**
     * Profile data property:
     * Byte array of profile samples.
     */
    static final String PROP_DATA = "Data";


    /* Commands ------------------------------------------------------------ */

    /**
     * Report profiler service capabilities to clients so they
     * can adjust to different implementations of the service.
     * @param ctx - a context ID.
     * @param done - command result call back object.
     * @return - pending command handle.
     * @since 1.3
     */
    IToken getCapabilities(String ctx, DoneGetCapabilities done);

    /**
     * Call back interface for 'getCapabilities' command.
     * @since 1.3
     */
    interface DoneGetCapabilities {
        /**
         * Called when 'getCapabilities' command is done.
         * @param token - command handle.
         * @param error - error object or null.
         * @param capabilities - profiler service capabilities description.
         */
        void doneGetCapabilities(IToken token, Exception error, Map<String,Object> capabilities);
    }

    /**
     * Configure profiling of a debug context 'ctx'.
     * Profiling is disabled (stopped) if 'params' is empty or null.
     * @param ctx - debug context to profile.
     * @param params - description of profiling method, see PARAM_*.
     * @param done - command result call back object.
     * @return - pending command handle.
     */
    IToken configure(String ctx, Map<String,Object> params, DoneConfigure done);

    interface DoneConfigure {
        /**
         * Called when "configure" command is done.
         * @param token - command handle.
         * @param error - error object or null.
         */
        void doneConfigure(IToken token, Exception error);
    }

    /**
     * Read profiling data buffers.
     * Successful read clears the buffer.
     * If a client has started profiling with "configure" command,
     * it is expected to read and process profiling data periodically.
     * The buffer has limited size, so profiling samples can be lost if they are not read timely.
     * @param ctx - debug context that is being profiled.
     * @param done - command result call back object.
     * @return - pending command handle.
     */
    IToken read(String ctx, DoneRead done);

    interface DoneRead {
        /**
         * Called when "read" command is done.
         * @param token - command handle.
         * @param error - error object or null.
         * @param data - array of profile data buffers.
         * Each buffer is collection of properties, see PROP_*.
         */
        void doneRead(IToken token, Exception error, Map<String,Object> data[]);
    }
}
