/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.tcf.te.ui.utils.TreeViewerUtil;
import org.eclipse.tcf.te.ui.views.editor.pages.AbstractTreeViewerExplorerEditorPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * The search handler to search elements in the tree of the properties editor.
 */
public class EditorSearchHandler extends AbstractHandler {
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActiveEditorChecked(event);
		if (part instanceof FormEditor) {
			FormEditor editor = (FormEditor) part;
			IFormPage formPage = editor.getActivePageInstance();
			if (formPage instanceof AbstractTreeViewerExplorerEditorPage) {
				AbstractTreeViewerExplorerEditorPage page = (AbstractTreeViewerExplorerEditorPage) formPage;
				TreeViewer viewer = (TreeViewer) page.getTreeControl().getViewer();
				TreeViewerUtil.doSearch(viewer);
			}
		}
		return null;
	}
}
