/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.tcf.te.runtime.callback.AsyncCallbackCollector;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tcf.te.ui.async.UICallbackInvocationDelegate;
import org.eclipse.tcf.te.ui.swt.DisplayUtil;
import org.eclipse.tcf.te.ui.terminals.actions.PinTerminalAction;
import org.eclipse.tcf.te.ui.terminals.interfaces.ILauncherDelegate;
import org.eclipse.tcf.te.ui.terminals.interfaces.IMementoHandler;
import org.eclipse.tcf.te.ui.terminals.launcher.LauncherDelegateManager;
import org.eclipse.tcf.te.ui.terminals.tabs.TabFolderToolbarHandler;
import org.eclipse.ui.IMemento;

/**
 * Take care of the persisted state handling of the "Terminals" view.
 */
public class TerminalsViewMementoHandler {
	// The list of items to save. See the workbench listener implementation
	// in o.e.tcf.te.ui.terminals.activator.UIPlugin.
	private final List<CTabItem> saveables = new ArrayList<CTabItem>();

	/**
	 * Sets the list of saveable items.
	 *
	 * @param saveables The list of saveable items. Must not be <code>null</code>.
	 */
	public void setSaveables(List<CTabItem> saveables) {
		Assert.isNotNull(saveables);
		this.saveables.clear();
		this.saveables.addAll(saveables);
	}

	/**
	 * Saves the view state in the given memento.
	 *
	 * @param view The terminals view. Must not be <code>null</code>.
	 * @param memento The memento. Must not be <code>null</code>.
	 */
	public void saveState(TerminalsView view, IMemento memento) {
		Assert.isNotNull(view);
		Assert.isNotNull(memento);

		// Create a child element within the memento holding the
		// connection info of the open, non-terminated tab items
		memento = memento.createChild("terminalConnections"); //$NON-NLS-1$
		Assert.isNotNull(memento);

		// Write the view id and secondary id
		memento.putString("id", view.getViewSite().getId()); //$NON-NLS-1$
		memento.putString("secondaryId", view.getViewSite().getSecondaryId()); //$NON-NLS-1$

		// Save the pinned state
		memento.putBoolean("pinned", view.isPinned()); //$NON-NLS-1$

		// Loop the saveable items and store the connection data of each
		// item to the memento
		for (CTabItem item : saveables) {
			// Ignore disposed items
			if (item.isDisposed()) continue;

			// Get the original terminal properties associated with the tab item
			IPropertiesContainer properties = (IPropertiesContainer)item.getData("properties"); //$NON-NLS-1$
			if (properties == null) continue;

			// Get the terminal launcher delegate
			String delegateId = properties.getStringProperty(ITerminalsConnectorConstants.PROP_DELEGATE_ID);
			ILauncherDelegate delegate = delegateId != null ? LauncherDelegateManager.getInstance().getLauncherDelegate(delegateId, false) : null;
			IMementoHandler mementoHandler = delegate != null ? (IMementoHandler)delegate.getAdapter(IMementoHandler.class) : null;
			if (mementoHandler != null) {
				// Create terminal connection child memento
				IMemento connectionMemento = memento.createChild("connection"); //$NON-NLS-1$
				Assert.isNotNull(connectionMemento);
				// Store the common attributes
				connectionMemento.putString(ITerminalsConnectorConstants.PROP_DELEGATE_ID, delegateId);

				String terminalConnectorId = properties.getStringProperty(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID);
				if (terminalConnectorId != null) {
					connectionMemento.putString(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID, terminalConnectorId);
				}

				String connectorTypeId = properties.getStringProperty(ITerminalsConnectorConstants.PROP_CONNECTOR_TYPE_ID);
				if (connectorTypeId != null) {
					connectionMemento.putString(ITerminalsConnectorConstants.PROP_CONNECTOR_TYPE_ID, connectorTypeId);
				}

				if (properties.getProperty(ITerminalsConnectorConstants.PROP_FORCE_NEW) != null) {
					connectionMemento.putBoolean(ITerminalsConnectorConstants.PROP_FORCE_NEW, properties.getBooleanProperty(ITerminalsConnectorConstants.PROP_FORCE_NEW));
				}

				// Pass on to the memento handler
				mementoHandler.saveState(connectionMemento, properties);
			}
		}
	}

