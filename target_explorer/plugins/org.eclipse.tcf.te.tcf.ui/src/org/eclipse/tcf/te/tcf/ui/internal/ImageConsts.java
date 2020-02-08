/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal;

/**
 * TCF UI Plug-in Image registry constants.
 */
public interface ImageConsts {

	// ***** The directory structure constants *****

	/**
	 * The root directory where to load the images from, relative to
	 * the bundle directory.
	 */
    public final static String  IMAGE_DIR_ROOT = "icons/"; //$NON-NLS-1$

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
     * The directory where to load model object images from,
     * relative to the image root directory.
     */
    public final static String  IMAGE_DIR_OBJ = "obj16/"; //$NON-NLS-1$

    /**
     * The directory where to load object overlay images from,
     * relative to the image root directory.
     */
    public final static String  IMAGE_DIR_OVR = "ovr16/"; //$NON-NLS-1$

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

    // ***** The image constants *****

    /**
     * The key to access the base peer object image.
     */
    public static final String PEER_NODE = "PeerObject"; //$NON-NLS-1$

    /**
     * The key to access the base peer object image (discovered).
     */
    public static final String PEER_DISCOVERED = "PeerObjectDiscovered"; //$NON-NLS-1$

    /**
     * The key to access the base peer object image (static).
     */
    public static final String PEER_STATIC = "PeerObjectStatic"; //$NON-NLS-1$

    /**
     * The key to access the new peer wizard image.
     */
    public static final String NEW_PEER_NODE = "NewPeerObject"; //$NON-NLS-1$

    /**
     * The key to access the new config action image.
     */
    public static final String NEW_CONFIG = "NewConfig"; //$NON-NLS-1$

    /**
     * The key to access the base connection object image.
     */
    public static final String CONNECTION = "ConnectionObject"; //$NON-NLS-1$

	/**
	 * The key to access the windriver image.
	 */
	public static final String SYSTEM_MGNT_VIEW = "SystemMgmtView"; //$NON-NLS-1$

    /**
     * The key to access the peer object busy overlay image.
     */
    public static final String BUSY_OVR = "BusyOverlay"; //$NON-NLS-1$

    /**
     * The key to access the peer object gold overlay image.
     */
    public static final String GOLD_OVR = "GoldOverlay"; //$NON-NLS-1$

    /**
     * The key to access the peer object warning overlay image.
     */
    public static final String WARNING_OVR = "WarningOverlay"; //$NON-NLS-1$

    /**
     * The key to access the peer object green overlay image.
     */
    public static final String GREEN_OVR = "GreenOverlay"; //$NON-NLS-1$

    /**
     * The key to access the peer object grey overlay image.
     */
    public static final String GREY_OVR = "GreyOverlay"; //$NON-NLS-1$

    /**
     * The key to access the peer object red overlay image.
     */
    public static final String RED_OVR = "RedOverlay"; //$NON-NLS-1$

    /**
     * The key to access the peer object red X overlay image.
     */
    public static final String RED_X_OVR = "RedXOverlay"; //$NON-NLS-1$

    /**
     * The key to access the peer object link overlay image.
     */
    public static final String LINK_OVR = "LinkOverlay"; //$NON-NLS-1$
}
