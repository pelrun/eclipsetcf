/**
 * EditorHandlerDelegate.java
 * Created on Jan 25, 2012
 *
 * Copyright (c) 2012, 2013 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.handlers;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.tcf.te.tcf.filesystem.core.model.FSModel;
import org.eclipse.tcf.te.tcf.filesystem.core.model.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.pages.FSExplorerEditorPage;
import org.eclipse.tcf.te.tcf.ui.handler.AbstractPeerModelEditorHandlerDelegate;
import org.eclipse.tcf.te.ui.swt.DisplayUtil;
import org.eclipse.tcf.te.ui.views.editor.Editor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;

/**
 * Systems context node properties command handler implementation.
 */
public class EditorHandlerDelegate extends AbstractPeerModelEditorHandlerDelegate {

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
					IFormPage page = formEditor.setActivePage("org.eclipse.tcf.te.tcf.filesystem.FSExplorerEditorPage"); //$NON-NLS-1$
					// If the element is a context node, select the node
					if (page != null && element instanceof FSTreeNode || element instanceof FSModel) {
						Viewer viewer = ((FSExplorerEditorPage)page).getTreeControl().getViewer();
						if (viewer != null) {
							viewer.setSelection(new StructuredSelection(element), true);
						}
					}
					else if (formEditor instanceof Editor) {
						((Editor)formEditor).setActivePage(0);
					}
				}
			});
		}
	}
}
