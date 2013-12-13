/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.AsyncCallbackCollector;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.tcf.core.async.CallbackInvocationDelegate;
import org.eclipse.tcf.te.tcf.locator.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.locator.interfaces.IScanner;
import org.eclipse.tcf.te.tcf.locator.interfaces.ITracing;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.model.Model;


/**
 * Locator model scanner implementation.
 */
public class Scanner extends Job implements IScanner {
	// Reference to the parent model instance.
	private final IPeerModel parentModel;

	// Reference to the scanner configuration
	private final Map<String, Object> configuration = new HashMap<String, Object>();

	// Flag to mark if the scanner is terminated
	private AtomicBoolean terminated = new AtomicBoolean(false);

	/**
	 * Constructor.
	 *
	 * @param parentModel The parent model instance. Must not be <code>null</code>.
	 */
	public Scanner(IPeerModel parentModel) {
		super(Scanner.class.getName());
		Assert.isNotNull(parentModel);
		this.parentModel = parentModel;

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(ITracing.ID_TRACE_SCANNER)) {
			CoreBundleActivator.getTraceHandler().trace("Scanner created.", ITracing.ID_TRACE_SCANNER, Scanner.this); //$NON-NLS-1$
		}
	}

	/**
	 * Returns the parent model instance.
	 *
	 * @return The parent model instance.
	 */
	protected IPeerModel getParentModel() {
		return parentModel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.IScanner#setConfiguration(java.util.Map)
	 */
	@Override
	public void setConfiguration(Map<String, Object> configuration) {
		Assert.isNotNull(configuration);
		this.configuration.clear();
		this.configuration.putAll(configuration);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.IScanner#getConfiguration()
	 */
	@Override
	public Map<String, Object> getConfiguration() {
		return configuration;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (monitor == null) monitor = new NullProgressMonitor();

		// Get the current list of peers known to the parent model
		IPeerNode[] peers = getParentModel().getPeers();
		// Do we have something to scan at all
		if (peers.length > 0) {
			try {
				// The first runnable is setting the thread which will finish
				// the job at the end
				Protocol.invokeLater(new Runnable() {
					@Override
					public void run() {
						Scanner.this.setThread(Thread.currentThread());
					}
				});

				// Create the callback collector keeping track of all scan processes
				final IProgressMonitor finMonitor = monitor;
				final AsyncCallbackCollector collector = new AsyncCallbackCollector(new Callback() {
					@Override
					protected void internalDone(Object caller, IStatus status) {
						// Terminate the job as soon all scanner runnable's are process
						// and reschedule the job (if not terminated)
						final IStatus result = finMonitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
						if (getState() != Job.NONE) Scanner.this.done(result);

						if (!isTerminated()) {
							Long delay = (Long)getConfiguration().get(IScanner.PROP_SCHEDULE);
							if (delay != null) {
								Scanner.this.schedule(delay.longValue());
							}
						}
					}
				}, new CallbackInvocationDelegate());

				// Loop the nodes and try to get an channel
				for (IPeerNode peer : peers) {
					// Check for the progress monitor getting canceled
					if (monitor.isCanceled() || isTerminated()) break;
					// Scan the peer
					doScan(peer, collector, monitor);
				}

				// Mark the collector initialization done
				collector.initDone();
			} catch (IllegalStateException e) {
				/* ignored on purpose */
			}
		}

		return peers.length > 0 ? ASYNC_FINISH : Status.OK_STATUS;
	}

	/**
	 * Scan the given peer model node and possible child nodes.
	 *
	 * @param peer The peer model node. Must not be <code>null</code>.
	 * @param collector The callback collector. Must not be <code>null</code>.
	 * @param monitor The progress monitor. Must not be <code>null</code>.
	 */
	/* default */ void doScan(final IPeerNode peer, final AsyncCallbackCollector collector, final IProgressMonitor monitor) {
		Assert.isNotNull(peer);
		Assert.isNotNull(collector);
		Assert.isNotNull(monitor);

		// Check for the progress monitor getting canceled
		if (monitor.isCanceled() || isTerminated()) return;

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(ITracing.ID_TRACE_SCANNER)) {
			CoreBundleActivator.getTraceHandler().trace("Schedule scanner runnable for peer '" + peer.getName() + "' (" + peer.getPeerId() + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
														ITracing.ID_TRACE_SCANNER, Scanner.this);
		}

		final AtomicBoolean isExcluded = new AtomicBoolean(false);

		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				isExcluded.set(peer.getBooleanProperty(IPeerNodeProperties.PROP_SCANNER_EXCLUDE));
			}
		};

		if (Protocol.isDispatchThread()) runnable.run();
		else Protocol.invokeAndWait(runnable);

		// If the PROP_SCANNER_EXCLUDE is not set, scan this node
		if (!isExcluded.get()) {
			// Create the callback
			ICallback callback = new Callback() {
				@Override
				protected void internalDone(Object caller, IStatus status) {
					// Check for the progress monitor getting canceled
					if (!monitor.isCanceled() && !isTerminated()) {
						// Get the children of the scanned peer model and make sure
						// they are scanned too if not excluded
						List<IPeerNode> candidates = Model.getModel().getChildren(peer.getPeerId());
						if (candidates != null && candidates.size() > 0) {
							for (IPeerNode candidate : candidates) {
								doScan(candidate, collector, monitor);
							}
						}
					}

					// Remove the callback from the collector
					collector.removeCallback(this);
				}
			};
			// Add the callback to the collector
			collector.addCallback(callback);
			// Create the scanner runnable
			Runnable scannerRunnable = new ScannerRunnable(this, peer, callback);
			// Submit for execution
			Protocol.invokeLater(scannerRunnable);
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.IScanner#terminate()
	 */
	@Override
	public void terminate() {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		// Return immediately if the scanner has been terminated already
		if (terminated.get()) return;

		// Mark the scanner job as terminated. This flag is checked by
		// the asynchronous callbacks and will stop the processing
		terminated.set(true);
		// Mark the job done from the job manager POV
		if (getState() != Job.NONE) done(Status.CANCEL_STATUS);

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(ITracing.ID_TRACE_SCANNER)) {
			CoreBundleActivator.getTraceHandler().trace("Scanner terminated.", ITracing.ID_TRACE_SCANNER, Scanner.this); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.IScanner#isTerminated()
	 */
	@Override
	public final boolean isTerminated() {
		return terminated.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#shouldRun()
	 */
	@Override
	public boolean shouldRun() {
		return Platform.isRunning() && !getParentModel().isDisposed() && !isTerminated();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#shouldSchedule()
	 */
	@Override
	public boolean shouldSchedule() {
		return Platform.isRunning() && !getParentModel().isDisposed() && !isTerminated();
	}
}
