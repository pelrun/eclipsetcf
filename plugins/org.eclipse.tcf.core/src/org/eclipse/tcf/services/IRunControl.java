/*******************************************************************************
 * Copyright (c) 2007-2021 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.services;

import java.util.Collection;
import java.util.Map;

import org.eclipse.tcf.protocol.IService;
import org.eclipse.tcf.protocol.IToken;

/**
 * Run Control service provides basic run control operations for execution contexts on a target.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IRunControl extends IService {

    /**
     * This service name, as it appears on the wire - a TCF name of the service.
     */
    static final String NAME = "RunControl";

    /* Context property names ---------------------------------------------- */

    static final String
        /** Run control context ID */
        PROP_ID = "ID",

        /** Context parent (owner) ID, for a thread it is same as process ID */
        PROP_PARENT_ID = "ParentID",

        /** Context process (memory space) ID */
        PROP_PROCESS_ID = "ProcessID",

        /** ID of a context that created this context */
        PROP_CREATOR_ID = "CreatorID",

        /** Human readable context name */
        PROP_NAME = "Name",

        /** true if the context is a container. Container can propagate run control commands to his children */
        PROP_IS_CONTAINER = "IsContainer",

        /** true if context has execution state - can be suspended/resumed */
        PROP_HAS_STATE = "HasState",

        /** Bit-set of RM_ values that are supported by the context */
        PROP_CAN_RESUME = "CanResume",

        /** Bit-set of RM_ values that can be used with count > 1 */
        PROP_CAN_COUNT = "CanCount",

        /** true if suspend command is supported by the context */
        PROP_CAN_SUSPEND = "CanSuspend",

        /** true if terminate command is supported by the context */
        PROP_CAN_TERMINATE = "CanTerminate",

        /** true if detach command is supported by the context */
        PROP_CAN_DETACH = "CanDetach",

        /** Context ID of a run control group that contains the context.
         * Members of same group are always suspended and resumed together:
         * resuming/suspending a context resumes/suspends all members of the group */
        PROP_RC_GROUP = "RCGroup",

        /** Context ID of a breakpoints group that contains the context.
         * Members of same group share same breakpoint instances:
         * a breakpoint is planted once for the group, no need to plant
         * the breakpoint for each member of the group */
        PROP_BP_GROUP = "BPGroup",

        /** Context ID of a symbols group that contains the context.
         * Members of a symbols group share same symbol reader configuration settings,
         * like user defined memory map entries and source lookup info */
        PROP_SYMBOLS_GROUP = "SymbolsGroup";

    /** @since 1.3 */
    static final String
        /** Array of String, the access types allowed for this context
         * when accessing context registers.
         */
        PROP_REG_ACCESS_TYPES = "RegAccessTypes";

    /**
     * Values of "RegAccessTypes".
     * @since 1.3
     */
    static final String
        REG_ACCESS_RD_RUNNING = "rd-running",   /** Context supports reading registers while running */
        REG_ACCESS_WR_RUNNUNG = "wr-running";   /** Context supports writing registers while running */

    /**
     * Values of "RegAccessTypes".
     * @since 1.4
     */
    static final String
        REG_ACCESS_RD_STOP = "rd-stop",         /** Debugger should stop the context to read registers */
        REG_ACCESS_WR_STOP = "wr-stop";         /** Debugger should stop the context to write registers */

    /**
     * Context resume modes.
     */
    static final int

        RM_RESUME = 0,

        /**
         * Step over a single instruction.
         * If the instruction is a function call then don't stop until the function returns.
         */
        RM_STEP_OVER = 1,

        /**
         * Step a single instruction.
         * If the instruction is a function call then stop at first instruction of the function.
         */
        RM_STEP_INTO = 2,

        /**
         * Step over a single source code line.
         * If the line contains a function call then don't stop until the function returns.
         */
        RM_STEP_OVER_LINE = 3,

        /**
         * Step a single source code line.
         * If the line contains a function call then stop at first line of the function.
         */
        RM_STEP_INTO_LINE = 4,

        /**
         * Run until control returns from current function.
         */
        RM_STEP_OUT = 5,

        /**
         * Start running backwards.
         * Execution will continue until suspended by command or breakpoint.
         */
        RM_REVERSE_RESUME = 6,

        /**
         * Reverse of RM_STEP_OVER - run backwards over a single instruction.
         * If the instruction is a function call then don't stop until get out of the function.
         */
        RM_REVERSE_STEP_OVER = 7,

        /**
         * Reverse of RM_STEP_INTO.
         * This effectively "un-executes" the previous instruction
         */
        RM_REVERSE_STEP_INTO = 8,

        /**
         * Reverse of RM_STEP_OVER_LINE.
         * Resume backward execution of given context until control reaches an instruction that belongs
         * to a different source line.
         * If the line contains a function call then don't stop until get out of the function.
         * Error is returned if line number information not available.
         */
        RM_REVERSE_STEP_OVER_LINE = 9,

        /**
         * Reverse of RM_STEP_INTO_LINE,
         * Resume backward execution of given context until control reaches an instruction that belongs
         * to a different line of source code.
         * If a function is called, stop at the beginning of the last line of the function code.
         * Error is returned if line number information not available.
         */
        RM_REVERSE_STEP_INTO_LINE = 10,

        /**
         * Reverse of RM_STEP_OUT.
         * Resume backward execution of the given context until control reaches the point where the current function was called.
         */
        RM_REVERSE_STEP_OUT = 11,

        /**
         * Step over instructions until PC is outside the specified range.
         * Any function call within the range is considered to be in range.
         */
        RM_STEP_OVER_RANGE = 12,

        /**
         * Step instruction until PC is outside the specified range for any reason.
         */
        RM_STEP_INTO_RANGE = 13,

        /**
         * Reverse of RM_STEP_OVER_RANGE
         */
        RM_REVERSE_STEP_OVER_RANGE = 14,

        /**
         * Reverse of RM_STEP_INTO_RANGE
         */
        RM_REVERSE_STEP_INTO_RANGE = 15,

        /**
         * Run until the context becomes active - scheduled to run on a target CPU
         */
        RM_UNTIL_ACTIVE = 16,

        /**
         * Run reverse until the context becomes active
         */
        RM_REVERSE_UNTIL_ACTIVE = 17;

    /**
     * State change reason of a context.
     * Reason can be any text, but if it is one of predefined strings,
     * a generic client might be able to handle it better.
     */
    static final String
        /** Context suspended by suspend command */
        REASON_USER_REQUEST = "Suspended",

        /** Context suspended by step command */
        REASON_STEP = "Step",

        /** Context suspended by breakpoint */
        REASON_BREAKPOINT = "Breakpoint",

        /** Context suspended by exception */
        REASON_EXCEPTION = "Exception",

        /** Context suspended as part of container */
        REASON_CONTAINER = "Container",

        /** Context suspended by watchpoint (data breakpoint) */
        REASON_WATCHPOINT = "Watchpoint",

        /** Context suspended because it received a signal */
        REASON_SIGNAL = "Signal",

        /** Context suspended because a shared library is loaded or unloaded */
        REASON_SHAREDLIB = "Shared Library",

        /** Context suspended because of an error in execution environment */
        REASON_ERROR = "Error";


    /* Optional parameters of context state -------------------------------- */

    /**
     * Context state parameter:
     * Integer - signal that caused the context to become suspended.
     */
    static final String STATE_SIGNAL = "Signal";

    /**
     * Context state parameter:
     * String - name of the signal that caused the context to become suspended.
     */
    static final String STATE_SIGNAL_NAME = "SignalName";

    /**
     * Context state parameter:
     * String - description of the signal that caused the context to become suspended.
     */
    static final String STATE_SIGNAL_DESCRIPTION = "SignalDescription";

    /**
     * Context state parameter:
     * Array of string - IDs of breakpoints that were triggered by the context.
     */
    static final String STATE_BREAKPOINT_IDS = "BPs";

    /**
     * Context state parameter:
     * Object - error report that describes a reason why program counter of the context is not available.
     */
    static final String STATE_PC_ERROR = "PCError";

    /**
     * Context state parameter:
     * Object - error report if last stepping operation failed to reach its destination.
     * @since 1.2
     */
    static final String STATE_STEP_ERROR = "StepError";

    /**
     * Context state parameter:
     * Boolean - true if the context is stopped by a function call injection.
     * @since 1.2
     */
    static final String STATE_FUNC_CALL = "FuncCall";

    /**
     * Context state parameter:
     * Boolean - true if the context is running in reverse.
     */
    static final String STATE_REVERSING = "Reversing";

    /**
     * Context state parameter:
     * String - name of a context that owns this context.
     */
    static final String STATE_CONTEXT = "Context";

    /**
     * Context state parameter:
     * String - name of CPU that is executing the context.
     */
    static final String STATE_CPU = "CPU";

    /**
     * Context state parameter:
     * String - name of current state if other than "Running", for example: "Sleeping", "Reset", "No Clock".
     */
    static final String STATE_NAME = "StateName";


    /* Optional parameters of resume command ------------------------------- */

    /**
     * Resume command parameter:
     * Integer - starting address of step range, inclusive.
     */
    static final String RP_RANGE_START = "RangeStart";

    /**
     * Resume command parameter:
     * Integer - ending address of step range, exclusive.
     */
    static final String RP_RANGE_END = "RangeEnd";

    /**
     * Resume command parameter:
     * Boolean - allow to stop in a hidden code during stepping.
     * @since 1.4
     */
    static final String RP_STEP_INTO_HIDDEN = "StepIntoHidden";


    /* Commands ------------------------------------------------------------ */

    /**
     * Retrieve context properties for given context ID.
     *
     * @param id - context ID.
     * @param done - callback interface called when operation is completed.
     */
    IToken getContext(String id, DoneGetContext done);

    /**
     * Client callback interface for getContext().
     */
    interface DoneGetContext {
        /**
         * Called when context data retrieval is done.
         * @param error - error description if operation failed, null if succeeded.
         * @param context - context data.
         */
        void doneGetContext(IToken token, Exception error, RunControlContext context);
    }

    /**
     * Retrieve children of given context.
     *
     * @param parent_context_id - parent context ID. Can be null -
     * to retrieve top level of the hierarchy, or one of context IDs retrieved
     * by previous getContext or getChildren commands.
     * @param done - callback interface called when operation is completed.
     */
    IToken getChildren(String parent_context_id, DoneGetChildren done);

    /**
     * Client callback interface for getChildren().
     */
    interface DoneGetChildren {
        /**
         * Called when context list retrieval is done.
         * @param error - error description if operation failed, null if succeeded.
         * @param context_ids - array of available context IDs.
         */
        void doneGetChildren(IToken token, Exception error, String[] context_ids);
    }

    /**
     * A context corresponds to an execution thread, process, address space, etc.
     * A context can belong to a parent context. Contexts hierarchy can be simple
     * plain list or it can form a tree. It is up to target agent developers to choose
     * layout that is most descriptive for a given target. Context IDs are valid across
     * all services. In other words, all services access same hierarchy of contexts,
     * with same IDs, however, each service accesses its own subset of context's
     * attributes and functionality, which is relevant to that service.
     * @noimplement This interface is not intended to be implemented by clients.
     */
    interface RunControlContext {

        /**
         * Get context properties. See PROP_* definitions for property names.
         * Context properties are read only, clients should not try to modify them.
         * @return Map of context properties.
         */
        Map<String,Object> getProperties();

        /**
         * Retrieve context ID.
         * Same as getProperties().get("ID")
         */
        String getID();

        /**
         * Retrieve parent context ID.
         * Same as getProperties().get("ParentID")
         */
        String getParentID();

        /**
         * Retrieve context process ID.
         * Same as getProperties().get("ProcessID")
         */
        String getProcessID();

        /**
         * Retrieve context creator ID.
         * Same as getProperties().get("CreatorID")
         */
        String getCreatorID();

        /**
         * Retrieve human readable context name.
         * Same as getProperties().get("Name")
         */
        String getName();

        /**
         * Utility method to read context property PROP_IS_CONTAINER.
         * Executing resume or suspend command on a container causes all its children to resume or suspend.
         * @return value of PROP_IS_CONTAINER.
         */
        boolean isContainer();

        /**
         * Utility method to read context property PROP_HAS_STATE.
         * Only context that has a state can be resumed or suspended.
         * @return value of PROP_HAS_STATE.
         */
        boolean hasState();

        /**
         * Utility method to read context property PROP_CAN_SUSPEND.
         * Value 'true' means suspend command is supported by the context,
         * however the method does not check that the command can be executed successfully in
         * the current state of the context. For example, the command still can fail if context is
         * already suspended.
         * @return value of PROP_CAN_SUSPEND.
         */
        boolean canSuspend();

        /**
         * Utility method to read a 'mode' bit in context property PROP_CAN_RESUME.
         * Value 'true' means resume command is supported by the context,
         * however the method does not check that the command can be executed successfully in
         * the current state of the context. For example, the command still can fail if context is
         * already resumed.
         * @param mode - resume mode, see RM_*.
         * @return value of requested bit of PROP_CAN_RESUME.
         */
        boolean canResume(int mode);

        /**
         * Utility method to read a 'mode' bit in context property PROP_CAN_COUNT.
         * Value 'true' means resume command with count other then 1 is supported by the context,
         * however the method does not check that the command can be executed successfully in
         * the current state of the context. For example, the command still can fail if context is
         * already resumed.
         * @param mode - resume mode, see RM_*.
         * @return value of requested bit of PROP_CAN_COUNT.
         */
        boolean canCount(int mode);

        /**
         * Utility method to read context property PROP_CAN_TERMINATE.
         * Value 'true' means terminate command is supported by the context,
         * however the method does not check that the command can be executed successfully in
         * the current state of the context. For example, the command still can fail if the context
         * already has exited.
         * @return value of PROP_CAN_TERMINATE.
         */
        boolean canTerminate();

        /**
         * Utility method to read context property PROP_CAN_DETACH.
         * Value 'true' means detach command is supported by the context,
         * however the method does not check that the command can be executed successfully in
         * the current state of the context. For example, the command still can fail if the context
         * already has exited.
         * @return value of PROP_CAN_DETACH.
         */
        boolean canDetach();

        /**
         * Utility method to read context property PROP_RC_GROUP -
         * context ID of a run control group that contains the context.
         * Members of same group are always suspended and resumed together:
         * resuming/suspending a context resumes/suspends all members of the group.
         * @return value of PROP_RC_GROUP.
         */
        String getRCGroup();

        /**
         * Utility method to read context property PROP_BP_GROUP -
         * context ID of a breakpoints group that contains the context.
         * Members of same group share same breakpoint instances:
         * a breakpoint is planted once for the group, no need to plant
         * the breakpoint for each member of the group
         * @return value of PROP_BP_GROUP or null if the context does not support breakpoints.
         */
        String getBPGroup();

        /**
         * Utility method to read context property PROP_SYMBOLS_GROUP -
         * context ID of a symbols group that contains the context.
         * Members of a symbols group share same symbol reader configuration settings,
         * like user defined memory map entries and source lookup info.
         * @return value of PROP_SYMBOLS_GROUP or null if the context is not a member of a symbols group.
         */
        String getSymbolsGroup();

        /**
         * Get the register access types allowed for this context.
         * @return collection of access type names.
         * @since 1.3
         */
        Collection<String> getRegAccessTypes();

        /**
         * Send a command to retrieve current state of a context.
         * @param done - command result call back object.
         * @return pending command handle, can be used to cancel the command.
         */
        IToken getState(DoneGetState done);

        /**
         * Send a command to retrieve current state of a context.
         * Similar to getState, but does not retrieve PC.
         * With some targets, it can be substantially faster.
         * @param done - command result call back object.
         * @return pending command handle, can be used to cancel the command.
         * @since 1.7
         */
        IToken getMinState(DoneGetMinState done);

        /**
         * Send a command to suspend a context.
         * Also suspends children if context is a container.
         * @param done - command result call back object.
         * @return pending command handle, can be used to cancel the command.
         */
        IToken suspend(DoneCommand done);

        /**
         * Send a command to resume a context.
         * Also resumes children if context is a container.
         * @param mode - defines how to resume the context, see RM_*.
         * @param count - if mode implies stepping, defines how many steps to perform.
         * @param done - command result call back object.
         * @return pending command handle, can be used to cancel the command.
         */
        IToken resume(int mode, int count, DoneCommand done);

        /**
         * Send a command to resume a context.
         * Also resumes children if context is a container.
         * @param mode - defines how to resume the context, see RM_*.
         * @param count - if mode implies stepping, defines how many steps to perform.
         * @param params - resume parameters, for example, step range definition, see RP_*.
         * @param done - command result call back object.
         * @return pending command handle, can be used to cancel the command.
         */
        IToken resume(int mode, int count, Map<String,Object> params, DoneCommand done);

        /**
         * Send a command to terminate a context.
         * @param done - command result call back object.
         * @return pending command handle, can be used to cancel the command.
         */
        IToken terminate(DoneCommand done);

        /**
         * Send a command to detach a context.
         * @param done - command result call back object.
         * @return pending command handle, can be used to cancel the command.
         */
        IToken detach(DoneCommand done);
    }

    interface DoneGetState {
        /**
         * Called when getState command execution is complete.
         * @param token - pending command handle.
         * @param error - command execution error or null.
         * @param suspended - true if the context is suspended
         * @param pc - program counter of the context (if suspended).
         * @param reason - suspend reason (if suspended), see REASON_*.
         * @param params - additional target specific data about context state, see STATE_*.
         */
        void doneGetState(IToken token, Exception error, boolean suspended, String pc,
                String reason, Map<String,Object> params);
    }

    /** @since 1.7 */
    interface DoneGetMinState {
        /**
         * Called when getMinState command execution is complete.
         * @param token - pending command handle.
         * @param error - command execution error or null.
         * @param suspended - true if the context is suspended
         * @param reason - suspend reason (if suspended), see REASON_*.
         * @param params - additional target specific data about context state, see STATE_*.
         */
        void doneGetMinState(IToken token, Exception error, boolean suspended,
                String reason, Map<String,Object> params);
    }

    interface DoneCommand {
        /**
         * Called when run control command execution is complete.
         * @param token - pending command handle.
         * @param error - command execution error or null.
         */
        void doneCommand(IToken token, Exception error);
    }

    /**
     * Add run control event listener.
     * @param listener - run control event listener to add.
     */
    void addListener(RunControlListener listener);

    /**
     * Remove run control event listener.
     * @param listener - run control event listener to remove.
     */
    void removeListener(RunControlListener listener);

    /**
     * Service events listener interface.
     */
    interface RunControlListener {

        /**
         * Called when new contexts are created.
         * @param contexts - array of new context properties.
         */
        void contextAdded(RunControlContext contexts[]);

        /**
         * Called when a context properties changed.
         * @param contexts - array of new context properties.
         */
        void contextChanged(RunControlContext contexts[]);

        /**
         * Called when contexts are removed.
         * @param context_ids - array of removed context IDs.
         */
        void contextRemoved(String context_ids[]);

        /**
         * Called when a thread is suspended.
         * @param context - ID of a context that was suspended.
         * @param pc - program counter of the context, can be null.
         * @param reason - human readable description of suspend reason.
         * @param params - additional, target specific data about suspended context.
         */
        void contextSuspended(String context, String pc,
                String reason, Map<String,Object> params);

        /**
         * Called when a thread is resumed.
         * @param context - ID of a context that was resumed.
         */
        void contextResumed(String context);

        /**
         * Called when target simultaneously suspends multiple threads in a container
         * (process, core, etc.).
         *
         * @param context - ID of a context responsible for the event. It can be container ID or
         * any one of container children, for example, it can be thread that hit "suspend all" breakpoint.
         * Client expected to move focus (selection) to this context.
         * @param pc - program counter of the context.
         * @param reason - suspend reason, see REASON_*.
         * @param params - additional target specific data about context state, see STATE_*.
         * @param suspended_ids - full list of all contexts that were suspended.
         */
        void containerSuspended(String context, String pc,
                String reason, Map<String,Object> params, String[] suspended_ids);

        /**
         * Called when target simultaneously resumes multiple threads in a container (process,
         * core, etc.).
         *
         * @param context_ids - full list of all contexts that were resumed.
         */
        void containerResumed(String[] context_ids);

        /**
         * Called when an exception is detected in a target thread.
         * @param context - ID of a context that caused an exception.
         * @param msg - human readable description of the exception.
         */
        void contextException(String context, String msg);
    }

    /**
     * Service events listener interface - extended.
     */
    interface RunControlListenerV1 extends RunControlListener {

        /**
         * Called when context state changes and the context is not and was not in suspended state.
         * Changes to and from suspended state should be reported by other events:
         * contextSuspended, contextResumed, containerSuspended, containerResumed.
         * @param context - ID of a context that changed state.
         */
        void contextStateChanged(String context);
    }
}
