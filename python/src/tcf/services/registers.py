# *****************************************************************************
# * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others.
# * All rights reserved. This program and the accompanying materials
# * are made available under the terms of the Eclipse Public License v1.0
# * which accompanies this distribution, and is available at
# * http://www.eclipse.org/legal/epl-v10.html
# *
# * Contributors:
# *     Wind River Systems - initial API and implementation
# *****************************************************************************

"""
Registers service provides access to target CPU register values and properties.
"""

from .. import services

NAME = "Registers"

# Context property names.
# String, ID of the context
PROP_ID = "ID"

# String, ID of a parent context
PROP_PARENT_ID = "ParentID"

# String, process ID
PROP_PROCESS_ID = "ProcessID"

# String, context name
PROP_NAME = "Name"

# String, context description
PROP_DESCRIPTION = "Description"

# Number, context size in bytes. Byte arrays in get/set commands should be same
# size
PROP_SIZE = "Size"

# Boolean, true if context value can be read
PROP_READBLE = "Readable"

# Boolean, true if reading the context (register) destroys its current value
PROP_READ_ONCE = "ReadOnce"

# Boolean, true if context value can be written
PROP_WRITEABLE = "Writeable"

# Boolean, true if register value can not be overwritten - every write counts
PROP_WRITE_ONCE = "WriteOnce"

# Boolean, true if writing the context can change values of other registers
PROP_SIDE_EFFECTS = "SideEffects"

# Boolean, true if the register value can change even when target is stopped
PROP_VOLATILE = "Volatile"

# Boolean, true if the register value is a floating-point value
PROP_FLOAT = "Float"

# Boolean, true if big endian
PROP_BIG_ENDIAN = "BigEndian"

# Boolean, true if the lowest numbered bit should be shown to user as the
# left-most bit
PROP_LEFT_TO_RIGHT = "LeftToRight"

# Number, bit numbering base (0 or 1) to use when showing bits to user
PROP_FIRST_BIT = "FirstBit"

# Number, if context is a bit field, contains the field bit numbers in the
# parent context
PROP_BITS = "Bits"

# Array of Map, predefined names (mnemonics) for some of context values
PROP_VALUES = "Values"

# Number, the address of a memory mapped register
PROP_MEMORY_ADDRESS = "MemoryAddress"

# String, the context ID of a memory context in which a memory mapped register
# is located
PROP_MEMORY_CONTEXT = "MemoryContext"

# Array of String, a list of attribute names which can be searched for starting
# on this context
PROP_CAN_SEARCH = "CanSearch"

# String, the role the register plays in a program execution
PROP_ROLE = "Role"

# Number, when present describes the offset in the data of the parent register
# where the value of a field can be found.
PROP_OFFSET = "Offset"


# Values of context property "Role".
# Program counter. Defines instruction to execute next
ROLE_PC = "PC"

# Register defining the current stack pointer location
ROLE_SP = "SP"

# Register defining the current frame pointer location
ROLE_FP = "FP"

# Register used to store the return address for calls
ROLE_RET = "RET"

# Indicates register or register groups which belong to the core state
ROLE_CORE = "CORE"

# Search filter properties.
# The name of the property this filter applies too
SEARCH_NAME = "Name"

# The value which is searched for
SEARCH_EQUAL_VALUE = "EqualValue"


