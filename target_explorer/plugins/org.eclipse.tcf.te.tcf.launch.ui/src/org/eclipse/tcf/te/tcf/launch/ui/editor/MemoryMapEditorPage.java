/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.editor;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.tcf.te.tcf.launch.ui.editor.tabs.MemoryMapTab;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;

/**
 * TCF memory map launch configuration tab container page implementation.
 */
public class MemoryMapEditorPage extends AbstractTcfLaunchTabContainerEditorPage implements ISelectionListener  {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormPage#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input) {
		super.init(site, input);

		ISelectionService service = (ISelectionService)getSite().getService(ISelectionService.class);
		service.addSelectionListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.editor.AbstractLaunchTabContainerEditorPage#createLaunchConfigurationTab()
	 */
	@Override
	protected AbstractLaunchConfigurationTab createLaunchConfigurationTab() {
		return new MemoryMapTab(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (isActive() && part instanceof IDebugView) {
			if (((MemoryMapTab)getLaunchConfigurationTab()).updateContext()) {
				((MemoryMapTab)getLaunchConfigurationTab()).initializeFrom(getLaunchConfig(getPeerModel(getEditorInput())));
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.launch.ui.editor.AbstractTcfLaunchTabContainerEditorPage#dispose()
	 */
	@Override
	public void dispose() {
		ISelectionService service = (ISelectionService)getSite().getService(ISelectionService.class);
		service.removeSelectionListener(this);
		super.dispose();
	}
}
