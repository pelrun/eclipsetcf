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

"""Breakpoint is represented by unique identifier and set of properties.

Breakpoint identifier (String id) needs to be unique across all hosts and
targets.

Breakpoint properties (Map<String,Object>) is extendible collection of named
attributes, which define breakpoint location and behavior. This module defines
some common attribute names (see PROP_*), host tools and target agents may
support additional attributes.

For each breakpoint a target agent maintains another extendible collection of
named attributes: breakpoint status (Map<String,Object>, see STATUS_*). While
breakpoint properties are persistent and represent user input, breakpoint
status reflects dynamic target agent reports about breakpoint current state,
like actual addresses where breakpoint is planted or planting errors.
"""

from .. import services

# Service name.
NAME = "Breakpoints"

# Breakpoint property names.
PROP_ID = "ID"  # String
PROP_ENABLED = "Enabled"  # Boolean
PROP_TYPE = "BreakpointType"  # String
PROP_CONTEXT_NAMES = "ContextNames"  # Array
PROP_CONTEXT_IDS = "ContextIds"  # Array
PROP_EXECUTABLE_PATHS = "ExecPaths"  # Array
PROP_CONTEXT_QUERY = "ContextQuery"  # String, see IContextQuery
PROP_LOCATION = "Location"  # String
PROP_SIZE = "Size"  # Number
PROP_ACCESS_MODE = "AccessMode"  # Number
PROP_FILE = "File"  # String
PROP_LINE = "Line"  # Number
PROP_COLUMN = "Column"  # Number
PROP_PATTERN = "MaskValue"  # Number
PROP_MASK = "Mask"  # Number
PROP_STOP_GROUP = "StopGroup"  # Array
PROP_IGNORE_COUNT = "IgnoreCount"  # Number
PROP_TIME = "Time"  # Number
PROP_SCALE = "TimeScale"  # String
PROP_UNITS = "TimeUnits"  # String
PROP_CONDITION = "Condition"  # String
PROP_TEMPORARY = "Temporary"  # Boolean
PROP_EVENT_TYPE = "EventType"  # String
PROP_EVENT_ARGS = "EventArgs"  # String or Object
PROP_CLIENT_DATA = "ClientData"  # Object

# Deprecated
PROP_CONTEXTNAMES = "ContextNames"  # Array
PROP_CONTEXTIDS = "ContextIds"  # Array
PROP_EXECUTABLEPATHS = "ExecPaths"  # Array
PROP_ACCESSMODE = "AccessMode"  # Number
PROP_IGNORECOUNT = "IgnoreCount"  # Number


# BreakpointType values
TYPE_SOFTWARE = "Software"
TYPE_HARDWARE = "Hardware"
TYPE_AUTO = "Auto"

# AccessMode values
ACCESSMODE_READ = 0x01
ACCESSMODE_WRITE = 0x02
ACCESSMODE_EXECUTE = 0x04
ACCESSMODE_CHANGE = 0x08

# TimeScale values
TIMESCALE_RELATIVE = "Relative"
TIMESCALE_ABSOLUTE = "Absolute"

# TimeUnits values
TIMEUNIT_NSECS = "Nanoseconds"
TIMEUNIT_CYCLE_COUNT = "CycleCount"
TIMEUNIT_INSTRUCTION_COUNT = "InstructionCount"

# Breakpoint status field names.
STATUS_INSTANCES = "Instances"  # Array of Map<String,Object>
STATUS_ERROR = "Error"  # String
STATUS_FILE = "File"  # String
STATUS_LINE = "Line"  # Number
STATUS_COLUMN = "Column"  # Number

# Breakpoint instance field names.
INSTANCE_ERROR = "Error"  # String
INSTANCE_CONTEXT = "LocationContext"  # String
INSTANCE_ADDRESS = "Address"  # Number
INSTANCE_SIZE = "Size"  # Number
INSTANCE_TYPE = "BreakpointType"  # String
INSTANCE_MEMORY_CONTEXT = "MemoryContext"  # String
INSTANCE_HIT_COUNT = "HitCount"  # Number

# Breakpoint service capabilities.
CAPABILITY_CONTEXT_ID = "ID"  # String
CAPABILITY_HAS_CHILDREN = "HasChildren"  # Boolean
CAPABILITY_BREAKPOINT_TYPE = "BreakpointType"  # Boolean
CAPABILITY_LOCATION = "Location"  # Boolean
CAPABILITY_CONDITION = "Condition"  # Boolean
CAPABILITY_FILE_LINE = "FileLine"  # Boolean
CAPABILITY_FILE_MAPPING = "FileMapping"  # Boolean
CAPABILITY_CONTEXT_IDS = "ContextIds"  # Boolean
CAPABILITY_CONTEXT_NAMES = "ContextNames"  # Boolean
CAPABILITY_CONTEXT_QUERY = "ContextQuery"  # Boolean
CAPABILITY_STOP_GROUP = "StopGroup"  # Boolean
CAPABILITY_TEMPORARY = "Temporary"  # Boolean
CAPABILITY_IGNORE_COUNT = "IgnoreCount"  # Boolean
CAPABILITY_ACCESS_MODE = "AccessMode"  # Number
CAPABILITY_CLIENT_DATA = "ClientData"  # Boolean

# Deprecated

CAPABILITY_CONTEXTNAMES = "ContextNames"
CAPABILITY_CONTEXTIDS = "ContextIds"
CAPABILITY_IGNORECOUNT = "IgnoreCount"
CAPABILITY_ACCESSMODE = "AccessMode"


