/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * Max Weninger (Wind River) - [361352] [TERMINALS][SSH] Add SSH terminal support
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.services.interfaces.constants;

/**
 * Defines the terminals connector constants.
 */
public interface ITerminalsConnectorConstants {

	/**
	 * Property: The unique id of the terminals view to open.
	 */
	public static final String PROP_ID = "terminal.id"; //$NON-NLS-1$

	/**
	 * Property: The unique secondary id of the terminals view to open.
	 */
	public static final String PROP_SECONDARY_ID = "terminal.secondaryId"; //$NON-NLS-1$

	/**
	 * Property: The title of the terminal tab to open.
	 */
	public static final String PROP_TITLE = "terminal.title"; //$NON-NLS-1$

	/**
	 * Property: The encoding of the terminal tab to open.
	 */
	public static final String PROP_ENCODING = "terminal.encoding"; //$NON-NLS-1$

	/**
	 * Property: Custom data object to associate with the terminal tab.
	 */
	public static final String PROP_DATA = "terminal.data"; //$NON-NLS-1$

	/**
	 * Property: External selection to associate with the terminal tab.
	 */
	public static final String PROP_SELECTION = "terminal.selection"; //$NON-NLS-1$

	/**
	 * Property: Flag to force a new terminal tab.
	 */
	public static final String PROP_FORCE_NEW = "terminal.forceNew"; //$NON-NLS-1$

	/**
	 * Property: Flag to signal if the terminal tab shall have a disconnect button or not.
	 */
	public static final String PROP_HAS_DISCONNECT_BUTTON = "terminal.hasDisconnectButton"; //$NON-NLS-1$

	/**
	 * Property: Terminals launcher delegate id.
	 */
	public static final String PROP_DELEGATE_ID = "terminal.delegateId"; //$NON-NLS-1$

	/**
	 * Property: Terminals connector type id.
	 */
	public static final String PROP_CONNECTOR_TYPE_ID = "terminal.connector.type.id"; //$NON-NLS-1$

	/**
	 * Property: Specific terminal connector type id. Allows clients to
	 *           override the specifically used terminal connector
	 *           implementation for a given type.
	 */
	public static final String PROP_TERMINAL_CONNECTOR_ID = "terminal.tm.terminal.connector.id"; //$NON-NLS-1$

	// ***** Generic terminals connector properties *****

	/**
	 * Property: Timeout to be passed to the terminal connector. The specific terminal
	 *           connector implementation may interpret this value differently. If not
	 *           set, the terminal connector may use a default value.
	 */
	public static final String PROP_TIMEOUT = "terminal.timeout"; //$NON-NLS-1$

	/**
	 * Property: Flag to control if a local echo is needed from the terminal widget.
	 *           <p>Typical for process and streams terminals.
	 */
	public static final String PROP_LOCAL_ECHO = "terminal.localEcho"; //$NON-NLS-1$

	/**
	 * Property: Data flag to tell the terminal to not reconnect when hitting enter
	 *           in a disconnected terminal.
	 *           The flag can be set by adding an IPropertiesContainer with the set
	 *           flag as PROP_DATA.
	 */
	public static final String PROP_DATA_NO_RECONNECT = "terminal.data.noReconnect"; //$NON-NLS-1$

	/**
	 * Property: The line separator used by the terminal input.
	 *           <p>Typical for process and streams terminals.
	 */
	public static final String PROP_LINE_SEPARATOR = "terminal.lineSeparator"; //$NON-NLS-1$

	/**
	 * Property: The list of stdout listeners to attach to the corresponding stream monitor.
	 *           <p>Typical for process and streams terminals.
	 */
	public static final String PROP_STDOUT_LISTENERS = "terminal.stdoutListeners"; //$NON-NLS-1$

	/**
	 * Property: The list of stderr listeners to attach to the corresponding stream monitor.
	 *           <p>Typical for process and streams terminals.
	 */
	public static final String PROP_STDERR_LISTENERS = "terminal.stderrListeners"; //$NON-NLS-1$

	// ***** IP based terminals connector properties *****

	/**
	 * Property: Host name or IP address the terminal server is running.
	 *           <p>Typical for telnet or ssh terminals.
	 */
	public static final String PROP_IP_HOST = "terminal.ip.host"; //$NON-NLS-1$

