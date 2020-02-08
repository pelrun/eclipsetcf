/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * William Chen (Wind River) - [345384] Provide property pages for remote file system nodes
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.nls;

import java.lang.reflect.Field;

import org.eclipse.osgi.util.NLS;

/**
 * File System plug-in externalized strings management.
 */
public class Messages extends NLS {

	// The plug-in resource bundle name
	private static final String BUNDLE_NAME = "org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages"; //$NON-NLS-1$

	/**
	 * Static constructor.
	 */
	static {
		// Load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	/**
	 * Returns if or if not this NLS manager contains a constant for
	 * the given externalized strings key.
	 *
	 * @param key The externalized strings key or <code>null</code>.
	 * @return <code>True</code> if a constant for the given key exists, <code>false</code> otherwise.
	 */
	public static boolean hasString(String key) {
		if (key != null) {
			try {
				Field field = Messages.class.getDeclaredField(key);
				return field != null;
			} catch (NoSuchFieldException e) { /* ignored on purpose */ }
		}

		return false;
	}

	/**
	 * Returns the corresponding string for the given externalized strings
	 * key or <code>null</code> if the key does not exist.
	 *
	 * @param key The externalized strings key or <code>null</code>.
	 * @return The corresponding string or <code>null</code>.
	 */
	public static String getString(String key) {
		if (key != null) {
			try {
				Field field = Messages.class.getDeclaredField(key);
				return (String)field.get(null);
			} catch (Exception e) { /* ignored on purpose */ }
		}

		return null;
	}

	// **** Declare externalized string id's down here *****

	public static String FSTreeNodeContentProvider_rootNodeLabel;
	public static String FSTreeNode_TypeFile;
	public static String FSTreeNode_TypeFileFolder;
	public static String FSTreeNode_TypeLocalDisk;
	public static String FSTreeNode_TypeSystemFile;
	public static String FSTreeNode_TypeUnknownFile;
	public static String FSTreeNodeWorkingCopy_commitOperation_name;

	public static String Operation_NotResponding;
	public static String Operation_OpeningChannelFailureMessage;
	public static String Operation_NoFileSystemError;
	public static String Operation_CannotOpenDir;
	public static String Operation_CopyNOfFile;
	public static String Operation_CopyOfFile;
	public static String Operation_CannotCreateDirectory;
	public static String Operation_TimeoutOpeningChannel;

	public static String OpCacheFileDigest_error_updatingDigest;
	public static String OpCacheFileDigest_name;
	public static String OpCommitAttr_error_cannotSetAttributes;
	public static String OpCommitAttr_name;
	public static String OpCopy_Copying;
	public static String OpCopy_CannotCopyFile;
	public static String OpCopy_CopyingFile;
	public static String OpCopy_DownloadingFile;
	public static String OpCopy_error_noDirectory;
	public static String OpCopy_error_noFile;

	public static String OpCreate_error_existingFile;
	public static String OpCreate_TaskName;
	public static String OpCreateFile_error_create;
	public static String OpCreateFolder_error_createFolder;

	public static String TcfURLConnection_errorInvalidURL;
	public static String TcfURLConnection_NoFileHandleReturned;
	public static String TcfURLConnection_NoPeerFound;
	public static String TcfURLConnection_NoSuchTcfAgent;
	public static String TcfURLConnection_relativePath;

	public static String OpDelete_Deleting;
	public static String OpDelete_error_delete;
	public static String OpDelete_error_readDir;
	public static String OpDelete_RemovingFileFolder;

	public static String OpDownload_Downloading;
	public static String OpDownload_DownloadingSingleFile;

	public static String OpMove_Moving;
	public static String OpMove_CannotMove;
	public static String OpMove_MovingFile;

	public static String OpParsePath_name;

	public static String OpRefresh_errorGetRoots;
	public static String OpRefresh_errorOpenDir;
	public static String OpRefresh_errorReadAttributes;
	public static String OpRefresh_name;

	public static String OpRename_TitleRename;
	public static String OpRestoreFavorites_name;

	public static String CacheManager_SetReadOnlyFailed;
	public static String OpStreamOp_Bytes;
	public static String OpStreamOp_KBs;
	public static String OpStreamOp_MBs;
	public static String OpTargetFileDigest_error_download;
	public static String OpTargetFileDigest_error_openFile;

	public static String OpUpload_error_openFile;
	public static String OpUpload_error_upload;
	public static String OpUpload_UploadingProgress;
	public static String OpUpload_UploadNFiles;
	public static String OpUpload_UploadSingleFile;

	public static String TcfInputStream_NoDataAvailable;
	public static String TcfInputStream_StreamClosed;
	public static String TcfOutputStream_StreamClosed;
	public static String TcfURLStreamHandlerService_ErrorURLFormat;
	public static String TcfURLStreamHandlerService_IllegalCharacter;
	public static String TcfURLStreamHandlerService_OnlyDiskPartError;

	public static String CacheManager_MkdirFailed;

	public static String FileTransferService_error_mkdirFailed;
	public static String BlockingFileSystemProxy_TimeoutOpeningFile;
	public static String BlockingFileSystemProxy_TimeoutClosingFile;
	public static String BlockingFileSystemProxy_TimeoutReadingFile;
	public static String BlockingFileSystemProxy_TimeoutWritingFile;
	public static String BlockingFileSystemProxy_TimeoutStat;
	public static String BlockingFileSystemProxy_TimeoutLstat;
	public static String BlockingFileSystemProxy_TimeoutFstat;
	public static String BlockingFileSystemProxy_TimeoutSetStat;
	public static String BlockingFileSystemProxy_TimeoutFSetStat;
	public static String BlockingFileSystemProxy_TimeoutOpeningDir;
	public static String BlockingFileSystemProxy_TimeoutReadingDir;
	public static String BlockingFileSystemProxy_TimeoutMakingDir;
	public static String BlockingFileSystemProxy_TimeoutRemovingDir;
	public static String BlockingFileSystemProxy_TimeoutListingRoots;
	public static String BlockingFileSystemProxy_TimeoutRemovingFile;
	public static String BlockingFileSystemProxy_TimeoutGettingRealPath;
	public static String BlockingFileSystemProxy_TimeoutRenamingFile;
	public static String BlockingFileSystemProxy_TimeoutReadingLink;
	public static String BlockingFileSystemProxy_TimeoutSymLink;
	public static String BlockingFileSystemProxy_TimeoutCopying;
	public static String BlockingFileSystemProxy_TimeoutGettingUser;
	public static String ModelManager_errorNoUserAccount;
	public static String ModelManager_errorOpenChannel;

	public static String FileSystem_ErrorMessage_Errno_Base;
	public static String FileSystem_ErrorMessage_Errno_65563;
	public static String FileSystem_ErrorMessage_Errno_65565;

}