class BreakpointsService(services.Service):
    def getName(self):
        return NAME

    def set(self, properties, done):  # @ReservedAssignment
        """Download breakpoints data to target agent.

        The command is intended to be used only to initialize target
        breakpoints table when communication channel is open. After that, host
        should notify target about (incremental) changes in breakpoint data by
        sending add, change and remove commands.

        @param properties - array of breakpoints.
        @param done - command result call back object.

        @return - pending command handle.
        @see DoneCommand
        """
        raise NotImplementedError("Abstract method")

    def add(self, properties, done):
        """Called when breakpoint is added into breakpoints table.

        @param properties - breakpoint properties.
        @param done - command result call back object.

        @return - pending command handle.
        @see DoneCommand
        """
        raise NotImplementedError("Abstract method")

    def change(self, properties, done):
        """Called when breakpoint properties are changed.

        @param properties - breakpoint properties.
        @param done - command result call back object.

        @return - pending command handle.
        @see DoneCommand
        """
        raise NotImplementedError("Abstract method")

    def enable(self, ids, done):
        """Tell target to change (only) PROP_ENABLED breakpoint property to
        'true'.

        @param ids - array of enabled breakpoint identifiers.
        @param done - command result call back object.

        @return - pending command handle.
        @see DoneCommand
        """
        raise NotImplementedError("Abstract method")

    def disable(self, ids, done):
        """Tell target to change (only) PROP_ENABLED breakpoint property to
        'false'.

        @param ids - array of disabled breakpoint identifiers.
        @param done - command result call back object.

        @return - pending command handle.
        @see DoneCommand
        """
        raise NotImplementedError("Abstract method")

    def remove(self, ids, done):
        """Tell target to remove breakpoints.

        @param id - unique breakpoint identifier.
        @param done - command result call back object.

        @return - pending command handle.
        @see DoneCommand
        """
        raise NotImplementedError("Abstract method")

    def getIDs(self, done):
        """Upload IDs of breakpoints known to target agent.

        @param done - command result call back object.

        @return - pending command handle.
        @see DoneGetIDs
        """
        raise NotImplementedError("Abstract method")

    def getProperties(self, bpID, done):
        """Upload properties of given breakpoint from target agent breakpoint
        table.

        @param bpID - unique breakpoint identifier.
        @param done - command result call back object.

        @see DoneGetProperties
        """
        raise NotImplementedError("Abstract method")

    def getStatus(self, bpID, done):
        """Upload status of given breakpoint from target agent.

        @param bpID - unique breakpoint identifier.
        @param done - command result call back object.

        @return - pending command handle.
        @see DoneGetStatus
        """
        raise NotImplementedError("Abstract method")

    def getCapabilities(self, bpID, done):
        """Report breakpoint service capabilities to clients so they can adjust
        to different implementations of the service.

        When called with a None ("") context ID the global capabilities are
        returned, otherwise context specific capabilities are returned. A
        special capability property is used to indicate that all child contexts
        have the same capabilities.

        @param id - a context ID or None.
        @param done - command result call back object.

        @return - pending command handle.
        @see DoneGetCapabilities
        """
        raise NotImplementedError("Abstract method")

    def addListener(self, listener):
        """Add breakpoints service event listener.

        @param listener - object that implements BreakpointsListener interface.
        """
        raise NotImplementedError("Abstract method")

    def removeListener(self, listener):
        """Remove breakpoints service event listener.

        @param listener - object that implements BreakpointsListener interface.
        """
        raise NotImplementedError("Abstract method")


class DoneCommand(object):
    """Call back interface for breakpoint service commands."""
    def doneCommand(self, token, error):
        """Called when command is done.

        @param token - command handle.
        @param error - error object or None.
        """
        pass


class DoneGetIDs(object):
    """Call back interface for 'getIDs' command."""
    def doneGetIDs(self, token, error, ids):
        """Called when 'getIDs' command is done.

        @param token - command handle.
        @param error - error object or None.
        @param ids - IDs of breakpoints known to target agent.
        """
        pass


class DoneGetProperties(object):
    """Call back interface for 'getProperties' command."""
    def doneGetProperties(self, token, error, properties):
        """Called when 'getProperties' command is done.

        @param token - command handle.
        @param error - error object or None.
        @param properties - properties of the breakpoint.
        """
        pass


class DoneGetStatus(object):
    """Call back interface for 'getStatus' command."""
    def doneGetStatus(self, token, error, status):
        """Called when 'getStatus' command is done.

        @param token - command handle.
        @param error - error object or None.
        @param status - status of the breakpoint.
        """
        pass


class DoneGetCapabilities(object):
    """Call back interface for 'getCapabilities' command."""
    def doneGetCapabilities(self, token, error, capabilities):
        """Called when 'getCapabilities' command is done.

        @param token - command handle.
        @param error - error object or None.
        @param capabilities - breakpoints service capabilities description.
        """
        pass


class BreakpointsListener(object):
    """Breakpoints service events listener.

    Note that contextAdded, contextChanged and contextRemoved events carry
    exactly same set of breakpoint properties that was sent by a client to a
    target. The purpose of these events is to let all clients know about
    breakpoints that were created by other clients.
    """

    def breakpointStatusChanged(self, bpID, status):
        """Called when breakpoint status changes.

        @param bpID - unique breakpoint identifier.
        @param status - breakpoint status.
        """
        pass

    def contextAdded(self, bps):
        """Called when a new breakpoints are added.

        @param bps - array of breakpoints.
        """
        pass

    def contextChanged(self, bps):
        """Called when breakpoint properties change.

        @param bps - array of breakpoints.
        """
        pass

    def contextRemoved(self, ids):
        """Called when breakpoints are removed .

        @param ids - array of breakpoint IDs.
        """
        pass
