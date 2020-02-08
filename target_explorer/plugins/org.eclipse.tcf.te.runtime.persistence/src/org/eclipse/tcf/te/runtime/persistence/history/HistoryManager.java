/*******************************************************************************
 * Copyright (c) 2012 - 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.persistence.history;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.persistence.activator.CoreBundleActivator;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IURIPersistenceService;
import org.eclipse.tcf.te.runtime.services.ServiceManager;

/**
 * History manager implementation.
 */
public class HistoryManager {
	// the maximum length of the history per id
	private final static int HISTORY_LENGTH = 5;

	// The map maintaining the history
	private Map<String, List<String>> history = new HashMap<String, List<String>>();

	/*
	 * Thread save singleton instance creation.
	 */
	private static class LazyInstance {
		public static HistoryManager instance = new HistoryManager();
	}

	/**
	 * Returns the singleton instance of the history point manager.
	 */
	public static HistoryManager getInstance() {
		return LazyInstance.instance;
	}

	/**
	 * Constructor.
	 */
	HistoryManager() {
		super();
		initialize();
	}

	/**
	 * Initialize the history manager.
	 */
	@SuppressWarnings("unchecked")
    private void initialize() {
		history.clear();
		try {
			// Get the persistence service
			IURIPersistenceService uRIPersistenceService = ServiceManager.getInstance().getService(IURIPersistenceService.class);
			if (uRIPersistenceService == null) {
				throw new IOException("Persistence service instance unavailable."); //$NON-NLS-1$
			}
			// Save the history to the persistence storage
			history = (Map<String,List<String>>)uRIPersistenceService.read(history, getURI());
		} catch (IOException e) {
		}
	}

	// Get the URI for history persistence
	private URI getURI() {
		IPath pluginPath = CoreBundleActivator.getDefault().getStateLocation();
		pluginPath = pluginPath.append(".history"); //$NON-NLS-1$

		return pluginPath.toFile().toURI();
	}

	/**
	 * Write the history to disk.
	 */
	public void flush() {
		synchronized (history) {
			try {
				// Get the persistence service
				IURIPersistenceService uRIPersistenceService = ServiceManager.getInstance().getService(IURIPersistenceService.class);
				if (uRIPersistenceService == null) {
					throw new IOException("Persistence service instance unavailable."); //$NON-NLS-1$
				}
				// Save the history to the persistence storage
				uRIPersistenceService.write(history, getURI());
			} catch (IOException e) {
			}
        }
	}

	/**
	 * Get the history for a given history id.
	 * @param historyId The history id.
	 * @return The list of entries within the history ids list or an empty list.
	 */
	public String[] getHistory(String historyId) {
		Assert.isNotNull(historyId);

		List<String> entries = history.get(historyId);
		if (entries == null) {
			entries = new ArrayList<String>();
		}

		return entries.toArray(new String[entries.size()]);
	}

	/**
	 * Get all history ids matching the given id pattern.
	 * @param historyIdPattern The history id regex pattern.
	 * @return The list of history ids matching the given pattern or an empty list.
	 */
	public String[] getMatchingHistoryIds(String historyIdPattern) {
		Assert.isNotNull(historyIdPattern);

		List<String> historyIds = new ArrayList<String>();
		for (String historyId : history.keySet()) {
	        if (historyId.matches(historyIdPattern)) {
	        	historyIds.add(historyId);
	        }
        }

		return historyIds.toArray(new String[historyIds.size()]);
	}

	/**
	 * Get the fist entry of a history ids list.
	 * @param historyId The history id.
	 * @return The first entry for the given history ids or null if no history is available for that id.
	 */
	public String getFirst(String historyId) {
		String[] history = getHistory(historyId);
		return history.length > 0 ? history[0] : null;
	}

	/**
	 * Add a new history entry to the top of the history ids list.
	 * If the list size exceeds the HISTORY_LENGTH, the last element of the list will be removed.
	 * @param historyId The history id.
	 * @param entry The entry to be added to the top of history ids list.
	 * @return <code>true</code> if the id
	 */
	public boolean add(String historyId, String entry) {
		return add(historyId, entry, HISTORY_LENGTH);
	}

	/**
	 * Add a new history entry to the top of the history ids list.
	 * If the list size exceeds the HISTORY_LENGTH, the last element of the list will be removed.
	 * @param historyId The history id.
	 * @param entry The id to be added to the top of history ids list.
	 * @param historyLength The maximum length of the history.
	 * @return <code>true</code> if the entry was added
	 */
	public boolean add(String historyId, String entry, int historyLength) {
		Assert.isNotNull(historyId);
		Assert.isNotNull(entry);
		Assert.isTrue(historyLength > 0);

		List<String> ids = history.get(historyId);
		if (ids == null) {
			ids = new ArrayList<String>();
			history.put(historyId, ids);
		}
		if (ids.contains(entry)) {
			ids.remove(entry);
		}

		ids.add(0, entry);

		while (ids.size() > historyLength) {
			ids.remove(historyLength);
		}

		flush();

		EventManager.getInstance().fireEvent(new ChangeEvent(this, ChangeEvent.ID_ADDED, historyId, historyId));

		return true;
	}

	/**
	 * Set a new list of history entries to the history ids list.
	 * If the list size exceeds the HISTORY_LENGTH, the last element of the list will be removed.
	 * @param historyId The history id.
	 * @param entries The entries to be set to the history ids list.
	 * @return <code>true</code> if the entries were added
	 */
	public void set(String historyId, String[] entries) {
		set(historyId, entries, HISTORY_LENGTH);
	}

	/**
	 * Set a new list of history entries to the history ids list.
	 * If the list size exceeds the HISTORY_LENGTH, the last element of the list will be removed.
	 * @param historyId The history id.
	 * @param entries The entries to be set to the history ids list.
	 * @param historyLength The maximum length of the history.
	 * @return <code>true</code> if the entries were set
	 */
	public void set(String historyId, String[] entries, int historyLength) {
		Assert.isNotNull(historyId);
		Assert.isNotNull(entries);

		history.put(historyId, Arrays.asList(entries));
		List<String> newIds = history.get(historyId);

		while (newIds.size() > historyLength) {
			newIds.remove(historyLength);
		}

		flush();

		EventManager.getInstance().fireEvent(new ChangeEvent(this, ChangeEvent.ID_CHANGED, historyId, historyId));
	}

	/**
	 * Remove a id from the history ids list.
	 * @param historyId The history id.
	 * @param entry The entry to be removed from the history ids list.
	 * @return <code>true</code> if the entry was removed from the history ids list.
	 */
	public boolean remove(String historyId, String entry) {
		Assert.isNotNull(historyId);
		Assert.isNotNull(entry);

		boolean removed = false;

		List<String> ids = history.get(historyId);
		if (ids != null) {
			removed |= ids.remove(entry);
			if (ids.isEmpty()) {
				history.remove(historyId);
			}
		}

		if (removed) {
			flush();
			EventManager.getInstance().fireEvent(new ChangeEvent(this, ChangeEvent.ID_REMOVED, historyId, historyId));
		}

		return removed;
	}

	/**
	 * Remove all ids from the history ids list.
	 * @param historyId The history id.
	 **/
	public void clear(String historyId) {
		List<String> entries = null;
		if (historyId == null) {
			history.clear();
		}
		else {
			entries = history.remove(historyId);
		}

		if (entries != null || historyId == null) {
			flush();
			EventManager.getInstance().fireEvent(new ChangeEvent(this, ChangeEvent.ID_REMOVED, historyId, historyId));
		}
	}

}
