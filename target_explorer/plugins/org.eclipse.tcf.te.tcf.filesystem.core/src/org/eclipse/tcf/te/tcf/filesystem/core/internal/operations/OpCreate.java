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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.core.concurrent.TCFOperationMonitor;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IResultOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

/**
 * The base operation class for creating a file or a folder in the file system of Target
 * Explorer.
 */
public abstract class OpCreate extends AbstractOperation implements IResultOperation<IFSTreeNode> {
	final protected FSTreeNode fDestination;
	final protected String fName;
	protected FSTreeNode fResult;

	public OpCreate(FSTreeNode folder, String name) {
		Assert.isNotNull(folder);
		Assert.isNotNull(name);
		fDestination = folder;
		fName = name;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		if (fDestination.getChildren() == null) {
			IStatus status = fDestination.operationRefresh(false).run(new SubProgressMonitor(monitor, 0));
			if (!status.isOK())
				return status;
		}
		FSTreeNode existing = fDestination.findChild(fName);
		if (existing != null) {
			return StatusHelper.createStatus(format(Messages.OpCreate_error_existingFile, existing.getLocation()), null);
		}

		final TCFOperationMonitor<FSTreeNode> result = new TCFOperationMonitor<FSTreeNode>();
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				tcfCreate(fDestination, fName, result);
			}
		});

		IStatus status = result.waitDone(monitor);
		fResult = result.getValue();
		return status;
	}

	protected abstract void tcfCreate(FSTreeNode destination, String name, TCFOperationMonitor<FSTreeNode> result);

	@Override
    public String getName() {
	    return NLS.bind(Messages.OpCreate_TaskName, fName);
    }

	@Override
	public FSTreeNode getResult() {
		return fResult;
	}
}
