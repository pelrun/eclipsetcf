/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.launcher;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IProcesses;
import org.eclipse.tcf.services.IProcessesV1;
import org.eclipse.tcf.services.IStreams;
import org.eclipse.tcf.te.runtime.callback.AsyncCallbackCollector;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.IDisposable;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.async.CallbackInvocationDelegate;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager.IStreamsListenerProxy;
import org.eclipse.tcf.te.tcf.core.streams.StreamsDataProvider;
import org.eclipse.tcf.te.tcf.core.streams.StreamsDataReceiver;
import org.eclipse.tcf.te.tcf.core.util.ExceptionUtils;
import org.eclipse.tcf.te.tcf.processes.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.launcher.IProcessContextAwareListener;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.tracing.ITraceIds;
import org.eclipse.tcf.te.tcf.processes.core.nls.Messages;
import org.eclipse.tcf.util.TCFTask;

/**
 * Remote process streams listener implementation.
 */
public class ProcessStreamsListener implements IChannelManager.IStreamsListener, IProcessContextAwareListener, IDisposable {
	// The channel instance
	/* default */ IChannel channel;
	// The streams service instance
	/* default */ IStreams svcStreams;
	// The processes service name
	/* default */ String svcProcessesName;
	// The remote process context
	private IProcesses.ProcessContext context;
	// The list of registered stream data receivers
	private final List<StreamsDataReceiver> dataReceiver = new ArrayList<StreamsDataReceiver>();
	// The stream data provider
	private StreamsDataProvider dataProvider;
	// The list of created runnable's
	private final List<Runnable> runnables = new ArrayList<Runnable>();
	// The streams listener proxy instance
	private IChannelManager.IStreamsListenerProxy proxy = null;
	// The list of already processed streams created events (simple string in format "<stream type>;<stream id>;<context id>")
	/* default */ List<String> processedCreatedEvents = new ArrayList<String>();

	/**
	 * Remote process stream reader runnable implementation. The
	 * runnable will be executed within a thread and is responsible to read the
	 * incoming data from the associated stream and forward them to the registered receivers.
	 */
	protected class StreamReaderRunnable implements Runnable {
		// The associated stream id
		/* default */ final String streamId;
		// The associated stream type id
		private final String streamTypeId;
		// The list of receivers applicable for the associated stream type id
		private final List<StreamsDataReceiver> receivers = new ArrayList<StreamsDataReceiver>();
		// The currently active read task
		private TCFTask<ReadData> activeTask;
		// The callback to invoke if the runnable stopped
		private ICallback callback;

		// Flag to stop the runnable
		private boolean stopped = false;

		/**
		 * Immutable class describing the result returned by {@link StreamReaderRunnable#read(IStreams, String, int)}.
		 */
		protected class ReadData {
			/**
			 * The number of lost bytes in case of a buffer overflow. If <code>-1</code>,
			 * an unknown number of bytes were lost. If non-zero and <code>data.length</code> is
			 * non-zero, the lost bytes are considered located right before the read bytes.
			 */
			public final int lostBytes;
			/**
			 * The read data as byte array.
			 */
			public final byte[] data;
			/**
			 * Flag to signal if the end of the stream has been reached.
			 */
			public final boolean eos;

			/**
			 * Constructor.
			 */
			public ReadData(int lostBytes, byte[] data, boolean eos) {
				this.lostBytes = lostBytes;
				this.data = data;
				this.eos = eos;
			}
		}

		/**
		 * Constructor.
		 *
		 * @param streamId The associated stream id. Must not be <code>null</code>.
		 * @param streamTypeId The associated stream type id. Must not be <code>null</code>.
		 * @param receivers The list of registered data receivers. Must not be <code>null</code>.
		 */
		public StreamReaderRunnable(String streamId, String streamTypeId, StreamsDataReceiver[] receivers) {
			Assert.isNotNull(streamId);
			Assert.isNotNull(streamTypeId);
			Assert.isNotNull(receivers);

			this.streamId = streamId;
			this.streamTypeId = streamTypeId;

			// Loop the list of receivers and filter out the applicable ones
			for (StreamsDataReceiver receiver : receivers) {
				if (receiver.isApplicable(this.streamTypeId))
					this.receivers.add(receiver);
			}
		}

