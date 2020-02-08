/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * William Chen (Wind River) - [345384] Provide property pages for remote file system nodes
 * William Chen (Wind River) - [352302]Opening a file in an editor depending on
 *                             the client's permissions.
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal;

import static java.util.Collections.singletonList;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.FileAttrs;
import org.eclipse.tcf.te.core.interfaces.IFilterable;
import org.eclipse.tcf.te.core.interfaces.IPropertyChangeProvider;
import org.eclipse.tcf.te.core.interfaces.IViewerInput;
import org.eclipse.tcf.te.tcf.filesystem.core.activator.CorePlugin;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IConfirmCallback;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IResultOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNodeWorkingCopy;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IUserAccount;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpCopy;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpCopyLocal;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpCreateFile;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpCreateFolder;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpDelete;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpDownload;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpMove;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpRefresh;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpRename;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpUpload;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.testers.TargetPropertyTester;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.url.TcfURLConnection;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.url.TcfURLStreamHandlerService;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.CacheManager;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.ContentTypeHelper;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.FileState;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.PersistenceManager;
import org.eclipse.tcf.te.tcf.filesystem.core.model.CacheState;
import org.eclipse.tcf.te.tcf.filesystem.core.model.RuntimeModel;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Representation of a file system tree node.
 * <p>
 * <b>Note:</b> Node construction and child list access is limited to the TCF
 * event dispatch thread.
 */
@SuppressWarnings("deprecation")
public final class FSTreeNode extends FSTreeNodeBase implements IFilterable, org.eclipse.tcf.te.tcf.filesystem.core.model.FSTreeNode {
	private static final QualifiedName EDITOR_KEY = new QualifiedName("org.eclipse.ui.internal.registry.ResourceEditorRegistry", "EditorProperty");//$NON-NLS-2$//$NON-NLS-1$
	static final String KEY_WIN32_ATTRS = "Win32Attrs"; //$NON-NLS-1$
	private static final Comparator<FSTreeNode> CMP_WIN = new Comparator<FSTreeNode>() {
		@Override
		public int compare(FSTreeNode o1, FSTreeNode o2) {
			return o1.getName().compareToIgnoreCase(o2.getName());
		}
	};
	private static final Comparator<FSTreeNode> CMP_UNIX = new Comparator<FSTreeNode>() {
		@Override
		public int compare(FSTreeNode o1, FSTreeNode o2) {
			return o1.getName().compareTo(o2.getName());
		}
	};

	public static final String NOSLASH_MARKER = "/./"; //$NON-NLS-1$

	public static String addNoSlashMarker(String path) {
		if (path.length() > 0 && path.charAt(0) != '/')
			return NOSLASH_MARKER + path;
		return path;
	}

	public static String stripNoSlashMarker(String path) {
		if (path.startsWith(NOSLASH_MARKER))
			return path.substring(NOSLASH_MARKER.length());
		return path;
	}



	private FSTreeNode fParent;
	private String fName;

    private Type fType;
    private IFileSystem.FileAttrs fAttributes;

	private FSTreeNode[] fChildren = null;

    private final RuntimeModel fRuntimeModel;
	private final boolean fWindowsNode;
	private long fRefreshTime;


	public FSTreeNode(RuntimeModel runtimeModel, String name) {
		fRuntimeModel = runtimeModel;
		fParent = null;
		fName = name;
	    fAttributes = null;
	    fType = Type.FILE_SYSTEM;
	    fWindowsNode = isWindowsNode(getPeerNode());
	    Assert.isTrue(Protocol.isDispatchThread());
	}

	private boolean isWindowsNode(IPeerNode peerNode) {
        String osname = TargetPropertyTester.getOSName(peerNode);
        if (osname != null){
            return osname.startsWith("Windows"); //$NON-NLS-1$
        }
        return false;
	}

