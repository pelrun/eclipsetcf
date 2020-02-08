/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.model.ContainerModelNode;
import org.eclipse.tcf.te.runtime.model.contexts.AsyncRefreshableCtxAdapter;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx.QueryState;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx.QueryType;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;

/**
 * Default locator node implementation.
 */
public class LocatorNode extends ContainerModelNode implements ILocatorNode {
	// Locator nodes needs asynchronous refreshes
	private final IAsyncRefreshableCtx refreshableCtxAdapter = new AsyncRefreshableCtxAdapter();

	/**
     * Constructor.
     */
    public LocatorNode(IPeer peer, boolean isStatic) {
		super();

		Assert.isNotNull(peer);

		// Set the default properties before enabling the change events.
		// The properties changed listeners should not be called from the
		// constructor.
		setProperty(IPeerNodeProperties.PROPERTY_INSTANCE, peer);
		setProperty(ILocatorNode.PROPERTY_STATIC_INSTANCE, isStatic ? peer : null);

		PendingOperationNode pendingNode = new PendingOperationNode();
		pendingNode.setParent(this);
		refreshableCtxAdapter.setPendingOperationNode(pendingNode);

		// Enable change events
		setChangeEventsEnabled(true);
    }

	/**
	 * Constructor.
	 *
	 * @param peer The peer. Must not be <code>null</code>.
	 */
	public LocatorNode(IPeer peer) {
		this(peer, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.model.ContainerModelNode#add(org.eclipse.tcf.te.runtime.model.interfaces.IModelNode)
	 */
	@Override
	public boolean add(IModelNode node) {
	    return super.add(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.properties.PropertiesContainer#checkThreadAccess()
	 */
	@Override
	protected final boolean checkThreadAccess() {
		return Protocol.isDispatchThread();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode#isDiscovered()
	 */
	@Override
	public boolean isDiscovered() {
	    final AtomicBoolean isDiscovered = new AtomicBoolean(false);
	    Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				isDiscovered.set(getProperty(PROPERTY_STATIC_INSTANCE) != getProperty(IPeerNodeProperties.PROPERTY_INSTANCE));
			}
		});
	    return isDiscovered.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode#isStatic()
	 */
	@Override
	public boolean isStatic() {
	    final AtomicBoolean isStatic = new AtomicBoolean(false);
	    Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				isStatic.set(getProperty(PROPERTY_STATIC_INSTANCE) != null);
			}
		});
	    return isStatic.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode#getPeer()
	 */
	@Override
	public IPeer getPeer() {
		return (IPeer)getAdapter(IPeer.class);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode#getPeers()
	 */
	@Override
	public IPeer[] getPeers() {
		List<IPeer> peers = new ArrayList<IPeer>();
		for (ILocatorNode locatorNode : getChildren(ILocatorNode.class)) {
	        peers.add(locatorNode.getPeer());
        }

		return peers.toArray(new IPeer[peers.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.model.ModelNode#getName()
	 */
	@Override
	public String getName() {
		return getPeer().getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(final Class adapter) {
		// NOTE: The getAdapter(...) method can be invoked from many place and
		//       many threads where we cannot control the calls. Therefore, this
		//       method is allowed be called from any thread.
		final AtomicReference<Object> object = new AtomicReference<Object>();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				object.set(doGetAdapter(adapter));
			}
		};

		if (Protocol.isDispatchThread()) {
			runnable.run();
		}
		else {
			Protocol.invokeAndWait(runnable);
		}

		return object.get() != null ? object.get() : super.getAdapter(adapter);
	}

	/**
	 * Returns an object which is an instance of the given class associated with this object.
	 * Returns <code>null</code> if no such object can be found.
	 * <p>
	 * This method must be called within the TCF dispatch thread!
	 *
	 * @param adapter The adapter class to look up.
	 * @return The adapter or <code>null</code>.
	 */
	protected Object doGetAdapter(Class<?> adapter) {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$

		if (ILocatorModel.class.isAssignableFrom(adapter)) {
			return ModelManager.getLocatorModel();
		}
		if (IAsyncRefreshableCtx.class.isAssignableFrom(adapter)) {
			return refreshableCtxAdapter;
		}

		Object peer = getProperty(IPeerNodeProperties.PROPERTY_INSTANCE);
		// Check with adapter.isAssignableFrom(...) to return the peer instance
		// correctly if adapter is IPeer.class.
		if (peer != null && adapter.isAssignableFrom(peer.getClass())) {
			return peer;
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.properties.PropertiesContainer#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder(getClass().getSimpleName());

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				IPeer peer = getPeer();
				buffer.append(": id=" + peer.getID()); //$NON-NLS-1$
				buffer.append(", name=" + peer.getName()); //$NON-NLS-1$
			}
		};

		if (Protocol.isDispatchThread()) {
			runnable.run();
		}
		else {
			Protocol.invokeAndWait(runnable);
		}

		buffer.append(", " + super.toString()); //$NON-NLS-1$
		return buffer.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.model.ContainerModelNode#hasChildren()
	 */
	@Override
	public boolean hasChildren() {
		QueryState ctxQuery = refreshableCtxAdapter.getQueryState(QueryType.CONTEXT);
		QueryState childQuery = refreshableCtxAdapter.getQueryState(QueryType.CHILD_LIST);
		if (ctxQuery == QueryState.IN_PROGRESS || ctxQuery == QueryState.PENDING) {
			return true;
		}
		if (childQuery == QueryState.IN_PROGRESS || childQuery == QueryState.PENDING) {
			return true;
		}
	    return super.hasChildren();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.model.ContainerModelNode#getChildren()
	 */
	@Override
	public IModelNode[] getChildren() {

		List<IModelNode> children = getChildren(IModelNode.class);

		QueryState ctxQuery = refreshableCtxAdapter.getQueryState(QueryType.CONTEXT);
		QueryState childQuery = refreshableCtxAdapter.getQueryState(QueryType.CHILD_LIST);
		if (ctxQuery == QueryState.PENDING || childQuery == QueryState.PENDING) {
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					ILocatorModelRefreshService service = ModelManager.getLocatorModel().getService(ILocatorModelRefreshService.class);
					service.refresh(LocatorNode.this, null);
				}
			});
			children.add(refreshableCtxAdapter.getPendingOperationNode());
		}
		if (ctxQuery == QueryState.IN_PROGRESS || childQuery == QueryState.IN_PROGRESS) {
			children.add(refreshableCtxAdapter.getPendingOperationNode());
		}
	    return children.toArray(new IModelNode[children.size()]);
	}
}
