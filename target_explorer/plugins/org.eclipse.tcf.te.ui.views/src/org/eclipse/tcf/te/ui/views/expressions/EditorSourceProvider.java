/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.expressions;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.tcf.te.ui.views.editor.Editor;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;
import org.eclipse.ui.services.IServiceLocator;

/**
 * Configuration editor source provider implementation.
 */
public class EditorSourceProvider extends AbstractSourceProvider implements IWindowListener, IPartListener2 {
	/**
	 * Source name identifying the editor input of the configuration editor which had the focus last.
	 */
	public static final String editorInputName = "editorInput"; //$NON-NLS-1$

	// The internal list of provided source names
	private final static String[] PROVIDED_SOURCE_NAMES = { editorInputName };

	// The cached editor input
	private IEditorInput cacheEditorInput = null;

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
	    if (PlatformUI.getWorkbench() != null) {
	    	// Unregister the service provide as workbench window listener
	    	PlatformUI.getWorkbench().removeWindowListener(this);
	    	// Unregister the part listener
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

		state.put(editorInputName, cacheEditorInput != null ? cacheEditorInput : IEvaluationContext.UNDEFINED_VARIABLE);

		return state;
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
		// Remove ourself as part listener
		if (window != null && window.getPartService() != null) {
			IPartService service = window.getPartService();
			service.removePartListener(this);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWindowListener#windowOpened(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
    public void windowOpened(IWorkbenchWindow window) {
		// A new workbench window opened. We have to register
		// ourself as part listener if not done
		if (window != null && window.getPartService() != null) {
			// Get the part service
			IPartService service = window.getPartService();

			// Unregister the source provider first, just in case to
			// avoid the listener to be registered multiple time
			service.removePartListener(this);

			// Register the source provider now as part listener
			service.addPartListener(this);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partActivated(org.eclipse.ui.IWorkbenchPartReference)
	 */
    @Override
    public void partActivated(IWorkbenchPartReference partRef) {
    	IWorkbenchPart part = partRef.getPart(false);
    	if (part instanceof Editor) {
    		// That's one of our configuration editors -> get the editor input
    		cacheEditorInput = ((Editor)part).getEditorInput();

    		// Fire the source changed notification
    		fireSourceChanged(ISources.WORKBENCH, editorInputName,
    						  cacheEditorInput != null ? cacheEditorInput : IEvaluationContext.UNDEFINED_VARIABLE);
    	}
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partBroughtToTop(org.eclipse.ui.IWorkbenchPartReference)
	 */
    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef) {
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partClosed(org.eclipse.ui.IWorkbenchPartReference)
	 */
    @Override
    public void partClosed(IWorkbenchPartReference partRef) {
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partDeactivated(org.eclipse.ui.IWorkbenchPartReference)
	 */
    @Override
    public void partDeactivated(IWorkbenchPartReference partRef) {
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partOpened(org.eclipse.ui.IWorkbenchPartReference)
	 */
    @Override
    public void partOpened(IWorkbenchPartReference partRef) {
    	IWorkbenchPart part = partRef.getPart(false);
    	if (part instanceof Editor) {
    		// That's one of our configuration editors -> get the editor input
    		cacheEditorInput = ((Editor)part).getEditorInput();

    		// Fire the source changed notification
    		fireSourceChanged(ISources.WORKBENCH, editorInputName,
    						  cacheEditorInput != null ? cacheEditorInput : IEvaluationContext.UNDEFINED_VARIABLE);
    	}
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partHidden(org.eclipse.ui.IWorkbenchPartReference)
	 */
    @Override
    public void partHidden(IWorkbenchPartReference partRef) {
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partVisible(org.eclipse.ui.IWorkbenchPartReference)
	 */
    @Override
    public void partVisible(IWorkbenchPartReference partRef) {
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partInputChanged(org.eclipse.ui.IWorkbenchPartReference)
	 */
    @Override
    public void partInputChanged(IWorkbenchPartReference partRef) {
    }
}
