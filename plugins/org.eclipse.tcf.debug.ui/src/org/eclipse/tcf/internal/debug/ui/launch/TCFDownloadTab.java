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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
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
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.util.TCFTask;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class TCFDownloadTab extends AbstractLaunchConfigurationTab {

    private TableViewer viewer;
    private Button button_add;
    private Button button_edit;
    private Button button_remove;
    private MenuItem item_add;
    private MenuItem item_edit;
    private MenuItem item_remove;

    private static final String[] column_ids = {
        TCFLaunchDelegate.FILES_CONTEXT_FULL_NAME,
        TCFLaunchDelegate.FILES_FILE_NAME,
    };

    private static final int[] column_size = {
        300,
        400,
    };

    private final List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();

    private static final String TAB_ID = "org.eclipse.tcf.launch.downloadTab"; //$NON-NLS-1$

    private class FileListContentProvider implements IStructuredContentProvider  {

        public Object[] getElements(Object input) {
            return list.toArray(new Map[list.size()]);
        }

        public void inputChanged(Viewer viewer, Object old_input, Object new_input) {
        }

        public void dispose() {
        }
    }

    private class FileListLabelProvider extends LabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object element, int column) {
            if (column == 0) return ImageCache.getImage(ImageCache.IMG_ATTRIBUTE);
            return null;
        }

        public String getColumnText(Object element, int column) {
            @SuppressWarnings("unchecked")
            Map<String,String> e = (Map<String,String>)element;
            Object o = e.get(column_ids[column]);
            if (o == null) return ""; //$NON-NLS-1$
            return o.toString();
        }
    }

    private Exception init_error;

    public String getName() {
        return "Download"; //$NON-NLS-1$
    }

    @Override
    public Image getImage() {
        return ImageCache.getImage(ImageCache.IMG_DOWNLOAD_TAB);
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
        map_label.setText("Files to download during launch:"); //$NON-NLS-1$

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
        viewer.setContentProvider(new FileListContentProvider());
        viewer.setLabelProvider(new FileListLabelProvider());
        viewer.setColumnProperties(column_ids);

        for (int i = 0; i < column_ids.length; i++) {
            TableColumn c = new TableColumn(table, SWT.NONE, i);
            c.setText(column_ids[i]);
            c.setWidth(column_size[i]);
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
                Map<String,Object> m = new HashMap<String,Object>();
                DownloadFileDialog dialog = new DownloadFileDialog(getShell(), getPeerID(), m);
                if (dialog.open() == Window.OK) {
                    list.add(m);
                    viewer.add(m);
                    viewer.setSelection(new StructuredSelection(m), true);
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
                    @SuppressWarnings("unchecked")
                    Map<String,String> m = (Map<String,String>)i.next();
                    list.remove(m);
                    viewer.remove(m);
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
        @SuppressWarnings("unchecked")
        Map<String,Object> m = (Map<String,Object>)selection.getFirstElement();
        DownloadFileDialog dialog = new DownloadFileDialog(getShell(), getPeerID(), m);
        dialog.open();
        viewer.refresh(m);
        viewer.setSelection(new StructuredSelection(m), true);
        viewer.getTable().setFocus();
        updateLaunchConfigurationDialog();
    }

    private String getPeerID() {
        String peer_id = "TCP:127.0.0.1:1534";
        for (ILaunchConfigurationTab t : getLaunchConfigurationDialog().getTabs()) {
            if (t instanceof TCFTargetTab) peer_id = ((TCFTargetTab)t).getPeerID();
        }
        return peer_id;
    }

    protected final TableViewer getViewer() {
        return viewer;
    }

    public void initializeFrom(ILaunchConfiguration config) {
        setErrorMessage(null);
        setMessage(null);
        try {
            list.clear();
            final String s = config.getAttribute(TCFLaunchDelegate.ATTR_FILES, ""); //$NON-NLS-1$
            list.addAll(new TCFTask<Collection<Map<String,Object>>>(10000) {
                @Override
                public void run() {
                    try {
                        ArrayList<Map<String,Object>> l = new ArrayList<Map<String,Object>>();
                        if (s != null && s.length() > 0) {
                            @SuppressWarnings("unchecked")
                            Collection<Map<String,Object>> c = (Collection<Map<String,Object>>)JSON.parseOne(s.getBytes("UTF-8"));
                            for (Map<String,Object> m : c) l.add(new HashMap<String,Object>(m));
                        }
                        done(l);
                    }
                    catch (Throwable e) {
                        error(e);
                    }
                }
            }.get());
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
        if (list.size() == 0) {
            config.removeAttribute(TCFLaunchDelegate.ATTR_FILES);
        }
        else {
            String s = new TCFTask<String>(10000) {
                @Override
                public void run() {
                    try {
                        done(JSON.toJSON(list));
                    }
                    catch (Throwable e) {
                        error(e);
                    }
                }
            }.getE();
            config.setAttribute(TCFLaunchDelegate.ATTR_FILES, s);
        }
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy config) {
        config.removeAttribute(TCFLaunchDelegate.ATTR_FILES);
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
