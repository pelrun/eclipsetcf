/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.nodes;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.core.utils.ConnectStateHelper;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.model.ContainerModelNode;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService;
import org.eclipse.tcf.te.runtime.stepper.utils.StepperHelper;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IStepperServiceOperations;


/**
 * Default peer model implementation.
 */
public class PeerNode extends ContainerModelNode implements IPeerNode, IPeerNodeProvider {
	// Reference to the parent locator model
	private final IPeerModel model;
	// Reference to the peer id (cached for performance optimization)
	private String peerId;

	/**
	 * Constructor.
	 *
	 * @param model The parent locator model. Must not be <code>null</code>.
	 * @param peer The peer. Must not be <code>null</code>.
	 */
	public PeerNode(IPeerModel model, IPeer peer) {
		super();

		Assert.isNotNull(model);
		this.model = model;

		Assert.isNotNull(peer);

		// Set the default properties before enabling the change events.
		// The properties changed listeners should not be called from the
		// constructor.
		setProperty(IPeerNodeProperties.PROP_INSTANCE, peer);
		setProperty(IPeerNodeProperties.PROP_CONNECT_STATE, IConnectable.STATE_DISCONNECTED);

		// Initialize the peer id
		peerId = peer.getID();
		Assert.isNotNull(peerId);

		// Peer model nodes can change the node parent at any time
		allowSetParentOnNonNullParent = true;
		// Peer model nodes does not have a parent by default
		//   -> allow change events with null parent
		suppressEventsOnNullParent = false;

		// Enable change events
		setChangeEventsEnabled(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider#getPeerModel()
	 */
	@Override
	public IPeerNode getPeerNode() {
	    return this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.properties.PropertiesContainer#checkThreadAccess()
	 */
	@Override
	protected final boolean checkThreadAccess() {
		return Protocol.isDispatchThread();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode#getModel()
	 */
	@Override
	public IPeerModel getModel() {
		return (IPeerModel)getAdapter(IPeerModel.class);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode#getPeer()
	 */
	@Override
	public IPeer getPeer() {
		return (IPeer)getAdapter(IPeer.class);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode#getPeerId()
	 */
	@Override
	public String getPeerId() {
		return peerId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.model.ModelNode#getName()
	 */
	@Override
	public String getName() {
		return getPeer().getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode#isComplete()
	 */
	@Override
	public boolean isComplete() {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$

		boolean complete = true;

		// Determine the transport method
		String transport = getPeer().getTransportName();
		// If the transport is not set, the peer attributes are incomplete
		if (transport == null) {
			complete = false;
		} else {
			// For TCP or SSL transport, ATTR_IP_HOST must not be null.
			String ip = getPeer().getAttributes().get(IPeer.ATTR_IP_HOST);
			String port = getPeer().getAttributes().get(IPeer.ATTR_IP_PORT);
			if (("TCP".equals(transport) || "SSL".equals(transport)) && (ip == null || port == null)) { //$NON-NLS-1$ //$NON-NLS-2$
				complete = false;
			}

			// Pipe and Loop transport does not require additional attributes
		}

		return complete;
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

		if (IPeerModel.class.isAssignableFrom(adapter)) {
			return model;
		}

		Object peer = getProperty(IPeerNodeProperties.PROP_INSTANCE);
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
	 * @see org.eclipse.tcf.te.runtime.properties.PropertiesContainer#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PeerNode) {
			return getPeerId().equals(((PeerNode)obj).getPeerId());
		}
		return super.equals(obj);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.properties.PropertiesContainer#hashCode()
	 */
	@Override
	public int hashCode() {
		return getPeerId().hashCode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.properties.PropertiesContainer#postSetProperties(java.util.Map)
	 */
	@Override
	protected void postSetProperties(Map<String, ?> properties) {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(properties);
		Assert.isNotNull(getPeer());

		// New properties applied. Update the element id
		peerId = getPeer().getID();
		Assert.isNotNull(peerId);

		super.postSetProperties(properties);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.properties.PropertiesContainer#postSetProperty(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void postSetProperty(String key, Object value, Object oldValue) {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(key);
		Assert.isNotNull(getPeer());

		// If the peer instance changed, update the element id
		if (IPeerNodeProperties.PROP_INSTANCE.equals(key)) {
			peerId = getPeer().getID();
			Assert.isNotNull(peerId);
		}

		super.postSetProperty(key, value, oldValue);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.model.ModelNode#isVisible()
	 */
	@Override
	public boolean isVisible() {
		IPeer peer = getPeer();
		return peer != null && peer.getAttributes().containsKey(IPeerNodeProperties.PROP_VISIBLE)
						? Boolean.valueOf(peer.getAttributes().get(IPeerNodeProperties.PROP_VISIBLE)).booleanValue() : true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.core.interfaces.IConnectable#getConnectState()
	 */
    @Override
    public int getConnectState() {
    	final AtomicInteger state = new AtomicInteger(STATE_UNKNOWN);
    	Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				if (getProperty(IPeerNodeProperties.PROP_CONNECT_STATE) != null) {
					state.set(getIntProperty(IPeerNodeProperties.PROP_CONNECT_STATE));
				}
			}
		});
    	return state.get();
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode#getPeerType()
     */
    @Override
    public String getPeerType() {
    	final AtomicReference<String> type = new AtomicReference<String>();
    	Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				type.set(getPeer().getAttributes().get(IPeerNodeProperties.PROP_TYPE));
			}
		});
    	return type.get();
    }

    @Override
    public boolean setConnectState(final int newState) {
    	final AtomicBoolean result = new AtomicBoolean(false);
    	if (isConnectStateChangeAllowed(newState)) {
	    	Protocol.invokeAndWait(new Runnable() {
				@Override
				public void run() {
			    	result.set(setProperty(IPeerNodeProperties.PROP_CONNECT_STATE, newState));
				}
			});
    	}
    	return result.get();
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.core.interfaces.IConnectable#changeConnectState(int, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
    @Override
    public void changeConnectState(final int action, ICallback callback, IProgressMonitor monitor) throws IllegalArgumentException {
		final int oldState = getConnectState();
    	if (!isConnectStateChangeActionAllowed(action)) {
    		IllegalArgumentException e = new IllegalArgumentException("Cannot change state from '" + ConnectStateHelper.getConnectState(oldState) + "' using action '" + ConnectStateHelper.getConnectState(action) + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    		if (callback != null) {
    			callback.done(this, StatusHelper.getStatus(e));
    		}
    		else {
    			throw e;
    		}
    	}

    	String operation = null;
    	int intermediateState = 0;

    	switch (action) {
		case ACTION_CONNECT:
			operation = IStepperServiceOperations.CONNECT;
			intermediateState = STATE_CONNECT_SCHEDULED;
			break;
		case ACTION_DISCONNECT:
			operation = IStepperServiceOperations.DISCONNECT;
			intermediateState = STATE_DISCONNECT_SCHEDULED;
			break;
		}

    	IStepperOperationService service = StepperHelper.getService(this, operation);
    	if (service != null) {
    		setConnectState(intermediateState);
    		StepperHelper.scheduleStepperJob(this, operation, service, callback, monitor);
    	}
    	else if (callback != null) {
    		callback.done(this, StatusHelper.getStatus(new NullPointerException("Missing stepper operation service for " + getName() + "."))); //$NON-NLS-1$ //$NON-NLS-2$
    	}
    }

    @Override
    public boolean isConnectStateChangeActionAllowed(int action) {
    	int state = getConnectState();
    	switch (state) {
			case STATE_CONNECTED:
			case STATE_CONNECT_SCHEDULED:
			case STATE_CONNECTING:
				return isAllowedState(action, ACTION_DISCONNECT);
			case STATE_DISCONNECTED:
				return isAllowedState(action, ACTION_CONNECT);
    	}
    	return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.core.interfaces.IConnectable#isConnectStateChangeAllowed(int)
     */
    @Override
    public boolean isConnectStateChangeAllowed(int newState) {
    	int state = getConnectState();
    	switch (state) {
			case STATE_CONNECTED:
				return isAllowedState(newState, STATE_DISCONNECTED, STATE_DISCONNECT_SCHEDULED, STATE_DISCONNECTING);
			case STATE_CONNECT_SCHEDULED:
				return isAllowedState(newState, STATE_CONNECTING, STATE_CONNECTED, STATE_DISCONNECTED, STATE_DISCONNECT_SCHEDULED, STATE_DISCONNECTING);
			case STATE_CONNECTING:
				return isAllowedState(newState, STATE_CONNECTED, STATE_DISCONNECT_SCHEDULED, STATE_DISCONNECTING, STATE_DISCONNECTED);
			case STATE_DISCONNECTED:
				return isAllowedState(newState, STATE_CONNECTED, STATE_CONNECT_SCHEDULED, STATE_CONNECTING);
			case STATE_DISCONNECT_SCHEDULED:
				return isAllowedState(newState, STATE_DISCONNECTING, STATE_DISCONNECTED);
			case STATE_DISCONNECTING:
				return isAllowedState(newState, STATE_DISCONNECTED);
			case STATE_UNKNOWN:
				return isAllowedState(newState, STATE_DISCONNECTED);
		}
        return false;
    }

    private boolean isAllowedState(int state, int... allowedStates) {
    	for (int allowedState : allowedStates) {
	        if (state == allowedState) {
	        	return true;
	        }
        }
    	return false;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.protocol.IChannel.IChannelListener#onChannelOpened()
	 */
    @Override
    public void onChannelOpened() {
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.protocol.IChannel.IChannelListener#onChannelClosed(java.lang.Throwable)
	 */
    @Override
    public void onChannelClosed(Throwable error) {
    	if (isConnectStateChangeActionAllowed(IConnectable.ACTION_DISCONNECT)) {
    		changeConnectState(IConnectable.ACTION_DISCONNECT, null, null);
    	}
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.protocol.IChannel.IChannelListener#congestionLevel(int)
	 */
    @Override
    public void congestionLevel(int level) {
    }
}
