/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.sections;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.runtime.persistence.PersistenceManager;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.ui.controls.ContextSelectorSectionControl;
import org.eclipse.tcf.te.ui.views.controls.AbstractContextSelectorControl;
import org.eclipse.tcf.te.ui.views.sections.AbstractContextSelectorSection;
import org.eclipse.ui.forms.IManagedForm;

/**
 * Locator model context selector section implementation.
 */
public abstract class ContextSelectorSection extends AbstractContextSelectorSection {

	/**
	 * Constructor.
	 * @param form The managed form.
	 * @param parent The parent composite.
	 */
	public ContextSelectorSection(IManagedForm form, Composite parent) {
		super(form, parent);
	}

	/**
	 * Constructor.
	 * @param form The managed form.
	 * @param parent The parent composite.
	 * @param style
	 */
	public ContextSelectorSection(IManagedForm form, Composite parent, int style) {
		super(form, parent, style);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.tabs.launchcontext.AbstractContextSelectorSection#doCreateContextSelector()
	 */
	@Override
	protected AbstractContextSelectorControl doCreateContextSelector() {
		return new ContextSelectorSectionControl(this, null);
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
				Object[] input = delegate.readList(IPeerModel.class, encoded);
				List<IModelNode> peers = new ArrayList<IModelNode>();
				for (Object object : input) {
	            	if (object instanceof IModelNode) {
	            		peers.add((IModelNode)object);
	            	}
            	}
				return peers.toArray(new IPeerModel[peers.size()]);
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
		String encoded = data.getStringProperty(getContextListDataKey());
		IModelNode[] list = decode(encoded);
		selector.setCheckedModelContexts(list);
		if (selector != null && selector.getViewer() != null) {
			selector.getViewer().refresh();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode#extractData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void extractData(IPropertiesContainer data) {
		data.setProperty(getContextListDataKey(), encode(selector.getCheckedModelContexts()));
	}
}
