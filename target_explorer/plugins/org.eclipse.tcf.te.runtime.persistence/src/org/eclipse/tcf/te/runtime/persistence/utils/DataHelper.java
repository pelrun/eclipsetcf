/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.persistence.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.persistence.PersistenceManager;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;

/**
 * Data helper for de/encoding.
 */
public class DataHelper {

	/**
	 * Encode a properties container to a string.
	 * @param data The properties container.
	 * @return String representing the properties container.
	 */
	public static final String encodePropertiesContainer(IPropertiesContainer data) {
		try {
			if (data != null && !data.isEmpty()) {
				IPersistenceDelegate delegate = PersistenceManager.getInstance().getDelegate(Map.class, String.class);
				return (String)delegate.write(data, String.class);
			}
		}
		catch (Exception e) {
		}
		return null;
	}

	/**
	 * Decode a string encoded properties container.
	 * @param encoded The string encoded properties container.
	 * @return Properties container.
	 */
	public static final IPropertiesContainer decodePropertiesContainer(String encoded) {
		if (encoded != null && encoded.trim().length() > 0) {
			try {
				IPersistenceDelegate delegate = PersistenceManager.getInstance().getDelegate(Map.class, String.class);
				return (IPropertiesContainer)delegate.read(IPropertiesContainer.class, encoded);
			}
			catch (Exception e) {
			}
		}
		return new PropertiesContainer();
	}

	/**
	 * Convert all keys of the map to lower case.
	 * @param list The list of maps to convert.
	 * @return List with new maps with lowercase keys.
	 */
	public static List<Map<String,Object>> keysToLowerCase(List<Map<String,Object>> list) {
		List<Map<String, Object>> paramListLowerCase = new ArrayList<Map<String,Object>>();
		for (Map<String, Object> map : list) {
			paramListLowerCase.add(keysToLowerCase(map));
		}
		return paramListLowerCase;
	}

	/**
	 * Convert all keys of the map to lower case.
	 * @param map The map to convert.
	 * @return New map with lowercase keys.
	 */
	@SuppressWarnings("unchecked")
	public static Map<String,Object> keysToLowerCase(Map<String,Object> map) {
		Map<String, Object> mapLowerCase = new HashMap<String, Object>();
		for (String key : map.keySet()) {
			Object value = map.get(key);
			if (value instanceof Map) {
				value = keysToLowerCase((Map<String,Object>)value);
			}
			else if (value instanceof List) {
				value = keysToLowerCase((List<Map<String,Object>>)value);
			}
			mapLowerCase.put(key.toLowerCase(), value);
		}
		return mapLowerCase;
	}
}
