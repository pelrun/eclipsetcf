/*******************************************************************************
 * Copyright (c) 2008, 2013 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.debug.ui.IDetailPaneFactory;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * The TCF detail pane factory is contributed to the <code>org.eclipse.debug.ui.detailPaneFactories</code>
 * extension. For any selection that contains TCFNode the factory can produce a <code>IDetailPane</code> object.
 */
public class TCFDetailPaneFactory implements IDetailPaneFactory {

    public IDetailPane createDetailPane(String paneID) {
        assert paneID.equals(TCFDetailPane.ID);
        return new TCFDetailPane();
    }

    public String getDefaultDetailPane(IStructuredSelection selection) {
        return TCFDetailPane.ID;
    }

    public String getDetailPaneDescription(String paneID) {
        return TCFDetailPane.NAME;
    }

    public String getDetailPaneName(String paneID) {
        return TCFDetailPane.DESC;
    }

    public Set<String> getDetailPaneTypes(IStructuredSelection selection) {
        HashSet<String> set = new HashSet<String>();
        set.add(TCFDetailPane.ID);
        return set;
    }
}
