/*******************************************************************************
 * Copyright (c) 2012 - 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.va;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.IDisposable;
import org.eclipse.tcf.te.runtime.interfaces.ISharedConstants;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.processes.ProcessOutputReaderThread;
import org.eclipse.tcf.te.runtime.utils.net.IPAddressUtil;
import org.eclipse.tcf.te.tcf.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.core.interfaces.tracing.ITraceIds;
import org.eclipse.tcf.te.tcf.core.nls.Messages;
import org.eclipse.tcf.te.tcf.core.peers.Peer;

/**
 * Abstract external value add implementation.
 */
public abstract class AbstractExternalValueAdd extends AbstractValueAdd {
	// The per peer id value add entry map
	/* default */ final Map<String, ValueAddEntry> entries = new HashMap<String, ValueAddEntry>();

	/**
	 * Class representing a value add entry
	 */
	protected class ValueAddEntry implements IDisposable {
		public Process process;
		public IPeer peer;
		public ProcessOutputReaderThread outputReader;
		public ProcessOutputReaderThread errorReader;

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.runtime.interfaces.IDisposable#dispose()
		 */
		@Override
		public void dispose() {
			if (process != null) {
				// Invoke the hook to dispose the value-add process before destroying the process
				if (!disposeProcess(process)) process.destroy();
				process = null;
			}
			if (outputReader != null) { outputReader.interrupt(); outputReader = null; }
			if (errorReader != null) { errorReader.interrupt(); errorReader = null; }
		}
	}

	/**
	 * Called from {@link #dispose()} to allow to customize the shutdown of
	 * the external value-add process. If the method returns with <code>false</code>,
	 * {@link #dispose()} will invoke {@link Process#destroy()} on the passed
	 * in process object.
	 * <p>
	 * The default implementation will do nothing.
	 *
	 * @param process The external value-add process to dispose. Must not be <code>null</code>.
	 * @return <code>True</code> if the external value-add process got successfully disposed, <code>false</code> otherwise.
	 */
	protected boolean disposeProcess(Process process) {
		Assert.isNotNull(process);
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.va.interfaces.IValueAdd#getPeer(java.lang.String)
	 */
	@Override
	public IPeer getPeer(String id) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(id);

		IPeer peer = null;

		ValueAddEntry entry = entries.get(id);
		if (entry != null) {
			peer = entry.peer;
		}

	    return peer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.va.interfaces.IValueAdd#isAlive(java.lang.String, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void isAlive(final String id, final ICallback done) {
		isAlive(id, done, true);
	}

	public void isAlive(final String id, final ICallback done, boolean testResponsive) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(id);
		Assert.isNotNull(done);

		// Assume that the value-add is not alive
		done.setResult(Boolean.FALSE);

		// Query the associated entry
		ValueAddEntry entry = entries.get(id);

		// If no entry is available yet, but a debug peer id
		// is set, create a corresponding entry for it
		if (entry == null && getDebugPeerId() != null) {
			String[] attrs = getDebugPeerId().split(":"); //$NON-NLS-1$
			if (attrs.length == 3) {
				Map<String, String> props = new HashMap<String, String>();
				props.put(IPeer.ATTR_ID, getDebugPeerId());
				props.put(IPeer.ATTR_TRANSPORT_NAME, attrs[0]);
				if (attrs[1].length() > 0) {
					props.put(IPeer.ATTR_IP_HOST, attrs[1]);
				} else {
					props.put(IPeer.ATTR_IP_HOST, IPAddressUtil.getInstance().getIPv4LoopbackAddress());
				}
				props.put(IPeer.ATTR_IP_PORT, attrs[2]);

				entry = new ValueAddEntry();
				entry.peer = new Peer(props);

				entries.put(id, entry);
			}
		}

