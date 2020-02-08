/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.navigator.runtime;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ISysMonitor;
import org.eclipse.tcf.services.ISysMonitor.SysMonitorContext;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.runtime.services.interfaces.delegates.ILabelProviderDelegate;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.processes.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.processes.ui.internal.ImageConsts;
import org.eclipse.tcf.te.tcf.processes.ui.nls.Messages;


/**
 * Runtime model label provider delegate implementation.
 */
public class LabelProviderDelegate extends AbstractLabelProviderDelegate implements ILabelProviderDelegate, ILabelDecorator {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(final Object element) {
		if (element instanceof IRuntimeModel) {
			final AtomicReference<IPeerNode> node = new AtomicReference<IPeerNode>();

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					node.set(((IRuntimeModel)element).getPeerNode());
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);

			String label = Messages.getStringDelegated(node.get(), "ProcessLabelProvider_RootNodeLabel"); //$NON-NLS-1$
			return label != null ? label : Messages.ProcessLabelProvider_RootNodeLabel;
		} else if (element instanceof IProcessContextNode) {
			final IProcessContextNode node = (IProcessContextNode)element;
			final AtomicReference<String> name = new AtomicReference<String>();
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					name.set(node.getName());
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);

			if (name.get() != null && !"".equals(name.get().trim())) { //$NON-NLS-1$
				return name.get();
			}
		}
		else if (element instanceof IModelNode) {
			return ((IModelNode)element).getName();
		}

		return ""; //$NON-NLS-1$
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(final Object element) {
		if (element instanceof IRuntimeModel) {
			return UIPlugin.getImage(ImageConsts.OBJ_Process_Root);
		} else if (element instanceof IProcessContextNode) {
			Image image = null;

			final AtomicBoolean isThread = new AtomicBoolean();

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					IProcessContextNode node = ((IProcessContextNode)element);
					boolean thread = false;
					if (node.getParent() instanceof IRuntimeModel) {
						// test for kernel thread
						SysMonitorContext smc = node.getSysMonitorContext();
						if (smc != null && Integer.valueOf(ISysMonitor.EXETYPE_KERNEL).equals(smc.getProperties().get(ISysMonitor.PROP_EXETYPE))) {
							thread = true;
						}
					} else {
						thread = true;
					}
					isThread.set(thread);
				}
			};
			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);

			image = UIPlugin.getImage(isThread.get()? ImageConsts.OBJ_Thread : ImageConsts.OBJ_Process);

			return image;
		}
		else if (element instanceof IModelNode) {
			return UIPlugin.getImage(((IModelNode)element).getImageId());
		}

		return super.getImage(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelDecorator#decorateImage(org.eclipse.swt.graphics.Image, java.lang.Object)
	 */
	@Override
    public Image decorateImage(final Image image, final Object element) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelDecorator#decorateText(java.lang.String, java.lang.Object)
	 */
	@Override
    public String decorateText(final String text, final Object element) {
		return null;
	}
}
