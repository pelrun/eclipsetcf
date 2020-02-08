/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.navigator.images;

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.utils.CommonUtils;
import org.eclipse.tcf.te.tcf.ui.internal.ImageConsts;
import org.eclipse.tcf.te.ui.jface.images.AbstractImageDescriptor;


/**
 * Peer model node image descriptor implementation.
 */
public class PeerNodeImageDescriptor extends AbstractImageDescriptor {
	// the base image to decorate with overlays
	private Image baseImage;
	// the image size
	private Point imageSize;

	// Flags representing the connect states to decorate
	private int connectState;

	// Flags representing the valid state to decorate
	private boolean valid;

	// Flags representing the warning state to decorate
	private boolean warning;


	/**
	 * Constructor.
	 */
	public PeerNodeImageDescriptor(final ImageRegistry registry, final Image baseImage, final IPeerNode node) {
		super(registry);

		this.baseImage = baseImage;
		imageSize = new Point(baseImage.getImageData().width, baseImage.getImageData().height);

		// Determine the current object state to decorate
		initialize(node);

		// build up the key for the image registry
		defineKey(baseImage.hashCode());
	}

	/**
	 * Initialize the image descriptor from the peer model.
	 *
	 * @param node The peer model. Must not be <code>null</code>.
	 */
	protected void initialize(IPeerNode node) {
		Assert.isNotNull(node);
		connectState = node.getConnectState();
		// If invoked in the TCF dispatch thread, node.isValid() may lead to a
		// deadlock if the initialize(...) where called as a result of an activity
		// state change event.
		valid = node.isValid();

		Map<String,String> warnings = CommonUtils.getPeerWarnings(node);
		warning = warnings != null && !warnings.isEmpty();
	}

	/**
	 * Define the peer image descriptor key.
	 *
	 * @param hashCode The hash code of the base image.
	 */
	protected void defineKey(int hashCode) {
		String key = "PNID:" +  //$NON-NLS-1$
			hashCode + ":" + //$NON-NLS-1$
			connectState  + ":" + //$NON-NLS-1$
			valid  + ":" + //$NON-NLS-1$
			warning;

		setDecriptorKey(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.resource.CompositeImageDescriptor#drawCompositeImage(int, int)
	 */
	@Override
	protected void drawCompositeImage(int width, int height) {
		drawCentered(baseImage, width, height);

		if (!valid) {
			drawBottomRight(ImageConsts.RED_X_OVR);
		}
		else {
			if (connectState < 0) {
				drawTopRight(ImageConsts.BUSY_OVR);
			}

			if (connectState == IConnectable.STATE_CONNECTED) {
				if (warning) {
					drawBottomRight(ImageConsts.WARNING_OVR);
				}
				else {
					drawBottomRight(ImageConsts.GREEN_OVR);
				}
			}
			else if (connectState == IConnectable.STATE_CONNECTING || connectState == IConnectable.STATE_DISCONNECTING ||
							connectState == IConnectable.STATE_CONNECTION_LOST || connectState == IConnectable.STATE_CONNECTION_RECOVERING) {
				drawBottomRight(ImageConsts.GREY_OVR);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.resource.CompositeImageDescriptor#getSize()
	 */
	@Override
	protected Point getSize() {
		return imageSize;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ide.util.ui.AbstractImageDescriptor#getBaseImage()
	 */
	@Override
	protected Image getBaseImage() {
		return baseImage;
	}
}
