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

import static java.text.MessageFormat.format;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DoneOpen;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.services.IFileSystem.IFileHandle;
import org.eclipse.tcf.te.tcf.core.concurrent.TCFOperationMonitor;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;
import org.eclipse.tcf.util.TCFFileInputStream;

/**
 * The operation that computes the digest of the cache file in the background.
 */
public class OpTargetFileDigest extends AbstractOperation {
	FSTreeNode node;
	byte[] digest;

	public OpTargetFileDigest(FSTreeNode node) {
	    this.node = node;
    }

	@Override
	public IStatus doRun(IProgressMonitor monitor) {
		long totalSize = node.getSize();
		monitor.beginTask(getName(), 100);

		final String path = node.getLocation(true);
		final TCFOperationMonitor<InputStream> result = new TCFOperationMonitor<InputStream>();
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				IFileSystem fs = node.getRuntimeModel().getFileSystem();
				if (fs == null) {
					result.setCancelled();
				} else {
					tcfGetInputStream(fs, path, result);
				}
			}
		});
		IStatus status = result.waitDone(monitor);
		if (!status.isOK())
			return status;

		InputStream in = new BufferedInputStream(result.getValue());
		try {
			int chunk_size = (int) totalSize / 100;
			int percentRead = 0;
			long bytesRead = 0;
			MessageDigest digest = MessageDigest.getInstance(MD_ALG);
			in = new DigestInputStream(in, digest);
			// The buffer used to download the file.
			byte[] data = new byte[DEFAULT_CHUNK_SIZE];
			int length;
			while ((length = in.read(data)) >= 0){
				bytesRead += length;
				if (chunk_size != 0) {
					int percent = (int) bytesRead / chunk_size;
					if (percent != percentRead) { // Update the progress.
						monitor.worked(percent - percentRead);
						percentRead = percent; // Remember the percentage.
					}
				}
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
			}
			this.digest = digest.digest();
			return Status.OK_STATUS;
		} catch (Exception e) {
			return StatusHelper.createStatus(format(Messages.OpTargetFileDigest_error_download, path), e);
        } finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
				}
			}
		}
	}

	protected void tcfGetInputStream(IFileSystem fileSystem, final String path, final TCFOperationMonitor<InputStream> result) {
		int flags = IFileSystem.TCF_O_READ;
		if (!result.checkCancelled()) {
			fileSystem.open(path, flags, null, new DoneOpen() {
				@Override
				public void doneOpen(IToken token, FileSystemException error, IFileHandle handle) {
					if (error != null) {
						result.setError(format(Messages.OpTargetFileDigest_error_openFile, path), error);
					} else {
						result.setDone(new TCFFileInputStream(handle));
					}
				}
			});
		}
	}


	public byte[] getDigest() {
		return digest;
	}

	@Override
	public String getName() {
		return "Update target digest"; //$NON-NLS-1$
	}
}
