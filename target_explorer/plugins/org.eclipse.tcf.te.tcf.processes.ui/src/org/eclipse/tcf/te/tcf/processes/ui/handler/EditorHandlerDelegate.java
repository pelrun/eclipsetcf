/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.handler;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.processes.ui.editor.ProcessMonitorEditorPage;
import org.eclipse.tcf.te.ui.swt.DisplayUtil;
import org.eclipse.tcf.te.ui.views.editor.Editor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;

/**
 * Processes properties command handler implementation.
 */
public class EditorHandlerDelegate extends org.eclipse.tcf.te.tcf.ui.handler.EditorHandlerDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.handler.IEditorHandlerDelegate#postOpenProperties(org.eclipse.ui.IEditorPart, java.lang.Object)
	 */
	@Override
	public void postOpenEditor(IEditorPart editor, final Object element) {
		if (editor instanceof FormEditor) {
			final FormEditor formEditor = (FormEditor)editor;
			DisplayUtil.safeAsyncExec(new Runnable() {
				@Override
				public void run() {
					IFormPage page = formEditor.setActivePage("org.eclipse.tcf.te.tcf.processes.ui.ProcessExplorerEditorPage"); //$NON-NLS-1$
					// If the element is a context node, select the node
					if (page != null && (element instanceof IRuntimeModel || element instanceof IProcessContextNode)) {
						Viewer viewer = ((ProcessMonitorEditorPage)page).getTreeControl().getViewer();
						if (viewer != null) {
							viewer.setSelection(new StructuredSelection(element), true);
						}
					}
					else if (page == null && formEditor instanceof Editor) {
						((Editor)formEditor).setActivePage(0);
					}
				}
			});
		}
	}
}
