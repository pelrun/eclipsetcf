/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.navigator;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService;
import org.eclipse.tcf.te.runtime.services.interfaces.delegates.ILabelProviderDelegate;
import org.eclipse.tcf.te.runtime.utils.net.IPAddressUtil;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.locator.utils.CommonUtils;
import org.eclipse.tcf.te.tcf.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.ui.internal.ImageConsts;
import org.eclipse.tcf.te.tcf.ui.navigator.images.PeerNodeImageDescriptor;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.jface.images.AbstractImageDescriptor;
import org.eclipse.tcf.te.ui.tables.properties.NodePropertiesTableTableNode;
import org.eclipse.ui.navigator.IDescriptionProvider;

/**
 * Label provider implementation.
 */
public class PeerLabelProviderDelegate extends LabelProvider implements ILabelDecorator, ILabelProviderDelegate, IDescriptionProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(final Object element) {
		if (element instanceof IPeerNode || element instanceof IPeer) {
			StringBuilder builder = new StringBuilder();

			// Copy the peer node and peer attributes
			final Map<String, Object> attrs = new HashMap<String, Object>();

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					if (element instanceof IPeerNode) {
						attrs.putAll(((IPeerNode)element).getProperties());
						attrs.putAll(((IPeerNode)element).getPeer().getAttributes());
					}
					else if (element instanceof IPeer) {
						attrs.putAll(((IPeer)element).getAttributes());
					}
				}
			};

			if (Protocol.isDispatchThread()) {
				runnable.run();
			}
			else {
				Protocol.invokeAndWait(runnable);
			}

			// Build up the base label from the peer name
			builder.append((String)attrs.get(IPeer.ATTR_NAME));

			// If the label is "TCF Agent" or "TCF Proxy", than append IP/dns.name
			// (if not localhost) and port to the label
			if (isAppendAddressText(builder.toString())) {
				String dnsName = (String)attrs.get("dns.name.transient"); //$NON-NLS-1$
				String ip = (String)attrs.get(IPeer.ATTR_IP_HOST);
				String port = (String)attrs.get(IPeer.ATTR_IP_PORT);

				if (ip != null && !"".equals(ip.trim())) { //$NON-NLS-1$
					builder.append(" "); //$NON-NLS-1$
					if (!IPAddressUtil.getInstance().isLocalHost(ip)) {
						builder.append(dnsName != null && !"".equals(dnsName.trim()) ? dnsName.trim() : ip.trim()); //$NON-NLS-1$
					}

					if (port != null && !"".equals(port.trim()) && !"1534".equals(port.trim())) { //$NON-NLS-1$ //$NON-NLS-2$
						builder.append(":"); //$NON-NLS-1$
						builder.append(port.trim());
					}
				}
			}

			String label = builder.toString();
			if (!"".equals(label.trim())) { //$NON-NLS-1$
				return label;
			}
		}
		else if (element instanceof ILocatorNode) {
			String name = ((ILocatorNode)element).getName();
			if (name == null) {
				name = ((ILocatorNode)element).getPeer().getID();
			}

			return name;
		}
		else if (element instanceof IModelNode) {
			return ((IModelNode)element).getName();
		}
		else if (element instanceof NodePropertiesTableTableNode) {
			NodePropertiesTableTableNode node = (NodePropertiesTableTableNode)element;

			if ("SimulatorType".equals(node.name)) { //$NON-NLS-1$
				ISimulatorService service = ServiceManager.getInstance().getService(node.value, ISimulatorService.class, false);
				if (service != null) {
					return service.getName();
				}

				return node.value;
			}

			String key = "PeerLabelProviderDelegate_NodePropertiesTable_" + node.name.trim() + "_" + node.value.replaceAll("\\.", "_"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			if (Messages.hasString(key)) {
				return Messages.getString(key);
			}
		}

		return null;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.IDescriptionProvider#getDescription(java.lang.Object)
	 */
    @Override
    public String getDescription(Object element) {
    	if (element instanceof IPeerNode) {
    		IPeerNode peerNode = (IPeerNode)element;

    		if (!peerNode.isValid()) {
    			String error = CommonUtils.getPeerError(peerNode);
    			if (error != null) {
    				return error;
    			}
    			return Messages.PeerLabelProviderDelegate_description_invalid;
    		}

    		Map<String,String> warnings = CommonUtils.getPeerWarnings(peerNode);
   			if (warnings != null && !warnings.isEmpty()) {
   				String desc = ""; //$NON-NLS-1$
   				for (String warning : warnings.values()) {
   					if (desc.trim().length() > 0) {
   						desc += "; "; //$NON-NLS-1$
   					}
   					desc += warning.replaceAll("\n", "").replaceAll("\t", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
   				}
   				return desc;
   			}
    	}
	    return null;
    }

    /**
	 * Determines if the IP-address and port needs to be appended
	 * to the given label.
	 * <p>
	 * The default implementation returns <code>true</code> if the label is either
	 * &quot;TCF Agent&quot; or &quot;TCF Proxy&quot;.
	 *
	 * @param label The label. Must not be <code>null</code>.
	 * @return <code>True</code> if the address shall be appended, <code>false</code> otherwise.
	 */
	protected boolean isAppendAddressText(final String label) {
		Assert.isNotNull(label);

		boolean append = "TCF Agent".equals(label) || "TCF Proxy".equals(label); //$NON-NLS-1$ //$NON-NLS-2$

		if (!append) {
			final AtomicInteger count = new AtomicInteger();

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					count.set(ModelManager.getPeerModel().getService(IPeerModelLookupService.class).lkupPeerModelByName(label).length);
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);

			append = count.get() > 1;
		}

		return append;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		if (element instanceof IPeerNode) {
			return UIPlugin.getImage(ImageConsts.CONNECTION);
		}
		if (element instanceof ILocatorNode) {
			if (((ILocatorNode)element).isDiscovered()) {
				return UIPlugin.getImage(ImageConsts.PEER_DISCOVERED);
			}
			return UIPlugin.getImage(ImageConsts.PEER_STATIC);
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
	public Image decorateImage(Image image, Object element) {
		Image decoratedImage = image;

		if (image != null && element instanceof IPeerNode) {
			AbstractImageDescriptor descriptor = new PeerNodeImageDescriptor(
							UIPlugin.getDefault().getImageRegistry(),
							image,
							(IPeerNode)element);
			decoratedImage = UIPlugin.getSharedImage(descriptor);
		}

		return decoratedImage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelDecorator#decorateText(java.lang.String, java.lang.Object)
	 */
	@Override
	public String decorateText(String text, Object element) {
		if (element instanceof ILocatorNode) {
			String ip = ((ILocatorNode)element).getPeer().getAttributes().get(IPeer.ATTR_IP_HOST);
			String port = ((ILocatorNode)element).getPeer().getAttributes().get(IPeer.ATTR_IP_PORT);
			if (port == null || port.trim().equals("1534")) { //$NON-NLS-1$
				port = ""; //$NON-NLS-1$
			}
			else {
				port = ":" + port.trim(); //$NON-NLS-1$
			}

			if (text != null && ip != null && !text.contains(ip)) {
				text += " (" + ip + port + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				return text;
			}
		}
		return null;
	}
}
