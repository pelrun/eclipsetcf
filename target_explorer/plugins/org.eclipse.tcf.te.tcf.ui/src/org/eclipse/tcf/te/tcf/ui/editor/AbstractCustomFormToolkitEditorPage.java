/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.ui.editor;

import java.util.EventObject;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

/**
 * AbstractCustomFormToolkitEditorPage
 */
public abstract class AbstractCustomFormToolkitEditorPage extends org.eclipse.tcf.te.ui.views.editor.pages.AbstractCustomFormToolkitEditorPage {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractEditorPage#setInput(org.eclipse.ui.IEditorInput)
	 */
	@Override
	protected void setInput(IEditorInput input) {
	    super.setInput(input);
	    if (getEditorInputNode() instanceof IPeerNode && !((IPeerNode)getEditorInputNode()).isVisible()) {
	    	getEditor().close(false);
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
				hyperlink.setText(Messages.AbstractCustomFormToolkitEditorPage_setAsDefault_link);
				hyperlink.setUnderlined(true);
				hyperlink.setForeground(getManagedForm().getToolkit().getHyperlinkGroup().getForeground());
				IPeerNode defaultNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
				setVisible(defaultNode == null || defaultNode != getEditorInputNode());
				hyperlink.addHyperlinkListener(new IHyperlinkListener() {
					@Override
					public void linkActivated(HyperlinkEvent e) {
						ServiceManager.getInstance().getService(IDefaultContextService.class).setDefaultContext((IPeerNode)getEditorInputNode());
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

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractCustomFormToolkitEditorPage#getFormTitleStateDecoration()
	 */
	@SuppressWarnings("restriction")
    @Override
	public String getFormTitleStateDecoration() {
		String stateStr = null;

		if (getEditorInputNode() instanceof IPeerNode) {
			final AtomicInteger state = new AtomicInteger(IConnectable.STATE_UNKNOWN);

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					state.set(((IPeerNode)getEditorInputNode()).getConnectState());
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);

			final int s = state.get();
			if (s == IConnectable.STATE_CONNECTED || s == IConnectable.STATE_CONNECTING || s == IConnectable.STATE_CONNECTION_LOST
					|| s == IConnectable.STATE_CONNECTION_RECOVERING || s == IConnectable.STATE_DISCONNECTING) {
				String candidate = org.eclipse.tcf.te.core.nls.Messages.getConnectStateString(s);
				if (candidate != null) stateStr = "(" + candidate + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

	    return stateStr;
	}
}
