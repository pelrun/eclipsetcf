/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tcf.te.runtime.activator.CoreBundleActivator;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventFireDelegate;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.runtime.interfaces.tracing.ITraceIds;
import org.eclipse.tcf.te.runtime.utils.Host;
import org.osgi.framework.Bundle;


/**
 * The event manager implementation.
 */
public final class EventManager {
	// Flag to remember if the extension point has been processed.
	private boolean extensionPointProcessed;
	// The list of registered listeners.
	private final List<ListenerListEntry> listeners = new ArrayList<ListenerListEntry>();

	/**
	 * Runnable implementation to fire a given event to a given listener.
	 */
	protected static class FireRunnable implements Runnable {
		private final IEventListener listener;
		private final EventObject event;

		/**
		 * Constructor.
		 *
		 * @param listener The event listener. Must not be <code>null</code>.
		 * @param event The event. Must not be <code>null</code>.
		 */
		public FireRunnable(IEventListener listener, EventObject event) {
			Assert.isNotNull(listener);
			Assert.isNotNull(event);
			this.listener = listener;
			this.event = event;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			listener.eventFired(event);
		}
	}

	/**
	 * Listener list entry.
	 * <p>
	 * Each entry contains a reference to the listener and a list of valid source classes.
	 * If an event source can be casted to one of the classes the listener is invoked.
	 */
	private static class ListenerListEntry {
		private final IEventListener listener;
		private Object[] eventSources;
		private ClassNotLoadedItem[] eventSourcesNotLoaded;
		private Class<?>[] eventTypes;
		private ClassNotLoadedItem[] eventTypesNotLoaded;

		/**
		 * Constructor.
		 *
		 * @param listener The listener.
		 */
		protected ListenerListEntry(IEventListener listener) {
			this(listener, null, null, null, null);
		}

		/**
		 * Constructor.
		 *
		 * @param listener The listener.
		 * @param eventTypes The event types the listener is interested in.
		 * @param eventTypesNotLoaded The event types this listener wants to be invoked but could not be loaded.
		 * @param eventSources The source types for which events should be fired to the listener.
		 * @param eventSourceNotLoaded The event types this listener wants to be invoked but could not be loaded.
		 */
		protected ListenerListEntry(IEventListener listener, Class<?>[] eventTypes, ClassNotLoadedItem[] eventTypesNotLoaded, Object[] eventSources, ClassNotLoadedItem[] eventSourcesNotLoaded) {
			this.listener = listener;
			if (eventTypes == null || eventTypes.length == 0) {
				this.eventTypes = null;
			} else {
				this.eventTypes = eventTypes;
			}
			if (eventTypesNotLoaded == null || eventTypesNotLoaded.length == 0) {
				this.eventTypesNotLoaded = null;
			} else {
				this.eventTypesNotLoaded = eventTypesNotLoaded;
			}
			if (eventSources == null || eventSources.length == 0) {
				this.eventSources = null;
			} else {
				this.eventSources = eventSources;
			}
			if (eventSourcesNotLoaded == null || eventSourcesNotLoaded.length == 0) {
				this.eventSourcesNotLoaded = null;
			} else {
				this.eventSourcesNotLoaded = eventSourcesNotLoaded;
			}
		}

		/**
		 * Get the listener of this entry.
		 */
		protected EventListener getListener() {
			return listener;
		}

		/**
		 * Attempts to load the class specified by the given class
		 * not found item.
		 *
		 * @param item The class not found item. Must not be <code>null</code>.
		 * @return The class object or <code>null</code>.
		 */
		private Class<?> loadClass(ClassNotLoadedItem item, String type) {
			Assert.isNotNull(item);
			Assert.isNotNull(type);

			Class<?> clazz = null;

			// If a bundle id got specified, use the specified bundle to load the service class
			Bundle bundle = Platform.getBundle(item.bundleId);
			// If we don't have a bundle to load from yet, fallback to the declaring bundle
			if (bundle == null) bundle = Platform.getBundle(item.declaringBundleId);
			// And finally, use our own bundle to load the class. This fallback is expected
			// to never be used.
			if (bundle == null) bundle = CoreBundleActivator.getContext().getBundle();

			// If the specified bundle is active, or "forceBundleActivation" is true, the class can be loaded
			if (bundle != null && bundle.getState() == Bundle.ACTIVE) {
				try {
					clazz = bundle.loadClass(item.className);
				} catch (Exception ex) {
					if (isTracingEnabled())
						CoreBundleActivator.getTraceHandler().trace("Error instantiating event listener " + type + " object instance: " + item.className, //$NON-NLS-1$ //$NON-NLS-2$
										0, ITraceIds.TRACE_EVENTS, IStatus.ERROR, this);
				}
			}

			return clazz;
		}

