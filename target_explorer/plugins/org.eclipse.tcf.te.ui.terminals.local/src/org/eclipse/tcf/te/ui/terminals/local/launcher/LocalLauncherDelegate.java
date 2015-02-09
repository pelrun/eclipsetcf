/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.local.launcher;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.te.core.terminals.TerminalServiceFactory;
import org.eclipse.tcf.te.core.terminals.interfaces.ITerminalService;
import org.eclipse.tcf.te.core.terminals.interfaces.ITerminalService.Done;
import org.eclipse.tcf.te.core.terminals.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tcf.te.ui.terminals.interfaces.IConfigurationPanel;
import org.eclipse.tcf.te.ui.terminals.interfaces.IConfigurationPanelContainer;
import org.eclipse.tcf.te.ui.terminals.interfaces.IMementoHandler;
import org.eclipse.tcf.te.ui.terminals.launcher.AbstractLauncherDelegate;
import org.eclipse.tcf.te.ui.terminals.local.activator.UIPlugin;
import org.eclipse.tcf.te.ui.terminals.local.controls.LocalWizardConfigurationPanel;
import org.eclipse.tcf.te.ui.terminals.local.showin.interfaces.IPreferenceKeys;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchEncoding;
import org.osgi.framework.Bundle;

/**
 * Serial launcher delegate implementation.
 */
public class LocalLauncherDelegate extends AbstractLauncherDelegate {

