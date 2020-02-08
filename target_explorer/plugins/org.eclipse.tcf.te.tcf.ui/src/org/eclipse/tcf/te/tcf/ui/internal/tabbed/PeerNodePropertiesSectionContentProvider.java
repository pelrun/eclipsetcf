/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal.tabbed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Control;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.core.utils.ConnectStateHelper;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.persistence.utils.DataHelper;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceUtils;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.utils.CommonUtils;
import org.eclipse.tcf.te.ui.interfaces.services.INodePropertiesTableUIDelegate;
import org.eclipse.tcf.te.ui.tables.properties.NodePropertiesTableTableNode;
import org.eclipse.tcf.te.ui.views.extensions.LabelProviderDelegateExtensionPointManager;
import org.eclipse.ui.forms.widgets.Section;


/**
 * Peer properties general section table content provider implementation.
 */
public class PeerNodePropertiesSectionContentProvider implements IStructuredContentProvider {

	// Flag to control if the content provide may update the parent section title
	private final boolean updateParentSectionTitle;

	/**
	 * Constructor.
	 *
	 * @param updateParentSectionTitle Specify <code>true</code> to allow the content provider to update
	 *                                 the parent section title, <code>false</code> if no title update is desired.
	 */
	public PeerNodePropertiesSectionContentProvider(boolean updateParentSectionTitle) {
		this.updateParentSectionTitle = updateParentSectionTitle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(final Object inputElement) {
		if (inputElement instanceof IPeerNode) {
			// Get all custom properties of the node
			final Map<String, Object> properties = new HashMap<String, Object>();
			final Map<String, Object> debugProperties = new HashMap<String, Object>();
			// And get all native properties of the peer
			if (Protocol.isDispatchThread()) {
				properties.putAll(((IPeerNode)inputElement).getPeer().getAttributes());
			} else {
				Protocol.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						IPeerNode peerNode = (IPeerNode)inputElement;
						properties.putAll(peerNode.getPeer().getAttributes());
						properties.put(IPeerNodeProperties.PROPERTY_CONNECT_STATE, ConnectStateHelper.getConnectState(peerNode.getConnectState()));
						String error = CommonUtils.getPeerError(peerNode);
						if (error != null) {
							properties.put(IPeerNodeProperties.PROPERTY_ERROR, error);
						}
						Map<String,String> warnings = CommonUtils.getPeerWarnings(peerNode);
						if (warnings != null && !warnings.isEmpty()) {
							IPropertiesContainer container = new PropertiesContainer();
							container.addProperties(warnings);
							properties.put(IPeerNodeProperties.PROPERTY_WARNINGS, DataHelper.encodePropertiesContainer(container));
						}
						if (peerNode.getConnectState() == IConnectable.STATE_CONNECTED) {
							properties.put(IPeerNodeProperties.PROPERTY_LOCAL_SERVICES, peerNode.getStringProperty(IPeerNodeProperties.PROPERTY_LOCAL_SERVICES));
							properties.put(IPeerNodeProperties.PROPERTY_REMOTE_SERVICES, peerNode.getStringProperty(IPeerNodeProperties.PROPERTY_REMOTE_SERVICES));
						}

						if (Platform.inDebugMode()) {
							debugProperties.putAll(peerNode.getProperties());
						}
					}
				});
			}

			INodePropertiesTableUIDelegate delegate = ServiceUtils.getUIServiceDelegate(inputElement, inputElement, INodePropertiesTableUIDelegate.class);
			List<NodePropertiesTableTableNode> nodes = new ArrayList<NodePropertiesTableTableNode>();
			for (Entry<String, Object> entry : properties.entrySet()) {
				String name = entry.getKey();
				// Check if the property is filtered
				if (Platform.inDebugMode() || (!name.endsWith(".silent") && !name.contains(".transient") &&  //$NON-NLS-1$ //$NON-NLS-2$
								(delegate == null || !delegate.isFiltered(inputElement, name, entry.getValue())))) {
					nodes.add(new NodePropertiesTableTableNode(name, entry.getValue() != null ? entry.getValue().toString() : "")); //$NON-NLS-1$
				}
			}
			Collections.sort(nodes, new Comparator<NodePropertiesTableTableNode>() {
				@Override
	            public int compare(NodePropertiesTableTableNode arg0, NodePropertiesTableTableNode arg1) {
		            return arg0.name.compareToIgnoreCase(arg1.name);
	            }
			});
			if (delegate != null) delegate.expandNodesAfterSort(inputElement, nodes);


