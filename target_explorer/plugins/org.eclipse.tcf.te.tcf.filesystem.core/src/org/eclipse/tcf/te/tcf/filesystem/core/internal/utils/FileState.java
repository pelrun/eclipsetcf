/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.utils;

import java.beans.PropertyChangeEvent;
import java.io.File;

import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.model.CacheState;

/**
 * The state object to describe a file's state.
 */
public class FileState {
	/**
	 * The base digest of the file data.
	 */
	private byte[] base_digest = null;

	/**
	 * The message digest of the file data.
	 */
	private byte[] target_digest = null;

	/**
	 * The message digest of the local cache data
	 */
	private byte[] cache_digest = null;

	/**
	 * The cache file's modification time.
	 */
	private long cache_mtime;

	/**
	 * The cache file's modification time.
	 */
	private long target_mtime;

	/**
	 * If the job that computes the local cache's digest is running.
	 */
	transient boolean cache_digest_running = false;

	/**
	 * If the job that computes the target file's digest is running.
	 */
	transient boolean target_digest_running = false;

	/**
	 * The file system node whose state is described.
	 */
	private transient FSTreeNode node;

	/**
	 * Create a file state using the node.
	 *
	 * @param node The file system node.
	 */
	public FileState(FSTreeNode node) {
		this.node = node;
	}

	/**
	 * Create a file state using the specified state data.
	 *
	 * @param mtime The cache file's modification time.
	 * @param cache_digest The cache file's digest.
	 * @param target_digest The target file's digest.
	 * @param base_digest The baseline digest.
	 */
	public FileState(long mtime, long target_mtime, byte[] cache_digest, byte[] target_digest, byte[]base_digest) {
		this.cache_mtime = mtime;
		this.target_mtime = target_mtime;
		this.cache_digest = cache_digest;
		this.target_digest = target_digest;
		this.base_digest = base_digest;
	}

	/**
	 * Set the file system node.
	 *
	 * @param node The file system node.
	 */
	void setNode(FSTreeNode node) {
		this.node = node;
	}

	/**
	 * Get the node's target file digest.
	 *
	 * @return The target file digest.
	 */
	public byte[] getTargetDigest() {
		return target_digest;
	}

	/**
	 * Get the node's baseline digest.
	 *
	 * @return The baseline digest.
	 */
	public byte[] getBaseDigest() {
		return base_digest;
	}

	/**
	 * Get the node's cache file modification time.
	 *
	 * @return The cache file's modification time.
	 */
	public long getCacheMTime() {
		return cache_mtime;
	}

	public long getTargetMTime() {
		return target_mtime;
	}

	/**
	 * Get the node's cache file digest.
	 *
	 * @return The cache file digest.
	 */
	public byte[] getCacheDigest() {
		return cache_digest;
	}

	/**
	 * Get this node's cache state using the current state data.
	 *
	 * @return The state expressed in a CacheState enum value.
	 */
	public synchronized CacheState getCacheState() {
		File file = CacheManager.getCacheFile(node);
		if (!file.exists())
			return CacheState.consistent;
		if (cache_digest == null || target_digest == null)
			return CacheState.consistent;
		if (isUnchanged(target_digest, cache_digest)) {
			base_digest = target_digest;
			return CacheState.consistent;
		}
		if(isUnchanged(base_digest, cache_digest)){
			return CacheState.outdated;
		}
		if (isUnchanged(target_digest, base_digest)) {
			return CacheState.modified;
		}
		return CacheState.conflict;
	}

	/**
	 * Update the node's target digest and fire an event.
	 *
	 * @param target_digest The new target digest data.
	 */
	public void updateTargetDigest(byte[] target_digest, long mtime) {
//		System.out.println("targt: " + mtime + " " + PersistenceManagerDelegate.digest2string(target_digest));

		this.target_digest = target_digest;
		this.target_mtime = mtime;
		PropertyChangeEvent event = new PropertyChangeEvent(this, "target_digest", null, target_digest); //$NON-NLS-1$
		node.getRuntimeModel().firePropertyChanged(event);
	}

	/**
	 * Compare the two digests to see if they are equal to each other.
	 *
	 * @param digest1 The first digest.
	 * @param digest2 The second digest.
	 * @return true if they are equal.
	 */
	private boolean isUnchanged(byte[] digest1, byte[] digest2){
		if(digest1 != null && digest2 != null && digest1.length == digest2.length) {
			for (int i = 0; i < digest1.length; i++) {
				if(digest1[i] != digest2[i]) return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Update the cache file digest data and fire an event.
	 *
	 * @param cache_digest The new cache file digest data.
	 */
	public void updateCacheDigest(byte[] cache_digest, long mtime) {
//		System.out.println("cache: " + mtime + " " + PersistenceManagerDelegate.digest2string(cache_digest));
		byte[] old_digest = cache_digest;
		this.cache_digest = cache_digest;
		this.cache_mtime = mtime;
		PropertyChangeEvent event = new PropertyChangeEvent(node, "cache_digest", old_digest, cache_digest); //$NON-NLS-1$
		node.getRuntimeModel().firePropertyChanged(event);
    }

	/**
	 * Reset all of the node's digest data to a new digest data.
	 *
	 * @param digest The new digest data.
	 */
	public void reset(byte[] digest, long cache_mtime, long target_mtime) {
//		System.out.println("reset: " + cache_mtime + " " + target_mtime + " " + PersistenceManagerDelegate.digest2string(digest));
		cache_digest = digest;
		target_digest = digest;
		base_digest = digest;
		this.cache_mtime = cache_mtime;
		this.target_mtime = target_mtime;
		PropertyChangeEvent event = new PropertyChangeEvent(node, "reset_digest", null, digest); //$NON-NLS-1$
		node.getRuntimeModel().firePropertyChanged(event);
    }
}
