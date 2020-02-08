/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.navigator.nodes;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.graphics.Image;

/**
 * New wizard node implementation.
 */
public final class NewWizardNode {
	// The new wizard node label
	private final String label;
	// The new wizard node image
	private final Image image;
	// The "new configuration" wizard id
	private final String wizardId;
	// The reference to the parent node
	private final Object parent;

	/**
	 * Constructor.
	 *
	 * @param wizardId The new wizard id. Must not be <code>null</code>.
	 * @param label The new wizard node. Must not be <code>null</code>.
	 * @param image The image for the node. Must not be <code>null</code>.
	 * @param parent The parent node. Must not be <code>null</code>.
	 */
	public NewWizardNode(String wizardId, String label, Image image, Object parent) {
		Assert.isNotNull(label);
		this.label = label;
		Assert.isNotNull(image);
		this.image = image;
		Assert.isNotNull(wizardId);
		this.wizardId = wizardId;
		Assert.isNotNull(parent);
		this.parent = parent;
	}

	/**
	 * Returns the new wizard node label.
	 *
	 * @return The new wizard node label.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Returns the new wizard node image.
	 *
	 * @return The new wizard node image.
	 */
	public Image getImage() {
		return image;
	}

	/**
	 * Returns the new wizard id.
	 *
	 * @return The new wizard id.
	 */
	public String getWizardId() {
		return wizardId;
	}

	/**
	 * Returns the parent node.
	 *
	 * @return The parent node.
	 */
	public Object getParent() {
		return parent;
	}
}
