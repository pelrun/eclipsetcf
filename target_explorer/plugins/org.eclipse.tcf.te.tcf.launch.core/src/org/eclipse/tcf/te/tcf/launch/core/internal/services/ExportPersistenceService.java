/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.core.internal.services;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.tcf.core.TransientPeer;
import org.eclipse.tcf.te.runtime.persistence.PersistenceManager;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate;
import org.eclipse.tcf.te.runtime.persistence.services.URIPersistenceService;
import org.eclipse.tcf.te.tcf.core.interfaces.IExportPersistenceService;
import org.eclipse.tcf.te.tcf.launch.core.interfaces.ILaunchTypes;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Persistence service implementation for import/export.
 */
public class ExportPersistenceService extends URIPersistenceService implements IExportPersistenceService {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.interfaces.IURIPersistenceService#write(java.lang.Object, java.net.URI)
	 */
	@Override
	public void write(Object context, URI uri) throws IOException {
		Assert.isNotNull(context);

		uri = uri != null ? uri : getURI(context);

		// Determine the persistence delegate
		IPersistenceDelegate delegate = PersistenceManager.getInstance().getDelegate(context, uri);
		// If the persistence delegate could not be determined, throw an IOException
		if (delegate == null) {
			throw new IOException("The persistence delegate for context '" + context.getClass().getName() + "' cannot be determined."); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (context instanceof IPeerNode) {
			final IPeerNode node = (IPeerNode)context;
			ILaunchConfiguration launchConfig = (ILaunchConfiguration)Platform.getAdapterManager().getAdapter(node, ILaunchConfiguration.class);
			if (launchConfig != null) {
				IPersistenceDelegate launchDelegate = PersistenceManager.getInstance().getDelegate(launchConfig, String.class);
				if (launchDelegate != null) {
					final String launchConfigString = (String)launchDelegate.write(launchConfig, String.class);
					Map<String,String> attrs = new HashMap<String, String>(node.getPeer().getAttributes());
					attrs.put(ILaunchTypes.ATTACH, launchConfigString);
					delegate.write(new TransientPeer(attrs), uri);
					return;
				}
			}
		}

		// Pass on to the delegate for writing
		delegate.write(context, uri);
	}
}
