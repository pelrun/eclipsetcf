/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.editor;

import java.util.EventObject;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.IDisposable;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.ui.events.AbstractEventListener;
import org.eclipse.tcf.te.ui.views.activator.UIPlugin;
import org.eclipse.tcf.te.ui.views.interfaces.tracing.ITraceIds;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISources;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.services.IEvaluationService;

/**
 * Editor event listener implementation.
 * <p>
 * The event listener is registered by an editor instance for a given editor input
 * and is supposed to receive events for the editor input only.
 */
public final class EditorEventListener extends AbstractEventListener implements IDisposable, IPropertyListener {
	// Reference to the parent editor
	protected final Editor editor;
	// Flag to remember the disposed action
	protected boolean disposed = false;
	protected boolean changePending;
	protected boolean fullRefresh;

	/**
     * Constructor.
     *
     * @param editor The parent editor. Must not be <code>null</code>.
     */
    public EditorEventListener(Editor editor) {
    	super();

    	Assert.isNotNull(editor);
    	this.editor = editor;

    	// Register the event listener if the editor input is a properties container
		Object node = editor.getEditorInput() != null ? editor.getEditorInput().getAdapter(Object.class) : null;
		if (node instanceof IPropertiesContainer) EventManager.getInstance().addEventListener(this, ChangeEvent.class, node);

		editor.addPropertyListener(this);
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.runtime.interfaces.IDisposable#dispose()
     */
    @Override
    public void dispose() {
    	disposed = true;
    	EventManager.getInstance().removeEventListener(this);
    	editor.removePropertyListener(this);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.events.IEventListener#eventFired(java.util.EventObject)
	 */
	@Override
	public void eventFired(EventObject event) {
		// Do nothing if already disposed
		if (disposed) return;

		// Ignore everything not being a change event
		if (!(event instanceof ChangeEvent)) return;

		ChangeEvent changeEvent = (ChangeEvent)event;

		if (UIPlugin.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_EDITOR_EVENT_LISTENER)) {
			UIPlugin.getTraceHandler().trace("Entered eventFired(...). eventId='" + changeEvent.getEventId() + "'" //$NON-NLS-1$ //$NON-NLS-2$
												+ ", oldValue='" + changeEvent.getOldValue() + "'" //$NON-NLS-1$ //$NON-NLS-2$
												+ ", newValue='" + changeEvent.getNewValue() + "'", //$NON-NLS-1$ //$NON-NLS-2$
												0, ITraceIds.TRACE_EDITOR_EVENT_LISTENER,
												IStatus.INFO, this);
		}

		// Get the event source
		Object source = event.getSource();
		// Double check with the parent editors input object
		Object node = editor.getEditorInput() != null ? editor.getEditorInput().getAdapter(Object.class) : null;
		// If the editor input cannot be determined or it does not match the event source
		// --> return immediately
		if (node == null || !node.equals(source)) {
			if (UIPlugin.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_EDITOR_EVENT_LISTENER)) {
				UIPlugin.getTraceHandler().trace("Dropping out of eventFired(...). Event source does not match editor input.", //$NON-NLS-1$
													0, ITraceIds.TRACE_EDITOR_EVENT_LISTENER,
													IStatus.WARNING, this);
			}

			return;
		}

		fullRefresh = fullRefresh || !"editor.refreshTab".equals(changeEvent.getEventId()); //$NON-NLS-1$
		if (changePending) return;

		changePending = true;
		Display display = editor.getSite().getShell().getDisplay();
		display.timerExec(200, new Runnable() {
			@Override
			public void run() {
				if (disposed) return;
				boolean doFullRefresh = fullRefresh;

				// Refresh the page list. Changing editor input element properties
				// may effect the page list -> Update in any case.
				editor.updatePageList();

				// If the event is a "editor.refreshTab" event, skip the rest
				if (doFullRefresh) {
					// Update the active page content by calling IFormPage#setActive(boolean)
					Object page = editor.getSelectedPage();
					if (page instanceof IFormPage) {
						((IFormPage)page).setActive(((IFormPage)page).isActive());
					}

					// Update the editor part name
					editor.updatePartName();

					// Request a re-evaluation if all expressions referring the "activeEditorInput" source.
					IEvaluationService service = (IEvaluationService)editor.getSite().getService(IEvaluationService.class);
					if (service != null) {
						service.requestEvaluation(ISources.ACTIVE_EDITOR_INPUT_NAME);
					}
				}
				fullRefresh = false;
				changePending = false;
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPropertyListener#propertyChanged(java.lang.Object, int)
	 */
    @Override
    public void propertyChanged(Object source, int propId) {
    	if (source == this.editor && (!changePending || !fullRefresh)) {
    		if (IEditorPart.PROP_DIRTY == propId) {
    			// Request a re-evaluation if all expressions referring the "activeEditorInput" source.
    			IEvaluationService service = (IEvaluationService)editor.getSite().getService(IEvaluationService.class);
    			if (service != null) {
    				service.requestEvaluation(ISources.ACTIVE_EDITOR_INPUT_NAME);
    			}
    		}
    	}
    }
}
