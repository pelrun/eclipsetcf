/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.operations;

import static java.text.MessageFormat.format;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DoneMkDir;
import org.eclipse.tcf.services.IFileSystem.DoneStat;
import org.eclipse.tcf.services.IFileSystem.FileAttrs;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.te.tcf.core.concurrent.TCFOperationMonitor;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

/**
 * The file operation class to create a folder in the file system of Target Explorer.
 */
public class OpCreateFolder extends OpCreate {

	public OpCreateFolder(FSTreeNode folder, String name) {
		super(folder, name);
	}

	@Override
	protected void tcfCreate(final FSTreeNode destination, final String name, final TCFOperationMonitor<FSTreeNode> result) {
		Assert.isTrue(Protocol.isDispatchThread());
		if (result.checkCancelled())
			return;

		final String path = getPath(destination, name);
		final IFileSystem fileSystem = destination.getRuntimeModel().getFileSystem();
		if (fileSystem == null) {
			result.setCancelled();
			return;
		}

		fileSystem.mkdir(path, null, new DoneMkDir() {
			@Override
			public void doneMkDir(IToken token, FileSystemException error) {
				if (error != null) {
					result.setError(StatusHelper.createStatus(format(Messages.OpCreateFolder_error_createFolder, path), error));
				} else if (!result.checkCancelled()) {
					fileSystem.stat(path, new DoneStat() {
						@Override
						public void doneStat(IToken token, FileSystemException error, FileAttrs attrs) {
							if (error != null) {
								result.setError(StatusHelper.createStatus(format(Messages.OpCreateFolder_error_createFolder, path), error));
							} else if (!result.checkCancelled()) {
								FSTreeNode node = new FSTreeNode(destination, name, false, attrs);
								node.setContent(new FSTreeNode[0], false);
								destination.addNode(node, true);
								result.setDone(node);
							}
						}
					});
				}
			}
		});
	}
}
