/*******************************************************************************
 * Copyright (c) 2013, 2015 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.profiler;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.services.IProfiler;

class ProfilerSettingsDlg extends Dialog {

    static final String PARAM_VIEW_UPDATE_PERIOD = "ViewUpdatePeriod";
    static final String PARAM_AGGREGATE = "Aggregate";
    static final String PARAM_STACK_TRACE = "StackTrace";

    Map<String,Object> conf;
    Map<String,Object> data;

    private Button aggregate_button;
    private Button stack_trace_button;
    private Text frame_cnt_text;
    private Text view_update_text;

    protected ProfilerSettingsDlg(Shell shell, Map<String,Object> conf) {
        super(shell);
        this.conf = conf;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Profiler Configuration");
        shell.setImage(ImageCache.getImage(ImageCache.IMG_PROFILER));
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
        createFields(composite);
        setData();
        composite.setSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        return composite;
    }

    private void createFields(Composite parent) {
        Font font = parent.getFont();
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        composite.setFont(font);
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        aggregate_button = new Button(composite, SWT.CHECK);
        aggregate_button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL, 0, true, false, 2, 1));
        aggregate_button.setFont(font);
        aggregate_button.setText("Aggregate per function");

        stack_trace_button = new Button(composite, SWT.CHECK);
        stack_trace_button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL, 0, true, false, 2, 1));
        stack_trace_button.setFont(font);
        stack_trace_button.setText("Enable stack tracing");

        stack_trace_button.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateButtons();
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                updateButtons();
            }
        });

        Label frame_cnt_label = new Label(composite, SWT.WRAP);
        frame_cnt_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        frame_cnt_label.setFont(font);
        frame_cnt_label.setText("Max stack frames count:");

        frame_cnt_text = new Text(composite, SWT.SINGLE | SWT.BORDER);
        frame_cnt_text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        frame_cnt_text.setFont(font);

        frame_cnt_text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateButtons();
            }
        });

        Label view_update_label = new Label(composite, SWT.WRAP);
        view_update_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        view_update_label.setFont(font);
        view_update_label.setText("View update interval (msec):");

        view_update_text = new Text(composite, SWT.SINGLE | SWT.BORDER);
        view_update_text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        view_update_text.setFont(font);

        view_update_text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateButtons();
            }
        });
    }

    private void updateButtons() {
        Button btn = getButton(IDialogConstants.OK_ID);
        if (btn != null) btn.setEnabled(true);
        frame_cnt_text.setEnabled(stack_trace_button.getSelection());
    }

    private void setData() {
        Boolean b_ag = (Boolean)conf.get(PARAM_AGGREGATE);
        aggregate_button.setSelection(b_ag != null && b_ag.booleanValue());
        Boolean b_st = (Boolean)conf.get(PARAM_STACK_TRACE);
        stack_trace_button.setSelection(b_st != null && b_st.booleanValue());
        Number n_fc = (Number)conf.get(IProfiler.PARAM_FRAME_CNT);
        if (n_fc == null) n_fc = Integer.valueOf(1);
        frame_cnt_text.setText(n_fc.toString());
        Number n_vu = (Number)conf.get(PARAM_VIEW_UPDATE_PERIOD);
        if (n_vu == null) n_vu = Integer.valueOf(1000);
        view_update_text.setText(n_vu.toString());
    }

    private void getData() {
        data = new HashMap<String,Object>();
        data.put(PARAM_AGGREGATE, aggregate_button.getSelection());
        data.put(PARAM_STACK_TRACE, stack_trace_button.getSelection());
        data.put(IProfiler.PARAM_FRAME_CNT, new Integer(frame_cnt_text.getText()));
        data.put(PARAM_VIEW_UPDATE_PERIOD, new Integer(view_update_text.getText()));
    }

    @Override
    protected void okPressed() {
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
        super.okPressed();
    }
}
