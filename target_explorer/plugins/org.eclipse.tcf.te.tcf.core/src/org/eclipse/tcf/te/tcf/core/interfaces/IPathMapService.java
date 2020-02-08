/*******************************************************************************
 * Copyright (c) 2013, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.interfaces;

import org.eclipse.tcf.services.IPathMap;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;

/**
 * Path map service.
 * <p>
 * Allow the access and manipulation of the configured (object) path maps
 * for a given context.
 */
public interface IPathMapService extends IService {
	/**
	 * Protocol used to mark path map rules to be used to map host paths to target paths.
	 */
	public final static String PATHMAP_PROTOCOL_HOST_TO_TARGET = IPathMap.PROTOCOL_TARGET;

	/**
	 * Name used to label the source path mapping container providing the generated
	 * source path mapping rules.
	 */
	public final static String SOURCE_PATH_MAPPING_CONTAINER_NAME = "Generated Mappings"; //$NON-NLS-1$

	/**
	 * Generates the source path mappings for the given context.
	 * <p>
	 * <b>Note:</b> This method must be called from outside the TCF event dispatch thread.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 */
	public void generateSourcePathMappings(Object context);

	/**
	 * Return the configured (object) path mappings for the given context.
	 * <p>
	 * <b>Note:</b> This method must be called from outside the TCF event dispatch thread.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @return The configured path map or <code>null</code>.
	 */
	public IPathMap.PathMapRule[] getPathMap(Object context);

	/**
	 * Returns the configured shared path mapping rules for the given context.
	 * <p>
	 * 
	 * @param context The context. Must not be <code>null</code>.
	 * @return The configured shared path map or <code>null</code>.
	 */
	public IPathMap.PathMapRule[] getSharedPathMapRules(Object context);
	
	/**
	 * Adds a new path mapping rule to the configured (object) path mapping for the
	 * given context.
	 * <p>
	 * The method will check the path mappings if a path map rule for the given source
	 * and destination already exist. If this is the case, the method will do nothing
	 * and returns the existing path map rule.
	 * <p>
	 * The method auto applies the new path map to an possibly open shared channel.
	 * <p>
	 * <b>Note:</b> This method must be called from outside the TCF event dispatch thread.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param source The path map rule source attribute value. Must not be <code>null</code>.
	 * @param destination The path map rule destination attribute value. Must not be <code>null</code>.
	 *
	 * @return The path map rule object representing the added path map rule.
	 */
	public IPathMap.PathMapRule addPathMap(Object context, String source, String destination);

	/**
	 * Adds new path mapping rules to the configured (object) path mapping for the
	 * given context.
	 * <p>
	 * The method will check the path mappings if path map rules given already exist. If
	 * this is the case, the method will do nothing for them and it will add just the
	 * new ones.
	 *
	 * <p>
	 * The method auto applies the new path maps to an possibly open shared channel.
	 * <p>
	 * <b>Note:</b> This method must be called from outside the TCF event dispatch thread.
	 *
	 * @param context
	 * @param rules
	 */
	public void addPathMap(Object context, IPathMap.PathMapRule[] rules);

	/**
	 * Adds new shared path mapping rules to the configured (object) path mapping for the
	 * given context.
	 * <p>
	 * The method will check the path mappings if path map rules given already exist. If
	 * this is the case, the method will do nothing for them and it will add just the
	 * new ones.
	 *
	 * <p>
	 * The method auto applies the new path map to an possibly open shared channel.
	 * <p>
	 * <b>Note:</b> This method must be called from outside the TCF event dispatch thread.
	 *
	 * @param context
	 * @param rules
	 */
	public void addSharedPathMapRules(Object context, IPathMap.PathMapRule[] rules);

	/**
	 * Removes the given path mapping rule from the configured (object) path mappings
	 * for the given context.
	 * <p>
	 * The method auto applies the new path map to an possibly open shared channel.
	 * <p>
	 * <b>Note:</b> This method must be called from outside the TCF event dispatch thread.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param rule The path map rule. Must not be <code>null</code>.
	 */
	public void removePathMap(Object context, IPathMap.PathMapRule rule);

	/**
	 * Removes the given path mapping rules from the configured (object) path mappings
	 * for the given context.
	 * <p>
	 * The method auto applies the new path maps to an possibly open shared channel.
	 * <p>
	 * <b>Note:</b> This method must be called from outside the TCF event dispatch thread.
	 *
	 * @param context
	 * @param rules
	 */
	public void removePathMap(Object context, IPathMap.PathMapRule[] rules);

	/**
	 * Removes all the shared path map rules for the given context.
	 * <p>
	 * The method auto applies the new path map to an possibly open shared channel.
	 * <p>
	 * <b>Note:</b> This method must be called from outside the TCF event dispatch thread.
	 *
	 * @param context
	 */
	public void cleanSharedPathMapRules(Object context);

	/**
	 * Apply the configured (object) path mappings to the given context.
	 * <p>
	 * <b>Note:</b> This method must be called from outside the TCF event dispatch thread.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param force If <code>true</code>, the path map will be set even if it appears not to be
	 *              different from the path map already set.
	 * @param forceEmpty If <code>true</code>, the path map will be set even if empty.
	 * @param callback The callback to invoke once the operation completed. Must not be <code>null</code>.
	 */
	public void applyPathMap(Object context, boolean force, boolean forceEmpty, ICallback callback);

	/**
	 * Returns the current client ID used to identify path map rules handled
	 * by the current Eclipse instance.
	 *
	 * @return The current client ID.
	 */
	public String getClientID();
}