	public FSTreeNode(FSTreeNode parent, String name, boolean isRootDir, IFileSystem.FileAttrs attribs) {
		fRuntimeModel = parent.getRuntimeModel();
		fWindowsNode = parent.isWindowsNode() || (isRootDir && name.endsWith("\\")); //$NON-NLS-1$
		fParent = parent;
		fName = name;
	    fAttributes = attribs;
	    if (isRootDir) {
	    	fType = Type.ROOT;
	    } else {
	    	fType = Type.DIRECTORY_OR_FILE;
	    }
	    Assert.isTrue(Protocol.isDispatchThread());
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": name=" + fName; //$NON-NLS-1$
	}

    @Override
    public Object getAdapter(Class adapter) {
		if (IViewerInput.class.equals(adapter)) {
			return getPeerNode().getAdapter(adapter);
		}
		if (IPropertyChangeProvider.class.equals(adapter)) {
			return getPeerNode().getAdapter(adapter);
		}
	    return super.getAdapter(adapter);
    }

	@Override
	public RuntimeModel getRuntimeModel() {
		return fRuntimeModel;
	}

	@Override
	public IPeerNode getPeerNode() {
		return fRuntimeModel.getPeerNode();
	}

	@Override
	public UserAccount getUserAccount() {
		return fRuntimeModel.getUserAccount();
	}

    @Override
	public Type getType() {
		return fType;
	}

    @Override
	public boolean isWindowsNode() {
    	return fWindowsNode;
    }

    @Override
	public boolean isFile() {
    	return fAttributes != null && fAttributes.isFile();
    }

    @Override
	public boolean isDirectory() {
    	switch(fType) {
    	case FILE_SYSTEM:
    		return false;
    	case ROOT:
    		return true;
    	case DIRECTORY_OR_FILE:
    		return fAttributes == null || fAttributes.isDirectory();
    	}
    	return false;
    }

    @Override
    public boolean isRootDirectory() {
    	return fType == Type.ROOT;
    }

    public FileAttrs getAttributes() {
		return fAttributes;
	}

	@Override
	protected int getWin32Attrs() {
    	final FileAttrs attribs = fAttributes;
    	if (attribs != null	&& attribs.attributes != null) {
    		Object val = attribs.attributes.get(KEY_WIN32_ATTRS);
    		if (val instanceof Integer) {
    			return ((Integer) val).intValue();
    		}
    	}
        return 0;
    }

    @Override
	protected int getPermissions() {
    	final FileAttrs attribs = fAttributes;
    	if (attribs != null) {
    		return attribs.permissions;
    	}
        return 0;
    }

    @Override
	public String getLocation() {
        return getLocation(false);
    }

    public String getLocation(boolean forceSlashes) {
    	return getLocation(isWindowsNode() && !forceSlashes ? '\\' : '/');
    }

    private String getLocation(char separator) {
		String name = getName();
    	if (fType == Type.ROOT) {
    		if (isWindowsNode() && name.charAt(name.length()-1) != separator) {
    			return name.substring(0, name.length()-1) + separator;
    		}
            return name;
    	}
    	if (fParent == null)
    		return name;


    	String pLoc = fParent.getLocation(separator);
    	if (pLoc.length() == 0)
    		return name;

    	char lastChar = pLoc.charAt(pLoc.length()-1);
    	if (lastChar != separator)
    		return pLoc + separator + name;

    	return pLoc + name;
    }

    /**
     * Get the URL of the file or folder. The URL's format is created in the
     * following way: tcf:/<peerName>/remote/path/to/the/resource... See
     * {@link TcfURLConnection#TcfURLConnection(URL)}
     *
     * @see TcfURLStreamHandlerService#parseURL(URL, String, int, int)
     * @see #getLocationURI()
     * @return The URL of the file/folder.
     */
    @Override
	public URL getLocationURL() {
    	try {
    		URI uri = getLocationURI();
			return uri == null ? null : uri.toURL();
        } catch (MalformedURLException e) {
        	CorePlugin.logError("Cannot create tcf url", e); //$NON-NLS-1$
        }
		return null;
    }



