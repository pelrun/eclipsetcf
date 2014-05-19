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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.ITerminalService;
import org.eclipse.tcf.te.runtime.services.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tcf.te.runtime.utils.net.IPAddressUtil;
import org.eclipse.tcf.te.ui.controls.BaseDialogPageControl;
import org.eclipse.tcf.te.ui.terminals.interfaces.IConfigurationPanel;
import org.eclipse.tcf.te.ui.terminals.interfaces.IMementoHandler;
import org.eclipse.tcf.te.ui.terminals.launcher.AbstractLauncherDelegate;
import org.eclipse.tcf.te.ui.terminals.local.controls.LocalWizardConfigurationPanel;
import org.eclipse.ui.WorkbenchEncoding;

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
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.ILauncherDelegate#getPanel(org.eclipse.tcf.te.ui.controls.BaseDialogPageControl)
	 */
	@Override
	public IConfigurationPanel getPanel(BaseDialogPageControl parentControl) {
		return new LocalWizardConfigurationPanel(parentControl);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.ILauncherDelegate#execute(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void execute(IPropertiesContainer properties, ICallback callback) {
		Assert.isNotNull(properties);

		// Set the terminal tab title
		String terminalTitle = getTerminalTitle(properties);
		if (terminalTitle != null) {
			properties.setProperty(ITerminalsConnectorConstants.PROP_TITLE, terminalTitle);
		}

		// If not configured, set the default encodings for the local terminal
		if (!properties.containsKey(ITerminalsConnectorConstants.PROP_ENCODING)) {
			String encoding = null;
			// Set the default encoding:
			//     Default UTF-8 on Mac for Local, Preferences:Platform encoding otherwise
			if (Platform.OS_MACOSX.equals(Platform.getOS())) {
				encoding = "UTF-8"; //$NON-NLS-1$
			} else {
				encoding = WorkbenchEncoding.getWorkbenchDefaultEncoding();
			}
			if (encoding != null && !"".equals(encoding)) properties.setProperty(ITerminalsConnectorConstants.PROP_ENCODING, encoding); //$NON-NLS-1$
		}

		// For local terminals, force a new terminal tab each time it is launched,
		// if not set otherwise from outside
		if (!properties.containsKey(ITerminalsConnectorConstants.PROP_FORCE_NEW)) {
			properties.setProperty(ITerminalsConnectorConstants.PROP_FORCE_NEW, true);
		}

		// Start the local terminal in the users home directory
		String home = System.getProperty("user.home"); //$NON-NLS-1$
		if (home != null && !"".equals(home)) { //$NON-NLS-1$
			properties.setProperty(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR, home);
		}

		// Get the terminal service
		ITerminalService terminal = ServiceManager.getInstance().getService(ITerminalService.class);
		// If not available, we cannot fulfill this request
		if (terminal != null) {
			terminal.openConsole(properties, callback);
		}
	}

	/**
	 * Returns the terminal title string.
	 * <p>
	 * The default implementation constructs a title like &quot;Serial &lt;port&gt; (Start time) &quot;.
	 *
	 * @return The terminal title string or <code>null</code>.
	 */
	private String getTerminalTitle(IPropertiesContainer properties) {
		String[] hostNames= IPAddressUtil.getInstance().getCanonicalHostNames();
		if (hostNames.length != 0){
			return hostNames[0];
		}
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
