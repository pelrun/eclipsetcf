/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.operations;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DoneSetStat;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.te.tcf.core.concurrent.TCFOperationMonitor;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

/**
 * The operation implementation to commit the new attributes to
 * the file system node.
 */
public class OpCommitAttr extends AbstractOperation {
	protected FSTreeNode fNode;
	protected IFileSystem.FileAttrs fAttributes;

	public OpCommitAttr(FSTreeNode node, IFileSystem.FileAttrs attrs) {
		fNode = node;
		fAttributes = attrs;
	}

	@Override
	public IStatus doRun(IProgressMonitor monitor) {
		monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);

		final TCFOperationMonitor<?> result = new TCFOperationMonitor<Object>();
		final IFileSystem fileSystem = fNode.getRuntimeModel().getFileSystem();
		if (fileSystem == null) {
			return result.setCancelled();
		}
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (!result.checkCancelled()) {
					String path = fNode.getLocation(true);
					fileSystem.setstat(path, fAttributes, new DoneSetStat() {
						@Override
						public void doneSetStat(IToken token, FileSystemException error) {
							if (error != null) {
								result.setError(Messages.OpCommitAttr_error_cannotSetAttributes, error);
							} else {
								fNode.setAttributes(fAttributes, true);
								result.setDone(null);
							}
						}
					});
				}
			}
		});
		return result.waitDone(monitor);
	}

	@Override
	public String getName() {
		return Messages.OpCommitAttr_name + fNode.getName();
	}
}
