/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.runtime.persistence.delegates;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.tcf.te.runtime.extensions.ExecutableExtension;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.persistence.PersistenceManager;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IVariableDelegate;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * GsonMapPersistenceDelegate
 */
public class GsonMapPersistenceDelegate extends ExecutableExtension implements IPersistenceDelegate {

	private final String defaultFileExtension;

	protected static final String VARIABLES = "__VariablesMap__"; //$NON-NLS-1$

	private final Gson gson = new GsonBuilder().setPrettyPrinting().enableComplexMapKeySerialization().create();

	/**
	 * Constructor.
	 */
	public GsonMapPersistenceDelegate() {
		this("json"); //$NON-NLS-1$
	}

	/**
	 * Constructor.
	 */
	public GsonMapPersistenceDelegate(String defaultFileExtension) {
		super();
		Assert.isNotNull(defaultFileExtension);
		this.defaultFileExtension = defaultFileExtension;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate#getPersistedClass(java.lang.Object)
	 */
	@Override
	public Class<?> getPersistedClass(Object context) {
		return Map.class;
	}

	/**
	 * Return the default file extension if container is an URI.
	 */
	protected String getDefaultFileExtension() {
		return defaultFileExtension;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate#writeList(java.lang.Object[], java.lang.Object)
	 */
	@Override
	public Object writeList(Object[] context, Object container) throws IOException {
		Assert.isNotNull(context);
		Assert.isNotNull(container);

		return write(context, container, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate#write(java.lang.Object, java.lang.Object)
	 */
	@Override
	public final Object write(Object context, Object container) throws IOException {
		Assert.isNotNull(context);
		Assert.isNotNull(container);

		return write(context, container, false);
	}

	private Object write(Object context, Object container, boolean isList) throws IOException {
		Assert.isNotNull(context);
		Assert.isNotNull(container);

		if (container instanceof URI) {
			URI uri = (URI) container;

			// Only "file:" URIs are supported
			if (!"file".equalsIgnoreCase(uri.getScheme())) { //$NON-NLS-1$
				throw new IOException("Unsupported URI schema '" + uri.getScheme() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			// Create the file object from the given URI
			File file = new File(uri.normalize());

			// The file must be absolute
			if (!file.isAbsolute()) {
				throw new IOException("URI must denote an absolute file path."); //$NON-NLS-1$
			}

			// If the file defaultFileExtension is no set, default to "properties"
			IPath path = new Path(file.getCanonicalPath());
			if (path.getFileExtension() == null) {
				file = path.addFileExtension(getDefaultFileExtension()).toFile();
			}

//			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8"); //$NON-NLS-1$
			if (!isList) {
				try {
					gson.toJson(internalToMap(context), Map.class, writer);
				}
				finally {
					writer.close();
				}
			}
			else {
				List<String> encoded = new ArrayList<String>();
				for (Object entry : (Object[]) context) {
					encoded.add(gson.toJson(internalToMap(entry)));
				}
				try {
					gson.toJson(encoded, List.class, writer);
				}
				finally {
					writer.close();
				}
			}
		}
		else if (String.class.equals(container)) {
//			Gson gson = new GsonBuilder().create();

			if (!isList) {
				container = gson.toJson(internalToMap(context));
			}
			else {
				List<String> encoded = new ArrayList<String>();
				for (Object entry : (Object[]) context) {
					encoded.add(gson.toJson(internalToMap(entry)));
				}
				container = gson.toJson(encoded);
			}
		}

		return container;
	}

	/*
	 * Convert the context to a Map, extract and use variables and add them to the map as key
	 * VARIABLE.
	 */
	private Map<String, Object> internalToMap(Object context) {
		try {
			Map<String, Object> data = toMap(context);

			if (data != null) {
				Map<String, String> variables = null;
				IVariableDelegate[] delegates = PersistenceManager.getInstance().getVariableDelegates(this);
				for (IVariableDelegate delegate : delegates) {
					variables = delegate.getVariables(data);
				}
				if (variables != null && !variables.isEmpty()) {
					data.put(VARIABLES, variables);
				}
			}
			Map<String, Object> sorted = new TreeMap<String, Object>(data);
			return sorted;
		}
		catch (Exception e) {

		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate#readList(java.lang.Class, java.lang.Object)
	 */
	@Override
	public Object[] readList(Class<?> contextClass, Object container) throws IOException {
		Assert.isNotNull(container);
		return (Object[])read(contextClass, container, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate#read(java.lang.Object, java.lang.Object)
	 */
	@Override
	public final Object read(Object context, Object container) throws IOException {
		Assert.isNotNull(container);
		return read(context, container, false);
	}

	@SuppressWarnings("unchecked")
    private Object read(Object context, Object container, boolean isList) throws IOException {
		Assert.isNotNull(container);

//		Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();

		if (container instanceof URI) {
			URI uri = (URI) container;

			// Only "file:" URIs are supported
			if (!"file".equalsIgnoreCase(uri.getScheme())) { //$NON-NLS-1$
				throw new IOException("Unsupported URI schema '" + uri.getScheme() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			// Create the file object from the given URI
			File file = new File(uri.normalize());

			// The file must be absolute
			if (!file.isAbsolute()) {
				throw new IOException("URI must denote an absolute file path."); //$NON-NLS-1$
			}

			if (!file.exists()) {
				IPath path = new Path(file.getCanonicalPath());
				if (path.getFileExtension() == null) {
					file = path.addFileExtension(getDefaultFileExtension()).toFile();
				}
			}

			Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8"); //$NON-NLS-1$
			if (!isList) {
				try {
					Map<String,Object> read = gson.fromJson(reader, Map.class);
					data.add(read);
				}
				finally {
					reader.close();
				}
			}
			else {
				try {
					List<String> strings = gson.fromJson(reader, List.class);
					for (String string : strings) {
						Map<String,Object> read = gson.fromJson(string, Map.class);
						data.add(read);
					}
				}
				finally {
					reader.close();
				}
			}
		}
		else if (container instanceof String) {
			if (!isList) {
				data.add(gson.fromJson((String) container, Map.class));
			}
			else {
				List<String> strings = gson.fromJson((String) container, List.class);
				for (String string : strings) {
					data.add(gson.fromJson(string, Map.class));
				}
			}
		}

		for (Map<String, Object> entry : data) {
			if (entry != null) {
				Map<String, String> variables = new HashMap<String, String>();
				if (entry.containsKey(VARIABLES)) {
					variables = (Map<String, String>) entry.remove(VARIABLES);
				}
				IVariableDelegate[] delegates = PersistenceManager.getInstance().getVariableDelegates(this);
				for (IVariableDelegate delegate : delegates) {
					entry = delegate.putVariables(entry, variables);
				}
			}
		}

		if (!isList) {
			return !data.isEmpty() && data.get(0) != null ? fromMap(data.get(0), context) : context;
		}

		List<Object> list = new ArrayList<Object>();
		for (Map<String, Object> entry : data) {
			list.add(fromMap(entry, context));
		}
		return list.toArray();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate#delete(java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean delete(Object context, Object container) throws IOException {
		Assert.isNotNull(container);

		if (container instanceof URI) {
			URI uri = (URI) container;

			// Only "file:" URIs are supported
			if (!"file".equalsIgnoreCase(uri.getScheme())) { //$NON-NLS-1$
				throw new IOException("Unsupported URI schema '" + uri.getScheme() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			// Create the file object from the given URI
			File file = new File(uri.normalize());

			// The file must be absolute
			if (!file.isAbsolute()) {
				throw new IOException("URI must denote an absolute file path."); //$NON-NLS-1$
			}

			if (!file.exists()) {
				IPath path = new Path(file.getCanonicalPath());
				if (path.getFileExtension() == null) {
					file = path.addFileExtension(getDefaultFileExtension()).toFile();
				}
			}

			// If the file defaultFileExtension is no set, default to "properties"
			IPath path = new Path(file.getCanonicalPath());
			if (path.getFileExtension() == null) {
				file = path.addFileExtension(getDefaultFileExtension()).toFile();
			}

			return file.delete();
		}

		return false;
	}

	/**
	 * Convert the given context to map.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @return Map representing the context.
	 *
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, Object> toMap(final Object context) throws IOException {
		Map<String, Object> result = new HashMap<String, Object>();

		Map<String, Object> attrs = null;
		if (context instanceof Map) {
			attrs = (Map<String, Object>) context;
		}
		else if (context instanceof IPropertiesContainer) {
			IPropertiesContainer container = (IPropertiesContainer) context;
			attrs = new HashMap<String, Object>(container.getProperties());
		}

		if (attrs != null) {
			for (Entry<String, Object> entry : attrs.entrySet()) {
				if (!entry.getKey().endsWith(".transient")) { //$NON-NLS-1$
					result.put(entry.getKey(), entry.getValue());
				}
			}
		}

		return result;
	}

	/**
	 * Convert a map into the needed context object.
	 *
	 * @param map The map representing the context. Must not be <code>null</code>.
	 * @param context The context to put the map values in or <code>null</code>.
	 * @return The context object.
	 *
	 * @throws IOException
	 */
	protected Object fromMap(Map<String, Object> map, Object context) throws IOException {
		if (context == null || Map.class.equals(context)) {
			return map;
		}
		else if (context instanceof Map) {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			Map<String, Object> newMap = new HashMap<String, Object>((Map) context);
			newMap.putAll(map);
			return newMap;
		}
		else if (IPropertiesContainer.class.equals(context)) {
			IPropertiesContainer container = new PropertiesContainer();
			container.setProperties(map);

			return container;
		}
		else if (context instanceof IPropertiesContainer) {
			IPropertiesContainer container = (IPropertiesContainer) context;
			container.setProperties(map);

			return container;
		}

		return null;
	}
}
