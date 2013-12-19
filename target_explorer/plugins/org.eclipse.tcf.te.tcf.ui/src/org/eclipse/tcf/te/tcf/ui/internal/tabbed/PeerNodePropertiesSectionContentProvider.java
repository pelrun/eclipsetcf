/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal.tabbed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Control;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IUIService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.ui.interfaces.services.INodePropertiesTableFilterUIDelegate;
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
		List<NodePropertiesTableTableNode> nodes = new ArrayList<NodePropertiesTableTableNode>();

		if (inputElement instanceof IPeerNode) {
			// Get the associated label provider
			IUIService service = ServiceManager.getInstance().getService(inputElement, IUIService.class);
			ILabelProvider provider = service != null ? service.getDelegate(inputElement, ILabelProvider.class) : null;

			// Get all custom properties of the node
			final Map<String, Object> properties = new HashMap<String, Object>();
			// And get all native properties of the peer
			if (Protocol.isDispatchThread()) {
				properties.putAll(((IPeerNode)inputElement).getPeer().getAttributes());
			} else {
				Protocol.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						properties.putAll(((IPeerNode)inputElement).getPeer().getAttributes());
					}
				});
			}

			INodePropertiesTableFilterUIDelegate filterDelegate = service != null ? service.getDelegate(inputElement, INodePropertiesTableFilterUIDelegate.class) : null;

			for (Entry<String, Object> entry : properties.entrySet()) {
				String name = entry.getKey();
				// Check if the property is filtered
				if (name.endsWith(".silent") || name.contains(".transient")) continue; //$NON-NLS-1$ //$NON-NLS-2$
				if (filterDelegate != null && filterDelegate.isFiltered(inputElement, name, entry.getValue())) continue;
				// Create the properties node
				NodePropertiesTableTableNode propertiesNode = new NodePropertiesTableTableNode(name, entry.getValue() != null ? entry.getValue().toString() : ""); //$NON-NLS-1$

				// Possible replacement for the node properties table table node value
				String text = null;

				// Get the label provider delegate for the input element
				ILabelProvider[] delegates = LabelProviderDelegateExtensionPointManager.getInstance().getDelegates(inputElement, false);
				if (delegates != null && delegates.length > 0) {
					text = delegates[0].getText(propertiesNode);
				}

				// Fallback to the label provider
				if (text == null && provider != null) {
					text = provider.getText(propertiesNode);
				}

				// Replace the node properties table table node value
				if (text != null && !"".equals(text)) { //$NON-NLS-1$
					propertiesNode = new NodePropertiesTableTableNode(name, text);
				}

				// Add the properties node
				nodes.add(propertiesNode);
			}
		}

		return nodes.toArray(new NodePropertiesTableTableNode[nodes.size()]);
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
