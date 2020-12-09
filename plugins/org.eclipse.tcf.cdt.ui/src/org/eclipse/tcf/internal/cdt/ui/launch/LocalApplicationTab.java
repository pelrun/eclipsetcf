/*******************************************************************************
 * Copyright (c) 2020 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui.launch;

import org.eclipse.tcf.internal.debug.ui.launch.TCFMainTab;

public class LocalApplicationTab extends TCFMainTab {

    protected boolean isLocal() {
        return true;
    }
}
