/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.editor;

import java.io.IOException;
import java.util.EventObject;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IURIPersistenceService;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.statushandler.StatusHandlerUtil;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

/**
 * Abstract configuration editor page implementation.
 */
public abstract class AbstractConfigurationEditorPage extends AbstractCustomFormToolkitEditorPage {

	private IEventListener listener = null;

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
	    if (listener == null) {
	    	listener = new IEventListener() {
				@SuppressWarnings("synthetic-access")
                @Override
				public void eventFired(EventObject event) {
					ChangeEvent changeEvent = (ChangeEvent)event;
					if ((IPeerNodeProperties.PROPERTY_CONNECT_STATE.equals(changeEvent.getEventId()) ||
									IPeerNodeProperties.PROPERTY_IS_VALID.equals(changeEvent.getEventId()) ||
									IPeerNodeProperties.PROPERTY_WARNINGS.equals(changeEvent.getEventId())) &&
									event.getSource() == getEditorInputNode()) {
						ExecutorsUtil.executeInUI(new Runnable() {
							@Override
							public void run() {
								if (!getManagedForm().getForm().isDisposed()) {
									setFormTitle(getFormTitle());
									setFormImage(getFormImage());
								}
							}
						});
					}
				}
			};
	    	EventManager.getInstance().addEventListener(listener, ChangeEvent.class);
	    }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractCustomFormToolkitEditorPage#dispose()
	 */
	@Override
	public void dispose() {
		if (listener != null) { EventManager.getInstance().removeEventListener(listener); listener = null; }
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractEditorPage#postDoSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void postDoSave(IProgressMonitor monitor) {
		super.postDoSave(monitor);

		// If necessary, write the changed peer attributes
		final Object input = getEditorInputNode();
		if (input instanceof IPeerNode) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						// Get the persistence service
						IURIPersistenceService uRIPersistenceService = ServiceManager.getInstance().getService(IURIPersistenceService.class);
						if (uRIPersistenceService == null) {
							throw new IOException("Persistence service instance unavailable."); //$NON-NLS-1$
						}
						// Save the peer node to the new persistence storage
						uRIPersistenceService.write(((IPeerNode)input).getPeer(), null);

						// Reopen the editor on the current page
//						ViewsUtil.reopenEditor(getEditor(), getEditor().getActivePageInstance().getId(), false);
					} catch (IOException e) {
						// Build up the message template
						String template = NLS.bind(Messages.AbstractConfigurationEditorPage_error_save, ((IPeerNode)input).getName(), Messages.AbstractConfigurationEditorPage_error_possibleCause);
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
					((IPeerNode)input).fireChangeEvent("properties", null, ((IPeerNode)input).getProperties()); //$NON-NLS-1$
				}
			});

			IPathMapService service = ServiceManager.getInstance().getService(input, IPathMapService.class);
			if (service != null) {
				service.generateSourcePathMappings(input);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractCustomFormToolkitEditorPage#doCreateLinkContribution(org.eclipse.jface.action.IToolBarManager)
	 */
	@Override
	protected IContributionItem doCreateLinkContribution(final IToolBarManager tbManager) {
		return new ControlContribution("SetAsDefaultContextLink") { //$NON-NLS-1$
			IEventListener eventListener = null;
			@Override
			public void dispose() {
				super.dispose();
				if (eventListener == null) {
					EventManager.getInstance().removeEventListener(eventListener);
				}
			}
			@Override
			protected Control createControl(Composite parent) {
				final ImageHyperlink hyperlink = new ImageHyperlink(parent, SWT.NONE);
				hyperlink.setText("Set as default connection"); //$NON-NLS-1$
				hyperlink.setUnderlined(true);
				hyperlink.setForeground(getManagedForm().getToolkit().getHyperlinkGroup().getForeground());
				IPeerNode defaultNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
				setVisible(defaultNode == null || defaultNode != getEditorInputNode());
				hyperlink.addHyperlinkListener(new IHyperlinkListener() {
					@Override
					public void linkActivated(HyperlinkEvent e) {
						if (getEditorInputNode() instanceof IPeerNode) {
							ServiceManager.getInstance().getService(IDefaultContextService.class).setDefaultContext((IPeerNode)getEditorInputNode());
						}
					}
					@Override
					public void linkEntered(HyperlinkEvent e) {
						hyperlink.setForeground(getManagedForm().getToolkit().getHyperlinkGroup().getActiveForeground());
					}
					@Override
					public void linkExited(HyperlinkEvent e) {
						hyperlink.setForeground(getManagedForm().getToolkit().getHyperlinkGroup().getForeground());
					}
				});

				eventListener = new IEventListener() {
					@Override
					public void eventFired(EventObject event) {
						if (event instanceof ChangeEvent) {
							ChangeEvent changeEvent = (ChangeEvent)event;
							if (changeEvent.getSource() instanceof IDefaultContextService) {
								IPeerNode defaultNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
								setVisible(defaultNode == null || getEditorInputNode() == null || defaultNode != getEditorInputNode());
								ExecutorsUtil.executeInUI(new Runnable() {
									@Override
									public void run() {
										tbManager.update(true);
									}
								});
							}
						}
					}
				};

				EventManager.getInstance().addEventListener(eventListener, ChangeEvent.class);

				return hyperlink;
			}
		};
	}
}
