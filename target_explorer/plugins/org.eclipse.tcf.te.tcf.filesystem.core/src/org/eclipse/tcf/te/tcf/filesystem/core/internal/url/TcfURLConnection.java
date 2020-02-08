/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * William Chen (Wind River)- [345387]Open the remote files with a proper editor
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.url;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DoneClose;
import org.eclipse.tcf.services.IFileSystem.DoneOpen;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.services.IFileSystem.IFileHandle;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.exceptions.TCFChannelException;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;
import org.eclipse.tcf.te.tcf.filesystem.core.services.Operation;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;

/**
 * The URL connection returned by TCF stream service used to handle "tcf"
 * stream protocol.
 */
public class TcfURLConnection extends URLConnection {
	// Default connecting timeout.
	private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
	// Default file opening timeout.
	private static final int DEFAULT_OPEN_TIMEOUT = 5000;
	// Default file reading timeout.
	private static final int DEFAULT_READ_TIMEOUT = 5000;
	// Default file closing timeout.
	private static final int DEFAULT_CLOSE_TIMEOUT = 5000;
	// The schema name of the stream protocol.
	public static final String PROTOCOL_SCHEMA = "tcf"; //$NON-NLS-1$

	// The input stream of this connection.
	private TcfInputStream inputStream;
	// The output stream of this connection.
	private TcfOutputStream outputStream;

	// The TCF agent peer of the connection.
	private IPeer peer;
	// The path to the resource on the remote file system.
	String path;
	// The timeout for opening a file.
	private int openTimeout;
	// The timeout for closing a file.
	private int closeTimeout;

	// The TCF channel used to open and read the resource.
	IChannel channel;
	// The file's handle
	IFileHandle handle;
	// The file service
	IFileSystem service;

	/**
	 * Create a TCF URL Connection using the specified url. The format of this
	 * URL should be: tcf:/<peerName>/remote/path/to/the/resource... The
	 * stream protocol schema is designed in this way in order to retrieve the
	 * agent peer ID without knowing the structure of a TCF peer id.
	 *
	 * @see TcfURLStreamHandlerService#parseURL(URL, String, int, int)
	 * @param url
	 *            The URL of the resource.
	 */
	public TcfURLConnection(final URL url) throws IOException {
		super(url);
		try {
			URI uri = url.toURI();
			String peerName = uri.getAuthority();
			Assert.isNotNull(peerName);
			peer = findPeer(peerName);
			if (peer == null) {
				throw new IOException(NLS.bind(Messages.TcfURLConnection_NoPeerFound, peerName));
			}
			path = FSTreeNode.stripNoSlashMarker(uri.getPath());
			// Set default timeout.
			setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
			setOpenTimeout(DEFAULT_OPEN_TIMEOUT);
			setReadTimeout(DEFAULT_READ_TIMEOUT);
			setCloseTimeout(DEFAULT_CLOSE_TIMEOUT);
		} catch (URISyntaxException e) {
			throw new IOException(Messages.TcfURLConnection_errorInvalidURL + url.toString(), e);
		}
	}

	/**
	 * Find the TCF peer with the specified ID.
	 *
	 * @param peerId The target peer's ID.
	 * @return The peer with this ID or null if not found.
	 */
    private IPeer findPeer(final String peerName) {
    	Assert.isNotNull(peerName);

    	final AtomicReference<IPeer> peer = new AtomicReference<IPeer>();

    	Runnable runnable = new Runnable() {

			@Override
			public void run() {
				IPeer p = Protocol.getLocator().getPeers().get(peerName);
				if (p == null) {
					IPeerNode[] peerNode = ModelManager.getPeerModel().getService(IPeerModelLookupService.class).lkupPeerModelByName(peerName);
					if (peerNode != null && peerNode.length > 0)
						p = peerNode[0].getPeer();
				}
				peer.set(p);
			}
		};

		if (Protocol.isDispatchThread()) runnable.run();
		else Protocol.invokeAndWait(runnable);

		return peer.get();
	}

	/**
	 * Get the timeout for closing a file.
	 *
	 * @return the timeout in milliseconds.
	 */
	public long getCloseTimeout() {
		return closeTimeout;
	}