    /**
     * Get the URI of the file or folder. The URI's format is created in the
     * following way: tcf:/<peerName>/remote/path/to/the/resource...
     *
     * @return The URI of the file/folder.
     */
	@Override
	public URI getLocationURI() {
        try {
            String name = getPeerNode().getName();
            String path = getLocation('/');
            return new URI(TcfURLConnection.PROTOCOL_SCHEMA, name, addNoSlashMarker(path), null, null);
        } catch (URISyntaxException e) {
        	CorePlugin.logError("Cannot create tcf uri", e); //$NON-NLS-1$
            return null;
        }
    }

    /**
     * Get the type label of the file for displaying purpose.
     *
     * @return The type label text.
     */
    @Override
	public String getFileTypeLabel() {
    	switch (fType) {
    	case FILE_SYSTEM:
    		return Messages.FSTreeNodeContentProvider_rootNodeLabel;
    	case ROOT:
            return Messages.FSTreeNode_TypeLocalDisk;
    	case DIRECTORY_OR_FILE:
    		break;
    	}

    	if (isDirectory())
    		return Messages.FSTreeNode_TypeFileFolder;

    	if (isSystemFile()) {
    		return Messages.FSTreeNode_TypeSystemFile;
        }
        IContentType contentType = Platform.getContentTypeManager().findContentTypeFor(getName());
        if (contentType != null) {
            return contentType.getName();
        }
        int lastDot = getName().lastIndexOf("."); //$NON-NLS-1$
        if (lastDot == -1) {
            return Messages.FSTreeNode_TypeUnknownFile;
        }
        return getName().substring(lastDot + 1).toUpperCase() + " " + Messages.FSTreeNode_TypeFile; //$NON-NLS-1$
    }


    /**
     * Get the local file's state of the specified tree node. The local file must exist
     * before calling this method to get its state.
     *
     * @return The tree node's latest cache state.
     */
    @Override
	public CacheState getCacheState() {
        File file = CacheManager.getCacheFile(this);
        if (!file.exists()) {
            return CacheState.consistent;
        }
        FileState digest = PersistenceManager.getInstance().getFileDigest(this);
        return digest.getCacheState();
    }

    @Override
	public FSTreeNode getParent() {
        return fParent;
    }

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public IFSTreeNodeWorkingCopy createWorkingCopy() {
		return new FSTreeNodeWorkingCopy(this);
	}

	@Override
	public boolean isFileSystem() {
		return fType == Type.FILE_SYSTEM;
	}

	@Override
	public long getAccessTime() {
		if (fAttributes != null)
			return fAttributes.atime;
		return 0;
	}

	@Override
	public long getModificationTime() {
		if (fAttributes != null)
			return fAttributes.mtime;
		return 0;
	}

	@Override
	public long getSize() {
		if (fAttributes != null)
			return fAttributes.size;
		return 0;
	}

	@Override
	public int getUID() {
		if (fAttributes != null)
			return fAttributes.uid;
		return 0;
	}

	@Override
	public int getGID() {
		if (fAttributes != null)
			return fAttributes.gid;
		return 0;
	}

	@Override
	public boolean isAncestorOf(IFSTreeNode node) {
		while (node != null) {
			if ((node = node.getParent()) == this)
				return true;
		}
		return false;
	}

	@Override
	public File getCacheFile() {
		return CacheManager.getCacheFile(this);
	}

	@Override
	public String getPreferredEditorID() {
		return PersistenceManager.getInstance().getPersistentProperties(this).get(EDITOR_KEY);
	}

	@Override
	public void setPreferredEditorID(String editorID) {
		PersistenceManager.getInstance().getPersistentProperties(this).put(EDITOR_KEY, editorID);
	}