class RegistersContext(object):
    """
    RegistersContext objects represent register groups, registers and bit
    fields.
    """
    def __init__(self, props):
        self._props = props or {}

    def __str__(self):
        return "[Registers Context %s]" % self._props

    def getProperties(self):
        """
        Get context properties. See PROP_* definitions for property names.
        Context properties are read only, clients should not try to modify
        them.
        @return Map of context properties.
        """
        return self._props

    def getID(self):
        """
        Get Context ID.
        @return context ID.
        """
        return self._props.get(PROP_ID)

    def getParentID(self):
        """
        Get parent context ID.
        @return parent context ID.
        """
        return self._props.get(PROP_PARENT_ID)

    def getProcessID(self):
        """
        Get process ID, if applicable.
        @return process ID.
        """
        return self._props.get(PROP_PROCESS_ID)

    def getName(self):
        """
        Get context (register, register group, bit field) name.
        @return context name.
        """
        return self._props.get(PROP_NAME)

    def getDescription(self):
        """
        Get context description.
        @return context description.
        """
        return self._props.get(PROP_DESCRIPTION)

    def getSize(self):
        """
        Get context size in bytes.
        Byte arrays in get()/set() methods should be same size.
        Hardware register can be smaller then this size, for example in case
        when register size is not an even number of bytes. In such case
        implementation should add/remove padding that consist of necessary
        number of zero bits.
        @return context size in bytes.
        """
        return self._props.get(PROP_SIZE, 0)

    def isReadable(self):
        """
        Check if context value can be read.
        @return true if can read value of the context.
        """
        return self._props.get(PROP_READBLE)

    def isReadOnce(self):
        """
        Check if reading the context (register) destroys its current value -
        it can be read only once.
        @return true if read-once register.
        """
        return self._props.get(PROP_READ_ONCE)

    def isWriteable(self):
        """
        Check if context value can be written.
        @return true if can write value of the context.
        """
        return self._props.get(PROP_WRITEABLE)

    def isWriteOnce(self):
        """
        Check if register value can not be overwritten - every write counts.
        @return true if write-once register.
        """
        return self._props.get(PROP_WRITE_ONCE)

    def hasSideEffects(self):
        """
        Check if writing the context can change values of other registers.
        @return true if has side effects.
        """
        return self._props.get(PROP_SIDE_EFFECTS)

    def isVolatile(self):
        """
        Check if the register value can change even when target is stopped.
        @return true if the register value can change at any time.
        """
        return self._props.get(PROP_VOLATILE)

    def isFloat(self):
        """
        Check if the register value is a floating-point value.
        @return true if a floating-point register.
        """
        return self._props.get(PROP_FLOAT)

    def isBigEndian(self):
        """
        Check endianness of the context.
        Big endian means decreasing numeric significance with increasing bit
        number.
        The endianness is used to encode and decode values of get, getm, set
        and setm commands.
        @return true if big endian.
        """
        return self._props.get(PROP_BIG_ENDIAN)

    def isLeftToRight(self):
        """
        Check if the lowest numbered bit (i.e. bit #0 or bit #1 depending on
        getFirstBitNumber() value) should be shown to user as the left-most bit
        or the right-most bit.
        @return true if the first bit is left-most bit.
        """
        return self._props.get(PROP_LEFT_TO_RIGHT)

    def getFirstBitNumber(self):
        """
        If the context has bit field children, bit positions of the fields
        can be zero-based or 1-based.
        @return first bit position - 0 or 1.
        """
        return self._props.get(PROP_FIRST_BIT, 0)

    def getBitNumbers(self):
        """
        If context is a bit field, get the field bit numbers in parent context.
        @return array of bit numbers.
        """
        return self._props.get(PROP_BITS)

    def getNamedValues(self):
        """
        A context can have predefined names (mnemonics) for some its values.
        This method returns a list of such named values.
        @return array of named values or None.
        """
        return self._props.get(PROP_VALUES)

    def getMemoryAddress(self):
        """
        Get the address of a memory mapped register.
        @return address.
        """
        return self._props.get(PROP_MEMORY_ADDRESS)

    def getMemoryContext(self):
        """
        Get the context ID of a memory context in which a memory mapped
        register is located.
        @return memory context ID.
        """
        return self._props.get(PROP_MEMORY_CONTEXT)

    def canSearch(self):
        """
        Get a list of property names which can be searched for starting on
        this context
        @return collection of property names.
        """
        return self._props.get(PROP_CAN_SEARCH)

    def getRole(self):
        """
        Get the role the register plays in a program execution.
        @return role name.
        """
        return self._props.get(PROP_ROLE)

    def getOffset(self):
        """
        Get the offset in the data of the parent register where the value of a
        field can be found.
        @return Register offset in parent register.
        """
        return self._props.get(PROP_OFFSET)

    def get(self, done):
        """
        Read value of the context.
        @param done - call back object.
        @return - pending command handle.
        """
        raise NotImplementedError("Abstract method")

    def set(self, value, done):  # @ReservedAssignment
        """
        Set value of the context.
        @param value - value to write into the context.
        @param done - call back object.
        @return - pending command handle.
        """
        raise NotImplementedError("Abstract method")

    def search(self, filterProps, done):
        """
        Search register contexts that passes given search filter.
        Search is only supported for properties listed in the "CanSearch"
        property.
        @param filterProps - properties bag that defines search filter.
        @param done - call back object.
        @return - pending command handle.
        """
        raise NotImplementedError("Abstract method")


