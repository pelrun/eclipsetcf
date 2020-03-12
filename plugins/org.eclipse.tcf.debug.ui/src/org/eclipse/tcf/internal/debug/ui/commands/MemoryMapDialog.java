/*******************************************************************************
 * Copyright (c) 2010-2020 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.commands;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.osgi.service.prefs.Preferences;

class MemoryMapDialog extends Dialog {

    private final TCFNode node;
    private final ILaunchConfiguration cfg;
    private MemoryMapWidget widget;
    private Button ok_button;

    MemoryMapDialog(Shell parent, TCFNode node) {
        super(parent);
        this.node = node;
        cfg = node.getModel().getLaunch().getLaunchConfiguration();
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        String key = MemoryMapDialog.class.getCanonicalName();
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(key);
        if (section != null) return section;
        return settings.addNewSection(key);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Symbol Files");
        shell.setImage(ImageCache.getImage(ImageCache.IMG_MEMORY_MAP));
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        ok_button = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        ok_button.setEnabled(widget != null && widget.getMemoryMapID() != null);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite)super.createDialogArea(parent);
        Preferences prefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        prefs = prefs.node(MemoryMapDialog.class.getCanonicalName());
        widget = new MemoryMapWidget(composite, node, prefs);
        widget.loadData(cfg);
        if (ok_button != null) ok_button.setEnabled(widget.getMemoryMapID() != null);
        composite.setSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        return composite;
    }

    @Override
    protected void okPressed() {
        try {
            ILaunchConfigurationWorkingCopy copy = cfg.getWorkingCopy();
            if (widget.saveData(copy)) copy.doSave();
            super.okPressed();
        }
        catch (Throwable x) {
            MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
            mb.setText("Cannot update memory map");
            mb.setMessage(TCFModel.getErrorMessage(x, true));
            mb.open();
        }
    }

    @Override
    public boolean close() {
        widget.dispose();
        return super.close();
    }
}
