/*******************************************************************************
 * Copyright (c) 2007-2020 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.launch;

import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;
import org.eclipse.tcf.internal.debug.model.TCFLaunch;
import org.eclipse.tcf.services.ILineNumbers;
import org.eclipse.tcf.services.IPathMap;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;

/**
 * TCF source lookup director.
 * For TCF source lookup there is one source lookup participant.
 */
public class TCFSourceLookupDirector extends AbstractSourceLookupDirector {

    private static Set<String> fSupportedContainerTypes;
    static {
        fSupportedContainerTypes = new HashSet<String>();
        // Bug 440538:
        // Only CDT path mapping source containers are currently supported
        fSupportedContainerTypes.add("org.eclipse.cdt.debug.core.containerType.mapping");
    };

    public static IStorage lookupByTargetPathMap(final TCFLaunch launch, final String ctx, final String str) {
        String key = str;
        if (ctx != null) key = ctx + "::" + str;
        Map<String,IStorage> map = launch.getTargetPathMappingCache();
        synchronized (map) {
            if (map.containsKey(str)) return map.get(key);
        }
        return new TCFTask<IStorage>(launch.getChannel()) {
            public void run() {
                TCFDataCache<IPathMap.PathMapRule[]> cache = launch.getTargetPathMap();
                if (cache != null) {
                    if (!cache.validate(this)) return;
                    IPathMap.PathMapRule[] data = cache.getData();
                    if (data != null) {
                        for (IPathMap.PathMapRule r : data) {
                            final String query = r.getContextQuery();
                            if (query != null && query.length() > 0 && !query.equals("*")) {
                                if (ctx == null) continue;
                                TCFDataCache<String[]> q_cache = launch.getContextQuery(query);
                                if (q_cache == null) continue;
                                if (!q_cache.validate(this)) return;
                                String[] q_data = q_cache.getData();
                                if (q_data == null) continue;
                                boolean ok = false;
                                for (String id : q_data) {
                                    if (ctx.equals(id)) ok = true;
                                }
                                if (!ok) continue;
                            }
                            String fnm = TCFSourceLookupParticipant.toFileName(r, str);
                            if (fnm == null) continue;
                            File file = new File(fnm);
                            if (file.isAbsolute() && file.exists() && file.isFile()) {
                                done(new LocalFileStorage(file));
                                return;
                            }
                        }
                    }
                }
                done(null);
            }
        }.getE();
    }

    public static Object lookup(TCFLaunch launch, String ctx, Object element) {
        if (element instanceof ILineNumbers.CodeArea) {
            element = TCFSourceLookupParticipant.toFileName((ILineNumbers.CodeArea)element);
        }
        Object source_element = null;
        ISourceLocator locator = launch.getSourceLocator();
        if (locator instanceof ISourceLookupDirector) {
            source_element = ((ISourceLookupDirector)locator).getSourceElement(element);
        }
        else if (element instanceof IStackFrame) {
            source_element = locator.getSourceElement((IStackFrame)element);
        }
        if (source_element == null && element instanceof String) {
            /* Try to lookup the element using target side path mapping rules */
            IStorage storage = lookupByTargetPathMap(launch, ctx, (String)element);
            if (storage != null) {
                /* Map to workspace resource */
                IPath path = storage.getFullPath();
                if (path != null) {
                    URI uri = URIUtil.toURI(path);
                    IFile[] arr = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(uri);
                    if (arr != null && arr.length > 0) {
                        for (IFile resource : arr) {
                            if (resource.isAccessible()) {
                                storage = resource;
                                break;
                            }
                        }
                    }
                }
            }
            source_element = storage;
        }
        return source_element;
    }

    public void initializeParticipants() {
        addParticipants(new ISourceLookupParticipant[] { new TCFSourceLookupParticipant() });
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.core.sourcelookup.ISourceLookupDirector#supportsSourceContainerType(org.eclipse.debug.core.sourcelookup.ISourceContainerType)
     */
    @Override
    public boolean supportsSourceContainerType(ISourceContainerType type) {
        return fSupportedContainerTypes.contains(type.getId());
    }
}
