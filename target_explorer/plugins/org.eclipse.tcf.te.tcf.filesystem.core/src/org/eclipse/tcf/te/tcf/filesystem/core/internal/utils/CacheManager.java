/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * William Chen (Wind River)- [345387] Open the remote files with a proper editor
 * William Chen (Wind River)- [345552] Edit the remote files with a proper editor
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.utils;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.te.tcf.filesystem.core.activator.CorePlugin;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

/**
 * The local file system cache used to manage the temporary files downloaded from a remote file
 * system.
 */
public class CacheManager {
	public static final char PATH_ESCAPE_CHAR = '$';

	/**
	 * Get the local path of a node's cached file.
	 * <p>
	 * The preferred location is within the plugin's state location, in example
	 * <code>&lt;state location&gt;agent_<hashcode_of_peerId>/remote/path/to/the/file...</code>.
	 * <p>
	 * If the plug-in is loaded in a RCP workspace-less environment, the fall back strategy is to
	 * use the users home directory.
	 *
	 * @param node The file/folder node.
	 * @return The local path of the node's cached file.
	 */
	public static IPath getCachePath(IFSTreeNode node) {
		File location = getCacheRoot();
		String agentId = node.getRuntimeModel().getPeerNode().getPeerId();
		// Use Math.abs to avoid negative hash value.
		String agent = agentId.replace(':', PATH_ESCAPE_CHAR);
		IPath agentDir = new Path(location.getAbsolutePath()).append(agent);
		File agentDirFile = agentDir.toFile();
		mkdirChecked(agentDirFile);
		return appendNodePath(agentDir, node);
	}

	/**
	 * Check and make a directory if it does not exist. Record the failure message if making fails.
	 *
	 * @param file The file to be deleted.
	 */
	static void mkdirChecked(final File dir) {
		if (!dir.exists()) {
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					if (!dir.mkdirs()) {
						throw new Exception(NLS.bind(Messages.CacheManager_MkdirFailed, dir
						                .getAbsolutePath()));
					}
				}

				@Override
				public void handleException(Throwable exception) {
					// Ignore on purpose
				}
			});
		}
	}

	/**
	 * Check if the file exists and set its read-only attribute if it does. Record the failure
	 * message if it fails.
	 *
	 * @param file The file to be set.
	 */
	static void setReadOnlyChecked(final File file) {
		if (file.exists()) {
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					if (!file.setReadOnly()) {
						throw new Exception(NLS.bind(Messages.CacheManager_SetReadOnlyFailed, file
						                .getAbsolutePath()));
					}
				}

				@Override
				public void handleException(Throwable exception) {
					// Ignore on purpose
				}
			});
		}
	}

	/**
	 * Get the local file of the specified node.
	 *
	 * <p>
	 * The preferred location is within the plugin's state location, in example
	 * <code>&lt;state location&gt;agent_<hashcode_of_peerId>/remote/path/to/the/file...</code>.
	 * <p>
	 * If the plug-in is loaded in a RCP workspace-less environment, the fall back strategy is to
	 * use the users home directory.
	 *
	 * @param node The file/folder node.
	 * @return The file object of the node's local cache.
	 */
	public static File getCacheFile(IFSTreeNode node) {
		return getCachePath(node).toFile();
	}

	/**
	 * Get the cache file system's root directory on the local host's file system.
	 *
	 * @return The root folder's location of the cache file system.
	 */
	public static File getCacheRoot() {
		File location;
		try {
			location = CorePlugin.getDefault().getStateLocation().toFile();
		}
		catch (IllegalStateException e) {
			// An RCP workspace-less environment (-data @none)
			location = new File(System.getProperty("user.home"), ".tcf"); //$NON-NLS-1$ //$NON-NLS-2$
			location = new File(location, "fs"); //$NON-NLS-1$
		}

		// Create the location if it not exist
		mkdirChecked(location);
		return location;
	}

	/**
	 * Append the path with the specified node's context path.
	 *
	 * @param path The path to be appended.
	 * @param node The file/folder node.
	 * @return The path to the node.
	 */
	private static IPath appendNodePath(IPath path, IFSTreeNode node) {
		if (!node.isRootDirectory() && node.getParent() != null) {
			path = appendNodePath(path, node.getParent());
			return appendPathSegment(node, path, node.getName());
		}
		String name = node.getName();
		if (node.isWindowsNode()) {
			name = name.replace('\\', '/');
		}
		name = name.replace(':', PATH_ESCAPE_CHAR);
		if (name.endsWith("/")) //$NON-NLS-1$
			name = name.substring(0, name.length()-1);

		return appendPathSegment(node, path, name);
	}

	/**
	 * Append the path with the segment "name". Create a directory if the node is a directory which
	 * does not yet exist.
	 *
	 * @param node The file/folder node.
	 * @param path The path to appended.
	 * @param name The segment's name.
	 * @return The path with the segment "name" appended.
	 */
	private static IPath appendPathSegment(IFSTreeNode node, IPath path, String name) {
		IPath newPath = path.append(name);
		File newFile = newPath.toFile();
		if (node.isDirectory()) {
			mkdirChecked(newFile);
		}
		return newPath;
	}

	public static void clearCache(FSTreeNode source) {
		if (source != null) {
			File cache = getCacheFile(source);
			if (cache.exists())
				deleteFileOrDir(cache);
		}
	}

	private static boolean deleteFileOrDir(File file) {
		File[] children = file.listFiles();
		boolean ok = true;
		if (children != null) {
			for (File child : children) {
				if (!deleteFileOrDir(child)) {
					ok = false;
				}
			}
		}
		return ok && file.delete();
	}
}
