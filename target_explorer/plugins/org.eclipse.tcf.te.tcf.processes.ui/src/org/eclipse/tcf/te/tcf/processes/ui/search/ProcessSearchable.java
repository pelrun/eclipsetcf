/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.search;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.services.ServiceUtils;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.processes.ui.interfaces.IProcessMonitorUIDelegate;
import org.eclipse.tcf.te.tcf.processes.ui.nls.Messages;
import org.eclipse.tcf.te.ui.interfaces.ISearchable;
import org.eclipse.tcf.te.ui.utils.CompositeSearchable;
import org.eclipse.tcf.te.ui.views.navigator.DelegatingLabelProvider;

/**
 * The ISearchable adapter for a ProcessTreeNode which creates a UI for the user to
 * input the matching condition and returns a matcher to do the matching.
 */
public class ProcessSearchable extends CompositeSearchable {
	// The label provider used to get a text for a process.
	ILabelProvider labelProvider = new DelegatingLabelProvider();

	/**
	 * Constructor
	 *
	 * @param node The peer model node context. Must not be <code>null</code>.
	 */
	public ProcessSearchable(IPeerNode peerNode) {
		super();

		IProcessMonitorUIDelegate delegate = ServiceUtils.getUIServiceDelegate(peerNode, peerNode, IProcessMonitorUIDelegate.class);
		ISearchable[] searchables = delegate != null ? delegate.getSearchables(peerNode) : null;
		if (searchables == null) {
			searchables = new ISearchable[] { new GeneralSearchable(), new ProcessUserSearchable(), new ProcessStateSearchable() };
		}
		setSearchables(searchables);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#getSearchTitle(java.lang.Object)
	 */
	@Override
	public String getSearchTitle(final Object rootElement) {
		final AtomicReference<IPeerNode> node = new AtomicReference<IPeerNode>();

		if (rootElement instanceof IRuntimeModel) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					if (rootElement instanceof IRuntimeModel) {
						node.set(((IRuntimeModel)rootElement).getPeerNode());
					}
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);
		}
		else if (rootElement != null) {
			node.set(rootElement instanceof IAdaptable ? (IPeerNode)((IAdaptable)rootElement).getAdapter(IPeerNode.class) : (IPeerNode)Platform.getAdapterManager().getAdapter(rootElement, IPeerNode.class));
		}

		String label = Messages.getStringDelegated(node.get(), "ProcessSearchable_SearchTitle"); //$NON-NLS-1$
	    return label != null ? label : Messages.ProcessSearchable_SearchTitle;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#getSearchMessage(java.lang.Object)
	 */
	@Override
    public String getSearchMessage(final Object rootElement) {
		if (rootElement == null || rootElement instanceof IRuntimeModel) {
			final AtomicReference<IPeerNode> node = new AtomicReference<IPeerNode>();

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					if (rootElement instanceof IRuntimeModel) {
						node.set(((IRuntimeModel)rootElement).getPeerNode());
					}
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);

			String label = Messages.getStringDelegated(node.get(), "ProcessSearchable_PromptFindInProcessList"); //$NON-NLS-1$
			return label != null ? label : Messages.ProcessSearchable_PromptFindInProcessList;
		}

		IPeerNode node = rootElement instanceof IAdaptable ? (IPeerNode)((IAdaptable)rootElement).getAdapter(IPeerNode.class) : (IPeerNode)Platform.getAdapterManager().getAdapter(rootElement, IPeerNode.class);
		String label = Messages.getStringDelegated(node, "ProcessSearchable_PromptFindUnderProcess"); //$NON-NLS-1$
		String message = label != null ? label : Messages.ProcessSearchable_PromptFindUnderProcess;
		String rootName = "\"" + getElementName(rootElement) + "\""; //$NON-NLS-1$//$NON-NLS-2$
		message = NLS.bind(message, rootName);
		return message;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#getCustomMessage(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getCustomMessage(final Object rootElement, final String key) {
		final AtomicReference<IPeerNode> node = new AtomicReference<IPeerNode>();

		if (rootElement instanceof IRuntimeModel) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					if (rootElement instanceof IRuntimeModel) {
						node.set(((IRuntimeModel)rootElement).getPeerNode());
					}
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);
		}
		else if (rootElement != null) {
			node.set(rootElement instanceof IAdaptable ? (IPeerNode)((IAdaptable)rootElement).getAdapter(IPeerNode.class) : (IPeerNode)Platform.getAdapterManager().getAdapter(rootElement, IPeerNode.class));
		}

		String message = Messages.getStringDelegated(node.get(), key);
	    return message;
 	}

	/**
	 * Get a name representation for each process node.
	 *
	 * @param rootElement The root element whose name is being retrieved.
	 * @return The node's name.
	 */
	private String getElementName(final Object rootElement) {
		if (rootElement == null || rootElement instanceof IRuntimeModel) {
			final AtomicReference<IPeerNode> node = new AtomicReference<IPeerNode>();

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					node.set(((IRuntimeModel)rootElement).getPeerNode());
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);

			String label = Messages.getStringDelegated(node.get(), "ProcessSearchable_ProcessList"); //$NON-NLS-1$
			return label != null ? label : Messages.ProcessSearchable_ProcessList;
		}
		return labelProvider.getText(rootElement);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#getElementText(java.lang.Object)
	 */
	@Override
    public String getElementText(Object element) {
	    return getElementName(element);
    }
}
