/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.filter;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;

/**
 * Filter implementation filtering peers not started by the current user.
 */
public class PeersByCurrentUserFilter extends ViewerFilter {
	/* default */ static final String USERNAME = System.getProperty("user.name"); //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
		if (element instanceof IPeerModel) {
			final AtomicReference<String> user = new AtomicReference<String>();

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					IPeerModel peerModel = (IPeerModel)element;

					String value = peerModel.getPeer().getAttributes().get("static.transient"); //$NON-NLS-1$
					boolean isStatic = value != null && Boolean.parseBoolean(value.trim());

					if (!isStatic) {
						user.set(peerModel.getPeer().getUserName());
					} else {
						user.set(USERNAME);
					}
				}
			};
			Assert.isTrue(!Protocol.isDispatchThread());
			Protocol.invokeAndWait(runnable);

			return USERNAME.equals(user.get());
		}

		return true;
	}

}