	/**
	 * Property: Port at which the terminal server is providing the console input and output.
	 *           <p>Typical for telnet or ssh terminals.
	 */
	public static final String PROP_IP_PORT = "terminal.ip.port"; //$NON-NLS-1$

	/**
	 * Property: An offset to add to the specified port number.
	 *           <p>Typical for telnet or ssh terminals.
	 */
	public static final String PROP_IP_PORT_OFFSET = "terminal.ip.port.offset"; //$NON-NLS-1$

	// ***** Process based terminals connector properties *****

	/**
	 * Property: Process image path.
	 * 			 <p>Typical for process terminals.
	 */
	public static final String PROP_PROCESS_PATH = "terminal.process.path"; //$NON-NLS-1$

	/**
	 * Property: Process arguments.
	 *           <p>Typical for process terminals.
	 */
	public static final String PROP_PROCESS_ARGS = "terminal.process.args"; //$NON-NLS-1$

	/**
	 * Property: Process arguments.
	 *           <p>Typical for process terminals.
	 */
	public static final String PROP_PROCESS_WORKING_DIR = "terminal.process.working_dir"; //$NON-NLS-1$

	/**
	 * Property: Process environment.
	 *           <p>Typical for process terminals.
	 */
	public static final String PROP_PROCESS_ENVIRONMENT = "terminal.process.environment"; //$NON-NLS-1$

	/**
	 * Property: Flag to merge process environment with native environment.
	 *           <p>Typical for process terminals.
	 */
	public static final String PROP_PROCESS_MERGE_ENVIRONMENT = "terminal.process.environment.merge"; //$NON-NLS-1$

	/**
	 * Property: Runtime process instance.
     *           <p>Typical for process terminals.
	 */
	public static final String PROP_PROCESS_OBJ = "terminal.process"; //$NON-NLS-1$

	/**
	 * Property: Runtime process PTY instance.
	 *           <p>Typical for process terminals.
	 */
	public static final String PROP_PTY_OBJ = "terminal.pty"; //$NON-NLS-1$

	// ***** Streams based terminals connector properties *****

	/**
	 * Property: Stdin streams instance.
	 *           <p>Typical for streams terminals.
	 */
	public static final String PROP_STREAMS_STDIN = "terminal.streams.stdin"; //$NON-NLS-1$

	/**
	 * Property: Stdout streams instance.
	 *           <p>Typical for streams terminals.
	 */
	public static final String PROP_STREAMS_STDOUT = "terminal.streams.stdout"; //$NON-NLS-1$

	/**
	 * Property: Stderr streams instance.
	 *           <p>Typical for streams terminals.
	 */
	public static final String PROP_STREAMS_STDERR = "terminal.streams.stderr"; //$NON-NLS-1$

	// ***** Ssh specific properties *****

	/**
	 * Property: ssh keep alive value.
	 */
	public static final String PROP_SSH_KEEP_ALIVE = "terminal.ssh.keep_alive"; //$NON-NLS-1$

	/**
	 * Property: Ssh password.
	 */
	public static final String PROP_SSH_PASSWORD = "terminal.ssh.password"; //$NON-NLS-1$

	/**
	 * Property: Ssh user.
	 */
	public static final String PROP_SSH_USER = "terminal.ssh.user"; //$NON-NLS-1$

	// ***** Serial specific properties *****

	/**
	 * The serial device name.
	 */
	public static final String PROP_SERIAL_DEVICE = "terminal.serial.device"; //$NON-NLS-1$

	/**
	 * The baud rate.
	 */
	public static final String PROP_SERIAL_BAUD_RATE = "terminal.serial.baudrate"; //$NON-NLS-1$

	/**
	 * The data bits
	 */
	public static final String PROP_SERIAL_DATA_BITS = "terminal.serial.databits"; //$NON-NLS-1$

	/**
	 * The parity
	 */
	public static final String PROP_SERIAL_PARITY = "terminal.serial.parity"; //$NON-NLS-1$

	/**
	 * The stop bits
	 */
	public static final String PROP_SERIAL_STOP_BITS = "terminal.serial.stopbits"; //$NON-NLS-1$

	/**
	 * The flow control
	 */
	public static final String PROP_SERIAL_FLOW_CONTROL = "terminal.serial.flowcontrol"; //$NON-NLS-1$
}
