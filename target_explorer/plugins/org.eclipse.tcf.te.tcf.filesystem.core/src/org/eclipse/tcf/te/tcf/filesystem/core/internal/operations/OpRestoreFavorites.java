/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.tcf.te.tcf.filesystem.core.activator.CorePlugin;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IResultOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.model.RuntimeModel;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

/**
 * The operation to restore the favorite nodes
 */
public class OpRestoreFavorites extends AbstractOperation implements IResultOperation<IFSTreeNode[]> {

	private RuntimeModel fRuntimeModel;
	private List<IFSTreeNode> fResult = new ArrayList<IFSTreeNode>();

	public OpRestoreFavorites(RuntimeModel runtimeModel) {
		fRuntimeModel = runtimeModel;
	}

	@Override
	public IFSTreeNode[] getResult() {
		return fResult.toArray(new IFSTreeNode[fResult.size()]);
	}

	@Override
	public IStatus doRun(IProgressMonitor monitor) {
		Set<String> favorites = CorePlugin.getDefault().getRevealOnConnect();
		if (favorites.isEmpty())
			return Status.OK_STATUS;

		SubMonitor sm = SubMonitor.convert(monitor, getName(), favorites.size());
		for (String fav : favorites) {
			if (openFavorite(fav, sm.newChild(1)).getSeverity() == IStatus.CANCEL) {
				monitor.done();
				return Status.CANCEL_STATUS;
			}
		}
		monitor.done();
		return Status.OK_STATUS;
	}


	private IStatus openFavorite(String fav, SubMonitor sm) {
		IResultOperation<IFSTreeNode> op = fRuntimeModel.operationRestoreFromPath(fav);
		IStatus s = op.run(sm);

		IFSTreeNode node = op.getResult();
		if (node != null) {
			fResult.add(node);
		}
		return s;
	}

	@Override
	public String getName() {
		return Messages.OpRestoreFavorites_name;
	}
}
