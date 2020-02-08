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

import static java.text.MessageFormat.format;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DoneOpen;
import org.eclipse.tcf.services.IFileSystem.DoneStat;
import org.eclipse.tcf.services.IFileSystem.FileAttrs;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.services.IFileSystem.IFileHandle;
import org.eclipse.tcf.te.tcf.core.concurrent.TCFOperationMonitor;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IConfirmCallback;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.FileState;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.PersistenceManager;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;
import org.eclipse.tcf.util.TCFFileOutputStream;

/**
 * Upload multiple files from local system to a remote system.
 */
public class OpUpload extends AbstractOperation {
	private static class WorkItem {
		final File fSource;
		final FSTreeNode fDestination;
		final boolean fDropToDestination;
		WorkItem(File source, FSTreeNode destination, boolean isDrop) {
			fSource = source;
			fDestination = destination;
			fDropToDestination = isDrop;
		}
	}

	IConfirmCallback fConfirmCallback;

	LinkedList<WorkItem> fWork = new LinkedList<WorkItem>();
	private long fStartTime;

	public OpUpload(IConfirmCallback confirm) {
		fConfirmCallback = confirm;
	}

	public void addUpload(File source, FSTreeNode destinationFile) {
		fWork.add(new WorkItem(source, destinationFile, false));
	}

	public void addDrop(File source, FSTreeNode destiniationFolder) {
		fWork.add(new WorkItem(source, destiniationFolder, true));
	}

	@Override
	public IStatus doRun(IProgressMonitor monitor) {
		fStartTime = System.currentTimeMillis();
		monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
		while (!fWork.isEmpty()) {
			IStatus s = runWorkItem(fWork.remove(), monitor);
			if (!s.isOK())
				return s;
		}
		return Status.OK_STATUS;
	}

