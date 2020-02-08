/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * William Chen (Wind River) - [345552] Edit the remote files with a proper editor
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.autosave;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IResultOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations.UiExecutor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * The execution listener of command "SAVE ALL", which synchronizes the local
 * file with the one on the target server after it is saved.
 */
public class SaveAllListener implements IExecutionListener {
	// Dirty nodes that should be saved and synchronized.
	List<IFSTreeNode> fDirtyNodes;
	/**
	 * Create the listener listening to command "SAVE ALL".
	 */
	public SaveAllListener() {
		this.fDirtyNodes = new ArrayList<IFSTreeNode>();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IExecutionListener#postExecuteSuccess(java.lang.String, java.lang.Object)
	 */
	@Override
	public void postExecuteSuccess(String commandId, Object returnValue) {
		if (!fDirtyNodes.isEmpty()) {
			if (UIPlugin.isAutoSaving()) {
				UiExecutor.execute(ModelManager.operationUpload(fDirtyNodes));
			} else {
				for (IFSTreeNode dirtyNode : fDirtyNodes) {
					dirtyNode.operationRefresh(false).runInJob(null);
				}
			}
		}
	}

	@Override
	public void preExecute(String commandId, ExecutionEvent event) {
		fDirtyNodes.clear();
        // In Eclipse 4.x, the HandlerUtil.getActiveWorkbenchWindow(event) may return null
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		if (window == null) window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		IEditorPart[] editors = page.getDirtyEditors();
		for (IEditorPart editor : editors) {
			IEditorInput input = editor.getEditorInput();
			IFSTreeNode node = null;
			if (input instanceof IURIEditorInput) {
				//Get the file that is being edited.
				IURIEditorInput fileInput = (IURIEditorInput) input;
				URI uri = fileInput.getURI();
				try {
					IFileStore store = EFS.getStore(uri);
					File localFile = store.toLocalFile(0, new NullProgressMonitor());
					if (localFile != null) {
						// Get the file's mapped FSTreeNode.
						IResultOperation<IFSTreeNode> parser = ModelManager.operationRestoreFromPath(localFile.getCanonicalPath());
						parser.run(null);
						node = parser.getResult();
						if (node != null) {
							// If it is a modified node, add it to the dirty node list.
							fDirtyNodes.add(node);
						}
					}
				} catch(Exception e){}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IExecutionListener#notHandled(java.lang.String, org.eclipse.core.commands.NotHandledException)
	 */
	@Override
	public void notHandled(String commandId, NotHandledException exception) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IExecutionListener#postExecuteFailure(java.lang.String, org.eclipse.core.commands.ExecutionException)
	 */
	@Override
	public void postExecuteFailure(String commandId, ExecutionException exception) {
	}
}
