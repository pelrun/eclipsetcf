/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.navigator.images;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.ui.internal.ImageConsts;
import org.eclipse.tcf.te.ui.jface.images.AbstractImageDescriptor;


/**
 * Peer model node image descriptor implementation.
 */
public class PeerImageDescriptor extends AbstractImageDescriptor {
	// the base image to decorate with overlays
	private Image baseImage;
	// the image size
	private Point imageSize;

	// Flags representing the connect states to decorate
	private int connectState;

	/**
	 * Constructor.
	 */
	public PeerImageDescriptor(final ImageRegistry registry, final Image baseImage, final IPeerModel node) {
		super(registry);

		this.baseImage = baseImage;
		imageSize = new Point(baseImage.getImageData().width, baseImage.getImageData().height);

		// Determine the current object state to decorate
		if (Protocol.isDispatchThread()) {
			initialize(node);
		} else {
			Protocol.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					initialize(node);
				}
			});
		}

		// build up the key for the image registry
		defineKey(baseImage.hashCode());
	}

	/**
	 * Initialize the image descriptor from the peer model.
	 *
	 * @param node The peer model. Must not be <code>null</code>.
	 */
	protected void initialize(IPeerModel node) {
		Assert.isNotNull(node);
		Assert.isTrue(Protocol.isDispatchThread());

		connectState = node instanceof IConnectable ? ((IConnectable)node).getConnectState() : IConnectable.STATE_UNKNOWN;
	}

	/**
	 * Define the peer image descriptor key.
	 *
	 * @param hashCode The hash code of the base image.
	 */
	protected void defineKey(int hashCode) {
		String key = "PMID:" +  //$NON-NLS-1$
			hashCode + ":" + //$NON-NLS-1$
			connectState;

		setDecriptorKey(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.resource.CompositeImageDescriptor#drawCompositeImage(int, int)
	 */
	@Override
	protected void drawCompositeImage(int width, int height) {
		drawCentered(baseImage, width, height);

		if (connectState < 0) {
			drawTopRight(ImageConsts.BUSY_OVR);
		}

		if (connectState == IConnectable.STATE_CONNECTED) {
			drawBottomRight(ImageConsts.GREEN_OVR);
		}
		else if (connectState == IConnectable.STATE_CONNECTING || connectState == IConnectable.STATE_DISCONNECTING) {
			drawBottomRight(ImageConsts.GREY_OVR);
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