		if (entry != null) {
			// Check if the process is still alive or has auto-exited already
			boolean exited = false;

			if (entry.process != null) {
				Assert.isNotNull(entry.peer);

				try {
					entry.process.exitValue();
					exited = true;
				} catch (IllegalThreadStateException e) {
					/* ignored on purpose */
				}
			}

			// If the process is still running, try to open a channel
			if (!exited) {
				if (testResponsive) {
					final ValueAddEntry finEntry = entry;
					final IChannel channel = entry.peer.openChannel();
					channel.addChannelListener(new IChannel.IChannelListener() {

						@Override
						public void onChannelOpened() {
							// Remove ourself as channel listener
							channel.removeChannelListener(this);
							// Close the channel, it is not longer needed
							channel.close();
							// Invoke the callback
							done.setResult(Boolean.TRUE);
							done.done(AbstractExternalValueAdd.this, Status.OK_STATUS);
						}

						@Override
						public void onChannelClosed(Throwable error) {
							// Remove ourself as channel listener
							channel.removeChannelListener(this);
							// External value-add is not longer alive, clean up
							entries.remove(id);
							finEntry.dispose();
							// Invoke the callback
							done.done(AbstractExternalValueAdd.this, Status.OK_STATUS);
						}

						@Override
						public void congestionLevel(int level) {
						}
					});
				} else {
					done.setResult(Boolean.TRUE);
					done.done(AbstractExternalValueAdd.this, Status.OK_STATUS);
				}
			} else {
				done.done(AbstractExternalValueAdd.this, Status.OK_STATUS);
			}
		} else {
			done.done(AbstractExternalValueAdd.this, Status.OK_STATUS);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.va.interfaces.IValueAdd#launch(java.lang.String, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void launch(String id, ICallback done) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(id);
		Assert.isNotNull(done);

		ValueAddException error = null;

		// Get the location of the executable image
		IPath path = getLocation();
		if (path != null && path.toFile().canRead()) {
			ValueAddLauncher launcher = createLauncher(id, path);
			try {
				launcher.launch();
			} catch (ValueAddException e) {
				error = e;
			}

			// Prepare the value-add entry
			ValueAddEntry entry = new ValueAddEntry();

			if (error == null) {
				if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
					CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.AbstractExternalValueAdd_start_at, ISharedConstants.TIME_FORMAT.format(new Date(System.currentTimeMillis())), id),
																0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, this);
				}

