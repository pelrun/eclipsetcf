/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.operations;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.te.tcf.filesystem.core.activator.CorePlugin;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.CacheManager;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

/**
 * The operation to calculate the message digest of a cache file.
 */
public class OpCacheFileDigest extends AbstractOperation {
	// The digest of which is going to be computed.
	private FSTreeNode node;
	// The computing result
	private byte[] digest;

	public OpCacheFileDigest(FSTreeNode node) {
		this.node = node;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		monitor.beginTask(getName(), 100);
		File file = CacheManager.getCacheFile(node);
		BufferedInputStream input = null;
		try {
			long totalSize = file.length();
			int chunk_size = (int) totalSize / 100;
			int percentRead = 0;
			long bytesRead = 0;
			MessageDigest digest = MessageDigest.getInstance(MD_ALG);
			input = new BufferedInputStream(new DigestInputStream(new FileInputStream(file), digest));
			byte[] data = new byte[DEFAULT_CHUNK_SIZE];
			int length;
			while ((length = input.read(data)) >= 0 && !monitor.isCanceled()){
				bytesRead += length;
				if (chunk_size != 0) {
					int percent = (int) bytesRead / chunk_size;
					if (percent != percentRead) { // Update the progress.
						monitor.worked(percent - percentRead);
						percentRead = percent; // Remember the percentage.
					}
				}
			}
			this.digest = digest.digest();
			return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
		} catch (Exception e) {
			return new Status(IStatus.ERROR, CorePlugin.getUniqueIdentifier(), Messages.OpCacheFileDigest_error_updatingDigest, e);
        } finally {
			if (input != null) {
				try {
					input.close();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * Get the computing result.
	 *
	 * @return The message digest of this cache file.
	 */
	public byte[] getDigest() {
		return digest;
	}

	@Override
	public String getName() {
		return Messages.OpCacheFileDigest_name;
	}
}
