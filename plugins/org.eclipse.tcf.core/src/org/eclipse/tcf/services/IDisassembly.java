/*******************************************************************************
 * Copyright (c) 2010-2019 Wind River Systems, Inc. and others.
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
 * TCF Disassembly service interface.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */

public interface IDisassembly extends IService {

    /**
     * This service name, as it appears on the wire - a TCF name of the service.
     */
    static final String NAME = "Disassembly";

    static final String
        /** The name of the instruction set architecture, String */
        CAPABILITY_ISA = "ISA",

        /** If true, simplified mnemonics are supported or requested, Boolean */
        CAPABILITY_SIMPLIFIED = "Simplified",

        /** If true, pseudo-instructions are supported or requested, Boolean */
        CAPABILITY_PSEUDO = "Pseudo",

        /** If true, instruction code bytes are supported or requested, Boolean */
        CAPABILITY_OPCODE = "OpcodeValue";

    /**
     * Retrieve disassembly service capabilities a given context-id.
     * @param context_id - a context ID, usually one returned by Run Control or Memory services.
     * @param done - command result call back object.
     * @return - pending command handle.
     */
    IToken getCapabilities(String context_id, DoneGetCapabilities done);

    /**
     * Call back interface for 'getCapabilities' command.
     */
    interface DoneGetCapabilities {
        /**
         * Called when capabilities retrieval is done.
         * @param token - command handle.
         * @param error - error object or null.
         * @param capabilities - array of capabilities, see CAPABILITY_* for contents of each array element.
         */
        void doneGetCapabilities(IToken token, Throwable error, Map<String,Object>[] capabilities);
    }

    /**
     * Disassemble instruction code from a specified range of memory addresses, in a specified context.
     * @param context_id - a context ID, usually one returned by Run Control or Memory services.
     * @param addr - address of first instruction to disassemble.
     * @param size - size in bytes of the address range.
     * @param params - properties to control the disassembly output, an element of capabilities array, see getCapabilities.
     * @param done - command result call back object.
     * @return - pending command handle.
     */
    IToken disassemble(String context_id, Number addr, int size, Map<String,Object> params, DoneDisassemble done);

    /**
     * Call back interface for 'disassemble' command.
     */
    interface DoneDisassemble {
        /**
         * Called when disassembling is done.
         * @param token - command handle.
         * @param error - error object or null.
         * @param disassembly - array of disassembly lines.
         */
        void doneDisassemble(IToken token, Throwable error, IDisassemblyLine[] disassembly);
    }

    /**
     * Interface to represent a single disassembly line.
     */
    interface IDisassemblyLine {

        /**
         * @return instruction address.
         */
        Number getAddress();

        /**
         * @return instruction size in bytes.
         */
        int getSize();

        /**
         * @return instruction binary representation.
         */
        byte[] getOpcodeValue();

        /**
         * @return array of instruction fields, each field is a collection of field properties, see FIELD_*.
         */
        Map<String,Object>[] getInstruction();
    }

    /** Instruction field properties */
    static final String
        /** The type of the instruction field. See FTYPE_*, String. */
        FIELD_TYPE = "Type",

        /** Value of the field for "String" and "Register" types, String. */
        FIELD_TEXT = "Text",

        /** Value of the field for "Address", "Displacement", or "Immediate" types, Number. */
        FIELD_VALUE = "Value",

        /** Context ID of the address space used with "Address" types, String. */
        FIELD_ADDRESS_SPACE = "AddressSpace";

    /** Instruction field types */
    static final String
        FTYPE_STRING = "String",
        FTYPE_ADDRESS = "Address",
        FTYPE_DISPLACEMENT = "Displacement",
        FTYPE_IMMEDIATE = "Immediate";
    /** @since 1.7 */
    static final String
        FTYPE_REGISTER = "Register";

    /**
     * @deprecated
     */
    static final String
        FTYPE_Register = "Register";
}
