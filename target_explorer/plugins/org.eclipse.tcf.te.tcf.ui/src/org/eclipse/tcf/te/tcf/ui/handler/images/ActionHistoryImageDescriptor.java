/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.handler.images;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.tcf.te.tcf.ui.internal.ImageConsts;
import org.eclipse.tcf.te.ui.jface.images.AbstractImageDescriptor;


/**
 * Action history toolbar action image descriptor implementation.
 */
public class ActionHistoryImageDescriptor extends AbstractImageDescriptor {
	// the base image to decorate with overlays
	private Image baseImage;
	// the image size
	private Point imageSize;

	// Flags representing the valid state to decorate
	private boolean valid;


	/**
	 * Constructor.
	 */
	public ActionHistoryImageDescriptor(final ImageRegistry registry, final Image baseImage, boolean valid) {
		super(registry);

		this.baseImage = baseImage;
		imageSize = new Point(baseImage.getImageData().width, baseImage.getImageData().height);

		// Determine the current object state to decorate
		initialize(valid);

		// build up the key for the image registry
		defineKey(baseImage.hashCode());
	}

	/**
	 * Initialize the image descriptor from the peer model.
	 *
	 * @param node The peer model. Must not be <code>null</code>.
	 */
	protected void initialize(boolean valid) {
		this.valid = valid;
	}

	/**
	 * Define the peer image descriptor key.
	 *
	 * @param hashCode The hash code of the base image.
	 */
	protected void defineKey(int hashCode) {
		String key = "AHID:" +  //$NON-NLS-1$
			hashCode + ":" + //$NON-NLS-1$
			valid;

		setDecriptorKey(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.resource.CompositeImageDescriptor#drawCompositeImage(int, int)
	 */
	@Override
	protected void drawCompositeImage(int width, int height) {
		drawCentered(baseImage, width, height);

		if (!valid) {
			drawTopLeft(ImageConsts.RED_X_OVR);
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