		/**
		 * Returns the associated stream id.
		 *
		 * @return The associated stream id.
		 */
		public final String getStreamId() {
			return streamId;
		}

		/**
		 * Returns if or if not the list of applicable receivers is empty.
		 *
		 * @return <code>True</code> if the list of applicable receivers is empty, <code>false</code> otherwise.
		 */
		public final boolean isEmpty() {
			return receivers.isEmpty();
		}

		/**
		 * Stop the runnable.
		 *
		 * @param callback The callback to invoke if the runnable stopped.
		 */
		public final synchronized void stop(ICallback callback) {
			onEOF(callback);
			// Mark the runnable as stopped
			stopped = true;
		}

		/**
		 * Notify callback on EOF.
		 *
		 * @param callback The callback to invoke on EOF
		 */
		public final synchronized void onEOF(ICallback callback) {
			// If the runnable is stopped already, invoke the callback directly
			if (stopped) {
				if (callback != null) callback.done(this, Status.OK_STATUS);
				return;
			}

			// Store the callback instance
			this.callback = callback;
		}

		/**
		 * Returns if the runnable should stop.
		 */
		protected final synchronized boolean isStopped() {
			return stopped;
		}

		/**
		 * Sets the currently active reader task.
		 *
		 * @param task The currently active reader task or <code>null</code>.
		 */
		protected final void setActiveTask(TCFTask<ReadData> task) {
			activeTask = task;
		}

		/**
		 * Returns the currently active reader task.
		 *
		 * @return The currently active reader task or <code>null</code>.
		 */
		protected final TCFTask<ReadData> getActiveTask() {
			return activeTask;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
        public void run() {
			// Create a snapshot of the receivers
			final StreamsDataReceiver[] receivers = this.receivers.toArray(new StreamsDataReceiver[this.receivers.size()]);

			// Run until stopped and the streams service is available
			while (!isStopped() && svcStreams != null) {
				try {
					ReadData streamData = read(svcStreams, streamId, 1024);
					if (streamData != null) {
						// Check if the received data contains some stream data
						if (streamData.data != null) {
							// Notify the data receivers about the new received data
							notifyReceiver(new String(streamData.data), receivers);
						}
						// If the end of the stream have been reached --> break out
						if (streamData.eos) {
							break;
						}
					}
				} catch (Exception e) {
					// An error occurred -> Dump to the error log
					e = ExceptionUtils.checkAndUnwrapException(e);
					// Check if the blocking read task got canceled
					if (!isStopped() && !(e instanceof CancellationException)) {
						// Log the error to the user, might be something serious
						IStatus status = new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(),
													NLS.bind(Messages.ProcessStreamReaderRunnable_error_readFailed, streamId, e.getLocalizedMessage()),
													e);
						Platform.getLog(CoreBundleActivator.getContext().getBundle()).log(status);
					}
					// break out of the loop
					break;
				}
			}

			// Disconnect from the stream
			if (svcStreams != null) {
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						svcStreams.disconnect(streamId, new IStreams.DoneDisconnect() {
							@Override
		                    @SuppressWarnings("synthetic-access")
							public void doneDisconnect(IToken token, Exception error) {
								synchronized (this) {
									// Mark the runnable definitely stopped
									stopped = true;
									// Disconnect is done, ignore any error, invoke the callback
									if (callback != null) callback.done(this, Status.OK_STATUS);
								}
							}
						});
					}
				};

