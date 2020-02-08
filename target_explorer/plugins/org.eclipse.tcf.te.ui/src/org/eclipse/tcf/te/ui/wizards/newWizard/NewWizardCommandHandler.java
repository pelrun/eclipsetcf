/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.wizards.newWizard;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.tcf.te.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.ui.wizards.AbstractWizardCommandHandler;

/**
 * &quot;org.eclipse.tcf.te.ui.command.newWizards" default command handler implementation.
 */
public class NewWizardCommandHandler extends AbstractWizardCommandHandler {

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.ui.wizards.AbstractWizardCommandHandler#createWizard()
     */
    @Override
    protected IWizard createWizard() {
		return new NewWizard(null);
	}

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.ui.wizards.AbstractWizardCommandHandler#getHelpId()
     */
    @Override
    protected String getHelpId() {
    	return IContextHelpIds.NEW_TARGET_WIZARD;
    }
}
