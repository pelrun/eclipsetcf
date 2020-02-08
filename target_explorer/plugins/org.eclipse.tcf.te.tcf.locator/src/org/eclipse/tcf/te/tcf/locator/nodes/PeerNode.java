/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.core.utils.ConnectStateHelper;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.events.NotifyEvent;
import org.eclipse.tcf.te.runtime.interfaces.IConditionTester;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.model.ContainerModelNode;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IDelegateService;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService;
import org.eclipse.tcf.te.runtime.stepper.utils.StepperHelper;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.locator.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.locator.interfaces.IStepperServiceOperations;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelUpdateService;
import org.eclipse.tcf.te.tcf.locator.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.utils.SimulatorUtils;


/**
 * Default peer model implementation.
 */
public class PeerNode extends ContainerModelNode implements IPeerNode, IPeerNodeProvider {
	// Reference to the parent locator model
	private final IPeerModel model;
	// Reference to the peer id (cached for performance optimization)
	private String peerId;

	private boolean isValid = true;

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
		setProperty(IPeerNodeProperties.PROPERTY_INSTANCE, peer);
		setProperty(IPeerNodeProperties.PROPERTY_CONNECT_STATE, IConnectable.STATE_DISCONNECTED);

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
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode#isValid()
	 */
	@Override
	public boolean isValid() {
		final AtomicBoolean valid = new AtomicBoolean(true);
		IService[] services = ServiceManager.getInstance().getServices(this, IDelegateService.class, false);
		for (IService service : services) {
			if (service instanceof IDelegateService) {
				IPeerNode.IDelegate delegate = ((IDelegateService)service).getDelegate(this, IPeerNode.IDelegate.class);
				if (delegate != null) {
					if (delegate.isVisible(this) && !delegate.isValid(this)) {
						valid.set(false);
						break;
					}
				}
			}
		}

		if (isValid != valid.get()) {
			isValid = valid.get();
			fireChangeEvent(IPeerNodeProperties.PROPERTY_IS_VALID, new Boolean(isValid), new Boolean(valid.get()));
		}

		return isValid;
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
		if (IPeerNodeProperties.PROPERTY_INSTANCE.equals(key)) {
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
		boolean visible = peer != null && peer.getAttributes().containsKey(IPeerProperties.PROP_VISIBLE)
						? Boolean.valueOf(peer.getAttributes().get(IPeerProperties.PROP_VISIBLE)).booleanValue() : true;
		if (visible) {
			IService[] services = ServiceManager.getInstance().getServices(this, IDelegateService.class, false);
			if (services != null && services.length > 0) {
				for (IService service : services) {
					if (service instanceof IDelegateService) {
						IPeerNode.IDelegate delegate = ((IDelegateService)service).getDelegate(this, IPeerNode.IDelegate.class);
						if (delegate != null) {
							return delegate.isVisible(this);
						}
					}
                }
			}
		}
		return visible;
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
				if (getProperty(IPeerNodeProperties.PROPERTY_CONNECT_STATE) != null) {
					state.set(getIntProperty(IPeerNodeProperties.PROPERTY_CONNECT_STATE));
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
		if (getPeer() != null) {
			return getPeer().getAttributes().get(IPeerProperties.PROP_TYPE);
		}
    	return null;
    }

    @Override
    public boolean setConnectState(final int newState) {
    	final AtomicBoolean result = new AtomicBoolean(false);
    	if (isConnectStateChangeAllowed(newState)) {
	    	Protocol.invokeAndWait(new Runnable() {
				@Override
				public void run() {
			    	result.set(setProperty(IPeerNodeProperties.PROPERTY_CONNECT_STATE, newState));
			    	if (newState != IConnectable.STATE_CONNECTED) {
			    		setProperty(IPeerNodeProperties.PROPERTY_WARNINGS, null);
			    	}
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
		case STATE_CONNECTION_LOST:
			operation = IStepperServiceOperations.CONNECTION_LOST;
			intermediateState = STATE_CONNECTION_LOST;
			break;
		case STATE_CONNECTION_RECOVERING:
			operation = IStepperServiceOperations.CONNECTION_RECOVERING;
			intermediateState = STATE_CONNECTION_RECOVERING;
			break;
		}

    	IStepperOperationService service = StepperHelper.getService(this, operation);
    	if (service != null) {
    		setConnectState(intermediateState);
    		StepperHelper.scheduleStepperJob(this, operation, service, new PropertiesContainer(), callback, monitor);
    	}
    	else if (callback != null) {
    		callback.done(this, StatusHelper.getStatus(new NullPointerException("Missing stepper operation service for " + getName() + "."))); //$NON-NLS-1$ //$NON-NLS-2$
    	}
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.core.interfaces.IConnectable#isConnectStateChangeActionAllowed(int)
     */
    @Override
    public boolean isConnectStateChangeActionAllowed(int action) {
    	int state = getConnectState();
    	switch (state) {
			case STATE_CONNECTED:
				return isAllowedStateOrAction(action, ACTION_DISCONNECT, STATE_CONNECTION_LOST);
			case STATE_CONNECTION_RECOVERING:
			case STATE_CONNECT_SCHEDULED:
			case STATE_CONNECTING:
				return isAllowedStateOrAction(action, ACTION_DISCONNECT);
			case STATE_DISCONNECTED:
				return isValid() && isAllowedStateOrAction(action, ACTION_CONNECT);
			case STATE_CONNECTION_LOST:
				return isAllowedStateOrAction(action, STATE_CONNECTION_RECOVERING);
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
    		case STATE_CONNECTION_LOST:
				return isAllowedStateOrAction(newState, STATE_DISCONNECTED, STATE_CONNECTION_RECOVERING);
    		case STATE_CONNECTION_RECOVERING:
				return isAllowedStateOrAction(newState, STATE_CONNECTED, STATE_DISCONNECT_SCHEDULED, STATE_DISCONNECTING, STATE_DISCONNECTED);
			case STATE_CONNECTED:
				return isAllowedStateOrAction(newState, STATE_CONNECTION_LOST, STATE_DISCONNECTED, STATE_DISCONNECT_SCHEDULED, STATE_DISCONNECTING);
			case STATE_CONNECT_SCHEDULED:
				return isAllowedStateOrAction(newState, STATE_CONNECTING, STATE_CONNECTED, STATE_DISCONNECTED, STATE_DISCONNECT_SCHEDULED, STATE_DISCONNECTING);
			case STATE_CONNECTING:
				return isAllowedStateOrAction(newState, STATE_CONNECTED, STATE_DISCONNECT_SCHEDULED, STATE_DISCONNECTING, STATE_DISCONNECTED);
			case STATE_DISCONNECTED:
				return isAllowedStateOrAction(newState, STATE_CONNECTED, STATE_CONNECT_SCHEDULED, STATE_CONNECTING);
			case STATE_DISCONNECT_SCHEDULED:
				return isAllowedStateOrAction(newState, STATE_DISCONNECTING, STATE_DISCONNECTED);
			case STATE_DISCONNECTING:
				return isAllowedStateOrAction(newState, STATE_DISCONNECTED);
			case STATE_UNKNOWN:
				return isAllowedStateOrAction(newState, STATE_DISCONNECTED);
		}
        return false;
    }

    private boolean isAllowedStateOrAction(int stateOrAction, int... allowedStatesOrActions) {
    	for (int allowedStateOrAction : allowedStatesOrActions) {
	        if (stateOrAction == allowedStateOrAction) {
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
    	final AtomicBoolean connectionLost = new AtomicBoolean(true);
    	if (SimulatorUtils.getSimulatorService(this) != null) {
    		ExecutorsUtil.waitAndExecute(1000, new IConditionTester() {
				@Override
				public boolean isConditionFulfilled() {
		    		Protocol.invokeAndWait(new Runnable() {
						@Override
						public void run() {
				    		Object simProcess = getProperty(ISimulatorService.PROP_SIM_INSTANCE);
				    		if (simProcess instanceof Process) {
				    			try {
				    				((Process)simProcess).exitValue();
					    			connectionLost.set(false);
				    			}
				    			catch (Exception e) {
				    			}
				    		}
			    			else {
				    			connectionLost.set(false);
			    			}
						}
					});
		    		return !connectionLost.get();
				}
				@Override
				public void cleanup() {
				}
			});
    	}


    	if (connectionLost.get() && isConnectStateChangeActionAllowed(IConnectable.STATE_CONNECTION_LOST)) {
    		Platform.getLog(CoreBundleActivator.getDefault().getBundle()).log(new Status(IStatus.INFO,
    						CoreBundleActivator.getUniqueIdentifier(),
    						NLS.bind(Messages.PeerNode_info_connectionLost, getName())));
    		changeConnectState(IConnectable.STATE_CONNECTION_LOST, new Callback() {
    			@Override
    			protected void internalDone(Object caller, IStatus status) {
    				Protocol.invokeLater(new Runnable() {
						@Override
						public void run() {
		    				IPeerModelUpdateService service = getModel().getService(IPeerModelUpdateService.class);
		    				service.updatePeerServices(PeerNode.this, null, null);
						}
					});
    				fireNotification(getConnectState());
    				if (status.isOK() && isConnectStateChangeAllowed(IConnectable.STATE_CONNECTION_RECOVERING)) {
    					changeConnectState(IConnectable.STATE_CONNECTION_RECOVERING, new Callback() {
    						@Override
    						protected void internalDone(Object caller, IStatus status) {
    							if (status.isOK()) {
    								fireNotification(IConnectable.STATE_CONNECTION_RECOVERING);
    							}
    						}
    					},
    	    			null);
    				}
    				else {
    				}
    			}
    		},
    		null);
    	}
    	else if (isConnectStateChangeActionAllowed(IConnectable.ACTION_DISCONNECT)) {
    		Platform.getLog(CoreBundleActivator.getDefault().getBundle()).log(new Status(IStatus.INFO,
    						CoreBundleActivator.getUniqueIdentifier(),
    						NLS.bind(Messages.PeerNode_info_connectionDisconnected, getName())));
    		changeConnectState(IConnectable.ACTION_DISCONNECT, new Callback() {
    			@Override
    			protected void internalDone(Object caller, IStatus status) {
    				Protocol.invokeLater(new Runnable() {
						@Override
						public void run() {
		    				IPeerModelUpdateService service = getModel().getService(IPeerModelUpdateService.class);
		    				service.updatePeerServices(PeerNode.this, null, null);
		    				fireNotification(IConnectable.ACTION_DISCONNECT);
						}
					});
    			}
    		},
    		null);
    	}
    	else {
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
    				IPeerModelUpdateService service = getModel().getService(IPeerModelUpdateService.class);
    				service.updatePeerServices(PeerNode.this, null, null);
				}
			});
    	}
    }

	/**
	 * Fire the module notification.
	 *
	 * @param node The module context node. Must not be <code>null</code>.
	 * @param operation The module operation (added, removed, changed). Must not be <code>null</code>.
	 * @param status The status of the operation (success, failed, ...). Must not be <code>null</code>
	 */
	protected void fireNotification(int state) {
		// Show a notification to the user
		String message = null;
		switch (state) {
		case IConnectable.STATE_CONNECTION_LOST:
			message = Messages.PeerNode_notification_message_connectionLost;
			break;
		case IConnectable.STATE_CONNECTION_RECOVERING:
			message = Messages.PeerNode_notification_message_connectionRecovered;
			break;
		case IConnectable.STATE_DISCONNECTED:
			message = Messages.PeerNode_notification_message_disconnected;
			break;
		}

		if (message != null) {
			IPropertiesContainer properties = new PropertiesContainer();
			properties.setProperty(NotifyEvent.PROP_TITLE_TEXT, getName());
			properties.setProperty(NotifyEvent.PROP_TITLE_IMAGE_ID, getPeerType());
			properties.setProperty(NotifyEvent.PROP_DESCRIPTION_TEXT, message);

			NotifyEvent event = new NotifyEvent(getModel(), null, properties);
			EventManager.getInstance().fireEvent(event);
		}
	}

    /* (non-Javadoc)
	 * @see org.eclipse.tcf.protocol.IChannel.IChannelListener#congestionLevel(int)
	 */
    @Override
    public void congestionLevel(int level) {
    }
}
