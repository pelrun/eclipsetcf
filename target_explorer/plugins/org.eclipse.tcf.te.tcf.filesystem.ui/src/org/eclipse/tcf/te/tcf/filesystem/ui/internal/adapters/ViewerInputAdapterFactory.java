/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.adapters;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IPropertyChangeProvider;
import org.eclipse.tcf.te.core.interfaces.IViewerInput;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * The adapter factory for IViewerInput.
 */
public class ViewerInputAdapterFactory implements IAdapterFactory {
	// The key to store and access the the viewer input object.
	private static final String VIEWER_INPUT_KEY = UIPlugin.getUniqueIdentifier()+".peer.viewerInput"; //$NON-NLS-1$
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
	 */
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if(adaptableObject instanceof IPeerNode) {
			if (IViewerInput.class.equals(adapterType) 
							|| IPropertyChangeProvider.class.equals(adapterType)) {
				IPeerNode peerNode = (IPeerNode) adaptableObject;
				return getViewerInput(peerNode);
			}
		}
		return null;
	}
	
	/**
	 * Get a viewer input from the specified peer model.
	 * 
	 * @param peerNode The peer model to get the viewer input from.
	 * @return The peer model's viewer input.
	 */
	PeerNodeViewerInput getViewerInput(final IPeerNode peerNode) {
		if (peerNode != null) {
			if (Protocol.isDispatchThread()) {
				PeerNodeViewerInput model = (PeerNodeViewerInput) peerNode.getProperty(VIEWER_INPUT_KEY);
				if (model == null) {
					model = new PeerNodeViewerInput(peerNode);
					peerNode.setProperty(VIEWER_INPUT_KEY, model);
				}
				return model;
			}
			final AtomicReference<PeerNodeViewerInput> reference = new AtomicReference<PeerNodeViewerInput>();
			Protocol.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					reference.set(getViewerInput(peerNode));
				}
			});
			return reference.get();
		}
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
	 */
	@Override
	public Class[] getAdapterList() {
		return new Class[] { IViewerInput.class, IPropertyChangeProvider.class };
	}

}
