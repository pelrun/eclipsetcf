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

import java.io.File;

import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.CacheManager;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.PersistenceManager;
import org.eclipse.tcf.te.tcf.filesystem.core.model.CacheState;

public class StateManagerTest extends UtilsTestBase {

	public void testCacheStateConsistent() throws Exception {
		cleanUp();
		writeFileContent("hello,world!"); //$NON-NLS-1$
		updateCache(testFile);
		CacheState cacheState = testFile.getCacheState();
		assertEquals(CacheState.consistent, cacheState);
	}

	public void testCacheStateModified() throws Exception {
		cleanUp();
		writeFileContent("hello,world!"); //$NON-NLS-1$
		updateCache(testFile);
	    writeCacheFileContent("hello, test!"); //$NON-NLS-1$
		updateCacheState();
		CacheState cacheState = testFile.getCacheState();
	    assertEquals(CacheState.modified, cacheState);
	}

	private void updateCacheState() {
		assertTrue(testFile.operationRefresh(false).run(null).isOK());
    }

	public void testCacheStateOutdated() throws Exception {
		cleanUp();
		writeFileContent("hello,world!"); //$NON-NLS-1$
		updateCache(testFile);
		writeFileContent("hello,test!"); //$NON-NLS-1$
		updateCacheState();
		CacheState cacheState = testFile.getCacheState();
		assertEquals(CacheState.outdated, cacheState);
	}

	private void cleanUp() {
		File cacheFile = CacheManager.getCacheFile(testFile);
		if(cacheFile.exists()) {
			cacheFile.delete();
		}
		PersistenceManager.getInstance().removeFileDigest(testFile.getLocationURI());
	}

	public void testCacheStateConflict() throws Exception {
		cleanUp();
		writeFileContent("hello,world!"); //$NON-NLS-1$
		updateCache(testFile);
		writeFileContent("hello,test!"); //$NON-NLS-1$
		updateCacheState();
	    writeCacheFileContent("hello, earth!"); //$NON-NLS-1$
		updateCacheState();
		CacheState cacheState = testFile.getCacheState();
		assertEquals(CacheState.conflict, cacheState);
	}
}
