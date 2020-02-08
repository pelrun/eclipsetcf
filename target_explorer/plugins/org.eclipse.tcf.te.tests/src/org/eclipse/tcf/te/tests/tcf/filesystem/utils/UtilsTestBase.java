/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf.filesystem.utils;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tests.tcf.filesystem.url.URLTestBase;

public class UtilsTestBase extends URLTestBase {
	protected void updateCache(FSTreeNode testFile) throws Exception {
		IOperation update = testFile.operationDownload(null);
		update.run(new NullProgressMonitor());
	}
}
