/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.editor.tree.columns;

import java.io.Serializable;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNodeProperties;

/**
 * The comparator for the tree column "Command Line".
 */
public class CommandLineComparator implements Comparator<IProcessContextNode> , Serializable {
    private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(final IProcessContextNode o1, final IProcessContextNode o2) {
		final AtomicReference<String> cmd1 = new AtomicReference<String>();
		final AtomicReference<String> cmdArgs1 = new AtomicReference<String>();
		final AtomicReference<String> cmd2 = new AtomicReference<String>();
		final AtomicReference<String> cmdArgs2 = new AtomicReference<String>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				String[] cmdline1 = (String[])o1.getProperty(IProcessContextNodeProperties.PROPERTY_CMD_LINE);
				if (cmdline1 != null && cmdline1.length > 0) {
					cmd1.set(cmdline1[0]);
					String[] args = new String[cmdline1.length - 1];
					System.arraycopy(cmdline1, 1, args, 0, cmdline1.length - 1);
					cmdArgs1.set(CommandLineLabelProvider.makeString(args));
				}

				String[] cmdline2 = (String[])o2.getProperty(IProcessContextNodeProperties.PROPERTY_CMD_LINE);
				if (cmdline2 != null && cmdline2.length > 0) {
					cmd2.set(cmdline2[0]);
					String[] args = new String[cmdline2.length - 1];
					System.arraycopy(cmdline2, 1, args, 0, cmdline2.length - 1);
					cmdArgs2.set(CommandLineLabelProvider.makeString(args));
				}
			}
		};

		Assert.isTrue(!Protocol.isDispatchThread());
		Protocol.invokeAndWait(runnable);

		if (cmd1.get() == null) {
			if (cmd2.get() == null) return 0;
			return -1;
		}
		if (cmd2.get() == null) return 1;

		// Compare the commands first
		int result = cmd1.get().compareTo(cmd2.get());
		// If equal, compare the arguments
		if (result == 0) {
			if (cmdArgs1.get() == null) {
				if (cmdArgs2.get() == null) return 0;
				return -1;
			}
			if (cmdArgs2.get() == null) return 1;

			result = cmdArgs1.get().compareTo(cmdArgs2.get());
		}

		return result;
	}
}