		/**
		 * Attempt to load event type class objects which could not be loaded
		 * before because the parent plug-in's are not activated.
		 */
		private void loadNotLoadedEventTypes() {
			if (eventTypesNotLoaded == null || eventTypesNotLoaded.length == 0) return;

			List<Class<?>> types = new ArrayList<Class<?>>();
			if (eventTypes != null && eventTypes.length > 0) types.addAll(Arrays.asList(eventTypes));

			List<ClassNotLoadedItem> notLoaded = new ArrayList<ClassNotLoadedItem>(Arrays.asList(eventTypesNotLoaded));

			boolean changed = false;

			Iterator<ClassNotLoadedItem> it = notLoaded.iterator();
			while (it.hasNext()) {
				ClassNotLoadedItem item = it.next();
				Class<?> clazz = loadClass(item, "event type"); //$NON-NLS-1$
				if (clazz != null) {
					it.remove();
					changed = true;
					if (!types.contains(clazz)) types.add(clazz);
				}
			}

			if (changed) {
				eventTypes = types.toArray(new Class<?>[types.size()]);
				eventTypesNotLoaded = notLoaded.toArray(new ClassNotLoadedItem[notLoaded.size()]);
			}
		}

		/**
		 * Attempt to load event source class objects which could not be loaded
		 * before because the parent plug-in's are not activated.
		 */
		private void loadNotLoadedEventSources() {
			if (eventSourcesNotLoaded == null || eventSourcesNotLoaded.length == 0) return;

			List<Object> sources = new ArrayList<Object>();
			if (eventSources != null && eventSources.length > 0) sources.addAll(Arrays.asList(eventSources));

			List<ClassNotLoadedItem> notLoaded = new ArrayList<ClassNotLoadedItem>(Arrays.asList(eventSourcesNotLoaded));

			boolean changed = false;

			Iterator<ClassNotLoadedItem> it = notLoaded.iterator();
			while (it.hasNext()) {
				ClassNotLoadedItem item = it.next();
				Class<?> clazz = loadClass(item, "event source type"); //$NON-NLS-1$
				if (clazz != null) {
					it.remove();
					changed = true;
					if (!sources.contains(clazz)) sources.add(clazz);
				}
			}

			if (changed) {
				eventSources = sources.toArray(new Object[sources.size()]);
				eventSourcesNotLoaded = notLoaded.toArray(new ClassNotLoadedItem[notLoaded.size()]);
			}
		}

