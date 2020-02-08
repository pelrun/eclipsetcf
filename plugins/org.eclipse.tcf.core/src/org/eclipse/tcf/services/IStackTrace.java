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
 * Stack Trace service implements thread stack back tracing.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IStackTrace extends IService {

    /**
     * This service name, as it appears on the wire - a TCF name of the service.
     */
    static final String NAME = "StackTrace";

    /**
     * Stack frame context property names.
     */

    static final String PROP_ID = "ID";

    /**
     * Stack frame property:
     * String, stack frame parent ID.
     */
    static final String PROP_PARENT_ID = "ParentID";

    /**
     * Stack frame property:
     * String, stack frame process ID.
     */
    static final String PROP_PROCESS_ID = "ProcessID";

    /**
     * Stack frame property:
     * String, human readable name.
     */
    static final String PROP_NAME = "Name";

    /**
     * Stack frame property:
     * Boolean, true if the frame is top frame on a stack.
     */
    static final String PROP_TOP_FRAME = "TopFrame";

    /**
     * Stack frame property:
     * Integer, stack frame index, starting from stack top (current frame).
     * @since 1.2
     */
    static final String PROP_INDEX = "Index";

    /**
     * Stack frame property:
     * Boolean, true if the frame data was computed using symbols info,
     * false or not set if the data was collected using stack crawl logic.
     * @since 1.2
     */
    static final String PROP_WALK = "Walk";

    /**
     * Stack frame property:
     * Number, stack frame memory address.
     */
    static final String PROP_FRAME_ADDRESS = "FP";

    /**
     * Stack frame property:
     * Number, return address.
     */
    static final String PROP_RETURN_ADDRESS = "RP";

    /**
     * Stack frame property:
     * Number, instruction pointer.
     */
    static final String PROP_INSTRUCTION_ADDRESS = "IP";

    /**
     * Stack frame property:
     * Integer, number of function arguments.
     */
    static final String PROP_ARGUMENTS_COUNT = "ArgsCnt";

    /**
     * Stack frame property:
     * Number, memory address of function arguments.
     */
    static final String PROP_ARGUMENTS_ADDRESS = "ArgsAddr";

    /**
     * Stack frame property:
     * ILineNumbers.CodeArea - source code location of the frame.
     * @since 1.3
     */
    static final String PROP_CODE_AREA = "CodeArea";

    /**
     * Stack frame property:
     * String - function symbol ID.
     * @since 1.3
     */
    static final String PROP_FUNC_ID = "FuncID";

    /**
     * Stack frame property:
     * Integer, stack frame level, starting from stack bottom
     * @deprecated, use "Index" property.
     * Note: "Index" is counted from the top of the stack, while "Level" is counted from the bottom.
     */
    static final String PROP_LEVEL = "Level";

    /**
     * Retrieve context info for given context IDs.
     *
     * The command will fail if parent thread is not suspended.
     * Client can use Run Control service to suspend a thread.
     *
     * @param id - array of context IDs.
     * @param done - call back interface called when operation is completed.
     */
    IToken getContext(String[] id, DoneGetContext done);

    /**
     * Client call back interface for getContext().
     */
    interface DoneGetContext {
        /**
         * Called when context data retrieval is done.
         * @param error - error description if operation failed, null if succeeded.
         * @param context - array of context data or null if error.
         */
        void doneGetContext(IToken token, Exception error, StackTraceContext[] context);
    }

    /**
     * Retrieve stack trace context ID list.
     * Parent context usually corresponds to an execution thread.
     * Some targets have more then one stack. In such case children of a thread
     * are stacks, and stack frames are deeper in the hierarchy - they can be
     * retrieved with additional getChildren commands.
     *
     * Stack frames are ordered from stack bottom to top.
     *
     * The command will fail if parent thread is not suspended.
     * Client can use Run Control service to suspend a thread.
     *
     * @param parent_context_id - parent context ID.
     * @param done - call back interface called when operation is completed.
     */
    IToken getChildren(String parent_context_id, DoneGetChildren done);

    /**
     * Retrieve a range of stack trace context IDs.
     * Parent context usually corresponds to an execution thread.
     *
     * Note: to allow partial and incremental stack tracing, IDs ordering in
     * the range is reversed relative to getChildren command order.
     * For example, range 0..1 represents last two stack frames:
     * current frame (top of the stack) and previous one.
     *
     * The command will fail if parent thread is not suspended.
     * Client can use Run Control service to suspend a thread.
     *
     * @param parent_context_id - parent context ID.
     * @param range_start - start of the range (inclusive).
     * @param range_end - end of the range (inclusive).
     * @param done - call back interface called when operation is completed.
     * @since 1.2
     */
    IToken getChildrenRange(String parent_context_id, int range_start, int range_end, DoneGetChildren done);

    /**
     * Client call back interface for getChildren() and getChildrenRange().
     */
    interface DoneGetChildren {
        /**
         * Called when context ID list retrieval is done.
         * @param error - error description if operation failed, null if succeeded.
         * @param context_ids - array of available context IDs.
         */
        void doneGetChildren(IToken token, Exception error, String[] context_ids);
    }

    /**
     * StackTraceContext represents stack trace objects - stacks and stack frames.
     * @noimplement This interface is not intended to be implemented by clients.
     */
    interface StackTraceContext {

        /**
         * Get Context ID.
         * @return context ID.
         */
        String getID();

        /**
         * Get parent context ID.
         * @return parent context ID.
         */
        String getParentID();

        /**
         * Get context name - if context represents a stack.
         * @return context name or null.
         */
        String getName();

        /**
         * Get memory address of this frame.
         * @return address or null if not a stack frame.
         */
        Number getFrameAddress();

        /**
         * Get program counter saved in this stack frame -
         * it is address of instruction to be executed when the function returns.
         * @return return address or null if not a stack frame.
         */
        Number getReturnAddress();

        /**
         * Get address of the next instruction to be executed in this stack frame.
         * For top frame it is same as PC register value.
         * For other frames it is same as return address of the next frame.
         * @return instruction address or null if not a stack frame.
         */
        Number getInstructionAddress();

        /**
         * Get number of function arguments for this frame.
         * @return function arguments count.
         */
        int getArgumentsCount();

        /**
         * Get address of function arguments area in memory.
         * @return function arguments address or null if not available.
         */
        Number getArgumentsAddress();

        /**
         * Get code area that describes source code location of the frame.
         * If null, client should use LineNumbers service to find frame source location.
         * @return code area or null.
         * @since 1.3
         */
        ILineNumbers.CodeArea getCodeArea();

        /**
         * Get function symbol ID.
         * If null, client should use Symbols service to find function symbol ID.
         * @return function symbol ID or null.
         * @since 1.3
         */
        String getFuncID();

        /**
         * Get complete map of context properties.
         * @return map of context properties.
         */
        Map<String,Object> getProperties();
    }
}