	protected IStatus runWorkItem(final WorkItem item, IProgressMonitor monitor) {
		final String path;
		final FSTreeNode destination = item.fDestination;
		FSTreeNode existing;
		final String name;
		final File source = item.fSource;
		if (item.fDropToDestination) {
			IStatus status = refresh(destination, fStartTime, monitor);
			if (!status.isOK())
				return status;

			name = item.fSource.getName();
			existing = destination.findChild(name);

			if (source.isDirectory()) {
				if (existing != null) {
					if (!existing.isDirectory()) {
						return StatusHelper.createStatus(format(Messages.OpCopy_error_noDirectory, existing.getLocation()), null);
					}
					int replace = confirmCallback(existing, fConfirmCallback);
					if (replace == IConfirmCallback.NO) {
						return Status.OK_STATUS;
					}
					if (replace != IConfirmCallback.YES) {
						return Status.CANCEL_STATUS;
					}
				} else {
					status = destination.operationNewFolder(name).run(new SubProgressMonitor(monitor, 0));
					if (!status.isOK())
						return status;
					existing = destination.findChild(name);
				}

				for (File child : source.listFiles()) {
					fWork.addFirst(new WorkItem(child, existing, true));
				}
				return Status.OK_STATUS;
			} else if (source.isFile()) {
				if (existing != null) {
					if (!existing.isFile()) {
						return StatusHelper.createStatus(format(Messages.OpCopy_error_noFile, existing.getLocation()), null);
					}
					int replace = confirmCallback(existing, fConfirmCallback);
					if (replace == IConfirmCallback.NO) {
						return Status.OK_STATUS;
					}
					if (replace != IConfirmCallback.YES) {
						return Status.CANCEL_STATUS;
					}
				}
				path = getPath(destination, name);
			} else {
				return Status.OK_STATUS;
			}
		} else {
			name = destination.getName();
			existing = destination;
			path = destination.getLocation(true);
		}

		final TCFOperationMonitor<OutputStream> result = new TCFOperationMonitor<OutputStream>();
		monitor.subTask(NLS.bind(Messages.OpUpload_UploadSingleFile, item.fSource));
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				IFileSystem fs = destination.getRuntimeModel().getFileSystem();
				if (fs == null) {
					result.setCancelled();
				} else {
					tcfGetOutputStream(fs, path, result);
				}
			}
		});
		IStatus status = result.waitDone(monitor);
		if (!status.isOK())
			return status;

		OutputStream out = new BufferedOutputStream(result.getValue());
		try {
			IStatus s = uploadFile(item.fSource, existing, out, new SubProgressMonitor(monitor, 0));
			if (!s.isOK())
				return s;
		} finally {
			try {
				out.close();
			} catch (IOException e) {
			}
		}

		return updateNode(path, name, destination, existing, monitor);
	}

	private IStatus updateNode(final String path, final String name,
			final FSTreeNode destination, final FSTreeNode existing, IProgressMonitor monitor) {
		final TCFOperationMonitor<?> r2 = new TCFOperationMonitor<Object>();
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				IFileSystem fs = destination.getRuntimeModel().getFileSystem();
				if (fs == null) {
					r2.setCancelled();
				} else if (!r2.checkCancelled()) {
					fs.stat(path, new DoneStat() {
						@Override
						public void doneStat(IToken token, FileSystemException error, FileAttrs attrs) {
							if (error != null) {
								r2.setError(format(Messages.OpUpload_error_upload, name), error);
							} else if (!r2.checkCancelled()) {
								if (existing != null) {
									existing.setAttributes(attrs, true);
								} else {
									destination.addNode(new FSTreeNode(destination, name, false, attrs), true);
								}
								r2.setDone(null);
							}
						}
					});
				}
			}
		});
		return r2.waitDone(monitor);
	}

	protected void tcfGetOutputStream(IFileSystem fileSystem, final String path, final TCFOperationMonitor<OutputStream> result) {
		int flags = IFileSystem.TCF_O_WRITE | IFileSystem.TCF_O_CREAT | IFileSystem.TCF_O_TRUNC;
		if (!result.checkCancelled()) {
			fileSystem.open(path, flags, null, new DoneOpen() {
				@Override
				public void doneOpen(IToken token, FileSystemException error, IFileHandle handle) {
					if (error != null) {
						result.setError(StatusHelper.createStatus(format(Messages.OpUpload_error_openFile, path), error));
					} else {
						result.setDone(new TCFFileOutputStream(handle));
					}
				}
			});
		}
	}

	private IStatus uploadFile(File source, FSTreeNode existing, OutputStream output, IProgressMonitor monitor) {
		byte[] data = new byte[DEFAULT_CHUNK_SIZE];
		// Calculate the total size.
		long totalSize = source.length();
		// Calculate the chunk size of one percent.
		int chunk_size = (int) totalSize / 100;
		// The current reading percentage.
		int percentRead = 0;
		// The current length of read bytes.
		long bytesRead = 0;
		MessageDigest digest = null;
		InputStream input = null;
		try {
			input = new BufferedInputStream(new FileInputStream(source));
			if (existing != null) {
				try {
					digest = MessageDigest.getInstance(MD_ALG);
					input = new DigestInputStream(input, digest);
				} catch (NoSuchAlgorithmException e) {
				}
			}

			// Total size displayed on the progress dialog.
			String fileLength = formatSize(totalSize);
			int length;
			while ((length = input.read(data)) >= 0) {
				output.write(data, 0, length);
				bytesRead += length;
				if (chunk_size != 0) {
					int percent = (int) bytesRead / chunk_size;
					if (percent != percentRead) { // Update the progress.
						monitor.worked(percent - percentRead);
						percentRead = percent; // Remember the percentage.
						// Report the progress.
						if (fWork.size() == 0)
							monitor.subTask(NLS.bind(Messages.OpUpload_UploadingProgress, new Object[]{source.getName(), formatSize(bytesRead), fileLength}));
					}
				}
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
			}

			if (digest != null && existing != null) {
				statFile(existing, monitor);
				FileState filedigest = PersistenceManager.getInstance().getFileDigest(existing);
				filedigest.reset(digest.digest(), existing.getCacheFile().lastModified(), existing.getModificationTime());
			}
			return Status.OK_STATUS;
		} catch (IOException e) {
			return StatusHelper.createStatus(format(Messages.OpUpload_error_upload, source), e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (Exception e) {
				}
			}
		}
	}

	private void statFile(final FSTreeNode node, IProgressMonitor monitor) {
		final TCFOperationMonitor<?> result = new TCFOperationMonitor<Object>();
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				tcfStat(node, result);
			}
		});
		result.waitDone(monitor);
	}

	protected void tcfStat(final FSTreeNode node, final TCFOperationMonitor<?> result) {
		if (!result.checkCancelled()) {
			final IFileSystem fs = node.getRuntimeModel().getFileSystem();
			if (fs == null) {
				result.setCancelled();
				return;
			}

			fs.stat(node.getLocation(true), new DoneStat() {
				@Override
				public void doneStat(IToken token, FileSystemException error, FileAttrs attrs) {
					if (error != null) {
						handleFSError(node, Messages.OpRefresh_errorReadAttributes, error, result);
					} else {
						node.setAttributes(attrs, false);
						result.setDone(null);
					}
				}
			});
		}
	}

	@Override
    public String getName() {
		String message;
		if(fWork.size()==1)
			message = NLS.bind(Messages.OpUpload_UploadSingleFile, fWork.element().fSource);
		else
			message = NLS.bind(Messages.OpUpload_UploadNFiles, Long.valueOf(fWork.size()));
		return message;
    }
}
