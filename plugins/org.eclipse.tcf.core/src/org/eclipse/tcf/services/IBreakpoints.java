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

import org.eclipse.tcf.protocol.IService;
import org.eclipse.tcf.protocol.IToken;


/**
 * Breakpoint is represented by unique identifier and set of properties.
 * Breakpoint identifier (String id) needs to be unique across all hosts and targets.
 *
 * Breakpoint properties (Map<String,Object>) is extendible collection of named attributes,
 * which define breakpoint location and behavior. This module defines some common
 * attribute names (see PROP_*), host tools and target agents may support additional attributes.
 *
 * For each breakpoint a target agent maintains another extendible collection of named attributes:
 * breakpoint status (Map<String,Object>, see STATUS_*). While breakpoint properties are
 * persistent and represent user input, breakpoint status reflects dynamic target agent reports
 * about breakpoint current state, like actual addresses where breakpoint is planted or planting errors.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IBreakpoints extends IService {

    /**
     * This service name, as it appears on the wire - a TCF name of the service.
     */
    static final String NAME = "Breakpoints";

    /**
     * Breakpoint property names.
     */
    static final String
        PROP_ID = "ID",                           // String
        PROP_ENABLED = "Enabled",                 // Boolean
        PROP_TYPE = "BreakpointType",             // String
        PROP_CONTEXT_NAMES = "ContextNames",      // Array
        PROP_CONTEXT_IDS = "ContextIds",          // Array
        PROP_EXECUTABLE_PATHS = "ExecPaths",      // Array
        PROP_CONTEXT_QUERY = "ContextQuery",      // String, see IContextQuery
        PROP_LOCATION = "Location",               // String
        PROP_SIZE = "Size",                       // Number
        PROP_ACCESS_MODE = "AccessMode",          // Number
        PROP_FILE = "File",                       // String
        PROP_LINE = "Line",                       // Number
        PROP_COLUMN = "Column",                   // Number
        PROP_PATTERN = "MaskValue",               // Number
        PROP_MASK = "Mask",                       // Number
        PROP_STOP_GROUP = "StopGroup",            // Array
        PROP_IGNORE_COUNT = "IgnoreCount",        // Number
        PROP_TIME = "Time",                       // Number
        PROP_SCALE = "TimeScale",                 // String
        PROP_UNITS = "TimeUnits",                 // String
        PROP_CONDITION = "Condition",             // String
        PROP_TEMPORARY = "Temporary",             // Boolean
        PROP_EVENT_TYPE = "EventType",            // String
        PROP_EVENT_ARGS = "EventArgs",            // String or Object
        PROP_CLIENT_DATA = "ClientData",          // Object
        PROP_ACTION = "Action";                   // String - expression or script
    /** @since 1.3 */
    static final String
        PROP_SKIP_PROLOGUE = "SkipPrologue";      // Boolean
    /** @since 1.4 */
    static final String
        PROP_LINE_OFFSET = "LineOffset";          // Number - max number of lines breakpoint is allowed
                                                  // to be moved in case of inexact line info match
    /** @since 1.5 */
    static final String
        PROP_SERVICE = "Service";                 // String - name of a service that owns the breakpoint.
                                                  // User breakpoints don't have this attribute.

    /**
     * Breakpoints service can control cross trigger matrix - if the target hardware has one.
     * If a breakpoint has cross trigger attributes, planting of the breakpoint enables routing of
     * CrossTriggerInp signals to CrossTriggerOut signals. If CrossTriggerOut array includes
     * CPU stop request, any CrossTriggerInp signal also stops software execution. In such case,
     * breakpoint attributes like Condition or StopGroup have same meaning as for regular breakpoint.
     * Values of cross trigger attributes are arrays of signal IDs. Signal ID can be either Number or String.
     * Mapping of signal IDs to hardware depends on the target.
     * @since 1.5
     */
    static final String
        PROP_CT_INP = "CrossTriggerInp",          // Array - Cross trigger inputs
        PROP_CT_OUT = "CrossTriggerOut";          // Array - Cross trigger outputs

    /**
     * @deprecated
     */
    static final String
        PROP_CONTEXTNAMES = "ContextNames",       // Array
        PROP_CONTEXTIDS = "ContextIds",           // Array
        PROP_EXECUTABLEPATHS = "ExecPaths",       // Array
        PROP_ACCESSMODE = "AccessMode",           // Number
        PROP_IGNORECOUNT = "IgnoreCount";         // Number

    /**
     * BreakpointType values
     */
    static final String
        TYPE_SOFTWARE = "Software",
        TYPE_HARDWARE = "Hardware",
        TYPE_AUTO = "Auto";

    /**
     * AccessMode values
     */
    static final int
        ACCESSMODE_READ    = 0x01,
        ACCESSMODE_WRITE   = 0x02,
        ACCESSMODE_EXECUTE = 0x04,
        ACCESSMODE_CHANGE  = 0x08;

    /**
     * TimeScale values
     */
    static final String
        TIMESCALE_RELATIVE = "Relative",
        TIMESCALE_ABSOLUTE = "Absolute";

    /**
     * TimeUnits values
     */
    static final String
        TIMEUNIT_NSECS = "Nanoseconds",
        TIMEUNIT_CYCLE_COUNT = "CycleCount",
        TIMEUNIT_INSTRUCTION_COUNT = "InstructionCount";

    /**
     * Breakpoint status field names.
     */
    static final String
        STATUS_INSTANCES = "Instances",         // Array of Map<String,Object>
        STATUS_ERROR = "Error",                 // String
        STATUS_FILE = "File",                   // String
        STATUS_LINE = "Line",                   // Number
        STATUS_COLUMN = "Column";               // Number

    /**
     * Breakpoint instance field names.
     */
    static final String
        INSTANCE_ERROR = "Error",               // String
        INSTANCE_CONTEXT = "LocationContext",   // String
        INSTANCE_ADDRESS = "Address",           // Number
        INSTANCE_SIZE = "Size",                 // Number
        INSTANCE_TYPE = "BreakpointType",       // String
        INSTANCE_MEMORY_CONTEXT = "MemoryContext",// String
        INSTANCE_HIT_COUNT = "HitCount";         // Number
    /** @since 1.3 */
    static final String
        INSTANCE_CONDITION_ERROR = "ConditionError"; // String

    /**
     * Breakpoint service capabilities.
     */
    static final String
        CAPABILITY_CONTEXT_ID = "ID",                   // String
        CAPABILITY_HAS_CHILDREN = "HasChildren",        // Boolean
        CAPABILITY_BREAKPOINT_TYPE = "BreakpointType",  // Boolean
        CAPABILITY_LOCATION = "Location",               // Boolean
        CAPABILITY_CONDITION = "Condition",             // Boolean
        CAPABILITY_FILE_LINE = "FileLine",              // Boolean
        CAPABILITY_FILE_MAPPING = "FileMapping",        // Boolean
        CAPABILITY_CONTEXT_IDS = "ContextIds",          // Boolean
        CAPABILITY_CONTEXT_NAMES = "ContextNames",      // Boolean
        CAPABILITY_CONTEXT_QUERY = "ContextQuery",      // Boolean
        CAPABILITY_STOP_GROUP = "StopGroup",            // Boolean
        CAPABILITY_TEMPORARY = "Temporary",             // Boolean
        CAPABILITY_IGNORE_COUNT = "IgnoreCount",        // Boolean
        CAPABILITY_ACCESS_MODE = "AccessMode",          // Number
        CAPABILITY_CLIENT_DATA = "ClientData";          // Boolean
    /** @since 1.3 */
    static final String
        CAPABILITY_SKIP_PROLOGUE = "SkipPrologue";      // Boolean
    /** @since 1.5 */
    static final String
        CAPABILITY_CROSS_TRIGGER = "CrossTrigger";      // Boolean

    /**
     * @deprecated
     */
    static final String
        CAPABILITY_CONTEXTNAMES = "ContextNames",
        CAPABILITY_CONTEXTIDS = "ContextIds",
        CAPABILITY_IGNORECOUNT = "IgnoreCount",
        CAPABILITY_ACCESSMODE = "AccessMode";

    /**
     * Call back interface for breakpoint service commands.
     */
    interface DoneCommand {
        /**
         * Called when command is done.
         * @param token - command handle.
         * @param error - error object or null.
         */
        void doneCommand(IToken token, Exception error);
    }

    /**
     * Download breakpoints data to target agent.
     * The command is intended to be used only to initialize target breakpoints table
     * when communication channel is open. After that, host should
     * notify target about (incremental) changes in breakpoint data by sending
     * add, change and remove commands.
     *
     * @param properties - array of breakpoints.
     * @param done - command result call back object.
     * @return - pending command handle.
     */
    IToken set(Map<String,Object>[] properties, DoneCommand done);

    /**
     * Called when breakpoint is added into breakpoints table.
     * @param properties - breakpoint properties.
     * @param done - command result call back object.
     * @return - pending command handle.
     */
    IToken add(Map<String,Object> properties, DoneCommand done);

    /**
     * Called when breakpoint properties are changed.
     * @param properties - breakpoint properties.
     * @param done - command result call back object.
     * @return - pending command handle.
     */
    IToken change(Map<String,Object> properties, DoneCommand done);

    /**
     * Tell target to change (only) PROP_ENABLED breakpoint property to 'true'.
     * @param ids - array of enabled breakpoint identifiers.
     * @param done - command result call back object.
     * @return - pending command handle.
     */
    IToken enable(String[] ids, DoneCommand done);

    /**
     * Tell target to change (only) PROP_ENABLED breakpoint property to 'false'.
     * @param ids - array of disabled breakpoint identifiers.
     * @param done - command result call back object.
     * @return - pending command handle.
     */
    IToken disable(String[] ids, DoneCommand done);

    /**
     * Tell target to remove breakpoints.
     * @param ids - array of breakpoint identifiers.
     * @param done - command result call back object.
     * @return - pending command handle.
     */
    IToken remove(String[] ids, DoneCommand done);

    /**
     * Upload IDs of breakpoints known to target agent.
     * @param done - command result call back object.
     * @return - pending command handle.
     */
    IToken getIDs(DoneGetIDs done);

    /**
     * Call back interface for 'getIDs' command.
     */
    interface DoneGetIDs {
        /**
         * Called when 'getIDs' command is done.
         * @param token - command handle.
         * @param error - error object or null.
         * @param ids - IDs of breakpoints known to target agent.
         */
        void doneGetIDs(IToken token, Exception error, String[] ids);
    }

    /**
     * Upload properties of given breakpoint from target agent breakpoint table.
     * @param id - unique breakpoint identifier.
     * @param done - command result call back object.
     * @return - pending command handle.
     */
    IToken getProperties(String id, DoneGetProperties done);

    /**
     * Call back interface for 'getProperties' command.
     */
    interface DoneGetProperties {
        /**
         * Called when 'getProperties' command is done.
         * @param token - command handle.
         * @param error - error object or null.
         * @param properties - properties of the breakpoint.
         */
        void doneGetProperties(IToken token, Exception error, Map<String,Object> properties);
    }

    /**
     * Upload status of given breakpoint from target agent.
     * @param id - unique breakpoint identifier.
     * @param done - command result call back object.
     * @return - pending command handle.
     */
    IToken getStatus(String id, DoneGetStatus done);

    /**
     * Call back interface for 'getStatus' command.
     */
    interface DoneGetStatus {
        /**
         * Called when 'getStatus' command is done.
         * @param token - command handle.
         * @param error - error object or null.
         * @param status - status of the breakpoint.
         */
        void doneGetStatus(IToken token, Exception error, Map<String,Object> status);
    }

    /**
     * Report breakpoint service capabilities to clients so they
     * can adjust to different implementations of the service.
     * When called with a null ("") context ID the global capabilities are returned,
     * otherwise context specific capabilities are returned.  A special capability
     * property is used to indicate that all child contexts have the same
     * capabilities.
     * @param id - a context ID or null.
     * @param done - command result call back object.
     * @return - pending command handle.
     */
    IToken getCapabilities(String id, DoneGetCapabilities done);

    /**
     * Call back interface for 'getCapabilities' command.
     */
    interface DoneGetCapabilities {
        /**
         * Called when 'getCapabilities' command is done.
         * @param token - command handle.
         * @param error - error object or null.
         * @param capabilities - breakpoints service capabilities description.
         */
        void doneGetCapabilities(IToken token, Exception error, Map<String,Object> capabilities);
    }

    /**
     * Breakpoints service events listener.
     * Note that contextAdded, contextChanged and contextRemoved events carry exactly same set
     * of breakpoint properties that was sent by a client to a target. The purpose of these events is to
     * let all clients know about breakpoints that were created by other clients.
     */
    interface BreakpointsListener {

        /**
         * Called when breakpoint status changes.
         * @param id - unique breakpoint identifier.
         * @param status - breakpoint status.
         */
        void breakpointStatusChanged(String id, Map<String,Object> status);

        /**
         * Called when a new breakpoints are added.
         * @param bps - array of breakpoints.
         */
        void contextAdded(Map<String,Object>[] bps);

        /**
         * Called when breakpoint properties change.
         * @param bps - array of breakpoints.
         */
        void contextChanged(Map<String,Object>[] bps);

        /**
         * Called when breakpoints are removed.
         * @param ids - array of breakpoint IDs.
         */
        void contextRemoved(String[] ids);
    }

    /**
     * Add breakpoints service event listener.
     * @param listener - object that implements BreakpointsListener interface.
     */
    void addListener(BreakpointsListener listener);

    /**
     * Remove breakpoints service event listener.
     * @param listener - object that implements BreakpointsListener interface.
     */
    void removeListener(BreakpointsListener listener);
}
