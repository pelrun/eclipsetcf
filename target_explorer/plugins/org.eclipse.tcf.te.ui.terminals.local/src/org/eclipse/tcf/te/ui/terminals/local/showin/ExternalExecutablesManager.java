/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.local.showin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.tcf.te.ui.terminals.local.activator.UIPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * External executables manager implementation.
 */
public class ExternalExecutablesManager {

	/**
	 * Loads the list of all saved external executables.
	 *
	 * @return The list of all saved external executables or <code>null</code>.
	 */
	public static List<Map<String, Object>> load() {
		List<Map<String, Object>> l = null;

		IPath stateLocation = UIPlugin.getDefault().getStateLocation();
		if (stateLocation != null) {
			File f = stateLocation.append(".executables/data.json").toFile(); //$NON-NLS-1$
			if (f.canRead()) {
				try {
					Gson g = new GsonBuilder().create();
					l = g.fromJson(new FileReader(f), List.class);
				} catch (Exception e) {
					if (Platform.inDebugMode()) {
						e.printStackTrace();
					}
				}
			}
		}

		return l;
	}

	/**
	 * Saves the list of external executables.
	 *
	 * @param l The list of external executables or <code>null</code>.
	 */
	public static void save(List<Map<String, Object>> l) {
		IPath stateLocation = UIPlugin.getDefault().getStateLocation();
		if (stateLocation != null) {
			File f = stateLocation.append(".executables/data.json").toFile(); //$NON-NLS-1$
			if (f.isFile() && (l == null || l.isEmpty())) {
				@SuppressWarnings("unused")
                boolean s = f.delete();
			} else {
				try {
					Gson g = new GsonBuilder().setPrettyPrinting().create();
					if (!f.exists()) {
						@SuppressWarnings("unused")
						boolean s = f.getParentFile().mkdirs();
						s = f.createNewFile();
					}
					FileWriter w = new FileWriter(f);
					g.toJson(l, w);
					w.flush();
					w.close();
				} catch (Exception e) {
					if (Platform.inDebugMode()) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Loads the image data suitable for showing an icon in a menu
	 * (16 x 16, 8bit depth) from the given file.
	 *
	 * @param path The image file path. Must not be <code>null</code>.
	 * @return The image data or <code>null</code>.
	 */
	public static ImageData loadImage(String path) {
		Assert.isNotNull(path);

		ImageData id = null;

		ImageLoader loader = new ImageLoader();
		ImageData[] data = loader.load(path);

		if (data != null) {
			for (ImageData d : data) {
				if (d.height == 16 && d.width == 16) {
					if (id == null || id.height != 16 && id.width != 16) {
						id = d;
					} else if (d.depth < id.depth && d.depth >= 8){
						id = d;
					}
				} else {
					if (id == null) {
						id = d;
					} else if (id.height != 16 && d.height < id.height && id.width != 16 && d.width < id.width) {
						id = d;
					}
				}
			}
		}

		return id;
	}
}
