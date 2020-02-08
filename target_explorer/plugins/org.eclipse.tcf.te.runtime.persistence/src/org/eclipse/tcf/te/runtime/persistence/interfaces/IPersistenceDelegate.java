/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.persistence.interfaces;

import java.io.IOException;

import org.eclipse.tcf.te.runtime.interfaces.extensions.IExecutableExtension;

/**
 * Interface to be implemented by persistence delegates.
 */
public interface IPersistenceDelegate extends IExecutableExtension {

	/**
	 * Writes the given context to the given persistence container.
	 * If the container does not exist yet, the class needs to be given.
	 *
	 * @param context The context to persist. Must not be <code>null</code>.
	 * @param container The persistence container or class for a new one. Must not be <code>null</code>.
	 *
	 * @return The new or updated container instance.
	 */
	public Object write(Object context, Object container) throws IOException;

	/**
	 * Writes the given list of context objects to the given persistence container.
	 * If the container does not exist yet, the class needs to be given.
	 *
	 * @param context The list of contexts to persist. Must not be <code>null</code>.
	 * @param container The persistence container or class for a new one. Must not be <code>null</code>.
	 *
	 * @return The new or updated container instance.
	 */
	public Object writeList(Object[] context, Object container) throws IOException;

	/**
	 * Get the class or interface for the context.
	 *
	 * @param context The context to persist. Must not be <code>null</code>.
	 * @return The class or interface for the given context.
	 */
	public Class<?> getPersistedClass(Object context);

	/**
	 * Reads the context from the given persistence container.
	 * If the context does not exist yet, the class needs to be given.
	 *
	 * @param context The context to update or class for a new context or <code>null</code>.
	 * @param container The persistence container. Must not be <code>null</code>.
	 *
	 * @return The new or updated context instance.
	 */
	public Object read(Object context, Object container) throws IOException;

	/**
	 * Reads a list of context objects from the given persistence container.
	 *
	 * @param contextClass The class type of the context objects to read or <code>null</code>.
	 * @param container The persistence container. Must not be <code>null</code>.
	 *
	 * @return The list of context objects. Might be empty.
	 */
	public Object[] readList(Class<?> contextClass, Object container) throws IOException;

	/**
	 * Deletes the given context inside the container or the whole container.
	 *
	 * @param context The context to delete inside the storage or <code>null</code>.
	 * @param container The persistence container. Must not be <code>null</code>.
	 *
	 * @return <code>True</code> if the persistence context or the whole storage was successfully deleted;
	 *         <code>false</code> otherwise.
	 */
	public boolean delete(Object context, Object container) throws IOException;
}
