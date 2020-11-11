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

import java.math.BigInteger;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
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
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.services.IMemoryMap;

class MemoryMapItemDialog extends Dialog {

    private final Map<String,Object> props;
    private final boolean enable_editing;

    private Text addr_text;
    private Text size_text;
    private Text offset_text;
    private Text query_text;
    private Text file_text;
    private Button loc_original;
    private Button loc_addrress;
    private Button loc_offset;
    private Button rd_button;
    private Button wr_button;
    private Button ex_button;
    private Color text_enabled;
    private Color text_disabled;

    MemoryMapItemDialog(Shell parent, Map<String,Object> props, boolean enable_editing) {
        super(parent);
        this.props = props;
        this.enable_editing = enable_editing;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Symbol File");
        shell.setImage(ImageCache.getImage(ImageCache.IMG_MEMORY_MAP));
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        if (enable_editing) {
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        }
        updateButtons();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite)super.createDialogArea(parent);
        createStatusFields(composite);
        createFileNameFields(composite);
        createAddressFields(composite);
        createPropsFields(composite);
        createQueryFields(composite);
        setData();
        composite.setSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        return composite;
    }

    protected void createStatusFields(Composite parent) {
    }

    private void createFileNameFields(Composite parent) {
        Font font = parent.getFont();
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        composite.setFont(font);
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label file_label = new Label(composite, SWT.WRAP);
        file_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        file_label.setFont(font);
        file_label.setText("File name:");

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 400;
        file_text = new Text(composite, SWT.SINGLE | SWT.BORDER);
        file_text.setLayoutData(gd);
        file_text.setFont(font);
        if (!enable_editing) {
            file_text.setEditable(false);
            file_text.setBackground(parent.getBackground());
        }

        file_text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateButtons();
            }
        });

        Button button = new Button(composite, SWT.PUSH);
        button.setFont(font);
        button.setText("...");
        button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        button.setEnabled(enable_editing);
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                FileDialog file_dialog = new FileDialog(getShell(), SWT.NONE);
                file_dialog.setFileName(file_text.getText());
                String path = file_dialog.open();
                if (path != null) file_text.setText(path);
            }
        });
    }

    private void createAddressFields(Composite parent) {
        Font font = parent.getFont();

        Group group = new Group(parent, SWT.NONE);
        group.setText("Memory location");
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        loc_original = new Button(group, SWT.RADIO);
        loc_original.setText("No relocation: use program headers from the file");
        loc_original.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (loc_original.getSelection()) {
                    updateButtons();
                }
            }
        });

        loc_offset = new Button(group, SWT.RADIO);
        loc_offset.setText("Offset - difference between load address and link address");
        loc_offset.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (loc_offset.getSelection()) {
                    updateButtons();
                }
            }
        });

        loc_addrress = new Button(group, SWT.RADIO);
        loc_addrress.setText("Absolute memory address");
        loc_addrress.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (loc_addrress.getSelection()) {
                    updateButtons();
                }
            }
        });

        GridData gd = new GridData();
        gd.widthHint = 200;
        addr_text = new Text(group, SWT.SINGLE | SWT.BORDER);
        addr_text.setLayoutData(gd);
        addr_text.setFont(font);
        text_enabled = addr_text.getBackground();
        text_disabled = parent.getBackground();

        addr_text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateButtons();
            }
        });
    }

    private void createPropsFields(Composite parent) {
        Font font = parent.getFont();
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(font);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createTextFields(composite);
        createFlagsGroup(composite);
    }

    private void createTextFields(Composite parent) {
        Font font = parent.getFont();
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        composite.setFont(font);
        composite.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 250;
        composite.setLayoutData(gd);

        Label size_label = new Label(composite, SWT.WRAP);
        size_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        size_label.setFont(font);
        size_label.setText("Size:");

        size_text = new Text(composite, SWT.SINGLE | SWT.BORDER);
        size_text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        size_text.setFont(font);

        Label offset_label = new Label(composite, SWT.WRAP);
        offset_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        offset_label.setFont(font);
        offset_label.setText("File offset:");

        offset_text = new Text(composite, SWT.SINGLE | SWT.BORDER);
        offset_text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        offset_text.setFont(font);
    }

    private void createFlagsGroup(Composite parent) {
        Font font = parent.getFont();

        Group group = new Group(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.verticalSpacing = 0;
        layout.numColumns = 1;
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        group.setFont(font);
        group.setText("Flags");

        rd_button = new Button(group, SWT.CHECK);
        rd_button.setFont(font);
        rd_button.setText("Data read");
        rd_button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        wr_button = new Button(group, SWT.CHECK);
        wr_button.setFont(font);
        wr_button.setText("Data write");
        wr_button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ex_button = new Button(group, SWT.CHECK);
        ex_button.setFont(font);
        ex_button.setText("Instructions read");
        ex_button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    private void createQueryFields(Composite parent) {
        Font font = parent.getFont();

        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        composite.setFont(font);
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label query_label = new Label(composite, SWT.WRAP);
        query_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        query_label.setFont(font);
        query_label.setText("Context query:");

        query_text = new Text(composite, SWT.SINGLE | SWT.BORDER);
        query_text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        query_text.setFont(font);
        if (!enable_editing) {
            query_text.setEditable(false);
            query_text.setBackground(parent.getBackground());
        }
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

    private void setText(Text text, String str) {
        if (str == null) str = "";
        text.setText(str);
    }

    private void setData() {
        if (props.get(IMemoryMap.PROP_ADDRESS) != null) {
            setText(addr_text, toHex((Number)props.get(IMemoryMap.PROP_ADDRESS)));
            loc_addrress.setSelection(true);
        }
        else if (props.get(IMemoryMap.PROP_BASE_ADDRESS) != null) {
            setText(addr_text, toHex((Number)props.get(IMemoryMap.PROP_BASE_ADDRESS)));
            loc_offset.setSelection(true);
        }
        else {
            addr_text.setText("");
            loc_original.setSelection(true);
        }
        setText(size_text, toHex((Number)props.get(IMemoryMap.PROP_SIZE)));
        if (props.get(IMemoryMap.PROP_SECTION_NAME) != null) {
            setText(offset_text, (String)props.get(IMemoryMap.PROP_SECTION_NAME));
        }
        else {
            setText(offset_text, toHex((Number)props.get(IMemoryMap.PROP_OFFSET)));
        }
        setText(query_text, (String)props.get(IMemoryMap.PROP_CONTEXT_QUERY));
        setText(file_text, (String)props.get(IMemoryMap.PROP_FILE_NAME));
        int flags = 0;
        Number n = (Number)props.get(IMemoryMap.PROP_FLAGS);
        if (n != null) flags = n.intValue();
        rd_button.setSelection((flags & IMemoryMap.FLAG_READ) != 0);
        wr_button.setSelection((flags & IMemoryMap.FLAG_WRITE) != 0);
        ex_button.setSelection((flags & IMemoryMap.FLAG_EXECUTE) != 0);
        updateButtons();
    }

    private void getNumber(Text text, String key) {
        String s = text.getText().trim();
        if (s == null || s.length() == 0) {
            props.remove(key);
        }
        else if (s.startsWith("0x")) {
            props.put(key, new BigInteger(s.substring(2), 16));
        }
        else {
            props.put(key, new BigInteger(s));
        }
    }

    private void getText(Text text, String key) {
        String s = text.getText().trim();
        if (s == null || s.length() == 0) {
            props.remove(key);
        }
        else {
            props.put(key, s);
        }
    }

    private void getData() {
        props.remove(IMemoryMap.PROP_ADDRESS);
        props.remove(IMemoryMap.PROP_BASE_ADDRESS);
        props.remove(IMemoryMap.PROP_SIZE);
        props.remove(IMemoryMap.PROP_OFFSET);
        props.remove(IMemoryMap.PROP_SECTION_NAME);
        getText(file_text, IMemoryMap.PROP_FILE_NAME);
        if (loc_addrress.getSelection()) {
            getNumber(addr_text, IMemoryMap.PROP_ADDRESS);
            getNumber(size_text, IMemoryMap.PROP_SIZE);
            String s = offset_text.getText().trim();
            if (s.length() > 0 && !Character.isDigit(s.charAt(0))) {
                props.put(IMemoryMap.PROP_SECTION_NAME, s);
            }
            else {
                getNumber(offset_text, IMemoryMap.PROP_OFFSET);
            }
        }
        else if (loc_offset.getSelection()) {
            getNumber(addr_text, IMemoryMap.PROP_BASE_ADDRESS);
        }
        int flags = 0;
        if (rd_button.getSelection()) flags |= IMemoryMap.FLAG_READ;
        if (wr_button.getSelection()) flags |= IMemoryMap.FLAG_WRITE;
        if (ex_button.getSelection()) flags |= IMemoryMap.FLAG_EXECUTE;
        props.put(IMemoryMap.PROP_FLAGS, flags);
        getText(query_text, IMemoryMap.PROP_CONTEXT_QUERY);
    }

    private void updateButtons() {
        Button btn = getButton(IDialogConstants.OK_ID);
        if (btn != null) {
            btn.setEnabled(!enable_editing ||
                    file_text != null && file_text.getText().trim().length() > 0 ||
                    addr_text != null && addr_text.getText().trim().length() > 0);
        }
        loc_original.setEnabled(enable_editing);
        loc_offset.setEnabled(enable_editing);
        loc_addrress.setEnabled(enable_editing);
        if (!enable_editing || loc_original.getSelection()) {
            addr_text.setEditable(false);
            addr_text.setBackground(text_disabled);
        }
        else {
            addr_text.setEditable(true);
            addr_text.setBackground(text_enabled);
        }
        if (!enable_editing || loc_original.getSelection() || loc_offset.getSelection()) {
            size_text.setEditable(false);
            size_text.setBackground(text_disabled);
            offset_text.setEditable(false);
            offset_text.setBackground(text_disabled);
            rd_button.setEnabled(false);
            wr_button.setEnabled(false);
            ex_button.setEnabled(false);
        }
        else {
            size_text.setEditable(true);
            size_text.setBackground(text_enabled);
            offset_text.setEditable(true);
            offset_text.setBackground(text_enabled);
            rd_button.setEnabled(true);
            wr_button.setEnabled(true);
            ex_button.setEnabled(true);
        }
    }

    @Override
    protected void okPressed() {
        if (enable_editing) {
            try {
                getData();
            }
            catch (Throwable x) {
                MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
                mb.setText("Invalid data");
                mb.setMessage(TCFModel.getErrorMessage(x, true));
                mb.open();
                return;
            }
        }
        super.okPressed();
    }
}
