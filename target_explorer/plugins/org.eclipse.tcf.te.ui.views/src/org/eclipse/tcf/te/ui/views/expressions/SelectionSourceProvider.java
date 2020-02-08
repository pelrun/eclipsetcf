/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.expressions;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.INullSelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;
import org.eclipse.ui.services.IServiceLocator;

/**
 * Selection service source provider implementation.
 */
public class SelectionSourceProvider extends AbstractSourceProvider implements INullSelectionListener, IWindowListener {

	/**
	 * Source name identifying the System Manager view selection.
	 */
	public static final String systemManagerViewSelectionName = "systemManagerViewSelection"; //$NON-NLS-1$

	/**
	 * Source name identifying the Debug View view selection.
	 */
	public static final String debugViewSelectionName = "debugViewSelection"; //$NON-NLS-1$

	// The internal list of provided source names
	private final static String[] PROVIDED_SOURCE_NAMES = { systemManagerViewSelectionName, debugViewSelectionName };

	/**
	 * Debug view identifier (value <code>"org.eclipse.debug.ui.DebugView"</code>).
	 * @see IDebugUIConstants
	 */
	private static final String ID_DEBUG_VIEW = "org.eclipse.debug.ui.DebugView"; //$NON-NLS-1$

	// The map containing the cached selections
	private final Map<String, ISelection> cache = new HashMap<String, ISelection>();

	// The reference to the expression evaluation service
	private IEvaluationService service = null;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.AbstractSourceProvider#initialize(org.eclipse.ui.services.IServiceLocator)
	 */
	@Override
	public void initialize(IServiceLocator locator) {
	    super.initialize(locator);

	    if (PlatformUI.getWorkbench() != null) {
	    	// Register the service provider as workbench window listener
	    	PlatformUI.getWorkbench().addWindowListener(this);
		    // Initialize the selection cache and the selection listener
	    	if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
	    		windowOpened(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
	    	}
	    }

	    // Register the source provider with the expression evaluation service
	    if (locator.hasService(IEvaluationService.class)) {
	    	service = (IEvaluationService)locator.getService(IEvaluationService.class);
	    	if (service != null) service.addSourceProvider(this);
	    }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISourceProvider#dispose()
	 */
	@Override
	public void dispose() {
		// Unregister the selection listener
	    if (PlatformUI.getWorkbench() != null) {
	    	// Unregister the service provide as workbench window listener
	    	PlatformUI.getWorkbench().removeWindowListener(this);
	    	// Unregister the selection listener
	    	if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
	    		windowClosed(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
	    	}
	    }

	    // Unregister the source provider from the expression evaluation service
	    if (service != null) { service.removeSourceProvider(this); service = null; }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISourceProvider#getProvidedSourceNames()
	 */
	@Override
	public String[] getProvidedSourceNames() {
		return PROVIDED_SOURCE_NAMES;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISourceProvider#getCurrentState()
	 */
	@Override
	public Map getCurrentState() {
		Map<String, Object> state = new HashMap<String, Object>();

		ISelection selection = cache.get(IUIConstants.ID_EXPLORER);
		state.put(systemManagerViewSelectionName, selection != null ? selection : IEvaluationContext.UNDEFINED_VARIABLE);

		selection = cache.get(ID_DEBUG_VIEW);
		state.put(debugViewSelectionName, selection != null ? selection : IEvaluationContext.UNDEFINED_VARIABLE);

		return state;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		String partId = part != null ? part.getSite().getId() : null;
		if (!IUIConstants.ID_EXPLORER.equals(partId) && !ID_DEBUG_VIEW.equals(partId)) {
			return;
		}

		// Update the cached selection
		if (selection != null) cache.put(partId, selection);
		else cache.remove(partId);

		// Fire the source changed notification
		fireSourceChanged(ISources.WORKBENCH, IUIConstants.ID_EXPLORER.equals(partId) ? systemManagerViewSelectionName : debugViewSelectionName,
						  selection != null ? selection : IEvaluationContext.UNDEFINED_VARIABLE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWindowListener#windowActivated(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
    public void windowActivated(IWorkbenchWindow window) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWindowListener#windowDeactivated(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
    public void windowDeactivated(IWorkbenchWindow window) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWindowListener#windowClosed(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
    public void windowClosed(IWorkbenchWindow window) {
		// Remove ourself as selection listener
		if (window != null && window.getSelectionService() != null) {
			ISelectionService service = window.getSelectionService();
			service.removePostSelectionListener(IUIConstants.ID_EXPLORER, this);
			service.removePostSelectionListener(ID_DEBUG_VIEW, this);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWindowListener#windowOpened(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
    public void windowOpened(IWorkbenchWindow window) {
		// A new workbench window opened. We have to register
		// ourself as selection listener if not done
		if (window != null && window.getSelectionService() != null) {
			// Get the selection service
			ISelectionService service = window.getSelectionService();

			// Unregister the source provider first, just in case to
			// avoid the listener to be registered multiple time
			service.removePostSelectionListener(IUIConstants.ID_EXPLORER, this);
			service.removePostSelectionListener(ID_DEBUG_VIEW, this);

			// Register the source provider now as post selection listener
			service.addPostSelectionListener(IUIConstants.ID_EXPLORER, this);
			service.addPostSelectionListener(ID_DEBUG_VIEW, this);

			// Initialize the selections
			ISelection selection = service.getSelection(IUIConstants.ID_EXPLORER);
			if (selection != null) cache.put(IUIConstants.ID_EXPLORER, selection);
			else cache.remove(IUIConstants.ID_EXPLORER);

			fireSourceChanged(ISources.WORKBENCH, systemManagerViewSelectionName,
							  selection != null ? selection : IEvaluationContext.UNDEFINED_VARIABLE);

			selection = service.getSelection(ID_DEBUG_VIEW);
			if (selection != null) cache.put(ID_DEBUG_VIEW, selection);
			else cache.remove(ID_DEBUG_VIEW);

			fireSourceChanged(ISources.WORKBENCH, debugViewSelectionName,
							  selection != null ? selection : IEvaluationContext.UNDEFINED_VARIABLE);
		}
	}
}
