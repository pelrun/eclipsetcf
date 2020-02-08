/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.model;

import static org.eclipse.tcf.te.tcf.locator.model.ModelManager.getPeerModel;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IChannel.IChannelListener;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.core.interfaces.IPropertyChangeProvider;
import org.eclipse.tcf.te.runtime.model.ContainerModelNode;
import org.eclipse.tcf.te.runtime.model.factory.Factory;
import org.eclipse.tcf.te.runtime.model.interfaces.factory.IFactory;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IDelegateService;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelService;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IConfirmCallback;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IResultOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.UserAccount;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpCopyLocal;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpParsePath;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpRestoreFavorites;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.CacheManager;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * The file system model implementation.
 */
public final class RuntimeModel extends ContainerModelNode implements IRuntimeModel, IChannelListener {

	private final IPeerNode fPeerNode;
	private final FSTreeNode fRoot;
	private final UserAccount fUserAccount;
	private IChannel fChannel;
	private IFileSystem fFileSystem;
	private Delegate fDelegate;

    /**
	 * Create a File System ModelManager.
	 */
	public RuntimeModel(IPeerNode peerNode, IChannel channel, IFileSystem fileSystem, UserAccount userAccount) {
		fPeerNode = peerNode;
		fChannel = channel;
		fFileSystem = fileSystem;
		fUserAccount = userAccount;
		fRoot = new FSTreeNode(this, Messages.FSTreeNodeContentProvider_rootNodeLabel);
		channel.addChannelListener(this);

		fDelegate = findDelegate();
	}

    private Delegate findDelegate() {
		IService[] services = ServiceManager.getInstance().getServices(fPeerNode, IDelegateService.class, false);
		for (IService service : services) {
			Delegate cand = ((IDelegateService) service).getDelegate(this, Delegate.class);
			if (cand != null)
				return cand;
		}
		return new Delegate();
	}

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.runtime.properties.PropertiesContainer#checkThreadAccess()
     */
	@Override
    protected boolean checkThreadAccess() {
        return Protocol.isDispatchThread();
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.protocol.IChannel.IChannelListener#onChannelOpened()
	 */
    @Override
    public void onChannelOpened() {
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.protocol.IChannel.IChannelListener#congestionLevel(int)
     */
    @Override
    public void congestionLevel(int level) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.protocol.IChannel.IChannelListener#onChannelClosed(java.lang.Throwable)
     */
    @Override
    public void onChannelClosed(Throwable error) {
    	ModelManager.disposeRuntimeModel(fPeerNode);
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.core.model.interfaces.IModel#dispose()
     */
    @Override
    public void dispose() {
        Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
        fFileSystem = null;
        fRoot.setContent(new FSTreeNode[0], false);
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.core.model.interfaces.IModel#isDisposed()
     */
    @Override
    public boolean isDisposed() {
        Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
        return fFileSystem == null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.core.model.interfaces.IModel#getService(java.lang.Class)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <V extends IModelService> V getService(Class<V> serviceInterface) {
        Assert.isNotNull(serviceInterface);
        return (V)getAdapter(serviceInterface);
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IRuntimeModel#getDelegate()
     */
    @Override
	public Delegate getDelegate() {
    	return fDelegate;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.runtime.properties.PropertiesContainer#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(Class adapter) {
        if (IPeerNode.class.isAssignableFrom(adapter) || IConnectable.class.isAssignableFrom(adapter)) {
        	return getPeerNode();
        }

        return super.getAdapter(adapter);
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.core.model.interfaces.IModel#setFactory(org.eclipse.tcf.te.runtime.model.interfaces.factory.IFactory)
     */
    @Override
    public void setFactory(IFactory factory) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.core.model.interfaces.IModel#getFactory()
     */
    @Override
    public IFactory getFactory() {
        return Factory.getInstance();
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider#getPeerNode()
     */
    @Override
    public final IPeerNode getPeerNode() {
        return fPeerNode;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.runtime.model.ModelNode#toString()
     */
    @Override
    public String toString() {
        if (isDisposed()) {
            return "*DISPOSED* : " + super.toString(); //$NON-NLS-1$
        }
        return super.toString();
    }

	@Override
	public FSTreeNode getRoot() {
		return fRoot;
	}

	public UserAccount getUserAccount() {
		return fUserAccount;
	}

	public void firePropertyChanged(PropertyChangeEvent propertyChangeEvent) {
		IPropertyChangeProvider provider = (IPropertyChangeProvider) fPeerNode.getAdapter(IPropertyChangeProvider.class);
		if (provider != null)
			provider.firePropertyChange(propertyChangeEvent);
	}

	@Override
	public IOperation operationDownload(List<IFSTreeNode> nodes, File destination, IConfirmCallback confirmCallback) {
		return new OpCopyLocal(nodes, destination, confirmCallback);
	}

	@Override
	public IResultOperation<IFSTreeNode> operationRestoreFromPath(String path) {
		return new OpParsePath(fPeerNode, path);
	}

	public static IPeerNode getPeerFromPath(String path) {
		String cacheRoot = CacheManager.getCacheRoot().getAbsolutePath();
		if (!path.startsWith(cacheRoot))
			return null;

		path = path.substring(cacheRoot.length() + 1);
		int slash = path.indexOf(File.separator);
		if (slash == -1)
			return null;

		String peerId = path.substring(0, slash);
		peerId = peerId.replace(CacheManager.PATH_ESCAPE_CHAR, ':');

		for (IPeerNode peer : getPeerModel().getPeerNodes()) {
			if (peerId.equals(peer.getPeerId()))
				return peer;
		}
		return null;
	}

	@Override
	public IResultOperation<IFSTreeNode[]> operationRestoreFavorites() {
		return new OpRestoreFavorites(this);
	}

	@Override
	public IChannel getChannel() {
		return fChannel;
	}

	public IFileSystem getFileSystem() {
		return fFileSystem;
	}
}
