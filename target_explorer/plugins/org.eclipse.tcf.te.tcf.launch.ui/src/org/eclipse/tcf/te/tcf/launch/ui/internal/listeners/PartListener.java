/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.internal.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPartReference;

/**
 * Part listener implementation.
 */
public class PartListener implements IPartListener2 {
	// The map of part listener registrations
	private final Map<IWorkbenchPartReference, List<Registration>> registrations = new HashMap<IWorkbenchPartReference, List<Registration>>();

	/**
	 * Part listener registration implementation.
	 */
	/* default */ static class Registration {
		/* default */ AbstractDebugView view = null;
		/* default */ MenuManager mgr = null;
		/* default */ IMenuListener listener = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partActivated(org.eclipse.ui.IWorkbenchPartReference)
	 */
	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partBroughtToTop(org.eclipse.ui.IWorkbenchPartReference)
	 */
	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partClosed(org.eclipse.ui.IWorkbenchPartReference)
	 */
	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		if (IDebugUIConstants.ID_DEBUG_VIEW.equals(partRef.getId())) {
			// Part closed --> Cleanup the registrations
			List<Registration> regs = registrations.remove(partRef);
			if (regs != null) {
				for (Registration reg : regs) {
					reg.mgr.removeMenuListener(reg.listener);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partDeactivated(org.eclipse.ui.IWorkbenchPartReference)
	 */
	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partOpened(org.eclipse.ui.IWorkbenchPartReference)
	 */
	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		if (IDebugUIConstants.ID_DEBUG_VIEW.equals(partRef.getId()) && partRef instanceof IViewReference) {
			// Get the list of existing registrations
			List<Registration> regs = registrations.get(partRef);
			if (regs == null) regs = new ArrayList<Registration>();

			// Get the view part
			IViewPart part = ((IViewReference)partRef).getView(false);
			if (part instanceof AbstractDebugView) {
				AbstractDebugView view = (AbstractDebugView)part;
				List<?> menuManagers = view.getContextMenuManagers();
				for (Object element : menuManagers) {
					if (!(element instanceof MenuManager)) continue;
					MenuManager mgr = (MenuManager)element;
					if ("#PopUp".equals(mgr.getMenuText())) { //$NON-NLS-1$
						// Check the registrations if this combination of view and menu manager
						// is already registered
						Registration reg = null;
						for (Registration candidate : regs) {
							if (candidate.view == view && candidate.mgr == mgr) {
								reg = candidate;
								break;
							}
						}

						if (reg == null) {
							IMenuListener listener = new MenuListener();
							mgr.addMenuListener(listener);

							// Create the registration
							reg = new Registration();
							reg.view = view;
							reg.mgr = mgr;
							reg.listener = listener;

							regs.add(reg);
						}
					}
				}
			}

			if (!regs.isEmpty()) registrations.put(partRef, regs);
			else registrations.remove(partRef);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partHidden(org.eclipse.ui.IWorkbenchPartReference)
	 */
	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partVisible(org.eclipse.ui.IWorkbenchPartReference)
	 */
	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partInputChanged(org.eclipse.ui.IWorkbenchPartReference)
	 */
	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
	}

}
