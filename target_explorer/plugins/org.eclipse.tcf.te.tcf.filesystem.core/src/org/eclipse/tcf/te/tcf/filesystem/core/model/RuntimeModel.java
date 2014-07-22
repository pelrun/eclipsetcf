/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.model;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.model.ContainerModelNode;
import org.eclipse.tcf.te.runtime.model.factory.Factory;
import org.eclipse.tcf.te.runtime.model.interfaces.factory.IFactory;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelService;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * The file system model implementation.
 */
public final class RuntimeModel extends ContainerModelNode implements IRuntimeModel {
    // Flag to mark the model disposed
    private boolean disposed;

    // Reference to the model node factory
    private IFactory factory = null;

    // The root node of the peer model
	private FSTreeNode root;
	private IPeerNode peerNode;

    /**
	 * Create a File System ModelManager.
	 */
	public RuntimeModel(IPeerNode peerNode) {
	    disposed = false;
		this.peerNode = peerNode;
	}

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.runtime.nodes.PropertiesContainer#checkThreadAccess()
     */
    @Override
    protected boolean checkThreadAccess() {
        return Protocol.isDispatchThread();
    }

    /* (non-Javadoc)
     * @see com.windriver.te.tcf.core.model.interfaces.IModel#dispose()
     */
    @Override
    public void dispose() {
        Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
        disposed = true;
    }

    /* (non-Javadoc)
     * @see com.windriver.te.tcf.core.model.interfaces.IModel#isDisposed()
     */
    @Override
    public boolean isDisposed() {
        Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
        return disposed;
    }


    /* (non-Javadoc)
     * @see com.windriver.te.tcf.core.model.interfaces.IModel#getService(java.lang.Class)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <V extends IModelService> V getService(Class<V> serviceInterface) {
        Assert.isNotNull(serviceInterface);
        return (V)getAdapter(serviceInterface);
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class adapter) {
        if (IPeerNode.class.isAssignableFrom(adapter) || IConnectable.class.isAssignableFrom(adapter)) {
            final AtomicReference<IPeerNode> peerNode = new AtomicReference<IPeerNode>();
            Protocol.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    peerNode.set(getPeerNode());
                }
            });
            return peerNode.get();
        }

        return super.getAdapter(adapter);
    }

    /* (non-Javadoc)
     * @see com.windriver.te.tcf.core.model.interfaces.IModel#setFactory(com.windriver.te.tcf.core.model.interfaces.IModelNodeFactory)
     */
    @Override
    public void setFactory(IFactory factory) {
        Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
        this.factory = factory;
    }

    /* (non-Javadoc)
     * @see com.windriver.te.tcf.core.model.interfaces.IModel#getFactory()
     */
    @Override
    public IFactory getFactory() {
        Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
        return factory != null ? factory : Factory.getInstance();
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider#getPeerModel()
     */
    @Override
    public IPeerNode getPeerNode() {
        Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
        return peerNode;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.runtime.model.ModelNode#toString()
     */
    @Override
    public String toString() {
        if (disposed) {
            return "*DISPOSED* : " + super.toString(); //$NON-NLS-1$
        }
        return super.toString();
    }

    /**
	 * Get the root node of the peer model.
	 *
	 * @return The root node.
	 */
	@Override
	public FSTreeNode getRoot() {
		if(root == null) {
			root = createRoot();
		}
		return root;
	}

	/**
	 * Create a root node for the specified peer.
	 *
	 * @param peerNode The peer.
	 */
	/* default */ FSTreeNode createRoot() {
		if (Protocol.isDispatchThread()) {
			return createRootNode(peerNode);
		}
		else {
			final AtomicReference<FSTreeNode> ref = new AtomicReference<FSTreeNode>();
			Protocol.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					ref.set(createRoot());
				}
			});
			return ref.get();
		}
	}

	/**
	 * Create a root node for the peer.
	 *
	 * @param peerNode The peer.
	 * @return The root file system node.
	 */
	public static FSTreeNode createRootNode(IPeerNode peerNode) {
		FSTreeNode node = new FSTreeNode();
		node.type = "FSRootNode"; //$NON-NLS-1$
		node.peerNode = peerNode;
		node.name = Messages.FSTreeNodeContentProvider_rootNode_label;
	    return node;
    }

	/**
	 * Create a file node under the folder specified folder using the new name.
	 *
	 * @param name The file's name.
	 * @param folder The parent folder.
	 * @return The file tree node.
	 */
	public static FSTreeNode createFileNode(String name, FSTreeNode folder) {
		return createTreeNode(name, "FSFileNode", folder); //$NON-NLS-1$
    }

	/**
	 * Create a folder node under the folder specified folder using the new name.
	 *
	 * @param name The folder's name.
	 * @param folder The parent folder.
	 * @return The folder tree node.
	 */
	public static FSTreeNode createFolderNode(String name, FSTreeNode folder) {
		return createTreeNode(name, "FSDirNode", folder); //$NON-NLS-1$
    }

	/**
	 * Create a tree node under the folder specified folder using the new name.
	 *
	 * @param name The tree node's name.
	 * @param type The new node's type.
	 * @param folder The parent folder.
	 * @return The tree node.
	 */
	private static FSTreeNode createTreeNode(String name, String type, FSTreeNode folder) {
	    FSTreeNode node = new FSTreeNode();
		node.name = name;
		node.parent = folder;
		node.peerNode = folder.peerNode;
		node.type = type;
	    return node;
    }
}
