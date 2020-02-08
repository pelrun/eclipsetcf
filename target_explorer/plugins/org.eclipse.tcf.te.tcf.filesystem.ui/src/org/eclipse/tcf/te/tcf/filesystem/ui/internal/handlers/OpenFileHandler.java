/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * William Chen (Wind River)- [345387]Open the remote files with a proper editor
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.handlers;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.model.CacheState;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations.UiExecutor;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.tcf.te.ui.swt.DisplayUtil;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;

/**
 * The action handler to open a file on the remote file system.
 */
public class OpenFileHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelectionChecked(event);
		final IFSTreeNode node = (IFSTreeNode) selection.getFirstElement();
		final IWorkbenchPage page = HandlerUtil.getActiveSite(event).getPage();
		if (node.isBinaryFile()) {
			// If the file is a binary file.
			Shell parent = HandlerUtil.getActiveShell(event);
			MessageDialog.openWarning(parent, Messages.OpenFileHandler_Warning,
					Messages.OpenFileHandler_OpeningBinaryNotSupported);
		} else {
			if (UIPlugin.isAutoSaving()) {
				node.operationRefresh(false).runInJob(new Callback() {
					@Override
					protected void internalDone(Object caller, IStatus status) {
						File file = node.getCacheFile();
						if (node.getCacheState() == CacheState.outdated) {
							file.delete();
						}

						DisplayUtil.safeAsyncExec(new Runnable() {
							@Override
							public void run() {
								// Open the file node.
								openFile(node, page);
							}
						});
					}
				});
			} else {
				// Open the file node.
				openFile(node, page);
			}

		}
		return null;
	}

	/**
	 * Open the file node in an editor of the specified workbench page. If the
	 * local cache file of the node is stale, then download it. Then open its
	 * local cache file.
	 *
	 * @param node
	 *            The file node to be opened.
	 * @param page
	 *            The workbench page in which the editor is opened.
	 */
	/* default */ void openFile(IFSTreeNode node, IWorkbenchPage page) {
		File file = node.getCacheFile();
		if (!file.exists() && !UiExecutor.execute(node.operationDownload(null)).isOK()) {
			return;
		}
		openEditor(page, node);
	}

	/**
	 * Open the editor to display the file node in the UI thread.
	 *
	 * @param page
	 *            The workbench page in which the editor is opened.
	 * @param node
	 *            The file node whose local cache file is opened.
	 */
	private void openEditor(final IWorkbenchPage page, final IFSTreeNode node) {
		Display display = page.getWorkbenchWindow().getWorkbench().getDisplay();
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				IPath path = new Path(node.getCacheFile().getAbsolutePath());
				IFileStore fileStore = EFS.getLocalFileSystem().getStore(path);
				String editorID = node.getPreferredEditorID();
				try {
					if(editorID!=null){
						FileStoreEditorInput input = new FileStoreEditorInput(fileStore);
						page.openEditor(input, editorID, true, IWorkbenchPage.MATCH_INPUT|IWorkbenchPage.MATCH_ID);
					}else{
						IDE.openEditorOnFileStore(page, fileStore);
					}
				} catch (PartInitException e) {
				}
			}
		});
	}
}
