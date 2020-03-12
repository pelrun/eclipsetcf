/*******************************************************************************
 * Copyright (c) 2011-2020 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.launch;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.internal.debug.ui.launch.ContextListControl.ContextInfo;
import org.eclipse.tcf.internal.debug.ui.launch.PeerListControl.PeerInfo;
import org.osgi.service.prefs.Preferences;

/**
 * Dialog to select a peer and context.
 */
public class ContextSelectionDialog extends Dialog {

    private final boolean processes;
    private ContextSelection selection;
    private ContextListControl context_list;

    public ContextSelectionDialog(IShellProvider parent, boolean processes) {
        super(parent);
        this.processes = processes;
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    public void setSelection(ContextSelection selection) {
        this.selection = selection;
    }

    public ContextSelection getSelection() {
        return selection;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        String key = ContextSelectionDialog.class.getCanonicalName();
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(key);
        if (section != null) return section;
        return settings.addNewSection(key);
    }

    @Override
    protected void configureShell(Shell shell) {
        shell.setText("Select Peer and Context");
        super.configureShell(shell);
    }

    @Override
    protected Control createContents(Composite parent) {
        Control control = super.createContents(parent);
        updateButtonState();
        return control;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout(1, false);
        composite.setLayout(layout);
        new Label(composite, SWT.NONE).setText("Peers:");
        Preferences prefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        prefs = prefs.node(ContextSelectionDialog.class.getCanonicalName());
        final PeerListControl peer_list = new PeerListControl(composite, prefs) {
            @Override
            protected void onPeerSelected(PeerInfo info) {
                handlePeerSelected(info);
            }
        };
        new Label(composite, SWT.NONE).setText("Contexts:");
        context_list = new ContextListControl(composite, processes);
        context_list.getTree().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ContextInfo contextInfo = context_list.findInfo((TreeItem) e.item);
                if (contextInfo != null) {
                    handleContextSelected(contextInfo);
                }
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
                if (getButton(IDialogConstants.OK_ID).isEnabled()) {
                    buttonPressed(IDialogConstants.OK_ID);
                }
            }
        });
        if (selection.fPeerId != null) {
            peer_list.setInitialSelection(selection.fPeerId);
        }
        if (selection.fContextFullName != null) {
            context_list.setInitialSelection(selection.fContextFullName);
        }
        return composite;
    }

    private void updateButtonState() {
        getButton(IDialogConstants.OK_ID).setEnabled(selection.fContextId != null);
    }

    protected void handleContextSelected(ContextInfo info) {
        selection.fContextId = info.id;
        selection.fContextName = info.name;
        selection.fContextFullName = context_list.getFullName(info);
        selection.fIsAttached = info.is_attached;
        updateButtonState();
    }

    protected void handlePeerSelected(PeerInfo info) {
        selection.fPeerId = info.id;
        context_list.setInput(info.peer);
    }
}
