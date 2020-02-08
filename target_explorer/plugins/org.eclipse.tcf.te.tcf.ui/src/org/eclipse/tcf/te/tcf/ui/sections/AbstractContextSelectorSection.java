/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.sections;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.runtime.persistence.PersistenceManager;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Locator model context selector section implementation.
 */
public abstract class AbstractContextSelectorSection extends org.eclipse.tcf.te.ui.views.sections.AbstractContextSelectorSection {

	// Reference to a copy of the original data
	private IPropertiesContainer odc = null;

	private boolean disposed = false;

	final IPeerModelListener modelListener = new IPeerModelListener() {
		@Override
		public void modelDisposed(IPeerModel model) {
			refreshSelectorControl();
		}
		@Override
		public void modelChanged(final IPeerModel model, final IPeerNode peerNode, final boolean added) {
			refreshSelectorControl();
		}
	};
	final IEventListener eventListener = new IEventListener() {
		@Override
		public void eventFired(EventObject event) {
			if (event.getSource() instanceof IPeer || event.getSource() instanceof IPeerNode) {
				refreshSelectorControl();
			}
		}
	};

	/**
	 * Constructor.
	 * @param form The managed form.
	 * @param parent The parent composite.
	 */
	public AbstractContextSelectorSection(IManagedForm form, Composite parent) {
		super(form, parent);
		addListener();
	}

	/**
	 * Constructor.
	 * @param form The managed form.
	 * @param parent The parent composite.
	 * @param style
	 */
	public AbstractContextSelectorSection(IManagedForm form, Composite parent, int style) {
		super(form, parent, style);
		addListener();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.sections.AbstractContextSelectorSection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
	    super.createClient(section, toolkit);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.sections.AbstractContextSelectorSection#dispose()
	 */
	@Override
	public void dispose() {
		disposed = true;
	    super.dispose();
	    if (modelListener != null) {
		    Protocol.invokeAndWait(new Runnable() {
				@Override
				public void run() {
			    	ModelManager.getPeerModel().removeListener(modelListener);
				}
			});
	    }
	    if (eventListener != null) {
	    	EventManager.getInstance().removeEventListener(eventListener);
	    }
	}

	protected void addListener() {
		if (disposed) return;
	    Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				ModelManager.getPeerModel().addListener(modelListener);
			}
		});
    	EventManager.getInstance().addEventListener(eventListener, ChangeEvent.class);
	}

	protected void refreshSelectorControl() {
		if (disposed) return;
		if (getSelectorControl() != null) {
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					ExecutorsUtil.executeInUI(new Runnable() {
						@Override
						public void run() {
							if (getSelectorControl() != null) {
								getSelectorControl().refresh();
							}
							getManagedForm().dirtyStateChanged();
						}
					});
				}
			});
		}
	}

	public static final String encode(IModelNode[] contexts) {
		try {
			if (contexts != null && contexts.length > 0) {
				IPersistenceDelegate delegate = PersistenceManager.getInstance().getDelegate(IPeer.class, String.class);
				return (String)delegate.writeList(contexts, String.class);
			}
		}
		catch (Exception e) {
		}
		return null;
	}

	public static final IModelNode[] decode (String encoded) {
		try {
			if (encoded != null && encoded.trim().length() > 0) {
				IPersistenceDelegate delegate = PersistenceManager.getInstance().getDelegate(IPeer.class, String.class);
				Object[] input = delegate.readList(IPeerNode.class, encoded);
				List<IModelNode> peers = new ArrayList<IModelNode>();
				for (Object object : input) {
	            	if (object instanceof IModelNode) {
	            		peers.add((IModelNode)object);
	            	}
            	}
				return peers.toArray(new IPeerNode[peers.size()]);
			}
		}
		catch (Exception e) {
		}
		return new IModelNode[0];
	}

	protected abstract String getContextListDataKey();

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode#setupData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void setupData(IPropertiesContainer data) {
		Assert.isNotNull(data);

		// store original data
		odc = new PropertiesContainer();
		odc.setProperties(data.getProperties());

		setIsUpdating(true);

		String encoded = data.getStringProperty(getContextListDataKey());
		IModelNode[] list = decode(encoded);
		if (selector != null) {
			selector.setCheckedModelContexts(list);
			if (selector.getViewer() != null) {
				selector.getViewer().refresh();
			}
		}

		// Mark the control update as completed now
		setIsUpdating(false);

		// Re-evaluate the dirty state
		dataChanged();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode#extractData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void extractData(IPropertiesContainer data) {
		if (selector != null) {
			data.setProperty(getContextListDataKey(), encode(selector.getCheckedModelContexts()));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.sections.AbstractContextSelectorSection#dataChanged()
	 */
	@Override
    public void dataChanged() {
		// dataChanged is not evaluated while the controls are updated or no data was set
		if (odc == null || isUpdating()) {
			return;
		}

		boolean isDirty = false;

		if (selector != null) {
			String newValue = encode(selector.getCheckedModelContexts());
			if ("".equals(newValue)) { //$NON-NLS-1$
				String oldValue = odc.getStringProperty(getContextListDataKey());
				isDirty |= oldValue != null && !"".equals(oldValue.trim()); //$NON-NLS-1$
			}
			else {
				isDirty |= !odc.isProperty(getContextListDataKey(), newValue);
			}
		}

		// If dirty, mark the form part dirty.
		// Otherwise call refresh() to reset the dirty (and stale) flag
		markDirty(isDirty);
	}
}
