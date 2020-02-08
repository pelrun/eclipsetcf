/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.operations;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.url.TcfURLConnection;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.FileState;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.PersistenceManager;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

public class OpDownload extends AbstractOperation {

	private final OutputStream fTarget;
	private final FSTreeNode fSource;
	private final boolean fResetDigest;

	public OpDownload(FSTreeNode srcNode, OutputStream target) {
		fTarget = target;
		fSource = srcNode;
		fResetDigest = target == null;
	}

	@Override
    public IStatus doRun(IProgressMonitor monitor) {
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance(MD_ALG);
		} catch (NoSuchAlgorithmException e) {
		}
		try {
			if (fTarget != null) {
				downloadFile(fSource, fTarget, digest, monitor);
			} else {
				OutputStream out = new BufferedOutputStream(new FileOutputStream(fSource.getCacheFile()));
				try {
					downloadFile(fSource, out, digest, monitor);
				} finally {
					try {
						out.close();
					} catch(IOException e) {
					}
				}
			}
		} catch (Exception e) {
			if (fTarget == null) {
				fSource.getCacheFile().delete();
			}
			return StatusHelper.createStatus("Cannot download " + fSource.getName(), e); //$NON-NLS-1$
		}

		if (!monitor.isCanceled() && digest != null) {
			updateNodeDigest(fSource, digest.digest());
		}
		return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
    }

	private void downloadFile(FSTreeNode source, OutputStream out, MessageDigest digest, IProgressMonitor monitor) throws IOException {
		byte[] data = new byte[DEFAULT_CHUNK_SIZE];
		long size = source.getSize();
		long percentSize = size / 100;
		int percentRead = 0;
		long bytesRead = 0;

		monitor.beginTask(getName(), 100);

		BufferedInputStream input = null;

		TcfURLConnection connection = (TcfURLConnection) source.getLocationURL().openConnection();
		try {
			if (digest != null) {
				input = new BufferedInputStream(new DigestInputStream(connection.getInputStream(), digest));
			} else {
				input = new BufferedInputStream(connection.getInputStream());
			}

			String fileLength = formatSize(size);
			int length;
			while ((length = input.read(data)) >= 0 && !monitor.isCanceled()) {
				out.write(data, 0, length);
				bytesRead += length;
				if (percentSize != 0) {
					int percent = (int) (bytesRead / percentSize);
					if (percent != percentRead) { // Update the progress.
						monitor.worked(percent - percentRead);
						percentRead = percent; // Remember the percentage.
						// Report the progress.
						monitor.subTask(NLS.bind(Messages.OpDownload_Downloading, new Object[]{source.getName(), formatSize(bytesRead), fileLength}));
					}
				}
			}
		} finally {
			out.flush();
			if (input != null) {
				try {
					input.close();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * Update the node's digest using the digest data.
	 *
	 * @param node The node whose digest should updated.
	 * @param digest The digest data.
	 */
	protected void updateNodeDigest(FSTreeNode node, byte[] digest) {
		FileState fileDigest = PersistenceManager.getInstance().getFileDigest(node);
		if (fResetDigest) {
			fileDigest.reset(digest, fSource.getCacheFile().lastModified(), fSource.getModificationTime());
		} else {
			fileDigest.updateTargetDigest(digest, fSource.getModificationTime());
		}
    }

	@Override
    public String getName() {
		return NLS.bind(Messages.OpDownload_DownloadingSingleFile, fSource.getName());
    }
}
