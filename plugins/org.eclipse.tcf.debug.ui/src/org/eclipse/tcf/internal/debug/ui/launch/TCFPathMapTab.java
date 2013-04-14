/*******************************************************************************
 * Copyright (c) 2009, 2012 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.launch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate;
import org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.PathMapRule;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.services.IPathMap;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

// TODO: add source lookup container that represents ATTR_PATH_MAP
public class TCFPathMapTab extends AbstractLaunchConfigurationTab {

    private TableViewer viewer;
    private Button button_add;
    private Button button_edit;
    private Button button_remove;
    private MenuItem item_add;
    private MenuItem item_edit;
    private MenuItem item_remove;

    private static final String[] column_ids = {
        IPathMap.PROP_SOURCE,
        IPathMap.PROP_DESTINATION,
        IPathMap.PROP_CONTEXT_QUERY,
    };

    private static final int[] column_size = {
        300,
        300,
        100,
    };

    private static final String TAB_ID = "org.eclipse.tcf.launch.pathMapTab"; //$NON-NLS-1$

    private ArrayList<PathMapRule> map;

    private class FileMapContentProvider implements IStructuredContentProvider  {

        public Object[] getElements(Object input) {
            return map.toArray(new PathMapRule[map.size()]);
        }

        public void inputChanged(Viewer viewer, Object old_input, Object new_input) {
        }

        public void dispose() {
        }
    }

    private class FileMapLabelProvider extends LabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object element, int column) {
            if (column == 0) return ImageCache.getImage(ImageCache.IMG_ATTRIBUTE);
            return null;
        }

        public String getColumnText(Object element, int column) {
            PathMapRule e = (PathMapRule)element;
            Object o = e.getProperties().get(column_ids[column]);
            if (o == null) return ""; //$NON-NLS-1$
            return o.toString();
        }
    }

    private Exception init_error;

    public String getName() {
        return "Path Map"; //$NON-NLS-1$
    }

    @Override
    public Image getImage() {
        return ImageCache.getImage(ImageCache.IMG_PATH);
    }

    @Override
    public String getId() {
        return TAB_ID;
    }

    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        composite.setFont(parent.getFont());
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 1, 1));
        createTable(composite);
        setControl(composite);
    }

    private void createTable(Composite parent) {
        Font font = parent.getFont();
        Label map_label = new Label(parent, SWT.WRAP);
        map_label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        map_label.setFont(font);
        map_label.setText("File Path Map Rules:"); //$NON-NLS-1$

        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setFont(font);
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 1, 1));

        viewer = new TableViewer(composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
        Table table = viewer.getTable();

        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setFont(font);
        viewer.setContentProvider(new FileMapContentProvider());
        viewer.setLabelProvider(new FileMapLabelProvider());
        viewer.setColumnProperties(column_ids);

        for (int i = 0; i < column_ids.length; i++) {
            TableColumn c = new TableColumn(table, SWT.NONE, i);
            c.setText(getColumnText(i));
            c.setWidth(getColumnWidth(i));
        }
        createTableButtons(composite);
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                updateLaunchConfigurationDialog();
            }
        });

        viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                if (button_edit.isEnabled()) {
                    onEdit((IStructuredSelection)viewer.getSelection());
                }
            }
        });
    }

    protected String getColumnText(int column) {
        return column_ids[column];
    }

    protected int getColumnWidth(int column) {
        return column_size[column];
    }

    private void createTableButtons(Composite parent) {
        Font font = parent.getFont();
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setFont(font);
        composite.setLayout(layout);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
        composite.setLayoutData(gd);

        Menu menu = new Menu(viewer.getTable());
        SelectionAdapter sel_adapter = null;

        button_add = new Button(composite, SWT.PUSH);
        button_add.setText(" &Add... "); //$NON-NLS-1$
        gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        button_add.setLayoutData(gd);
        button_add.addSelectionListener(sel_adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // To guarantee a predictable path map properties iteration order,
                // we have to use a LinkedHashMap.
                PathMapRule pathMapRule = new PathMapRule(new LinkedHashMap<String,Object>());
                PathMapRuleDialog dialog = new PathMapRuleDialog(getShell(), null, pathMapRule, true);
                if (dialog.open() == Window.OK) {
                    map.add(pathMapRule);
                    viewer.add(pathMapRule);
                    viewer.setSelection(new StructuredSelection(pathMapRule), true);
                    viewer.getTable().setFocus();
                    updateLaunchConfigurationDialog();
                }
            }
        });
        item_add = new MenuItem(menu, SWT.PUSH);
        item_add.setText("&Add..."); //$NON-NLS-1$
        item_add.addSelectionListener(sel_adapter);
        item_add.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));

        button_edit = new Button(composite, SWT.PUSH);
        button_edit.setText(" &Edit... "); //$NON-NLS-1$
        button_edit.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        button_edit.addSelectionListener(sel_adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onEdit((IStructuredSelection)viewer.getSelection());
            }
        });
        item_edit= new MenuItem(menu, SWT.PUSH);
        item_edit.setText("&Edit..."); //$NON-NLS-1$
        item_edit.addSelectionListener(sel_adapter);

        button_remove = new Button(composite, SWT.PUSH);
        button_remove.setText(" &Remove "); //$NON-NLS-1$
        button_remove.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        button_remove.addSelectionListener(sel_adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                for (Iterator<?> i = ((IStructuredSelection)viewer.getSelection()).iterator(); i.hasNext();) {
                    PathMapRule pathMapRule = (PathMapRule)i.next();
                    map.remove(pathMapRule);
                    viewer.remove(pathMapRule);
                }
                updateLaunchConfigurationDialog();
            }
        });
        item_remove = new MenuItem(menu, SWT.PUSH);
        item_remove.setText("&Remove"); //$NON-NLS-1$
        item_remove.addSelectionListener(sel_adapter);
        item_remove.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_DELETE));

        viewer.getTable().setMenu(menu);
    }

    private void onEdit(IStructuredSelection selection) {
        PathMapRule pathMapRule = (PathMapRule)selection.getFirstElement();
        PathMapRuleDialog dialog = new PathMapRuleDialog(getShell(), null, pathMapRule, true);
        dialog.open();
        viewer.refresh(pathMapRule);
        viewer.setSelection(new StructuredSelection(pathMapRule), true);
        viewer.getTable().setFocus();
        updateLaunchConfigurationDialog();
    }

    protected final TableViewer getViewer() {
        return viewer;
    }

    List<IPathMap.PathMapRule> getPathMap() {
        List<IPathMap.PathMapRule> l = new ArrayList<IPathMap.PathMapRule>();
        for (PathMapRule r : map) l.add(r);
        return Collections.unmodifiableList(l);
    }

    public void initializeFrom(ILaunchConfiguration config) {
        setErrorMessage(null);
        setMessage(null);
        try {
            String s = config.getAttribute(TCFLaunchDelegate.ATTR_PATH_MAP, ""); //$NON-NLS-1$
            map = TCFLaunchDelegate.parsePathMapAttribute(s);
            viewer.setInput(config);
            button_remove.setEnabled(!viewer.getSelection().isEmpty());
            button_edit.setEnabled(((IStructuredSelection)viewer.getSelection()).size()==1);
            item_remove.setEnabled(!viewer.getSelection().isEmpty());
            item_edit.setEnabled(((IStructuredSelection)viewer.getSelection()).size()==1);
        }
        catch (Exception e) {
            init_error = e;
            setErrorMessage("Cannot read launch configuration: " + e); //$NON-NLS-1$
            Activator.log(e);
        }
    }

    public void performApply(ILaunchConfigurationWorkingCopy config) {
        for (PathMapRule m : map) m.getProperties().remove(IPathMap.PROP_ID);
        StringBuffer bf = new StringBuffer();
        for (PathMapRule m : map) bf.append(m.toString());
        if (bf.length() == 0) config.removeAttribute(TCFLaunchDelegate.ATTR_PATH_MAP);
        else config.setAttribute(TCFLaunchDelegate.ATTR_PATH_MAP, bf.toString());
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy config) {
        config.removeAttribute(TCFLaunchDelegate.ATTR_PATH_MAP);
    }

    @Override
    protected void updateLaunchConfigurationDialog() {
        super.updateLaunchConfigurationDialog();
        button_remove.setEnabled(!viewer.getSelection().isEmpty());
        button_edit.setEnabled(((IStructuredSelection)viewer.getSelection()).size() == 1);
        item_remove.setEnabled(!viewer.getSelection().isEmpty());
        item_edit.setEnabled(((IStructuredSelection)viewer.getSelection()).size() == 1);
    }

    @Override
    public boolean isValid(ILaunchConfiguration config) {
        setMessage(null);

        if (init_error != null) {
            setErrorMessage("Cannot read launch configuration: " + init_error); //$NON-NLS-1$
            return false;
        }

        setErrorMessage(null);
        return true;
    }
}
