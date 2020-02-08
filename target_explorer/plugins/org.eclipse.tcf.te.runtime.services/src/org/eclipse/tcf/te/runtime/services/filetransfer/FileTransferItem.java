/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.runtime.services.filetransfer;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.interfaces.filetransfer.IFileTransferItem;

/**
 * FileTransferItem
 */
public class FileTransferItem extends PropertiesContainer implements IFileTransferItem {

	private boolean fPatchingSetProperty;

	/**
	 * Constructor.
	 */
	public FileTransferItem() {
		setProperty(PROPERTY_ENABLED, true);
		setProperty(PROPERTY_DIRECTION, ""+HOST_TO_TARGET); //$NON-NLS-1$
	}

	public FileTransferItem(IPath fromHost, boolean enabled) {
		this();
		if (fromHost != null)
			setProperty(PROPERTY_HOST, fromHost.toPortableString());
		setProperty(PROPERTY_ENABLED, enabled);
	}

	/**
	 * @deprecated use {@link FileTransferItem#FileTransferItem(IPath, String)}, instead.
	 */
	@Deprecated
    public FileTransferItem(IPath fromHost, IPath toTarget) {
		this();
		if (fromHost != null)
			setProperty(PROPERTY_HOST, fromHost.toPortableString());
		if (toTarget != null) {
			setProperty(PROPERTY_TARGET, toTarget.toPortableString());
		}
	}

	public FileTransferItem(IPath fromHost, String toTarget) {
		this();
		if (fromHost != null)
			setProperty(PROPERTY_HOST, fromHost.toPortableString());
		if (toTarget != null) {
			setProperty(PROPERTY_TARGET_STRING, toTarget);
		}
	}

	private String normalizeTargetPath(Object tgtPath) {
		if (tgtPath == null)
			return null;

		String path = String.valueOf(tgtPath);
		// Replace multiple slashes with a single slash
		path = path.replaceAll("/+", "/"); //$NON-NLS-1$ //$NON-NLS-2$
		// Remove trailing slash
		if (path.endsWith("/") && path.length() > 1) //$NON-NLS-1$
			path = path.substring(0, path.length()-1);

	    return path;
    }

	@SuppressWarnings("deprecation")
    @Override
	public boolean setProperty(String key, Object value) {
		if (!fPatchingSetProperty) {
			try {
				fPatchingSetProperty = true;
				if (PROPERTY_TARGET.equals(key)) {
					setProperty(PROPERTY_TARGET_STRING, pathToString(value));
					return setProperty(PROPERTY_TARGET, value);
				}
				if (PROPERTY_TARGET_STRING.equals(key)) {
					value = normalizeTargetPath(value);
					setProperty(PROPERTY_TARGET, stringToPath(value));
					return setProperty(PROPERTY_TARGET_STRING, value);
				}
			} finally {
				fPatchingSetProperty = false;
			}
		}
		return super.setProperty(key, value);
	}

	@SuppressWarnings("deprecation")
    @Override
	public final void setProperties(Map<String, Object> properties) {
		if (properties.containsKey(PROPERTY_TARGET)) {
			if (!properties.containsKey(PROPERTY_TARGET_STRING)) {
				properties = new HashMap<String, Object>(properties);
				properties.put(PROPERTY_TARGET_STRING, pathToString(properties.get(PROPERTY_TARGET)));
			}
		} else if (properties.containsKey(PROPERTY_TARGET_STRING)) {
			properties = new HashMap<String, Object>(properties);
			properties.put(PROPERTY_TARGET, stringToPath(properties.get(PROPERTY_TARGET_STRING)));
		}
		super.setProperties(properties);
	}

	private String pathToString(Object value) {
		if (value == null)
			return null;
		return Path.fromPortableString(String.valueOf(value)).toString();
	}

	private String stringToPath(Object value) {
		if (value == null)
			return null;
		return new Path(String.valueOf(value)).toPortableString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IFileTransferItem#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		return getBooleanProperty(PROPERTY_ENABLED);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IFileTransferItem#getHostPath()
	 */
	@Override
	public IPath getHostPath() {
		return getStringProperty(PROPERTY_HOST) != null ? new Path(getStringProperty(PROPERTY_HOST)) : null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IFileTransferItem#getTargetPath()
	 */
	@Deprecated
    @Override
	public IPath getTargetPath() {
		if (getStringProperty(PROPERTY_TARGET) != null) {
			return new Path(getStringProperty(PROPERTY_TARGET));
		}
		if (getStringProperty(PROPERTY_TARGET_STRING) != null) {
			return new Path(getStringProperty(PROPERTY_TARGET_STRING));
		}
		return null;
	}

	@Override
	public String getTargetPathString() {
		return getStringProperty(PROPERTY_TARGET_STRING);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IFileTransferItem#getDirection()
	 */
	@Override
	public int getDirection() {
		int direction = getIntProperty(PROPERTY_DIRECTION);
		return direction == TARGET_TO_HOST ? TARGET_TO_HOST : HOST_TO_TARGET;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IFileTransferItem#getOptions()
	 */
	@Override
	public String getOptions() {
		return getStringProperty(PROPERTY_OPTIONS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.properties.PropertiesContainer#hashCode()
	 */
	@Override
	public int hashCode() {
		int hc = getHostPath() != null ? getHostPath().hashCode() : 0;
		hc = hc << 8 + (getTargetPathString() != null ? getTargetPathString().hashCode() : 0);
		hc = hc << 8 + getDirection();
	    return hc;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.properties.PropertiesContainer#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean equals = super.equals(obj);
		if (!equals && obj instanceof IFileTransferItem) {
			boolean hostPathEqual = getHostPath() != null ? getHostPath().equals(((IFileTransferItem)obj).getHostPath()) : ((IFileTransferItem)obj).getHostPath() == null;
			boolean targetPathEqual = getTargetPathString() != null ? getTargetPathString().equals(((IFileTransferItem)obj).getTargetPathString()) : ((IFileTransferItem)obj).getTargetPathString() == null;
			boolean directionEqual = getDirection() == ((IFileTransferItem)obj).getDirection();
			return hostPathEqual && targetPathEqual && directionEqual;
		}
		return equals;
	}
}
