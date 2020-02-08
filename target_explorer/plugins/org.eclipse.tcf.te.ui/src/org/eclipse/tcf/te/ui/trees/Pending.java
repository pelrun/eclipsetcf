/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.trees;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tcf.te.runtime.model.MessageModelNode;
import org.eclipse.tcf.te.ui.activator.UIPlugin;
import org.eclipse.tcf.te.ui.nls.Messages;

/**
 * The pending node used in various tree viewer. It can start an animation that
 * displays an animated GIF image read from "pending.gif".
 */
public class Pending {
	// Reference to the parent tree viewer
	TreeViewer viewer;
	// The display used to create image and timer.
	Display display;

	/**
	 * Create a pending node for the specified tree viewer.
	 *
	 * @param viewer The tree viewer in which the pending node is added to.
	 */
	public Pending(TreeViewer viewer) {
		this.viewer = viewer;
		this.display = viewer.getTree().getDisplay();
	}

	/**
	 * Get the label for this pending node.
	 *
	 * @return The label of this pending node.
	 */
	public String getText() {
		return Messages.Pending_Label;
	}

	/**
	 * Get the current image in the pending image list.
	 *
	 * @return The current image.
	 */
	public Image getImage() {
		return UIPlugin.getImage(MessageModelNode.OBJECT_MESSAGE_PENDING_ID);
	}

}
