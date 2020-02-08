/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.editor.tree.columns;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IPendingOperationNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNodeProperties;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;

/**
 * The label provider for the tree column "Command Line".
 */
public class CommandLineLabelProvider extends LabelProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	@Override
	public String getText(Object element) {
		if (element instanceof IRuntimeModel || element instanceof IPendingOperationNode) {
			return ""; //$NON-NLS-1$
		}

		if (element instanceof IProcessContextNode) {
			final IProcessContextNode node = (IProcessContextNode)element;

			final AtomicReference<String> cmd = new AtomicReference<String>();

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					String[] cmdline = (String[])node.getProperty(IProcessContextNodeProperties.PROPERTY_CMD_LINE);
					if (cmdline != null) cmd.set(makeString(cmdline));
				}
			};

			Assert.isTrue(!Protocol.isDispatchThread());
			Protocol.invokeAndWait(runnable);

			if (cmd.get() != null) return cmd.get();
		}

		return ""; //$NON-NLS-1$
	}

	/**
	 * Makes a string from the given string array.
	 *
	 * @param cmdline The string array. Must not be <code>null</code>.
	 * @return The array as string.
	 */
	public final static String makeString(String[] cmdline) {
		Assert.isNotNull(cmdline);

		StringBuilder buffer = new StringBuilder();
		for (String arg : cmdline) {
			buffer.append(arg.contains(" ") ? "\"" + arg + "\"" : arg); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			buffer.append(" "); //$NON-NLS-1$
		}

		return buffer.toString();
	}
}