	private final IMementoHandler mementoHandler = new LocalMementoHandler();

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.ILauncherDelegate#needsUserConfiguration()
	 */
	@Override
	public boolean needsUserConfiguration() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.ILauncherDelegate#getPanel(org.eclipse.tcf.te.ui.terminals.interfaces.IConfigurationPanelContainer)
	 */
	@Override
	public IConfigurationPanel getPanel(IConfigurationPanelContainer container) {
		return new LocalWizardConfigurationPanel(container);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.ILauncherDelegate#execute(java.util.Map, org.eclipse.tcf.te.core.terminals.interfaces.ITerminalService.Done)
	 */
	@Override
	public void execute(Map<String, Object> properties, Done done) {
		Assert.isNotNull(properties);

		// Set the terminal tab title
		String terminalTitle = getTerminalTitle(properties);
		if (terminalTitle != null) {
			properties.put(ITerminalsConnectorConstants.PROP_TITLE, terminalTitle);
		}

		// If not configured, set the default encodings for the local terminal
		if (!properties.containsKey(ITerminalsConnectorConstants.PROP_ENCODING)) {
			String encoding = null;
			// Set the default encoding:
			//     Default UTF-8 on Mac or Windows for Local, Preferences:Platform encoding otherwise
			if (Platform.OS_MACOSX.equals(Platform.getOS()) || Platform.OS_WIN32.equals(Platform.getOS())) {
				encoding = "UTF-8"; //$NON-NLS-1$
			} else {
				encoding = WorkbenchEncoding.getWorkbenchDefaultEncoding();
			}
			if (encoding != null && !"".equals(encoding)) properties.put(ITerminalsConnectorConstants.PROP_ENCODING, encoding); //$NON-NLS-1$
		}

		// For local terminals, force a new terminal tab each time it is launched,
		// if not set otherwise from outside
		if (!properties.containsKey(ITerminalsConnectorConstants.PROP_FORCE_NEW)) {
			properties.put(ITerminalsConnectorConstants.PROP_FORCE_NEW, Boolean.TRUE);
		}

		// Initialize the local terminal working directory.
		// By default, start the local terminal in the users home directory
		String initialCwd = UIPlugin.getScopedPreferences().getString(IPreferenceKeys.PREF_LOCAL_TERMINAL_INITIAL_CWD);
		String cwd = null;
		if (initialCwd == null || IPreferenceKeys.PREF_INITIAL_CWD_USER_HOME.equals(initialCwd) || "".equals(initialCwd.trim())) { //$NON-NLS-1$
			cwd = System.getProperty("user.home"); //$NON-NLS-1$
		} else if (IPreferenceKeys.PREF_INITIAL_CWD_ECLIPSE_HOME.equals(initialCwd)) {
			String eclipseHomeLocation = System.getProperty("eclipse.home.location"); //$NON-NLS-1$
			if (eclipseHomeLocation != null) {
				try {
					URI uri = URIUtil.fromString(eclipseHomeLocation);
					File f = URIUtil.toFile(uri);
					cwd = f.getAbsolutePath();
				} catch (URISyntaxException ex) { /* ignored on purpose */ }
			}
		} else if (IPreferenceKeys.PREF_INITIAL_CWD_ECLIPSE_WS.equals(initialCwd)) {
			Bundle bundle = Platform.getBundle("org.eclipse.core.resources"); //$NON-NLS-1$
			if (bundle != null && (bundle.getState() == Bundle.RESOLVED || bundle.getState() == Bundle.ACTIVE)) {
		        if (org.eclipse.core.resources.ResourcesPlugin.getWorkspace() != null
		        	            && org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot() != null
		        	            && org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().getLocation() != null) {
		        	cwd = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
		        }
			}
		} else {
			IPath p = new Path(initialCwd);
			if (p.toFile().canRead() && p.toFile().isDirectory()) {
				cwd = p.toOSString();
			}
		}

		if (cwd != null && !"".equals(cwd)) { //$NON-NLS-1$
			properties.put(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR, cwd);
		}

		// If the current selection resolved to an folder, default the working directory
		// to that folder and update the terminal title
		ISelectionService service = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();
		if ((service != null && service.getSelection() != null) || properties.containsKey(ITerminalsConnectorConstants.PROP_SELECTION)) {
			ISelection selection = (ISelection)properties.get(ITerminalsConnectorConstants.PROP_SELECTION);
			if (selection == null) selection = service.getSelection();
			if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
				String dir = null;
				Iterator<?> iter = ((IStructuredSelection)selection).iterator();
				while (iter.hasNext()) {
					Object element = iter.next();

					Bundle bundle = Platform.getBundle("org.eclipse.core.resources"); //$NON-NLS-1$
					if (bundle != null && (bundle.getState() == Bundle.RESOLVED || bundle.getState() == Bundle.ACTIVE)) {
						// If the element is not an IResource, try to adapt to IResource
						if (!(element instanceof org.eclipse.core.resources.IResource)) {
							Object adapted = element instanceof IAdaptable ? ((IAdaptable)element).getAdapter(org.eclipse.core.resources.IResource.class) : null;
							if (adapted == null) adapted = Platform.getAdapterManager().getAdapter(element, org.eclipse.core.resources.IResource.class);
							if (adapted != null) element = adapted;
						}

						if (element instanceof org.eclipse.core.resources.IResource && ((org.eclipse.core.resources.IResource)element).exists()) {
							IPath location = ((org.eclipse.core.resources.IResource)element).getLocation();
							if (location == null) continue;
							if (location.toFile().isFile()) location = location.removeLastSegments(1);
							if (location.toFile().isDirectory() && location.toFile().canRead()) {
								dir = location.toFile().getAbsolutePath();
								break;
							}
						}
					}
				}
				if (dir != null) {
					properties.put(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR, dir);

					String basename = new Path(dir).lastSegment();
					properties.put(ITerminalsConnectorConstants.PROP_TITLE, basename + " (" + terminalTitle + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}

		// Get the terminal service
		ITerminalService terminal = TerminalServiceFactory.getService();
		// If not available, we cannot fulfill this request
		if (terminal != null) {
			terminal.openConsole(properties, done);
		}
	}

	/**
	 * Returns the terminal title string.
	 * <p>
	 * The default implementation constructs a title like &quot;Serial &lt;port&gt; (Start time) &quot;.
	 *
	 * @return The terminal title string or <code>null</code>.
	 */
	private String getTerminalTitle(Map<String, Object> properties) {
		try {
			String hostname = InetAddress.getLocalHost().getHostName();
			if (hostname != null && !"".equals(hostname.trim())) { //$NON-NLS-1$
				return hostname;
			}
		} catch (UnknownHostException e) { /* ignored on purpose */ }
		return "Local"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (IMementoHandler.class.equals(adapter)) {
			return mementoHandler;
		}
	    return super.getAdapter(adapter);
	}
}
