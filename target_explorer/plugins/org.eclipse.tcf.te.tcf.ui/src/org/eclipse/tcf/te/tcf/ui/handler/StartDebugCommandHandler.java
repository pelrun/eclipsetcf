/**
 * StartDebugCommandHandler.java
 * Created on Jun 29, 2012
 *
 * Copyright (c) 2012, 2013 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.tcf.ui.handler;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.tcf.te.runtime.callback.AsyncCallbackCollector;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IDebugService;
import org.eclipse.tcf.te.runtime.services.interfaces.IDelegateService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.steps.StartDebuggerStep.IDelegate;
import org.eclipse.tcf.te.ui.async.UICallbackInvocationDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.EditorPart;

/**
 * Start debugger command handler implementation.
 */
public class StartDebugCommandHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Get the active part
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		// Get the current selection
		ISelection selection = HandlerUtil.getCurrentSelection(event);

		// If the handler is invoked from an editor part, ignore the selection and
		// construct an artificial selection from the active editor input.
		if (part instanceof EditorPart) {
			IEditorInput input = ((EditorPart)part).getEditorInput();
			Object element = input != null ? input.getAdapter(Object.class) : null;
			if (element != null) {
				selection = new StructuredSelection(element);
			}
		}

		// If the selection is not empty, iterate over the selection and execute
		// the operation for each peer model node in the selection.
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			// Create the collector keeping track of the callbacks for each peer model
			// node within the selection
			final AsyncCallbackCollector collector = new AsyncCallbackCollector(new Callback() {
				@Override
				protected void internalDone(Object caller, IStatus status) {
					// Signal that all operations completed
				}
			}, new UICallbackInvocationDelegate());

			Iterator<?> iterator = ((IStructuredSelection)selection).iterator();
			while (iterator.hasNext()) {
				final Object element = iterator.next();
				if (element instanceof IPeerNode) {
					startDebugger((IPeerNode)element, new AsyncCallbackCollector.SimpleCollectorCallback(collector));
				}
			}

			// Mark the collector initialization done
			collector.initDone();
		}

		return null;
	}

	/**
	 * Starts the debugger for the given peer model node.
	 *
	 * @param peerNode The peer model node. Must not be <code>null</code>.
	 * @param callback The callback. Must not be <code>null</code>.
	 */
	public void startDebugger(final IPeerNode peerNode, final ICallback callback) {
		Assert.isNotNull(peerNode);
		Assert.isNotNull(callback);

		IDebugService dbgService = ServiceManager.getInstance().getService(peerNode, IDebugService.class, false);
		if (dbgService != null) {
			final IProgressMonitor monitor = new NullProgressMonitor();
			IPropertiesContainer props = new PropertiesContainer();
			dbgService.attach(peerNode, props, monitor, new Callback() {
				@Override
                protected void internalDone(Object caller, IStatus status) {
					// Check if there is a delegate registered
					IDelegateService service = ServiceManager.getInstance().getService(peerNode, IDelegateService.class, false);
					IDelegate delegate = service != null ? service.getDelegate(peerNode, IDelegate.class) : null;

					if (delegate != null) {
						delegate.postAttachDebugger(peerNode, monitor, callback);
					} else {
						callback.done(caller, status);
					}
				}
			});
		}
	}
}
