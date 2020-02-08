/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
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
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.tcf.core.TransientPeer;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.runtime.persistence.PersistenceManager;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate;
import org.eclipse.tcf.te.runtime.persistence.services.URIPersistenceService;
import org.eclipse.tcf.te.tcf.core.interfaces.IExportPersistenceService;
import org.eclipse.tcf.te.tcf.launch.core.interfaces.ILaunchTypes;

/**
 * Persistence service implementation for import/export.
 */
public class ImportPersistenceService extends URIPersistenceService implements IExportPersistenceService {

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

		if (context instanceof IPeer) {
			final String launchConfigString = ((IPeer)context).getAttributes().get(ILaunchTypes.ATTACH);
			if (launchConfigString != null && launchConfigString.trim().length() > 0) {
				IPersistenceDelegate launchDelegate = PersistenceManager.getInstance().getDelegate(ILaunchConfigurationWorkingCopy.class, launchConfigString);
				if (launchDelegate != null) {
					Map<String,String> attrs = new HashMap<String, String>(((IPeer)context).getAttributes());
					attrs.remove(ILaunchTypes.ATTACH);
					IPeer peer = new TransientPeer(attrs);
					delegate.write(peer, uri);

					try {
						ILaunchConfigurationWorkingCopy config = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(ILaunchTypes.ATTACH).newInstance(null, peer.getName());
						config = (ILaunchConfigurationWorkingCopy)launchDelegate.read(config, launchConfigString);
						config.doSave();
					}
					catch (Exception e) {
					}
					return;
				}
			}
		}

		// Pass on to the delegate for writing
		delegate.write(context, uri);
	}
}