			ILabelProvider provider = ServiceUtils.getUIServiceDelegate(inputElement, inputElement, ILabelProvider.class);
			List<NodePropertiesTableTableNode> result = new ArrayList<NodePropertiesTableTableNode>();
			for (NodePropertiesTableTableNode node : nodes) {
				// Possible replacement for the node properties table table node value
				String text = null;

				// Get the label provider delegate for the input element
				ILabelProvider[] delegates = LabelProviderDelegateExtensionPointManager.getInstance().getDelegates(inputElement, false);
				if (delegates != null && delegates.length > 0) {
					text = delegates[0].getText(node);
				}

				// Fallback to the label provider
				if (text == null && provider != null) {
					text = provider.getText(node);
				}

				// Replace the node properties table table node value
				if (text != null && !"".equals(text)) { //$NON-NLS-1$
					node = new NodePropertiesTableTableNode(node.name, text);
				}

				result.add(node);
            }

			if (!debugProperties.isEmpty()) {
				nodes.clear();
				for (Entry<String, Object> entry : debugProperties.entrySet()) {
					String name = entry.getKey();
					if (!name.equals(IPeerNodeProperties.PROPERTY_CONNECT_STATE) &&
									!name.equals(IPeerNodeProperties.PROPERTY_ERROR) &&
									!name.equals(IPeerNodeProperties.PROPERTY_WARNINGS) &&
									!name.equals(IPeerNodeProperties.PROPERTY_LOCAL_SERVICES) &&
									!name.equals(IPeerNodeProperties.PROPERTY_REMOTE_SERVICES)) {
						nodes.add(new NodePropertiesTableTableNode(name, entry.getValue() != null ? entry.getValue().toString() : "")); //$NON-NLS-1$
					}
				}
				Collections.sort(nodes, new Comparator<NodePropertiesTableTableNode>() {
					@Override
		            public int compare(NodePropertiesTableTableNode arg0, NodePropertiesTableTableNode arg1) {
			            return arg0.name.compareToIgnoreCase(arg1.name);
		            }
				});
				result.add(new NodePropertiesTableTableNode("", "")); //$NON-NLS-1$ //$NON-NLS-2$
				result.addAll(nodes);
			}
			return result.toArray(new NodePropertiesTableTableNode[result.size()]);

		}

		return new Object[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// Do nothing if we shall not update the section title
		if (!updateParentSectionTitle) return;

		String sectionTitle = null;
		Object element = null;

		// If the input is a tree selection, extract the element from the tree path
		if (newInput instanceof ITreeSelection && !((ITreeSelection)newInput).isEmpty()) {
			// Cast to the correct type
			ITreeSelection selection = (ITreeSelection)newInput;
			// Get the selected tree pathes
			TreePath[] pathes = selection.getPaths();
			// If there are more than one elements selected, we care only about the first path
			TreePath path = pathes.length > 0 ? pathes[0] : null;
			// Get the last element within the tree path
			element = path != null ? path.getLastSegment() : null;
		}

		// If the input is a peer model node, set it directly
		if (newInput instanceof IPeerNode) element = newInput;

		// Determine the section header text
		if (element instanceof IPeerNode) {
			sectionTitle = NLS.bind(org.eclipse.tcf.te.ui.nls.Messages.NodePropertiesTableControl_section_title, "Peer"); //$NON-NLS-1$
		}

		// Set the standard (no selection) section title if none could be determined
		if (sectionTitle == null || "".equals(sectionTitle.trim())) sectionTitle = org.eclipse.tcf.te.ui.nls.Messages.NodePropertiesTableControl_section_title_noSelection; //$NON-NLS-1$
		// Stretch to a length of 40 characters to make sure the title can be changed
		// to hold and show text up to this length
		if (sectionTitle.length() < 40) {
			StringBuilder buffer = new StringBuilder(sectionTitle);
			while (buffer.length() < 40) buffer.append(" "); //$NON-NLS-1$
			sectionTitle = buffer.toString();
		}

		// Find the parent section the node properties tables is embedded in
		Control control = viewer.getControl();
		while (control != null && !control.isDisposed()) {
			if (control instanceof Section) {
				Section section = (Section)control;
				// We cannot get access to the Label control used to set the text, so just catch the
				// probably SWTException
				try { section.setText(sectionTitle); } catch(SWTException e) { /* ignored on purpose */ }
				break;
			}
			control = control.getParent();
		}
	}
}
