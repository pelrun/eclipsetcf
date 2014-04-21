/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.internal;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.tcf.te.runtime.services.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tcf.te.ui.terminals.launcher.LauncherDelegateManager;
import org.eclipse.tm.internal.terminal.control.ITerminalViewControl;
import org.eclipse.tm.internal.terminal.provisional.api.TerminalState;


/**
 * Terminals property tester implementation.
 */
@SuppressWarnings("restriction")
public class PropertyTester extends org.eclipse.core.expressions.PropertyTester {

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if ("hasApplicableLauncherDelegates".equals(property)) { //$NON-NLS-1$
			ISelection selection = receiver instanceof ISelection ? (ISelection)receiver : new StructuredSelection(receiver);
			return expectedValue.equals(Boolean.valueOf(LauncherDelegateManager.getInstance().getApplicableLauncherDelegates(selection).length > 0));
		}

		if ("hasDisconnectButton".equals(property) && receiver instanceof CTabItem) { //$NON-NLS-1$
            Boolean hasDisconnectButton = (Boolean)((CTabItem)receiver).getData(ITerminalsConnectorConstants.PROP_HAS_DISCONNECT_BUTTON);
            return expectedValue.equals(hasDisconnectButton);
		}

		if ("canDisconnect".equals(property) && receiver instanceof CTabItem && ((CTabItem)receiver).getData() instanceof ITerminalViewControl) { //$NON-NLS-1$
            ITerminalViewControl terminal = (ITerminalViewControl)((CTabItem)receiver).getData();
            TerminalState state = terminal.getState();
            return expectedValue.equals(Boolean.valueOf(state != TerminalState.CLOSED));
		}

		return false;
	}

}