		/**
		 * Check whether the listener wants to be called for changes of the source.
		 * The check is made through <code>instanceof</code>.
		 *
		 * @param source The source of the event.
		 * @return True, if the source can be casted to one of the registered event source types
		 * 		   or no event sources are registered.
		 */
		protected boolean listensTo(EventObject event) {
			boolean types = ((eventTypes == null || eventTypes.length == 0) && (eventTypesNotLoaded == null || eventTypesNotLoaded.length == 0));
			boolean sources = ((eventSources == null || eventSources.length == 0) && (eventSourcesNotLoaded == null || eventSourcesNotLoaded.length == 0));

			if (!types) loadNotLoadedEventTypes();

			int t = 0;
			while (!types && eventTypes != null && t < eventTypes.length) {
				types = eventTypes[t].isInstance(event);
				t++;
			}

			if (!sources) loadNotLoadedEventSources();

			int s = 0;
			while (!sources && eventSources != null && s < eventSources.length) {
				Object eventSource = eventSources[s];
				if (eventSource instanceof Class<?>) {
					Class<?> eventSourceClass = (Class<?>)eventSource;
					sources = eventSourceClass.isInstance(event.getSource());
				} else {
					sources = eventSource == event.getSource();
				}
				s++;
			}

			return types && sources;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			if (getListener() != null) {
				return getListener().hashCode();
			}
		    return super.hashCode();
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ListenerListEntry) {
				ListenerListEntry other = (ListenerListEntry)obj;
				return this.getListener() == other.getListener();
			}
			return false;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return getClass().getName() + "{" + //$NON-NLS-1$
											"listener=" + listener + //$NON-NLS-1$
											",eventTypes=" + Arrays.deepToString(eventTypes) + //$NON-NLS-1$
											",eventTypesNotLoaded=" + Arrays.deepToString(eventTypesNotLoaded) + //$NON-NLS-1$
											",eventSources=" + Arrays.deepToString(eventSources) + //$NON-NLS-1$
											",eventSourcesNotLoaded=" + Arrays.deepToString(eventSourcesNotLoaded) + //$NON-NLS-1$
										  "}"; //$NON-NLS-1$
		}
	}

	/**
	 * eventType or eventSourceType extension elements which could not be loaded.
	 */
	private static class ClassNotLoadedItem {
		public final String bundleId;
		public final String className;
		public final String declaringBundleId;

		/**
         * Constructor
         *
         */
        public ClassNotLoadedItem(String bundleId, String className, String declaringBundleId) {
        	Assert.isNotNull(bundleId);
        	this.bundleId = bundleId;
        	Assert.isNotNull(className);
        	this.className = className;
        	Assert.isNotNull(declaringBundleId);
        	this.declaringBundleId = declaringBundleId;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
        	if (obj instanceof ClassNotLoadedItem) {
        		return bundleId.equals(((ClassNotLoadedItem)obj).bundleId)
        					&& className.equals(((ClassNotLoadedItem)obj).className)
        					&& declaringBundleId.equals(((ClassNotLoadedItem)obj).declaringBundleId);
        	}
            return false;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return bundleId.hashCode() ^ className.hashCode() ^ declaringBundleId.hashCode();
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
			return getClass().getName() + "{" + //$NON-NLS-1$
							"bundleId=" + bundleId + //$NON-NLS-1$
							",className=" + className + //$NON-NLS-1$
							",declaringBundleId=" + declaringBundleId + //$NON-NLS-1$
						  "}"; //$NON-NLS-1$
        }
	}

	/*
	 * Thread save singleton instance creation.
	 */
	private static class LazyInstance {
		public static EventManager instance = new EventManager();
	}

	/**
	 * Private Constructor.
	 */
	EventManager() {
		extensionPointProcessed = false;
	}

	/**
	 * Returns the singleton instance for the event manager.
	 */
	public static EventManager getInstance() {
		return LazyInstance.instance;
	}

	/**
	 * Add a change listener to listen to a single event.
	 *
	 * @param listener The listener to add.
	 * @param eventType The event type this listeners wants to be invoked.
	 */
	public void addEventListener(IEventListener listener, Class<?> eventType) {
		addEventListener(listener, eventType != null ? new Class[] { eventType } : null, null, null, null);
	}

	/**
	 * Add a change listener to listen to multiple events.
	 *
	 * @param listener The listener to add.
	 * @param eventTypes The event types this listeners wants to be invoked.
	 */
	public void addEventListener(IEventListener listener, Class<?>[] eventTypes) {
		addEventListener(listener, eventTypes, null, null, null);
	}

	/**
	 * Add a change listener to listen to event from the specified event
	 * source. If the listener instance had been registered already, the listener
	 * event sources are updated
	 *
	 * @param listener The listener to add.
	 * @param eventType The event type this listeners wants to be invoked.
	 * @param eventSource The event source type this listeners wants to be invoked.
	 */
	public void addEventListener(IEventListener listener, Class<?> eventType, Object eventSource) {
		addEventListener(listener, eventType != null ? new Class[] { eventType } : null, null, eventSource != null ? new Object[] { eventSource } : null, null);
	}

	/**
	 * Add a change listener to listen to events from the specified event
	 * sources. If the listener instance had been registered already, the listener
	 * event sources are updated
	 *
	 * @param listener The listener to add.
	 * @param eventType The event type this listeners wants to be invoked.
	 * @param eventSources The event sources type this listeners wants to be invoked.
	 */
	public void addEventListener(IEventListener listener, Class<?> eventType, Object[] eventSources) {
		addEventListener(listener, eventType != null ? new Class[] { eventType } : null, null, eventSources, null);
	}

	/**
	 * Add a change listener to listen to event from the specified event
	 * sources. If the listener instance had been registered already, the listener
	 * event sources are updated
	 *
	 * @param listener The listener to add.
	 * @param eventTypes The event types this listener wants to be invoked.
	 * @param eventTypesNotLoaded The event types this listener wants to be invoked but could not be loaded.
	 * @param eventSources The event source types this listener wants to be invoked.
	 * @param eventSourceTypesNotLoaded The event source types this listener wants to be invoked but could not be loaded.
	 */
	public void addEventListener(IEventListener listener, Class<?>[] eventTypes, ClassNotLoadedItem[] eventTypesNotLoaded, Object[] eventSources, ClassNotLoadedItem[] eventSourcesNotLoaded) {
		ListenerListEntry listEntry = new ListenerListEntry(listener, eventTypes, eventTypesNotLoaded, eventSources, eventSourcesNotLoaded);
		// We must assure that the existing list entries can _never_ change!
		synchronized (listeners) {
			if (listeners.contains(listEntry)) {
				listeners.remove(listEntry);
			}
			listeners.add(listEntry);
		}
	}

	/**
	 * Remove a change listener for all event types and sources.
	 *
	 * @param listener The listener to remove.
	 */
	public void removeEventListener(IEventListener listener) {
		ListenerListEntry listEntry = new ListenerListEntry(listener);
		listeners.remove(listEntry);
	}

	/**
	 * Remove all change listeners for all event types and sources.
	 */
	public void clear() {
		listeners.clear();
		synchronized (this) {
			extensionPointProcessed = false;
        }
	}

	/**
	 * Notify all registered listeners.
	 *
	 * @param event The event. Must not be <code>null</code>
	 */
	public void fireEvent(final EventObject event) {
		Assert.isNotNull(event);

		// In non-interactive mode, the notification events are suppressed.
		if (event instanceof NotifyEvent && !Host.isInteractive()) return;

		synchronized (this) {
			// if the extension point has not been processed till here, now we have to do
			if (!extensionPointProcessed) {
				addExtensionPointNotificationListeners();
				extensionPointProcessed = true;
			}
		}

		// Based on the current listener listener list, compile a list of event
		// listeners to where this event would have been send to in a synchronous invocation scheme.
		List<ListenerListEntry> affected = new ArrayList<ListenerListEntry>();

		// Get the array of registered event listeners.
		ListenerListEntry[] registered = listeners.toArray(new ListenerListEntry[listeners.size()]);

		for (ListenerListEntry listEntry : registered) {
			// ignore listeners not listening to the event type and source
			if (listEntry.listensTo(event)) {
				affected.add(listEntry);
			}
		}

		// If no current listener is affected, return now immediately
		if (affected.size() == 0) {
			return;
		}

		// Loop over the list of affected listeners and fire the event.
		// If the affected listener is a fire delegate -> use it itself to fire the event
		for (ListenerListEntry listEntry : affected) {
			if (!(listEntry.getListener() instanceof IEventListener)) {
				continue;
			}
			// Create the runnable to use for executing the event firing
			Runnable runnable = new FireRunnable((IEventListener)listEntry.getListener(), event);
			// Check on how to fire the runnable
			if (listEntry.getListener() instanceof IEventFireDelegate) {
				// The listener is a fire delegate -> use it itself to fire the runnable
				((IEventFireDelegate)listEntry.getListener()).fire(runnable);
			} else {
				// Listener isn't a fire delegate -> fire the runnable directly
				runnable.run();
			}
		}
	}

	/*
	 * Register change listeners defined by extension.
	 */
	private void addExtensionPointNotificationListeners() {
		IExtensionPoint ep = Platform.getExtensionRegistry().getExtensionPoint("org.eclipse.tcf.te.runtime.eventListeners"); //$NON-NLS-1$
		if (ep != null) {
			IExtension[] extensions = ep.getExtensions();
			if (extensions != null && extensions.length > 0) {
				for (IExtension extension : extensions) {
					IConfigurationElement[] configElements = extension.getConfigurationElements();
					if (configElements != null && configElements.length > 0) {
						for (IConfigurationElement configElement : configElements) {
							String name = configElement.getName();
							if ("eventListener".equals(name)) { //$NON-NLS-1$
								// try to read the "eventType" and "eventSourceType" configuration elements if any.
								List<Class<?>> eventTypes = new ArrayList<Class<?>>();
								List<Class<?>> eventSourceTypes = new ArrayList<Class<?>>();

								List<ClassNotLoadedItem> eventTypesNotLoaded = new ArrayList<ClassNotLoadedItem>();
								List<ClassNotLoadedItem> eventSourceTypesNotLoaded = new ArrayList<ClassNotLoadedItem>();

								IConfigurationElement[] children = configElement.getChildren();
								for (IConfigurationElement child : children) {
									if ("eventType".equals(child.getName())) { //$NON-NLS-1$
										String className = child.getAttribute("class"); //$NON-NLS-1$
										if (className == null || className.trim().length() == 0) {
											continue;
										}

										String bundleId = child.getAttribute("bundleId"); //$NON-NLS-1$

										// If a bundle id got specified, use the specified bundle to load the service class
										Bundle bundle = bundleId != null ? Platform.getBundle(bundleId) : null;
										// If we don't have a bundle to load from yet, fallback to the declaring bundle
										if (bundle == null) bundle = Platform.getBundle(child.getDeclaringExtension().getNamespaceIdentifier());
										// And finally, use our own bundle to load the class. This fallback is expected
										// to never be used.
										if (bundle == null) bundle = CoreBundleActivator.getContext().getBundle();

										// If the specified bundle is active, or "forceBundleActivation" is true, the class can be loaded
										if (bundle != null && bundle.getState() == Bundle.ACTIVE) {
											try {
												Class<?> eventType = bundle.loadClass(className);
												if (eventType != null && !eventTypes.contains(eventType)) {
													eventTypes.add(eventType);
												}
											} catch (Exception ex) {
												if (isTracingEnabled())
													CoreBundleActivator.getTraceHandler().trace("Error instantiating event listener event type object instance: " + child.getAttribute("class"), //$NON-NLS-1$ //$NON-NLS-2$
																	0, ITraceIds.TRACE_EVENTS, IStatus.ERROR, this);
											}
										}
										// If the bundle could not be found or is not yet active, don't try to load the class
										else {
											ClassNotLoadedItem item = new ClassNotLoadedItem(bundleId, className, child.getDeclaringExtension().getNamespaceIdentifier());
											if (!eventTypesNotLoaded.contains(item)) eventTypesNotLoaded.add(item);
										}
									}

									if ("eventSourceType".equals(child.getName())) { //$NON-NLS-1$
										String className = child.getAttribute("class"); //$NON-NLS-1$
										if (className == null || className.trim().length() == 0) {
											continue;
										}

										String bundleId = child.getAttribute("bundleId"); //$NON-NLS-1$

										// If a bundle id got specified, use the specified bundle to load the service class
										Bundle bundle = bundleId != null ? Platform.getBundle(bundleId) : null;
										// If we don't have a bundle to load from yet, fallback to the declaring bundle
										if (bundle == null) bundle = Platform.getBundle(child.getDeclaringExtension().getNamespaceIdentifier());
										// And finally, use our own bundle to load the class. This fallback is expected
										// to never be used.
										if (bundle == null) bundle = CoreBundleActivator.getContext().getBundle();

										// If the specified bundle is active, the class can be loaded
										if (bundle != null && bundle.getState() == Bundle.ACTIVE) {
											try {
												Class<?> eventSourceType = bundle.loadClass(className);
												if (eventSourceType != null && !eventSourceTypes.contains(eventSourceType)) {
													eventSourceTypes.add(eventSourceType);
												}
											} catch (Exception ex) {
												if (isTracingEnabled())
													CoreBundleActivator.getTraceHandler().trace("Error instantiating event listener event source type object instance: " + child.getAttribute("class"), //$NON-NLS-1$ //$NON-NLS-2$
																	0, ITraceIds.TRACE_EVENTS, IStatus.ERROR, this);
											}
										}
										// If the bundle could not be found or is not yet active, don't try to load the class
										else {
											ClassNotLoadedItem item = new ClassNotLoadedItem(bundleId, className, child.getDeclaringExtension().getNamespaceIdentifier());
											if (!eventSourceTypesNotLoaded.contains(item)) eventSourceTypesNotLoaded.add(item);
										}
									}
								}

								// For extension point contributed event listeners, we use delegating
								// event listener instances
								IEventListener listener = new EventListenerProxy(configElement);
								addEventListener(listener,
								                 !eventTypes.isEmpty() ? eventTypes.toArray(new Class[eventTypes.size()]) : null,
								                 !eventTypesNotLoaded.isEmpty() ? eventTypesNotLoaded.toArray(new ClassNotLoadedItem[eventTypesNotLoaded.size()]) : null,
								                 !eventSourceTypes.isEmpty() ? eventSourceTypes.toArray(new Class[eventSourceTypes.size()]) : null,
												 !eventSourceTypesNotLoaded.isEmpty() ? eventSourceTypesNotLoaded.toArray(new ClassNotLoadedItem[eventSourceTypesNotLoaded.size()]) : null
								                );

								if (isTracingEnabled())
									CoreBundleActivator.getTraceHandler().trace("Add extension point change listener: " + configElement.getAttribute("class"), //$NON-NLS-1$ //$NON-NLS-2$
									                                            0, ITraceIds.TRACE_EVENTS, IStatus.INFO, this);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Internal class used to delay the instantiation and plug-in activation of
	 * event listeners which are contributed via extension point till they
	 * are really fired.
	 */
	private class EventListenerProxy implements IEventListener, IEventFireDelegate {
		private final IConfigurationElement configElement;
		private IEventListener delegate;

		/**
		 * Constructor.
		 *
		 * @param configElement The contributing configuration element of the encapsulated event listener.
		 *                      Must not be <code>null</code>.
		 */
		public EventListenerProxy(IConfigurationElement configElement) {
			Assert.isNotNull(configElement);
			this.configElement = configElement;
			delegate = null;
		}

		/**
		 * Returns the event listener delegate and instantiate the delegate
		 * if not yet done.
		 *
		 * @return The event listener delegate or <code>null</code> if the instantiation fails.
		 */
		private IEventListener getDelegate() {
			if (delegate == null) {
				// Check the contributing plug-in state
				boolean forcePluginActivation = Boolean.parseBoolean(configElement.getAttribute("forcePluginActivation")); //$NON-NLS-1$
				if (!forcePluginActivation) {
					Bundle bundle = Platform.getBundle(configElement.getContributor().getName());
					forcePluginActivation = bundle != null ? bundle.getState() == Bundle.ACTIVE : false;
				}
				// Load the event listener implementation class if plug-in activations is allowed.
				if (forcePluginActivation) {
					try {
						Object executable = configElement.createExecutableExtension("class"); //$NON-NLS-1$
						if (executable instanceof IEventListener) {
							delegate = (IEventListener)executable;
						}
					} catch (Exception ex) {
						if (isTracingEnabled())
							CoreBundleActivator.getTraceHandler().trace("Error instantiating extension point event listener: " + configElement.getAttribute("class") //$NON-NLS-1$ //$NON-NLS-2$
                                    										+ "(Possible Cause: " + ex.getLocalizedMessage() + ")", //$NON-NLS-1$ //$NON-NLS-2$
                                    										0, ITraceIds.TRACE_EVENTS, IStatus.ERROR, this);
					}
				}
			}

			return delegate;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.runtime.interfaces.events.IEventListener#eventFired(java.util.EventObject)
		 */
		@Override
		public void eventFired(EventObject event) {
			Assert.isNotNull(event);
			// Get the delegate (may force instantiation)
			IEventListener delegate = getDelegate();
			// And pass on the event to the delegate if we got a valid delegate
			if (delegate != null) delegate.eventFired(event);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.runtime.interfaces.events.IEventFireDelegate#fire(java.lang.Runnable)
		 */
		@Override
		public void fire(Runnable runnable) {
			Assert.isNotNull(runnable);
			// Pass on to the delegate if the delegate itself is an fire delegate,
			if (getDelegate() instanceof IEventFireDelegate) {
				((IEventFireDelegate)getDelegate()).fire(runnable);
			}
			else {
				runnable.run();
			}
		}
	}

	/**
	 * Return <code>true</code> if the tracing mode is enabled for the
	 * event manager and trace messages shall be printed to the debug console.
	 */
	public static boolean isTracingEnabled() {
		return CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_EVENTS);
	}

}