	@Override
	public IContentType getContentType() {
		return ContentTypeHelper.getContentType(this);
	}

	@Override
	public boolean isBinaryFile() {
		return ContentTypeHelper.isBinaryFile(this);
	}

	@Override
	public FSTreeNode[] getChildren() {
		return fChildren;
	}

	@Override
	public IOperation operationRefresh(boolean recursive) {
		return new OpRefresh(this, recursive);
	}


	@Override
	public IOperation operationRename(String newName) {
		return new OpRename(this, newName);
	}

	@Override
	public IOperation operationUploadContent(File srcFile) {
		if (srcFile == null)
			srcFile = getCacheFile();

		OpUpload upload =  new OpUpload(null);
		upload.addUpload(srcFile, this);
		return upload;
	}

	@Override
	public IOperation operationDelete(IConfirmCallback readonlyCallback) {
		return new OpDelete(singletonList(this), readonlyCallback);
	}

	@Override
	public IOperation operationDownload(OutputStream output) {
		return new OpDownload(this, output);
	}

	@Override
	public IOperation operationDownload(File destinationFolder, IConfirmCallback confirmCallback) {
		return new OpCopyLocal(singletonList(this), destinationFolder, confirmCallback);
	}

	@Override
	public IOperation operationDropFiles(List<String> files, IConfirmCallback confirmCallback) {
		OpUpload upload =  new OpUpload(confirmCallback);
		for (String file : files) {
			upload.addDrop(new File(file), this);
		}
		return upload;
	}

	@Override
	public IOperation operationDropMove(List<IFSTreeNode> nodes, IConfirmCallback confirmCallback) {
		return new OpMove(nodes, this, confirmCallback);
	}

	@Override
	public IOperation operationDropCopy(List<IFSTreeNode> nodes, boolean cpPerm, boolean cpOwn,
			IConfirmCallback moveCopyCallback) {
		return new OpCopy(nodes, this, cpPerm, cpOwn, moveCopyCallback);
	}

	@Override
	public IResultOperation<IFSTreeNode> operationNewFile(String name) {
		return new OpCreateFile(this, name);
	}

	@Override
	public IResultOperation<IFSTreeNode> operationNewFolder(String name) {
		return new OpCreateFolder(this, name);
	}

	public void changeParent(FSTreeNode newParent) {
		fParent = newParent;
	}

	public void changeName(String newName) {
		fName = newName;
	}

	@Override
	public FSTreeNode findChild(String name) {
		return binarySearch(fChildren, name);
	}

	public void addNode(FSTreeNode newNode, boolean notify) {
		final FSTreeNode[] children = fChildren;
		if (children == null) {
			setChildren(new FSTreeNode[] {newNode}, notify);
		} else {
			int ip = Arrays.binarySearch(children, newNode, getComparator());
			if (ip >= 0) {
				children[ip] = newNode;
			} else {
				ip = -ip - 1;
				FSTreeNode[] newChildren = new FSTreeNode[children.length+1];
				System.arraycopy(children, 0, newChildren, 0, ip);
				newChildren[ip] = newNode;
				System.arraycopy(children, ip, newChildren, ip+1, children.length-ip);
				setChildren(newChildren, notify);
			}
		}
	}

	public void removeNode(FSTreeNode node, boolean notify) {
		final FSTreeNode[] children = fChildren;
		if (children == null)
			return;

		int ip = Arrays.binarySearch(children, node, getComparator());
		if (ip < 0 || children[ip] != node)
			return;

		FSTreeNode[] newChildren = new FSTreeNode[children.length-1];
		System.arraycopy(children, 0, newChildren, 0, ip);
		System.arraycopy(children, ip+1, newChildren, ip, children.length-ip-1);
		setChildren(newChildren, notify);
	}

	public void setContent(FSTreeNode[] children, boolean notify) {
		setContent(children, notify, true);
	}

