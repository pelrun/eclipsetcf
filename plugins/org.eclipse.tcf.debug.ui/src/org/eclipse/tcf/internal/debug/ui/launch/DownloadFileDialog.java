/*******************************************************************************
 * Copyright (c) 2013 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.launch;

import java.math.BigInteger;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.protocol.JSON;

class DownloadFileDialog extends Dialog {

    private final Image image;
    private final String peer_id;
    private final Map<String,Object> map;

    private Text context_text;
    private Text file_text;
    private Text addr_text;
    private Text size_text;
    private Text offs_text;
    private Button file_button;
    private Button context_button;
    private Button load_syms_button;
    private Button relocate_button;
    private Button download_button;
    private Button set_pc_button;
    private Button osa_button;

    DownloadFileDialog(Shell parentShell, String peer_id,  Map<String,Object> map) {
        super(parentShell);
        this.image = ImageCache.getImage(ImageCache.IMG_DOWNLOAD_TAB);
        this.peer_id = peer_id;
        this.map = map;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Download File"); //$NON-NLS-1$
        shell.setImage(image);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        updateButtons();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite)super.createDialogArea(parent);
        createFileFields(composite);
        setData();
        composite.setSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        return composite;
    }

    private void createFileFields(Composite parent) {
        Font font = parent.getFont();
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        composite.setFont(font);
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label context_label = new Label(composite, SWT.NONE);
        context_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        context_label.setFont(font);
        context_label.setText("Context:"); //$NON-NLS-1$

        context_text = new Text(composite, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 400;
        context_text.setLayoutData(gd);
        context_text.setFont(font);

        context_text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateButtons();
            }
        });

        context_button = new Button(composite, SWT.PUSH);
        context_button.setText("Select..."); //$NON-NLS-1$
        context_button.setFont(font);
        context_button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        context_button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String context = context_text.getText().trim();
                ContextSelection selection = new ContextSelection();
                selection.fPeerId = peer_id;
                selection.fContextFullName = context;
                ContextSelectionDialog diag = new ContextSelectionDialog(DownloadFileDialog.this, false);
                diag.setSelection(selection);
                if (diag.open() == Window.OK) {
                    selection = diag.getSelection();
                    context_text.setText(selection.fContextFullName);
                }
            }
        });

        Label file_label = new Label(composite, SWT.NONE);
        file_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        file_label.setFont(font);
        file_label.setText("File:"); //$NON-NLS-1$

        file_text = new Text(composite, SWT.SINGLE | SWT.BORDER);
        file_text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        file_text.setFont(font);

        file_text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateButtons();
            }
        });

        file_button = new Button(composite, SWT.PUSH);
        file_button.setText("Browse..."); //$NON-NLS-1$
        file_button.setFont(font);
        file_button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        file_button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String path = file_text.getText().trim();
                if (path.length() == 0) path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
                FileDialog dialog = new FileDialog(getShell(), SWT.NONE);
                dialog.setFilterPath(path);
                path = dialog.open();
                if (path != null) file_text.setText(path);
            }
        });

        load_syms_button = new Button(composite, SWT.CHECK);
        load_syms_button.setText("Load symbols");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        load_syms_button.setLayoutData(gd);
        load_syms_button.setFont(font);
        load_syms_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateButtons();
            }
        });
        load_syms_button.setEnabled(true);

        relocate_button = new Button(composite, SWT.CHECK);
        relocate_button.setText("Relocate the file");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        relocate_button.setLayoutData(gd);
        relocate_button.setFont(font);
        relocate_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateButtons();
            }
        });
        relocate_button.setEnabled(true);
        Composite rel_group = createRelocateGroup(composite);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        rel_group.setLayoutData(gd);

        download_button = new Button(composite, SWT.CHECK);
        download_button.setText("Download the file into the context memory");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        download_button.setLayoutData(gd);
        download_button.setFont(font);
        download_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateButtons();
            }
        });
        download_button.setEnabled(true);

        set_pc_button = new Button(composite, SWT.CHECK);
        set_pc_button.setText("Set PC to program entry address");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        set_pc_button.setLayoutData(gd);
        set_pc_button.setFont(font);
        set_pc_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateButtons();
            }
        });
        set_pc_button.setEnabled(true);

        osa_button = new Button(composite, SWT.CHECK);
        osa_button.setText("Enable OS awareness - the file is an OS kernel");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        osa_button.setLayoutData(gd);
        osa_button.setFont(font);
        osa_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateButtons();
            }
        });
        osa_button.setEnabled(true);
    }

    private Composite createRelocateGroup(Composite parent) {
        Font font = parent.getFont();

        Group group = new Group(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.verticalSpacing = 0;
        layout.numColumns = 2;
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        group.setFont(font);
        group.setText("File location in the context memory");

        Label addr_label = new Label(group, SWT.WRAP);
        addr_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        addr_label.setFont(font);
        addr_label.setText("Address:");

        addr_text = new Text(group, SWT.SINGLE | SWT.BORDER);
        addr_text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        addr_text.setFont(font);

        Label size_label = new Label(group, SWT.WRAP);
        size_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        size_label.setFont(font);
        size_label.setText("Size:");

        size_text = new Text(group, SWT.SINGLE | SWT.BORDER);
        size_text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        size_text.setFont(font);

        Label offset_label = new Label(group, SWT.WRAP);
        offset_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        offset_label.setFont(font);
        offset_label.setText("File offset:");

        offs_text = new Text(group, SWT.SINGLE | SWT.BORDER);
        offs_text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        offs_text.setFont(font);

        return group;
    }

    private String toHex(Number n) {
        if (n == null) return null;
        BigInteger x = JSON.toBigInteger(n);
        String s = x.toString(16);
        int l = 16 - s.length();
        if (l < 0) l = 0;
        if (l > 16) l = 16;
        return "0x0000000000000000".substring(0, 2 + l) + s;
    }

    private void setString(Text text, String key) {
        String s = (String)map.get(key);
        if (s == null) s = "";
        text.setText(s);
    }

    private void setBoolean(Button btn, String key) {
        Boolean b = (Boolean)map.get(key);
        if (b == null) btn.setSelection(false);
        else btn.setSelection(b.booleanValue());
    }

    private void setNumber(Text text, String key) {
        Number n = (Number)map.get(key);
        if (n == null) text.setText("");
        else text.setText(toHex(n));
    }

    private void setData() {
        setString(context_text, TCFLaunchDelegate.FILES_CONTEXT_FULL_NAME);
        setString(file_text, TCFLaunchDelegate.FILES_FILE_NAME);
        setBoolean(load_syms_button, TCFLaunchDelegate.FILES_LOAD_SYMBOLS);
        setBoolean(relocate_button, TCFLaunchDelegate.FILES_RELOCATE);
        setBoolean(download_button, TCFLaunchDelegate.FILES_DOWNLOAD);
        setBoolean(set_pc_button, TCFLaunchDelegate.FILES_SET_PC);
        setBoolean(osa_button, TCFLaunchDelegate.FILES_ENABLE_OSA);
        setNumber(addr_text, TCFLaunchDelegate.FILES_ADDRESS);
        setNumber(offs_text, TCFLaunchDelegate.FILES_OFFSET);
        setNumber(size_text, TCFLaunchDelegate.FILES_SIZE);
        updateButtons();
    }

    private void getBoolean(Button btn, String key) {
        boolean b = btn.getSelection();
        if (!b) {
            map.remove(key);
        }
        else {
            map.put(key, Boolean.TRUE);
        }
    }

    private void getString(Text text, String key) {
        String s = text.getText().trim();
        if (s == null || s.length() == 0) {
            map.remove(key);
        }
        else {
            map.put(key, s);
        }
    }

    private void getNumber(Text text, String key) {
        String s = text.getText().trim();
        if (s == null || s.length() == 0) {
            map.remove(key);
        }
        else if (s.startsWith("0x")) {
            map.put(key, new BigInteger(s.substring(2), 16));
        }
        else {
            map.put(key, new BigInteger(s));
        }
    }

    private void getData() {
        getString(context_text, TCFLaunchDelegate.FILES_CONTEXT_FULL_NAME);
        getString(file_text, TCFLaunchDelegate.FILES_FILE_NAME);
        getBoolean(load_syms_button, TCFLaunchDelegate.FILES_LOAD_SYMBOLS);
        getBoolean(relocate_button, TCFLaunchDelegate.FILES_RELOCATE);
        getBoolean(download_button, TCFLaunchDelegate.FILES_DOWNLOAD);
        getBoolean(set_pc_button, TCFLaunchDelegate.FILES_SET_PC);
        getBoolean(osa_button, TCFLaunchDelegate.FILES_ENABLE_OSA);
        getNumber(addr_text, TCFLaunchDelegate.FILES_ADDRESS);
        getNumber(offs_text, TCFLaunchDelegate.FILES_OFFSET);
        getNumber(size_text, TCFLaunchDelegate.FILES_SIZE);
    }

    private void updateButtons() {
        Button btn = getButton(IDialogConstants.OK_ID);
        if (btn != null && context_text != null && file_text != null) {
            String context = context_text.getText().trim();
            String file = file_text.getText().trim();
            btn.setEnabled(context.length() > 0 && file.length() > 0);
        }
        if (relocate_button != null) {
            boolean reloc = relocate_button.getSelection();
            addr_text.setEnabled(reloc);
            size_text.setEnabled(reloc);
            offs_text.setEnabled(reloc);
        }
    }

    @Override
    protected void okPressed() {
        try {
            getData();
        }
        catch (Throwable x) {
            MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
            mb.setText("Invalid data"); //$NON-NLS-1$
            mb.setMessage(TCFModel.getErrorMessage(x, true));
            mb.open();
            return;
        }
        super.okPressed();
    }
}
