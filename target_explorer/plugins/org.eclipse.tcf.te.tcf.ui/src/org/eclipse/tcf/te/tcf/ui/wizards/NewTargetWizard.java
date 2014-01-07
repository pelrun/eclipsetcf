/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.wizards;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IURIPersistenceService;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService;
import org.eclipse.tcf.te.runtime.services.interfaces.constants.IPropertiesAccessServiceConstants;
import org.eclipse.tcf.te.tcf.core.peers.Peer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.ui.wizards.pages.NewTargetWizardPage;
import org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode;
import org.eclipse.tcf.te.ui.swt.DisplayUtil;
import org.eclipse.tcf.te.ui.views.ViewsUtil;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.tcf.te.ui.wizards.AbstractWizard;
import org.eclipse.tcf.te.ui.wizards.pages.AbstractWizardPage;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * New peer wizard implementation.
 */
public class NewTargetWizard extends AbstractWizard implements INewWizard {
	// Session wide new peer counter
	private final static AtomicInteger counter = new AtomicInteger();

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);
		// Set the window title
		setWindowTitle(Messages.NewTargetWizard_windowTitle);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		addPage(new NewTargetWizardPage());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.wizards.AbstractWizard#getInitialData()
	 */
	@Override
	protected IPropertiesContainer getInitialData() {
		IStructuredSelection selection = getSelection();
		if (selection != null) {
			IPeer peer = null;
			if (selection.getFirstElement() instanceof IPeer) {
				peer = (IPeer)selection.getFirstElement();
			}
			if (selection.getFirstElement() instanceof IPeerNode) {
				peer = ((IPeerNode)selection.getFirstElement()).getPeer();
			}

			if (peer != null) {
				IPropertiesContainer data = new PropertiesContainer();
				IPropertiesAccessService service = ServiceManager.getInstance().getService(peer, IPropertiesAccessService.class);
				Map<String,String> attrs = service.getTargetAddress(peer);
				String peerName = attrs.get(IPropertiesAccessServiceConstants.PROP_NAME);
				String peerHost = attrs.get(IPropertiesAccessServiceConstants.PROP_ADDRESS);
				String peerPort = attrs.get(IPropertiesAccessServiceConstants.PROP_PORT);

				data.setProperty(IPeer.ATTR_NAME, peerName);
				data.setProperty(IPeer.ATTR_IP_HOST, peerHost);
				data.setProperty(IPeer.ATTR_IP_PORT, peerPort);

				return data;
			}
		}
		return super.getInitialData();
	}

	/**
	 * Extract the peer attributes from the wizard pages.
	 *
	 * @param peerAttributes The peer attributes. Must not be <code>null</code>.
	 */
	protected void extractData(IPropertiesContainer peerAttributes) {
		Assert.isNotNull(peerAttributes);

		// Walk through the page list and extract the attributes from it
		for (IWizardPage page : getPages()) {
			if (page instanceof AbstractWizardPage) ((AbstractWizardPage)page).saveWidgetValues();
			if (page instanceof IDataExchangeNode) {
				((IDataExchangeNode)page).extractData(peerAttributes);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		// Create the peer attributes
		final IPropertiesContainer peerAttributes = new PropertiesContainer();

		// Extract the data from the wizard pages
		extractData(peerAttributes);

		// Fill in the minimum set of peer attributes to create a new peer
		if (!peerAttributes.containsKey(IPeer.ATTR_ID)) {
			peerAttributes.setProperty(IPeer.ATTR_ID, UUID.randomUUID().toString());
		}
		if (!peerAttributes.containsKey(IPeer.ATTR_NAME)) {
			peerAttributes.setProperty(IPeer.ATTR_NAME, NLS.bind(Messages.NewTargetWizard_newPeer_name, Integer.valueOf(counter.incrementAndGet())));
		}

		// Convert the properties container into a Map<String, String>
		final Map<String, String> attrs = new HashMap<String, String>();
		for (Entry<String, Object> entry : peerAttributes.getProperties().entrySet()) {
			if (entry.getKey() == null || entry.getValue() == null) continue;
			attrs.put(entry.getKey(), entry.getValue() instanceof String ? (String)entry.getValue() : entry.getValue().toString());
		}

		try {
			// Save the new peer
			IURIPersistenceService persistenceService = ServiceManager.getInstance().getService(IURIPersistenceService.class);
			if (persistenceService == null) {
				throw new IOException("Persistence service instance unavailable."); //$NON-NLS-1$
			}
			persistenceService.write(new Peer(attrs), null);

			// Trigger a refresh of the model to read in the newly created static peer
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					IPeerModelRefreshService service = ModelManager.getPeerModel().getService(IPeerModelRefreshService.class);
					// Refresh the model now (must be executed within the TCF dispatch thread)
					if (service != null) service.refresh(new Callback() {
						@Override
						protected void internalDone(Object caller, IStatus status) {
							// Get the peer model node from the model and select it in the tree
							final IPeerNode peerNode = ModelManager.getPeerModel().getService(IPeerModelLookupService.class).lkupPeerModelById(attrs.get(IPeer.ATTR_ID));
							if (peerNode != null) {
								// Refresh the viewer
								ViewsUtil.refresh(IUIConstants.ID_EXPLORER);
								// Create the selection
								ISelection selection = new StructuredSelection(peerNode);
								// Set the selection
								ViewsUtil.setSelection(IUIConstants.ID_EXPLORER, selection);
								// And open the properties on the selection
								if (isOpenEditorOnPerformFinish()) ViewsUtil.openEditor(selection);
								// Allow subclasses to add logic to the performFinish().
								DisplayUtil.safeAsyncExec(new Runnable() {
									@Override
									public void run() {
										postPerformFinish(peerNode);
									}
								});
							}
						}
					});
				}
			});
		} catch (IOException e) {
			if (getContainer().getCurrentPage() instanceof WizardPage) {
				String message = NLS.bind(Messages.NewTargetWizard_error_savePeer, e.getLocalizedMessage());
				((WizardPage)getContainer().getCurrentPage()).setMessage(message, IMessageProvider.ERROR);
				getContainer().updateMessage();
			}
			return false;
		}

		return true;
	}

	/**
	 * Returns if or if not the wizard should open the editor
	 * on "Finish". The default is <code>true</code>.
	 *
	 * @return <code>True</code> to open the editor, <code>false</code> otherwise.
	 */
	protected boolean isOpenEditorOnPerformFinish() {
		return true;
	}

	/**
	 * Called from {@link #performFinish()} after the configuration got created.
	 * <p>
	 * <b>Note:</b> The method is called from within the UI thread.
	 *
	 * @param peerNode The peer model node. Must not be <code>null</code>.
	 */
	protected void postPerformFinish(IPeerNode peerNode) {
		// Do nothing
	}

}
