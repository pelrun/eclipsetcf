/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.core.nls;

import java.lang.reflect.Field;

import org.eclipse.osgi.util.NLS;

/**
 * Target Explorer Core plugin externalized strings management.
 */
public class Messages extends NLS {

	// The plug-in resource bundle name
	private static final String BUNDLE_NAME = "org.eclipse.tcf.te.core.nls.Messages"; //$NON-NLS-1$

	/**
	 * Static constructor.
	 */
	static {
		// Load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	/**
	 * Returns the corresponding string for the given externalized strings
	 * key or <code>null</code> if the key does not exist.
	 *
	 * @param key The externalized strings key or <code>null</code>.
	 * @return The corresponding string or <code>null</code>.
	 */
	public static String getString(String key) {
		if (key != null) {
			try {
				Field field = Messages.class.getDeclaredField(key);
				return (String)field.get(null);
			} catch (Exception e) { /* ignored on purpose */ }
		}

		return null;
	}

	/**
	 * Returns the string representation of the given connect state.
	 *
	 * @param state The connect state.
	 * @return The string representation of the state or <code>null</code>.
	 */
	public static String getConnectStateString(int state) {
		String key = "Connectable_state_"; //$NON-NLS-1$
		if (state < 0) key += "minus_"; //$NON-NLS-1$
		key += Integer.toString(Math.abs(state));
		return getString(key);
	}

	// **** Declare externalized string id's down here *****

	public static String ModelNodePersistableAdapter_export_invalidPersistable;
	public static String ModelNodePersistableAdapter_export_unknownType;
	public static String ModelNodePersistableAdapter_import_invalidReference;
	public static String ModelNodePersistableAdapter_import_cannotLoadClass;

	public static String Connectable_state_0;
	public static String Connectable_state_1;
	public static String Connectable_state_11;
	public static String Connectable_state_21;
	public static String Connectable_state_minus_1;
	public static String Connectable_state_minus_2;
	public static String Connectable_state_minus_11;
	public static String Connectable_state_minus_12;
	public static String Connectable_state_minus_21;
}
