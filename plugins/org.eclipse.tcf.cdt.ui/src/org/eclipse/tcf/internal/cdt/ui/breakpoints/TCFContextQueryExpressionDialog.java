/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui.breakpoints;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.SelectionDialog;

public class TCFContextQueryExpressionDialog extends SelectionDialog {

    private final String[] attributes;
    private String expression_text;
    private int expression_parent;
    private final Map<String,String> expression_attrs = new HashMap<String,String>();

    private final String[] column_names = new String[] { "Parameter", "Value" };

    private int pos;
    private int len;

    protected TCFContextQueryExpressionDialog(Shell parentShell, String[] attributes, String expression) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE);
        this.attributes = attributes;
        expression_text = expression;
        len = expression.length();
        parseExpression();
    }

    private void parseExpression() {
        if (pos < len && expression_text.charAt(pos) == '/') {
            pos++;
            expression_parent = pos;
            expression_attrs.clear();
        }
        while (pos < len) {
            parseExpressionPart();
            if (pos >= len) break;
            char ch = expression_text.charAt(pos);
            if (ch == '/') {
                pos++;
                expression_parent = pos;
                expression_attrs.clear();
            }
            else {
                // Syntax error
                break;
            }
        }
    }

    private void parseExpressionPart() {
        while (pos < len) {
            String name = parseString();
            if (pos < len && expression_text.charAt(pos) == '=') {
                pos++;
                String value = parseString();
                expression_attrs.put(name,  value);
            }
            if (pos < len && expression_text.charAt(pos) == ',') {
                pos++;
            }
            else {
                break;
            }
        }
    }

    private String parseString() {
        StringBuffer bf = new StringBuffer();
        if (pos < len && expression_text.charAt(pos) == '"') {
            pos++;
            while (pos < len) {
                char ch = expression_text.charAt(pos++);
                if (ch == '"') break;
                if (ch == '\\' && pos < len) {
                    ch = expression_text.charAt(pos++);
                }
                bf.append(ch);
            }
        }
        else {
            while (pos < len) {
                char ch = expression_text.charAt(pos);
                if (ch == '=' || ch == '/' || ch == ',') break;
                bf.append(ch);
                pos++;
            }
        }
        return bf.toString();
    }

    private final class ParameterTableLabelProvider extends LabelProvider implements ITableLabelProvider {
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int column) {
            if (column == 0) return (String)element;
            return expression_attrs.get((String)element);
        }
    }

    public final class ExpressionEditingSupport extends EditingSupport {

        private TextCellEditor editor;
        private ColumnViewer viewer;

        private ExpressionEditingSupport(ColumnViewer viewer) {
            super(viewer);
            this.viewer = viewer;
            editor = new TextCellEditor((Composite) getViewer().getControl(), SWT.NONE);
        }

        @Override
        protected CellEditor getCellEditor(Object element) {
            return editor;
        }

        @Override
        protected boolean canEdit(Object element) {
            return true;
        }

        @Override
        protected Object getValue(Object element) {
            String value = expression_attrs.get((String)element);
            if (value == null) value = "";
            return value;
        }

        @Override
        protected void setValue(Object element, Object value) {
            String name = (String)element;
            String str = (String)value;
            if (str == null || str.length() == 0) {
                expression_attrs.remove(name);
            }
            else {
                expression_attrs.put(name, str);
            }
            viewer.update(element, null);
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite page = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout(1, true);
        page.setLayout(gridLayout);
        page.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        TableLayout tableLayout = new TableLayout();
        tableLayout.addColumnData(new ColumnWeightData(1));
        tableLayout.addColumnData(new ColumnWeightData(1));
        Table table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        table.setLayout(tableLayout);
        TableViewer tableViewer = new TableViewer(table);
        tableViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        Control cntrl = tableViewer.getControl();
        cntrl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        TableViewerColumn labelColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        labelColumn.getColumn().setText(column_names[0]);
        TableViewerColumn valueColumn = new TableViewerColumn(tableViewer, SWT.Modify);
        valueColumn.getColumn().setText(column_names[1]);
        tableViewer.setContentProvider(new ArrayContentProvider());
        tableViewer.setLabelProvider(new ParameterTableLabelProvider());
        valueColumn.setEditingSupport(new ExpressionEditingSupport(valueColumn.getViewer()));
        tableViewer.setInput(attributes);
        tableViewer.setComparator(new ViewerComparator() {
            @Override
            public int compare(Viewer viewer, Object e1, Object e2) {
                String t1 = (String)e1;
                String t2 = (String)e2;
                return t1.compareTo(t2);
            };
        });
        return parent;
    }

    private void appendString(StringBuffer bf, String s) {
        int l = s.length();
        boolean q = false;
        for (int i = 0; !q && i < l; i++) {
            char ch = s.charAt(i);
            q = !Character.isDigit(ch) && !Character.isLetter(ch);
        }
        if (!q) {
            bf.append(s);
            return;
        }
        bf.append('"');
        for (int i = 0; i < l; i++) {
            char ch = s.charAt(i);
            if (ch == '\\' || ch == '"') bf.append('\\');
            bf.append(ch);
        }
        bf.append('"');
    }

    public String getExpression() {
        StringBuffer bf = new StringBuffer();
        for (String name : attributes) {
            String value = expression_attrs.get(name);
            if (value != null && value.length() > 0) {
                if (bf.length() > 0) bf.append(',');
                appendString(bf, name);
                bf.append('=');
                appendString(bf, value);
            }
        }
        return expression_text.substring(0, expression_parent) + bf.toString();
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Select Expression Parameters");
    }
}
