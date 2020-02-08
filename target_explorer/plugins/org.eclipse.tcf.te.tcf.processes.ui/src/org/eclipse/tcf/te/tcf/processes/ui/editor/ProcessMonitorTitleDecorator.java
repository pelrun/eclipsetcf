/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.editor;

import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.processes.ui.activator.UIPlugin;
import org.eclipse.tcf.te.ui.jface.images.AbstractImageDescriptor;

/**
 * The title bar decorator for Process Monitor.
 */
public class ProcessMonitorTitleDecorator extends LabelProvider implements ILabelDecorator {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelDecorator#decorateImage(org.eclipse.swt.graphics.Image, java.lang.Object)
	 */
	@Override
	public Image decorateImage(Image image, Object element) {
		if (element instanceof IProcessContextNode) {
			IProcessContextNode node = (IProcessContextNode) element;
			IRuntimeModel model = node.getParent(IRuntimeModel.class);
			if (model.getAutoRefreshInterval() > 0) {
				Image decoratedImage = image;
				if (image != null) {
					AbstractImageDescriptor descriptor = new RefreshingImageDescriptor(UIPlugin
					                .getDefault().getImageRegistry(), image);
					decoratedImage = UIPlugin.getSharedImage(descriptor);
				}
				return decoratedImage;
			}
			return image;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelDecorator#decorateText(java.lang.String, java.lang.Object)
	 */
	@Override
	public String decorateText(String text, Object element) {
		if (element instanceof IProcessContextNode) {
			IProcessContextNode node = (IProcessContextNode) element;
			IRuntimeModel model = node.getParent(IRuntimeModel.class);
			if (model.getAutoRefreshInterval() > 0) {
				return text + " [Auto Refreshing]"; //$NON-NLS-1$
			}
			return text;
		}
		return null;
	}
}