				Protocol.invokeLater(runnable);
			} else {
				synchronized (this) {
					// Mark the runnable definitely stopped
					stopped = true;
					// Invoke the callback directly, if any
					if (callback != null) callback.done(this, Status.OK_STATUS);
				}
			}
			// Make sure that data receivers are disposed before leaving the thread.
			// This will closes the PipedOutputStream wrapped by the writer in the data receivers,
			// thus the PipedInputStream read an EOS and terminate gracefully.
			for(StreamsDataReceiver receiver:receivers) {
				receiver.dispose();
			}
		}

		/**
		 * Reads data from the stream and blocks until some data has been received.
		 *
		 * @param service The streams service. Must not be <code>null</code>.
		 * @param streamId The stream id. Must not be <code>null</code>.
		 * @param size The size of the data to read.
		 *
		 * @return The read data.
		 *
		 * @throws Exception In case the read fails.
		 */
		protected final ReadData read(final IStreams service, final String streamId, final int size) throws Exception {
			Assert.isNotNull(service);
			Assert.isNotNull(streamId);
			Assert.isTrue(!Protocol.isDispatchThread());

			// Create the task object
			TCFTask<ReadData> task = new TCFTask<ReadData>(channel) {
				@Override
                public void run() {
					service.read(streamId, size, new IStreams.DoneRead() {
						/* (non-Javadoc)
						 * @see org.eclipse.tcf.services.IStreams.DoneRead#doneRead(org.eclipse.tcf.protocol.IToken, java.lang.Exception, int, byte[], boolean)
						 */
						@Override
                        public void doneRead(IToken token, Exception error, int lostSize, byte[] data, boolean eos) {
							if (error == null) done(new ReadData(lostSize, data, eos));
							else if (!isStopped())
								error(error);
						}
					});
				}
			};

			// Push the task object to the runnable instance
			setActiveTask(task);

			// Block until some data is received
			return task.get();
		}

		/**
		 * Notify the data receiver that some data has been received.
		 *
		 * @param data The data or <code>null</code>.
		 */
		protected final void notifyReceiver(final String data, final StreamsDataReceiver[] receivers) {
			if (data == null) return;
			// Notify the data receiver
			for (StreamsDataReceiver receiver : receivers) {
				try {
					// Get the writer
					Writer writer = receiver.getWriter();
					// Append the data
					writer.write(data);
					// And flush it
					writer.flush();
					// Notify potential listeners
					receiver.notifyListener(data);
				} catch (IOException e) {
					if (CoreBundleActivator.getTraceHandler().isSlotEnabled(1, null)) {
						IStatus status = new Status(IStatus.WARNING, CoreBundleActivator.getUniqueIdentifier(),
													NLS.bind(Messages.ProcessStreamReaderRunnable_error_appendFailed, streamId, data),
													e);
						Platform.getLog(CoreBundleActivator.getContext().getBundle()).log(status);
					}
				}
			}
		}
	}

	/**
	 * Default TCF remote process stream writer runnable implementation. The
	 * runnable will be executed within a thread and is responsible to read the
	 * incoming data from the registered providers and forward them to the associated stream.
	 */
	protected class StreamWriterRunnable implements Runnable {
		// The associated stream id
		/* default */ final String streamId;
		// The associated stream type id
		private final String streamTypeId;
		// The data provider applicable for the associated stream type id
		private final StreamsDataProvider provider;
		// The currently active write task
		private TCFTask<Object> activeTask;
		// The callback to invoke if the runnable stopped
		private ICallback callback;

		// Flag to stop the runnable
		private boolean stopped = false;

		/**
		 * Constructor.
		 *
		 * @param streamId The associated stream id. Must not be <code>null</code>.
		 * @param streamTypeId The associated stream type id. Must not be <code>null</code>.
		 * @param provider The data provider. Must not be <code>null</code> and must be applicable for the stream type.
		 */
		public StreamWriterRunnable(String streamId, String streamTypeId, StreamsDataProvider provider) {
			Assert.isNotNull(streamId);
			Assert.isNotNull(streamTypeId);
			Assert.isNotNull(provider);
			Assert.isTrue(provider.isApplicable(streamTypeId));

			this.streamId = streamId;
			this.streamTypeId = streamTypeId;
			this.provider = provider;
		}

		/**
		 * Returns the associated stream id.
		 *
		 * @return The associated stream id.
		 */
		public final String getStreamId() {
			return streamId;
		}

		/**
		 * Returns the associated stream type id.
		 *
		 * @return The associated stream type id.
		 */
		public final String getStreamTypeId() {
			return streamTypeId;
		}

		/**
		 * Stop the runnable.
		 *
		 * @param callback The callback to invoke if the runnable stopped.
		 */
		public final synchronized void stop(ICallback callback) {
			onEOF(callback);
			// Mark the runnable as stopped
			stopped = true;
		}

		/**
		 * Notify callback on EOF.
		 *
		 * @param callback The callback to invoke on EOF
		 */
		public final synchronized void onEOF(ICallback callback) {
			// If the runnable is stopped already, invoke the callback directly
			if (stopped) {
				if (callback != null) callback.done(this, Status.OK_STATUS);
				return;
			}

			// Store the callback instance
			if (callback != null)
				this.callback = callback;
		}

		/**
		 * Returns if the runnable should stop.
		 */
		protected final synchronized boolean isStopped() {
			return stopped;
		}

		/**
		 * Sets the currently active writer task.
		 *
		 * @param task The currently active writer task or <code>null</code>.
		 */
		protected final void setActiveTask(TCFTask<Object> task) {
			activeTask = task;
		}

		/**
		 * Returns the currently active writer task.
		 *
		 * @return The currently active writer task or <code>null</code>.
		 */
		protected final TCFTask<Object> getActiveTask() {
			return activeTask;
		}

		/**
		 * Returns the callback instance to invoke.
		 *
		 * @return The callback instance or <code>null</code>.
		 */
		protected final ICallback getCallback() {
			return callback;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
        public void run() {
			// If not data provider is set, we are done here immediately
			if (provider == null) {
				synchronized (this) {
					// Mark the runnable definitely stopped
					stopped = true;
				}
				// Invoke the callback directly, if any
				if (callback != null) callback.done(this, Status.OK_STATUS);

				return;
			}

			// Create the data buffer instance
			final char[] buffer = new char[1024];

			// Run until stopped and the streams service is available
			while (!isStopped() && svcStreams != null) {
				try {
					// Read available data from the data provider
					int charactersRead = provider.getReader().read(buffer, 0, 1024);
					// Have we reached the end of the stream -> break out
					if (charactersRead == -1) break;
					// If we read some data from the provider, write it to the stream
					if (charactersRead > 0) write(svcStreams, streamId, new String(buffer).getBytes(), charactersRead);
				} catch (Exception e) {
					// An error occurred -> Dump to the error log
					e = ExceptionUtils.checkAndUnwrapException(e);
					// Check if the blocking read task got canceled
					if (!(e instanceof CancellationException)) {
						// Log the error to the user, might be something serious
						IStatus status = new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(),
													NLS.bind(Messages.ProcessStreamWriterRunnable_error_writeFailed, streamId, e.getLocalizedMessage()),
													e);
						Platform.getLog(CoreBundleActivator.getContext().getBundle()).log(status);
					}
					// break out of the loop
					break;
				}
			}

			// Disconnect from the stream
			if (svcStreams != null) {
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						svcStreams.eos(streamId, new IStreams.DoneEOS() {
							@Override
							public void doneEOS(IToken token, Exception error) {
								svcStreams.disconnect(streamId, new IStreams.DoneDisconnect() {
									@Override
				                    @SuppressWarnings("synthetic-access")
									public void doneDisconnect(IToken token, Exception error) {
										synchronized (this) {
											// Mark the runnable definitely stopped
											stopped = true;
										}
										// Disconnect is done, ignore any error, invoke the callback
										if (getCallback() != null) getCallback().done(this, Status.OK_STATUS);
									}
								});
							}
						});
					}
				};

				Protocol.invokeLater(runnable);
			} else {
				synchronized (this) {
					// Mark the runnable definitely stopped
					stopped = true;
				}
				// Invoke the callback directly, if any
				if (callback != null) callback.done(this, Status.OK_STATUS);
			}
		}

		/**
		 * Writes data to the stream.
		 *
		 * @param service The streams service. Must not be <code>null</code>.
		 * @param streamId The stream id. Must not be <code>null</code>.
		 * @param data The data buffer. Must not be <code>null</code>.
		 * @param size The size of the data to write.
		 *
		 * @throws Exception In case the write fails.
		 */
		protected final void write(final IStreams service, final String streamId, final byte[] data, final int size) throws Exception {
			Assert.isNotNull(service);
			Assert.isNotNull(streamId);
			Assert.isTrue(!Protocol.isDispatchThread());

			// Create the task object
			TCFTask<Object> task = new TCFTask<Object>() {
				@Override
                public void run() {
					service.write(streamId, data, 0, size, new IStreams.DoneWrite() {
						/* (non-Javadoc)
						 * @see org.eclipse.tcf.services.IStreams.DoneWrite#doneWrite(org.eclipse.tcf.protocol.IToken, java.lang.Exception)
						 */
						@Override
                        public void doneWrite(IToken token, Exception error) {
							if (error == null) done(null);
							else error(error);
						}
					});
				}
			};
			task.get();

			// Push the task object to the runnable instance
			setActiveTask(task);

			// Execute the write
			task.get();
		}
	}

	/**
	 * Constructor.
	 *
	 * @param parent The parent process launcher instance. Must not be <code>null</code>
	 */
	public ProcessStreamsListener(ProcessLauncher parent) {
		Assert.isNotNull(parent);
		this.channel = parent.getChannel();
		this.svcStreams = parent.getSvcStreams();
		this.svcProcessesName = parent.getSvcProcesses() instanceof IProcessesV1 ? IProcessesV1.NAME : IProcesses.NAME;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IDisposable#dispose()
	 */
	@Override
	public void dispose() {
		dispose(false, null);
	}

	/**
	 * Dispose the streams listener instance.
	 *
	 * @param callback The callback to invoke if the dispose finished or <code>null</code>.
	 */
	public void dispose(final ICallback callback) {
		dispose(false, callback);
	}

	/**
	 * Dispose the streams listener instance only on EOF.
	 * This allows to keep reading streams after the process has exited.
	 *
	 * @param callback The callback to invoke if the dispose finished or <code>null</code>.
	 */
	void disposeOnEOF(final ICallback callback) {
		dispose(true, callback);
	}

	/**
	 * Dispose the streams listener instance.
	 * @param onEof dispose as soon as all streams have seen EOF
	 * @param callback The callback to invoke if the dispose finished or <code>null</code>.
	 */
	private void dispose(boolean onEof, final ICallback callback) {
		// Store a final reference to the streams listener instance
		final IChannelManager.IStreamsListener finStreamsListener = this;

		// Store a final reference to the data receivers list
		final List<StreamsDataReceiver> finDataReceivers;
		synchronized (dataReceiver) {
			finDataReceivers = new ArrayList<StreamsDataReceiver>(dataReceiver);
			dataReceiver.clear();
		}

		// Create a new collector to catch all runnable stop callback's
		AsyncCallbackCollector collector = new AsyncCallbackCollector(new Callback() {
			/* (non-Javadoc)
			 * @see org.eclipse.tcf.te.runtime.callback.Callback#internalDone(java.lang.Object, org.eclipse.core.runtime.IStatus)
			 */
			@Override
			protected void internalDone(final Object caller, final IStatus status) {
				Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
				// Unsubscribe the streams listener from the service
				Tcf.getChannelManager().unsubscribeStream(channel, svcProcessesName, finStreamsListener, new IChannelManager.DoneUnsubscribeStream() {
					@Override
					public void doneUnsubscribeStream(Throwable error) {
						// Loop all registered listeners and close them
						for (StreamsDataReceiver receiver : finDataReceivers) receiver.dispose();
						// Call the original outer callback
						if (callback != null) callback.done(caller, status);
					}
				});

				// Clean the list of processed created events
				processedCreatedEvents.clear();
			}
		}, new CallbackInvocationDelegate());

		// Loop all runnable's and attach our callback
		synchronized (runnables) {
			for (Runnable runnable : runnables) {
				AsyncCallbackCollector.SimpleCollectorCallback cb = new AsyncCallbackCollector.SimpleCollectorCallback(collector);
				if (runnable instanceof StreamReaderRunnable) {
					if (onEof)
						((StreamReaderRunnable)runnable).onEOF(cb);
					else
						((StreamReaderRunnable)runnable).stop(cb);
				}
				else if (runnable instanceof StreamWriterRunnable) {
					if (onEof)
						((StreamWriterRunnable)runnable).onEOF(cb);
					else
						((StreamWriterRunnable)runnable).stop(cb);
				}
			}
			runnables.clear();
		}

		// Mark the collector initialization done
		collector.initDone();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager.IStreamsListener#setProxy(org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager.IStreamsListenerProxy)
	 */
	@Override
	public void setProxy(IStreamsListenerProxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * Adds the given receiver to the stream data receiver list.
	 *
	 * @param receiver The data receiver. Must not be <code>null</code>.
	 */
	public void registerDataReceiver(StreamsDataReceiver receiver) {
		Assert.isNotNull(receiver);
		synchronized (dataReceiver) {
			if (!dataReceiver.contains(receiver)) dataReceiver.add(receiver);
		}
	}

	/**
	 * Removes the given receiver from the stream data receiver list.
	 *
	 * @param receiver The data receiver. Must not be <code>null</code>.
	 */
	public void unregisterDataReceiver(StreamsDataReceiver receiver) {
		Assert.isNotNull(receiver);
		synchronized (dataReceiver) {
			dataReceiver.remove(receiver);
		}
	}

	/**
	 * Sets the stream data provider instance.
	 *
	 * @param provider The stream data provider instance or <code>null</code>.
	 */
	public void setDataProvider(StreamsDataProvider provider) {
		dataProvider = provider;
	}

	/**
	 * Returns the stream data provider instance.
	 *
	 * @return The stream data provider instance or <code>null</code>.
	 */
	public StreamsDataProvider getDataProvider() {
		return dataProvider;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.core.interfaces.launcher.IProcessContextAwareListener#setProcessContext(org.eclipse.tcf.services.IProcesses.ProcessContext)
	 */
	@Override
    public void setProcessContext(IProcesses.ProcessContext context) {
		Assert.isNotNull(context);
		this.context = context;

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_STREAMS_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("Process context set to: id='" + context.getID() + "', name='" + context.getName() + "'", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			                                            0, ITraceIds.TRACE_STREAMS_LISTENER,
			                                            IStatus.INFO, getClass());
		}

		// Ask the proxy to process all delayed created events
		if (proxy != null) proxy.processDelayedCreatedEvents();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.core.interfaces.launcher.IProcessContextAwareListener#getProcessContext()
	 */
	@Override
    public final IProcesses.ProcessContext getProcessContext() {
		return context;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager.IStreamsListener#hasContext()
	 */
	@Override
	public final boolean hasContext() {
		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_STREAMS_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("Remote process stream listener: hasContext = " + (context != null), //$NON-NLS-1$
			                                            0, ITraceIds.TRACE_STREAMS_LISTENER,
			                                            IStatus.INFO, getClass());
		}
	    return context != null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager.IStreamsListener#isCreatedConsumed(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public final boolean isCreatedConsumed(String stream_type, String stream_id, String context_id) {
		boolean consumed = context != null && context.getID().equals(context_id);

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_STREAMS_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("Remote process stream listener: isCreatedConsumed = " + consumed, //$NON-NLS-1$
			                                            0, ITraceIds.TRACE_STREAMS_LISTENER,
			                                            IStatus.INFO, getClass());
		}

	    return consumed;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IStreams.StreamsListener#created(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
    public void created(final String streamType, final String streamId, final String contextId) {
		// We ignore any other stream type than the associated process service name
		if (!svcProcessesName.equals(streamType)) return;

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_STREAMS_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("New remote process stream created: streamId='" + streamId + "', contextId='" + contextId + "'", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			                                            0, ITraceIds.TRACE_STREAMS_LISTENER,
			                                            IStatus.INFO, getClass());
		}

		// Create the internal representation of the created event
		final String event = streamType + ";" + streamId + ";" + contextId; //$NON-NLS-1$ //$NON-NLS-2$

		// Check if the created event is really consumed by us
		if (isCreatedConsumed(streamType, streamId, contextId) && !processedCreatedEvents.contains(event)) {
			// Create a snapshot of the registered data receivers
			StreamsDataReceiver[] receivers;
			synchronized (dataReceiver) {
				receivers = dataReceiver.toArray(new StreamsDataReceiver[dataReceiver.size()]);
			}
			// The created event is for the monitored process context
			// --> Create the stream reader thread(s)
			if (streamId != null && streamId.equals(context.getProperties().get(IProcesses.PROP_STDIN_ID))) {
				// Data provider set?
				if (dataProvider != null) {
					// Create the stdin stream writer runnable
					StreamWriterRunnable runnable = new StreamWriterRunnable(streamId, IProcesses.PROP_STDIN_ID, dataProvider);
					// Add to the list of created runnable's
					synchronized (runnables) { runnables.add(runnable); }
					// And create and start the thread
					Thread thread = new Thread(runnable, "Thread-" + IProcesses.PROP_STDIN_ID + "-" + streamId); //$NON-NLS-1$ //$NON-NLS-2$
					thread.start();
				}
			}
			if (streamId != null && streamId.equals(context.getProperties().get(IProcesses.PROP_STDOUT_ID))) {
				// Create the stdout stream reader runnable
				StreamReaderRunnable runnable = new StreamReaderRunnable(streamId, IProcesses.PROP_STDOUT_ID, receivers);
				// If not empty, create the thread
				if (!runnable.isEmpty()) {
					// Add to the list of created runnable's
					synchronized (runnables) { runnables.add(runnable); }
					// And create and start the thread
					Thread thread = new Thread(runnable, "Thread-" + IProcesses.PROP_STDOUT_ID + "-" + streamId); //$NON-NLS-1$ //$NON-NLS-2$
					thread.start();
				}
			}
			if (streamId != null && streamId.equals(context.getProperties().get(IProcesses.PROP_STDERR_ID))) {
				// Create the stdout stream reader runnable
				StreamReaderRunnable runnable = new StreamReaderRunnable(streamId, IProcesses.PROP_STDERR_ID, receivers);
				// If not empty, create the thread
				if (!runnable.isEmpty()) {
					// Add to the list of created runnable's
					synchronized (runnables) { runnables.add(runnable); }
					// And create and start the thread
					Thread thread = new Thread(runnable, "Thread-" + IProcesses.PROP_STDERR_ID + "-" + streamId); //$NON-NLS-1$ //$NON-NLS-2$
					thread.start();
				}
			}

			// Remember that we have seen this event already in order to avoid to process it again
			// if the streams listener proxy is iterating through delayed events
			processedCreatedEvents.add(event);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IStreams.StreamsListener#disposed(java.lang.String, java.lang.String)
	 */
	@Override
    public void disposed(String streamType, String streamId) {
		// We ignore any other stream type than the associated process service name
		if (!svcProcessesName.equals(streamType)) return;

		boolean consumed = false;

		// Stop the thread(s) if the disposed event is for the active
		// monitored stream id(s).
		synchronized (runnables) {
			Iterator<Runnable> iterator = runnables.iterator();
			while (iterator.hasNext()) {
				Runnable runnable = iterator.next();
				if (runnable instanceof StreamReaderRunnable) {
					StreamReaderRunnable myRunnable = (StreamReaderRunnable)runnable;
					if (myRunnable.getStreamId().equals(streamId)) {
						// This method is called within the TCF event dispatch thread, so
						// we cannot wait for a callback here
						myRunnable.stop(null);
						iterator.remove();
						consumed |= true;
					}
				}
			}
		}

		if (consumed) {
			if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_STREAMS_LISTENER)) {
				CoreBundleActivator.getTraceHandler().trace("Remote process stream disposed: streamId='" + streamId + "'", //$NON-NLS-1$ //$NON-NLS-2$
				                                            0, ITraceIds.TRACE_STREAMS_LISTENER,
				                                            IStatus.INFO, getClass());
			}
		}
	}
}