	public void setContent(FSTreeNode[] children, boolean notify, boolean updateRefreshTime) {
		final Comparator<FSTreeNode> comparator = getComparator();
		Arrays.sort(children, comparator);
		if (fChildren != null) {
			int j = 0;
			for (int i=0; i<children.length; i++) {
				FSTreeNode node = children[i];
				for (; j<fChildren.length; j++) {
					FSTreeNode old = fChildren[j];
					int cmp = comparator.compare(old, node);
					if (cmp == 0) {
						old.setAttributes(node.fAttributes, false);
						children[i] = old;
						j++;
						break;
					} else if (cmp > 0) {
						break;
					}
				}
			}
		}
		if (updateRefreshTime) fRefreshTime = System.currentTimeMillis();
		setChildren(children, notify);
	}

	private Comparator<FSTreeNode> getComparator() {
		return isWindowsNode() ? CMP_WIN : CMP_UNIX;
	}

	private void setChildren(FSTreeNode[] children, boolean notify) {
		Assert.isTrue(Protocol.isDispatchThread());
	    FSTreeNode[] oldChildren = fChildren;
	    fChildren = children;
	    if (notify) {
	    	notifyChange("children", oldChildren, children); //$NON-NLS-1$
	    }
	}

	public void setAttributes(FileAttrs attrs, boolean notify) {
	    FileAttrs oldAttrs = fAttributes;
	    fAttributes = attrs;
	    if (attrs != null && attrs.isFile())
	    	fRefreshTime = System.currentTimeMillis();
	    if (notify) {
	    	notifyChange("attributes", oldAttrs, attrs); //$NON-NLS-1$
	    }
	}

	public void notifyChange() {
		notifyChange("children", null, null); //$NON-NLS-1$
	}

	private void notifyChange(String prop, Object oldValue, Object newValue) {
        fRuntimeModel.firePropertyChanged(new PropertyChangeEvent(this, prop, oldValue, newValue));
	}

	@Override
	public long getLastRefresh() {
		return fRefreshTime;
	}

	private FSTreeNode binarySearch(final FSTreeNode[] children, String name) {
		if (children == null)
			return null;

		boolean caseSensitive = !isWindowsNode();
		int low = 0;
		int high = children.length - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			FSTreeNode midVal = children[mid];
			int cmp = caseSensitive ? midVal.getName().compareTo(name) : midVal.getName().compareToIgnoreCase(name);
			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return midVal;
		}
		return null;
	}

	@Override
	public void setRevealOnConnect(boolean value) {
		if (value) {
			if (CorePlugin.getDefault().addToRevealOnConnect(getLocation(true))) {
				notifyChange("favorites", Boolean.FALSE, Boolean.TRUE); //$NON-NLS-1$
			}
		} else {
			if (CorePlugin.getDefault().removeFromRevealOnConnect(getLocation(true))) {
				notifyChange("favorites", Boolean.TRUE, Boolean.FALSE); //$NON-NLS-1$
			}
		}
	}

	@Override
	public boolean isRevealOnConnect() {
		return CorePlugin.getDefault().isRevealOnConnect(getLocation(true));
	}

	@Override
	protected boolean checkPermission(int user, int group, int other) {
		int permissionsMode = fRuntimeModel.getDelegate().getCheckPermissionsMode();
		if (IRuntimeModel.PERMISSIONS_MODE_ALWAYS_WRITABLE == permissionsMode) {
			return true;
		} else if (IRuntimeModel.PERMISSIONS_MODE_USE_ALWAYS_OWNER == permissionsMode) {
			return getPermission(user);
		}

		IUserAccount account = getUserAccount();
        int permissions = getPermissions();
        if (account != null && permissions != 0) {
            if (getUID() == account.getEUID()) {
                return getPermission(user);
            }
            if (getGID() == account.getEGID()) {
                return getPermission(group);
            }
            return getPermission(other);
        }
        return false;
	}
}
