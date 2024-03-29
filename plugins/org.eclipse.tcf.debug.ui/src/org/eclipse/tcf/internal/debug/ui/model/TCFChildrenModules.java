/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext.MemoryRegion;
import org.eclipse.tcf.util.TCFDataCache;

/**
 * Provides and caches memory regions (modules) for a context.
 */
public class TCFChildrenModules extends TCFChildren {

    public TCFChildrenModules(TCFNode node) {
        super(node, 128);
    }

    void onMemoryMapChanged() {
        for (TCFNode n : getNodes()) ((TCFNodeModule)n).onMemoryMapChanged();
        reset();
    }

    @Override
    protected boolean startDataRetrieval() {
        TCFNodeExecContext exe = (TCFNodeExecContext)node;
        TCFDataCache<MemoryRegion[]> map_cache = exe.getMemoryMap();
        if (!map_cache.validate(this)) return false;
        MemoryRegion[] map = map_cache.getData();
        Map<String,TCFNode> data = new HashMap<String,TCFNode>();
        if (map != null) {
            int cnt = 0;
            for (int index = 0; index < map.length; index++) {
                String id = exe.id + ".Module-" + index;
                TCFNodeModule module = (TCFNodeModule)node.model.getNode(id);
                if (module == null) module = new TCFNodeModule(exe, id, index);
                // Check if the node can be assigned the default sort position
                if (module.getDefaultSortPositionFlag())
                    module.setSortPosition(cnt++);
                data.put(id, module);
            }
        }
        set(null, map_cache.getError(), data);
        return true;
    }
}
