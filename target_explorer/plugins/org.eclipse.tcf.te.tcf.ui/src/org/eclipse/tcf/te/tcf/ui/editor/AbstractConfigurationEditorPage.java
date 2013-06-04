/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.editor;

import java.io.IOException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.persistence.history.HistoryManager;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IURIPersistenceService;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService;
import org.eclipse.tcf.te.runtime.statushandler.StatusHandlerUtil;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelRefreshService;
import org.eclipse.tcf.te.tcf.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.ui.sections.TargetSelectorSection;
import org.eclipse.tcf.te.ui.views.ViewsUtil;
import org.eclipse.tcf.te.ui.views.editor.pages.AbstractCustomFormToolkitEditorPage;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.TableWrapData;

/**
 * Abstract configuration editor page implementation.
 */
public abstract class AbstractConfigurationEditorPage extends AbstractCustomFormToolkitEditorPage {

	// Section to select real or simulator
	/* default */ TargetSelectorSection targetSelectorSection = null;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractEditorPage#setInput(org.eclipse.ui.IEditorInput)
	 */
	@Override
	protected void setInput(IEditorInput input) {
		IEditorInput oldInput = getEditorInput();
		// do nothing when input did not change
		if (oldInput != null && oldInput.equals(input)) {
			return;
		}
		super.setInput(input);
		final IPeerModel peerModel = (IPeerModel)input.getAdapter(IPeerModel.class);
		if (peerModel != null) {
			// save history to reopen the editor on eclipse startup
			HistoryManager.getInstance().add(getHistoryId(), peerModel.getPeerId());
		}
	}

	/**
	 * Returns the history id to use to save the editor to the
	 * history manager.
	 *
	 * @return The history id. Never <code>null</code>.
	 */
	protected abstract String getHistoryId();

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractCustomFormToolkitEditorPage#hasApplyAction()
	 */
	@Override
	protected boolean hasApplyAction() {
		return true;
	}

	/**
	 * Add the target selector section if an {@link ISimulatorService} is available.
	 * @param form The form.
	 * @param parent The parent composite.
	 */
	protected void addTargetSelectorSection(IManagedForm form, Composite parent) {
		ISimulatorService service = ServiceManager.getInstance().getService(getEditorInputNode(), ISimulatorService.class);
		if (service != null) {
			targetSelectorSection = doCreateTargetSelectorSection(form, parent);
			targetSelectorSection.getSection().setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP));
			getManagedForm().addPart(targetSelectorSection);
		}
	}

	protected TargetSelectorSection getTargetSelectorSection() {
		return targetSelectorSection;
	}

	/**
	 * Create the target selector section.
	 * @param form The form.
	 * @param parent The parent composite.
	 * @return The target selector section.
	 */
	protected TargetSelectorSection doCreateTargetSelectorSection (IManagedForm form, Composite parent) {
		return new TargetSelectorSection(getManagedForm(), parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractCustomFormToolkitEditorPage#dispose()
	 */
	@Override
	public void dispose() {
		if (targetSelectorSection != null) { targetSelectorSection.dispose(); targetSelectorSection = null; }
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractEditorPage#setActive(boolean)
	 */
	@Override
	public void setActive(boolean active) {
		super.setActive(active);

		if (targetSelectorSection != null) {
			targetSelectorSection.setActive(active);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractEditorPage#doValidate()
	 */
	@Override
	protected ValidationResult doValidate() {
		ValidationResult result = super.doValidate();

		if (targetSelectorSection != null) {
			targetSelectorSection.isValid();
			result.setResult(targetSelectorSection);
		}

		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractEditorPage#postDoSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void postDoSave(IProgressMonitor monitor) {
		super.postDoSave(monitor);

		// If necessary, write the changed peer attributes
		final Object input = getEditorInputNode();
		if (input instanceof IPeerModel) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						boolean isDynamic = !((IPeerModel)input).isStatic();

						// Get the persistence service
						IURIPersistenceService uRIPersistenceService = ServiceManager.getInstance().getService(IURIPersistenceService.class);
						if (uRIPersistenceService == null) {
							throw new IOException("Persistence service instance unavailable."); //$NON-NLS-1$
						}
						// Save the peer node to the new persistence storage
						uRIPersistenceService.write(((IPeerModel)input).getPeer(), null);

						// In case the node had been dynamically discovered before, we have to trigger a refresh
						// to the locator model to read in the newly created static peer
						if (isDynamic) {
							// Refresh the static peers
							((IPeerModel)input).getModel().getService(ILocatorModelRefreshService.class).refreshStaticPeers();

							// Reopen the editor on the current page
							ViewsUtil.reopenEditor(getEditor(), getEditor().getActivePageInstance().getId(), false);
						}
					} catch (IOException e) {
						// Build up the message template
						String template = NLS.bind(Messages.AbstractConfigurationEditorPage_error_save, ((IPeerModel)input).getName(), Messages.AbstractConfigurationEditorPage_error_possibleCause);
						// Handle the status
						StatusHandlerUtil.handleStatus(StatusHelper.getStatus(e), input, template, null, IContextHelpIds.MESSAGE_SAVE_FAILED, AbstractConfigurationEditorPage.this, null);
					}
				}
			};
			Assert.isTrue(!Protocol.isDispatchThread());
			Protocol.invokeAndWait(runnable);

			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					// Trigger a change event for the original data node
					((IPeerModel)input).fireChangeEvent("properties", null, ((IPeerModel)input).getProperties()); //$NON-NLS-1$
				}
			});
		}
	}
}