				// Get the external process
				Process process = launcher.getProcess();
				try {
					// Check if the process exited right after the launch
					int exitCode = process.exitValue();
					// Died -> Construct the error
					error = onProcessDied(launcher, exitCode);

					if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
						CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.AbstractExternalValueAdd_died_at, new Object[] { ISharedConstants.TIME_FORMAT.format(new Date(System.currentTimeMillis())), Integer.valueOf(exitCode), id }),
																	0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, this);
					}
				} catch (IllegalThreadStateException e) {
					// Still running -> Associate the process with the entry
					entry.process = process;
					// Associate the reader threads with the entry
					entry.outputReader = launcher.getOutputReader();
					entry.errorReader = launcher.getErrorReader();

					if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
						CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.AbstractExternalValueAdd_running_at, ISharedConstants.TIME_FORMAT.format(new Date(System.currentTimeMillis())), id),
																	0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, this);
					}
				}
			}

			String output = null;

			if (error == null) {
				// The agent is started with "-S" to write out the peer attributes in JSON format.
				long timeout = getWaitForValueAddOutputTimeout();
				int counter = Long.valueOf(Math.max(timeout, 200) / 200).intValue();

				if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
					CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.AbstractExternalValueAdd_start_waiting_at, new Object[] { ISharedConstants.TIME_FORMAT.format(new Date(System.currentTimeMillis())), Long.valueOf(timeout), Integer.valueOf(counter), id }),
																0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, this);
				}

				while (counter > 0 && output == null) {
					try {
						// Check if the process is still alive or died in the meanwhile
						int exitCode = entry.process.exitValue();
						// Died -> Construct the error
						error = onProcessDied(launcher, exitCode);

						if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
							CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.AbstractExternalValueAdd_died_at, new Object[] { ISharedConstants.TIME_FORMAT.format(new Date(System.currentTimeMillis())), Integer.valueOf(exitCode), id }),
																		0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, this);
						}
					} catch (IllegalThreadStateException e) { /* ignored on purpose */ }

					if (error != null) break;

					// Try to read in the output
					output = launcher.getOutputReader().getOutput();
					if ("".equals(output) || output.indexOf("Server-Properties:") == -1) { //$NON-NLS-1$ //$NON-NLS-2$
						output = null;
						try {
	                        Thread.sleep(200);
                        } catch (InterruptedException e) {
	                        /* ignored on purpose */
                        }
					}
					counter--;
				}
				if (output == null && error == null) {
					String stderr = !"".equals(launcher.getErrorReader().getOutput()) ? NLS.bind(Messages.AbstractExternalValueAdd_error_output, getLabel(), formatErrorOutput(launcher.getErrorReader().getOutput())) : ""; //$NON-NLS-1$ //$NON-NLS-2$
					error = new ValueAddException(new IOException(NLS.bind(Messages.AbstractExternalValueAdd_error_failedToReadOutput, getLabel(), stderr)));
				}
			}

			if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
				CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.AbstractExternalValueAdd_stop_waiting_at, new Object[] { ISharedConstants.TIME_FORMAT.format(new Date(System.currentTimeMillis())), error, id }),
															0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, this);
			}

			 Map<String, String> attrs = null;

			if (error == null) {
				if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
					CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.AbstractExternalValueAdd_output, output, id),
																0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, this);
				}

				// Find the "Server-Properties: ..." string within the output
				int start = output.indexOf("Server-Properties:"); //$NON-NLS-1$
				if (start != -1 && start > 0) {
					output = output.substring(start);
				}

				// Strip away "Server-Properties:"
				output = output.replace("Server-Properties:", " "); //$NON-NLS-1$ //$NON-NLS-2$
				output = output.trim();

				// Expectation is that the value-add is printing the server properties as single line.
				// If we have still a newline in the string, ignore everything after it
				if (output.indexOf('\n') != -1) {
					output = output.substring(0, output.indexOf('\n'));
					output = output.trim();
				}

				// Read into an object
				Object object = null;
				try {
					object = JSON.parseOne(output.getBytes("UTF-8")); //$NON-NLS-1$
			        attrs = new HashMap<String, String>((Map<String, String>)object);
				} catch (IOException e) {
					error = new ValueAddException(e);
				}
			}

			if (error == null) {
				// Construct the peer id from peer attributes

				// The expected peer id is "<transport>:<canonical IP>:<port>"
				String transport = attrs.get(IPeer.ATTR_TRANSPORT_NAME);
				String port = attrs.get(IPeer.ATTR_IP_PORT);
				String ip = IPAddressUtil.getInstance().getIPv4LoopbackAddress();

				if (transport != null && ip != null && port != null) {
					String peerId = transport + ":" + ip + ":" + port; //$NON-NLS-1$ //$NON-NLS-2$
					attrs.put(IPeer.ATTR_ID, peerId);
					attrs.put(IPeer.ATTR_IP_HOST, ip);

					entry.peer = new Peer(attrs);
				} else {
					error = new ValueAddException(new IOException(NLS.bind(Messages.AbstractExternalValueAdd_error_invalidPeerAttributes, getLabel())));
				}
			}

			if (error == null) {
				Assert.isNotNull(entry.process);
				Assert.isNotNull(entry.peer);

				entries.put(id, entry);
			}

			// Stop the buffering of the output reader
			if (launcher.getOutputReader() != null) {
				launcher.getOutputReader().setBuffering(false);
			}
			// Stop the buffering of the error reader
			if (launcher.getErrorReader() != null) {
				launcher.getErrorReader().setBuffering(false);
			}

			// On error, dispose the entry
			if (error != null) entry.dispose();
		} else {
			error = new ValueAddException(new FileNotFoundException(NLS.bind(Messages.AbstractExternalValueAdd_error_invalidLocation, getLabel(), (path != null ? path.toOSString() : "n/a")))); //$NON-NLS-1$
		}

		IStatus status = Status.OK_STATUS;
		if (error != null) {
			status = new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), error.getLocalizedMessage(), error);
		}

	    done.done(AbstractExternalValueAdd.this, status);
	}

	/**
	 * Returns the absolute path to the value-add executable image.
	 *
	 * @return The absolute path or <code>null</code> if not found.
	 */
	protected abstract IPath getLocation();

	/**
	 * Called if the value add process dies while launching.
	 *
	 * @param launcher The value add launcher. Must not be <code>null</code>.
	 * @param exitCode The process exit code.
	 *
	 * @return The error to report.
	 */
	protected ValueAddException onProcessDied(ValueAddLauncher launcher, int exitCode) {
		Assert.isNotNull(launcher);

		// Read the error output if there is any
		String output = launcher.getErrorReader() != null ? launcher.getErrorReader().getOutput() : null;
		String cause = output != null && !"".equals(output) ? NLS.bind(Messages.AbstractExternalValueAdd_error_cause, formatErrorOutput(output)) : null; //$NON-NLS-1$
		// Create the exception
		String message = NLS.bind(Messages.AbstractExternalValueAdd_error_processDied, getLabel(), Integer.valueOf(exitCode));
		return new ValueAddException(new IOException(cause != null ? message + cause : message));
	}

	/**
	 * Formats the error output to possible beautify it for the user.
	 * <p>
	 * The default implementation returns the passed in output unmodified.
	 *
	 * @param output The output. Must not be <code>null</code>.
	 * @return The beautified output.
	 */
	protected String formatErrorOutput(String output) {
		Assert.isNotNull(output);
		return output;
	}

	/**
	 * Returns the timeout to wait from the value-add output to appear.
	 * <p>
	 * The timeout is in milliseconds and ideally should be <code>&lt;n&gt; * 200</code>.
	 *
	 * @return The timeout to wait for the value-add output to appear in milliseconds.
	 */
	protected long getWaitForValueAddOutputTimeout() {
		return 10 * 200;
	}

	/**
	 * Create a new value-add launcher instance.
	 *
	 * @param id The target peer id. Must not be <code>null</code>.
	 * @param path The absolute path to the value-add executable image. Must not be <code>null</code>.
	 *
	 * @return The value-add launcher instance.
	 */
	protected ValueAddLauncher createLauncher(String id, IPath path) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(id);
		Assert.isNotNull(path);

		return new ValueAddLauncher(id, path, getLabel() != null ? getLabel() : getId());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.va.interfaces.IValueAdd#shutdown(java.lang.String, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void shutdown(final String id, final ICallback done) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(id);
		Assert.isNotNull(done);

		final ValueAddEntry entry = entries.get(id);
		if (entry != null) {
			isAlive(id, new Callback() {
				@Override
				protected void internalDone(Object caller, IStatus status) {
					boolean alive = ((Boolean)getResult()).booleanValue();
					if (alive) {
						entries.remove(id);
						entry.dispose();
					}
					done.done(AbstractExternalValueAdd.this, Status.OK_STATUS);
				}
			}, false);
		} else {
			done.done(AbstractExternalValueAdd.this, Status.OK_STATUS);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.va.interfaces.IValueAdd#shutdownAll(org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void shutdownAll(ICallback done) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(done);

		// On shutdown all, we don't care about the alive state of the value-add.
		// We force the value-add to shutdown if not yet gone by destroying the process.
		for (Entry<String, ValueAddEntry> entry : entries.entrySet()) {
			ValueAddEntry value = entry.getValue();
			value.dispose();
		}
		// Clear all entries from the list
		entries.clear();

		done.done(AbstractExternalValueAdd.this, Status.OK_STATUS);
	}
}
