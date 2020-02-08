/*******************************************************************************
 * Copyright (c) 2012, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;
import org.eclipse.ui.services.IServiceLocator;

/**
 * Source provider for peer nodes and default context implementation.
 */
public class SourceProvider extends AbstractSourceProvider implements IEventListener {

	/**
	 * Source name identifying the System Manager view selection.
	 */
	public static final String defaultContextSelectionName = "defaultContextSelection"; //$NON-NLS-1$

	// see org.eclipse.ui.internal.services.EvaluationService
	private static final String RE_EVAL = "org.eclipse.ui.internal.services.EvaluationService.evaluate"; //$NON-NLS-1$

	// The internal list of provided source names
	private final static String[] PROVIDED_SOURCE_NAMES = {defaultContextSelectionName};

	// The reference to the expression evaluation service
	private IEvaluationService service = null;

	private IPeerNode defaultContext;
	private IPeerNode prevContext;
	private volatile boolean changePending;

	/**
     * Constructor.
     */
    public SourceProvider() {
    	super();
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.AbstractSourceProvider#initialize(org.eclipse.ui.services.IServiceLocator)
	 */
	@Override
	public void initialize(IServiceLocator locator) {
	    super.initialize(locator);

	    defaultContext = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
	    EventManager.getInstance().addEventListener(this, ChangeEvent.class);

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
	    EventManager.getInstance().removeEventListener(this);

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

		state.put(defaultContextSelectionName, defaultContext != null ? defaultContext : IEvaluationContext.UNDEFINED_VARIABLE);

		return state;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.events.IEventListener#eventFired(java.util.EventObject)
	 */
    @Override
    public void eventFired(EventObject event) {
    	if (event instanceof ChangeEvent) {
    		ChangeEvent changeEvent = (ChangeEvent)event;
    		if (changeEvent.getSource() instanceof IDefaultContextService || changeEvent.getSource() == defaultContext) {
    			defaultContext = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
    			if (changePending || !PlatformUI.isWorkbenchRunning() || PlatformUI.getWorkbench().isClosing())
    				return;
    			changePending = true;
    			// Fire the source changed notification within the UI thread
    			final Display display = PlatformUI.getWorkbench().getDisplay();
    			display.asyncExec(new Runnable() {
    				private boolean scheduled;
    				@SuppressWarnings("synthetic-access")
    				@Override
    				public void run() {
    					if (service == null) return;
    					if (!scheduled) {
    						scheduled = true;
    						display.timerExec(100, this);
    						return;
    					}
    					IPeerNode newContext = defaultContext;
    	    			changePending = false;
    					if (newContext == prevContext) {
    						// force re-evaluation of enablement expressions
    						service.getCurrentState().addVariable(RE_EVAL, new Object());
    					} else
    						fireSourceChanged(ISources.WORKBENCH, defaultContextSelectionName, newContext != null ? newContext : IEvaluationContext.UNDEFINED_VARIABLE);
    					prevContext = newContext;
    				}
    			});
    		}
    	}
    }
}
