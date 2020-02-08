/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.notifications.interfaces;

/**
 * Image registry constants.
 */
public interface ImageConsts {

	// ***** The directory structure constants *****

	/**
	 * The root directory where to load the images from, relative to
	 * the bundle directory.
	 */
    public final static String IMAGE_DIR_ROOT = "icons/"; //$NON-NLS-1$

    /**
     * The directory where to load disabled local toolbar images from,
     * relative to the image root directory.
     */
    public final static String  IMAGE_DIR_DLCL = "dlcl16/"; //$NON-NLS-1$

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
     * The directory where to load wizard banner images from,
     * relative to the image root directory.
     */
    public final static String IMAGE_DIR_WIZBAN = "wizban/"; //$NON-NLS-1$

    /**
     * The directory where to load object overlay images from,
     * relative to the image root directory.
     */
    public final static String  IMAGE_DIR_OVR = "ovr16/"; //$NON-NLS-1$

	/**
	 * The directory where to load model object images from, relative to the image root directory.
	 */
	public final static String IMAGE_DIR_OBJ = "obj16/"; //$NON-NLS-1$

    // ***** The image constants *****

    /**
     * The key to access the New target wizard banner image.
     */
    public static final String NOTIFICATION_CLOSE = "NotificationClose"; //$NON-NLS-1$

    /**
     * The key to access the New target wizard image (enabled).
     */
    public static final String  NOTIFICATION_CLOSE_HOVER = "NotificationCloseHover"; //$NON-NLS-1$
}