	/**
	 * Restore the view state from the given memento.
	 *
	 * @param view The terminals view. Must not be <code>null</code>.
	 * @param memento The memento. Must not be <code>null</code>.
	 */
	protected void restoreState(final TerminalsView view, IMemento memento) {
		Assert.isNotNull(view);
		Assert.isNotNull(memento);

		// Get the "terminalConnections" memento
		memento = memento.getChild("terminalConnections"); //$NON-NLS-1$
		if (memento != null) {
			// Read view id and secondary id
			String id = memento.getString("id"); //$NON-NLS-1$
			String secondaryId = memento.getString("secondaryId"); //$NON-NLS-1$
			if ("null".equals(secondaryId)) secondaryId = null; //$NON-NLS-1$

			final IMemento finMemento = memento;
			// Restore the pinned state of the after all connections completed
			AsyncCallbackCollector collector = new AsyncCallbackCollector(new Callback() {
				@Override
				protected void internalDone(Object caller, IStatus status) {
					// Restore the pinned state
					if (finMemento.getBoolean("pinned") != null) { //$NON-NLS-1$
						DisplayUtil.safeAsyncExec(new Runnable() {
							@Override
							public void run() {
								view.setPinned(finMemento.getBoolean("pinned").booleanValue()); //$NON-NLS-1$

								TabFolderToolbarHandler toolbarHandler = (TabFolderToolbarHandler)view.getAdapter(TabFolderToolbarHandler.class);
								if (toolbarHandler != null) {
									PinTerminalAction action = (PinTerminalAction)toolbarHandler.getAdapter(PinTerminalAction.class);
									if (action != null) action.setChecked(view.isPinned());
								}
							}
						});
					}
				}
			}, new UICallbackInvocationDelegate());
			// Get all the "connection" memento's.
			IMemento[] connections = memento.getChildren("connection"); //$NON-NLS-1$
			for (IMemento connection : connections) {
				// Create the properties container that holds the terminal properties
				IPropertiesContainer properties = new PropertiesContainer();

				// Set the view id attributes
				properties.setProperty(ITerminalsConnectorConstants.PROP_ID, id);
				properties.setProperty(ITerminalsConnectorConstants.PROP_SECONDARY_ID, secondaryId);

				// Restore the common attributes
				properties.setProperty(ITerminalsConnectorConstants.PROP_DELEGATE_ID, connection.getString(ITerminalsConnectorConstants.PROP_DELEGATE_ID));
				properties.setProperty(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID, connection.getString(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID));
				properties.setProperty(ITerminalsConnectorConstants.PROP_CONNECTOR_TYPE_ID, connection.getString(ITerminalsConnectorConstants.PROP_CONNECTOR_TYPE_ID));
				properties.setProperty(ITerminalsConnectorConstants.PROP_FORCE_NEW, connection.getBoolean(ITerminalsConnectorConstants.PROP_FORCE_NEW));

                // Get the terminal launcher delegate
                String delegateId = properties.getStringProperty(ITerminalsConnectorConstants.PROP_DELEGATE_ID);
                ILauncherDelegate delegate = delegateId != null ? LauncherDelegateManager.getInstance().getLauncherDelegate(delegateId, false) : null;
                IMementoHandler mementoHandler = delegate != null ? (IMementoHandler)delegate.getAdapter(IMementoHandler.class) : null;
                if (mementoHandler != null) {
                	// Pass on to the memento handler
                	mementoHandler.restoreState(connection, properties);
                }

                // Restore the terminal connection
                if (delegate != null && !properties.isEmpty()) {
                	delegate.execute(properties, new AsyncCallbackCollector.SimpleCollectorCallback(collector));
                }
			}

			collector.initDone();
		}
	}
}
