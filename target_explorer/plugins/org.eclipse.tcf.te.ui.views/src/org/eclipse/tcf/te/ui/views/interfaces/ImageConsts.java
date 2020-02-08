/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.interfaces;

/**
 * Image registry constants.
 */
public interface ImageConsts {

	// ***** The directory structure constants *****

	/**
	 * The root directory where to load the images from, relative to
	 * the bundle directory.
	 */
    public final static String  IMAGE_DIR_ROOT = "icons/"; //$NON-NLS-1$

    /**
     * The directory where to load enabled local toolbar images from,
     * relative to the image root directory.
     */
    public final static String  IMAGE_DIR_ELCL = "elcl16/"; //$NON-NLS-1$

    /**
     * The directory where to load view related images from, relative to
     * the image root directory.
     */
    public final static String  IMAGE_DIR_EVIEW = "eview16/"; //$NON-NLS-1$

    /**
     * The directory where to load disabled toolbar images from,
     * relative to the image root directory.
     */
    public final static String  IMAGE_DIR_DTOOL = "dtool16/"; //$NON-NLS-1$

    /**
     * The directory where to load enabled toolbar images from,
     * relative to the image root directory.
     */
    public final static String  IMAGE_DIR_ETOOL = "etool16/"; //$NON-NLS-1$

    /**
     * The directory where to load model object images from,
     * relative to the image root directory.
     */
    public final static String  IMAGE_DIR_OBJ = "obj16/"; //$NON-NLS-1$

    // ***** The image constants *****

	/**
	 * The key to access the refresh action image (enabled).
	 */
	public static final String  ACTION_Refresh_Enabled = "RefreshAction_enabled"; //$NON-NLS-1$

	/**
     * The key to access the editor image.
     */
    public static final String  EDITOR = "Editor"; //$NON-NLS-1$

    /**
     * The key to access the view image.
     */
    public static final String  VIEW = "View"; //$NON-NLS-1$

    /**
     * The key to access the help action image.
     */
    public static final String  HELP = "HelpAction"; //$NON-NLS-1$

    /**
     * The key to access the menu action image.
     */
    public static final String  MENU = "MenuAction"; //$NON-NLS-1$

    /**
     * The key to access the favorites category image.
     */
    public static final String FAVORITES = "Favorites"; //$NON-NLS-1$
}
