/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.ui.wizards.newWizard;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.actions.NewWizardShortcutAction;
import org.eclipse.ui.wizards.IWizardDescriptor;

/**
 * AbstractNewSingleWizardHandler
 */
@SuppressWarnings("restriction")
public abstract class AbstractNewSingleWizardHandler extends NewWizardCommandHandler {

	/**
     * Constructor.
     */
    public AbstractNewSingleWizardHandler() {
    	super();
    }

	abstract protected String getWizardId(ExecutionEvent event);

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String wizardId = getWizardId(event);
		if (wizardId != null) {
			IWizardDescriptor wizardDesc = NewWizardRegistry.getInstance().findWizard(wizardId);
			if (wizardDesc != null) {
				IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
				if (window == null) {
					window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				}
				new NewWizardShortcutAction(window, wizardDesc).run();
				return null;
			}
		}
		return super.execute(event);
	}
}
