/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.operations;

import static java.text.MessageFormat.format;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DoneRename;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.te.tcf.core.concurrent.TCFOperationMonitor;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.CacheManager;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;
/**
 * FSRename renames the specified file/folder to a
 * new name.
 *
 */
public class OpRename extends AbstractOperation {
	FSTreeNode node;
	String newName;

	public OpRename(FSTreeNode node, String newName) {
		this.node = node;
		this.newName = newName;
	}

	@Override
	public IStatus doRun(IProgressMonitor monitor) {
		monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);

		CacheManager.clearCache(node);
		final TCFOperationMonitor<?> result = new TCFOperationMonitor<Object>();
		monitor.subTask(NLS.bind(Messages.OpMove_Moving, node.getLocation()));
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				tcfRename(result);
			}
		});
		return result.waitDone(monitor);
	}


	protected void tcfRename(final TCFOperationMonitor<?> result) {
		if (result.checkCancelled())
			return;

		final IFileSystem fileSystem = node.getRuntimeModel().getFileSystem();
		if (fileSystem == null) {
			result.setCancelled();
			return;
		}

		CacheManager.clearCache(node);

		final String sourcePath = node.getLocation(true);
		final String destPath = getPath(node.getParent(), newName);

		fileSystem.rename(sourcePath, destPath, new DoneRename() {
			@Override
			public void doneRename(IToken token, FileSystemException error) {
				if (error != null) {
					result.setError(format(Messages.OpMove_CannotMove, sourcePath), error);
				} else {
					FSTreeNode parent = node.getParent();
					parent.removeNode(node, true);
					node.changeName(newName);
					parent.addNode(node, true);
					result.setDone(null);
				}
			}
		});
	}

	@Override
    public String getName() {
	    return Messages.OpRename_TitleRename;
    }
}