class RegistersService(services.Service):
    def getName(self):
        return NAME

    def getContext(self, contextID, done):
        """
        Retrieve context info for given context ID.

        @param contextID - context ID.
        @param done - call back interface called when operation is completed.
        """
        raise NotImplementedError("Abstract method")

    def getChildren(self, parent_context_id, done):
        """
        Retrieve contexts available for registers commands.
        A context corresponds to an execution thread, stack frame, registers
        group, etc.
        A context can belong to a parent context. Contexts hierarchy can be
        simple plain list or it can form a tree. It is up to target agent
        developers to choose layout that is most descriptive for a given
        target. Context IDs are valid across all services. In other words, all
        services access same hierarchy of contexts, with same IDs, however,
        each service accesses its own subset of context's attributes and
        functionality, which is relevant to that service.

        @param parent_context_id - parent context ID. Can be None -
                                   to retrieve top level of the hierarchy, or
                                   one of context IDs retrieved by previous
                                   getChildren commands.
        @param done - call back interface called when operation is completed.
        """
        raise NotImplementedError("Abstract method")

    def getm(self, locs, done):
        """
        Read values of multiple locations in registers.
        @param locs - array of data locations.
        @param done - call back object.
        @return - pending command handle.
        """
        raise NotImplementedError("Abstract method")

    def setm(self, locs, value, done):
        """
        Set values of multiple locations in registers.
        @param locs - array of data locations.
        @param value - value to write into the context.
        @param done - call back object.
        @return - pending command handle.
        """
        raise NotImplementedError("Abstract method")

    def addListener(self, listener):
        """
        Add registers service event listener.
        @param listener - event listener implementation.
        """
        raise NotImplementedError("Abstract method")

    def removeListener(self, listener):
        """
        Remove registers service event listener.
        @param listener - event listener implementation.
        """
        raise NotImplementedError("Abstract method")


class NamedValue(object):
    """
    A register context can have predefined names (mnemonics) for some its
    values.
    NamedValue objects represent such values.
    """
    def __init__(self, value, name, description):
        self.value = value
        self.name = name
        self.description = description

    def getValue(self):
        """
        Get value associated with the name.
        @return the value as an array of bytes.
        """
        return self.value

    def getName(self):
        """
        Get name (mnemonic) of the value.
        @return value name.
        """
        return self.name

    def getDescription(self):
        """
        Get human readable description of the value.
        @return value description.
        """
        return self.description


class DoneGet(object):
    """
    'get' command call back interface.
    """
    def doneGet(self, token, error, value):
        """
        Called when value retrieval is done.
        @param token - command handle
        @param error - error description if operation failed, None if
                       succeeded.
        @param value - context value as array of bytes.
        """
        pass


class DoneSet(object):
    """
    'set' command call back interface.
    """
    def doneSet(self, token, error):
        """
        Called when value setting is done.
        @param token - command handle.
        @param error - error description if operation failed, None if
                       succeeded.
        """
        pass


class DoneSearch(object):
    """
    'search' command call back interface.
    """
    def doneSearch(self, token, error, paths):
        """
        Called when context search is done.
        @param token - command handle.
        @param error - error description if operation failed, None if
                       succeeded.
        @param paths - array of paths to each context with properties matching
                       the filter
        """
        pass


class DoneGetContext(object):
    def doneGetContext(self, token, error, context):
        """
        Called when context data retrieval is done.
        @param token - command handle
        @param error - error description if operation failed, None if
                       succeeded.
        @param context - context data.
        """
        pass


class DoneGetChildren(object):
    """
    Client call back interface for getChildren().
    """
    def doneGetChildren(self, token, error, context_ids):
        """
        Called when context list retrieval is done.
        @param token - command handle
        @param error - error description if operation failed, None if
                       succeeded.
        @param context_ids - array of available context IDs.
        """
        pass


class RegistersListener(object):
    """
    Registers event listener is notified when registers context hierarchy
    changes, and when a register is modified by the service commands.
    """

    def contextChanged(self):
        """
        Called when register context properties changed.
        Most targets have static set of registers and register properties.
        Such targets never generate this event. However, some targets,
        for example, JTAG probes, allow user to modify register definitions.
        Clients should flush all cached register context data.
        """
        pass

    def registerChanged(self, contextID):
        """
        Called when register content was changed and clients
        need to update themselves. Clients, at least, should invalidate
        corresponding cached registers data.
        Not every change is notified - it is not possible,
        only those, which are not caused by normal execution of the debuggee.
        At least, changes caused by "set" command should be notified.
        @param contextID - register context ID.
        """
        pass


class Location(object):
    """
    Class Location represents value location in register context
    """
    def __init__(self, contextID, offs, size):
        # Register context ID
        self.id = contextID
        # offset in the context, in bytes
        self.offs = offs
        # value size in bytes
        self.size = size

    def __iter__(self):
        yield self.id
        yield self.offs
        yield self.size