	/**
	 * Set the timeout for closing a file.
	 *
	 * @param closeTimeout
	 *            the timeout in milliseconds.
	 */
	public void setCloseTimeout(int closeTimeout) {
		this.closeTimeout = closeTimeout;
	}

	/**
	 * Get the timeout for opening a file.
	 *
	 * @return the timeout in milliseconds.
	 */
	public long getOpenTimeout() {
		return openTimeout;
	}

	/**
	 * Set the timeout for opening a file.
	 *
	 * @param openTimeout
	 *            the timeout in milliseconds.
	 */
	public void setOpenTimeout(int openTimeout) {
		this.openTimeout = openTimeout;
	}

	/**
	 * Open a file on the remote file system for read/write and store the file handle.
	 *
	 * @throws IOException Opening file fails.
	 */
	private void openFile() throws IOException {
		if(peer == null)
			throw new IOException(Messages.TcfURLConnection_NoSuchTcfAgent);
		try {
			// Open the channel
			channel = Operation.openChannel(peer);
		} catch (TCFChannelException e) {
			throw new IOException(e.getMessage());
		}
		if (channel != null) {
			service = Operation.getBlockingFileSystem(channel);
			if (service != null) {
				final FileSystemException[] errors = new FileSystemException[1];
				// Open the file.
				int open_flag = 0;
				if (doInput)
					open_flag |= IFileSystem.TCF_O_READ;
				if (doOutput)
					open_flag |= IFileSystem.TCF_O_WRITE | IFileSystem.TCF_O_CREAT | IFileSystem.TCF_O_TRUNC;
				service.open(path, open_flag, null, new DoneOpen() {
					@Override
					public void doneOpen(IToken token, FileSystemException error, IFileHandle hdl) {
						errors[0] = error;
						handle = hdl;
					}
				});
				if (errors[0] != null) {
					IOException exception = new IOException(errors[0].toString());
					exception.initCause(errors[0]);
					throw exception;
				}
				if (handle == null) {
					throw new IOException(Messages.TcfURLConnection_NoFileHandleReturned);
				}
			} else {
				throw new IOException(Messages.Operation_NoFileSystemError);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.net.URLConnection#connect()
	 */
	@Override
	public void connect() throws IOException {
		if (!connected) {
			openFile();
			if (doInput) {
				inputStream = new TcfInputStream(this);
			}
			if (doOutput) {
				outputStream = new TcfOutputStream(this);
			}
			connected = true;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.net.URLConnection#getInputStream()
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		if (!connected)
			connect();
		return inputStream;
	}

	/*
	 * (non-Javadoc)
	 * @see java.net.URLConnection#getOutputStream()
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		if (!connected)
			connect();
		return outputStream;
	}

	/**
	 * Close the stream, release its file handler and close
	 * the TCF channel used if possible.
	 *
	 * @param stream The stream either the input stream or the output stream.
	 * @throws IOException If closing file handle times out.
	 */
	public synchronized void closeStream(Closeable stream) throws IOException {
		boolean shouldClose = shouldCloseFileHandle(stream);
		if (shouldClose) {
			service.close(handle, new DoneClose() {
				@Override
				public void doneClose(IToken token, FileSystemException error) {
					Tcf.getChannelManager().closeChannel(channel);
				}
			});
		}
	}

	/**
	 * Decide if the file handle and the TCF channel should be closed if
	 * the specified stream is closed. If the stream is the last stream
	 * that depends on the file handle and the TCF channel, then it should
	 * be closed.
	 *
	 * @param stream The stream to be closed.
	 * @return true if the file handle and the TCF channel should be closed.
	 */
	private boolean shouldCloseFileHandle(Closeable stream) {
		boolean shouldClose = false;
		if (stream == inputStream) {
			if (doOutput) {
				if (outputStream.closed) {
					shouldClose = true;
				}
			} else {
				shouldClose = true;
			}
		} else if (stream == outputStream) {
			if (doInput) {
				if (inputStream.closed)
					shouldClose = true;
			} else {
				shouldClose = true;
			}
		}
		return shouldClose;
	}
}
