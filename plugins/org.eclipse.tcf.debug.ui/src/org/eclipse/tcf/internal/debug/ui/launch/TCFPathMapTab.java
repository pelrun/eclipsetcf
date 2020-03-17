/*******************************************************************************
 * Copyright (c) 2009-2020 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
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
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

// TODO: add source lookup container that represents ATTR_PATH_MAP
public class TCFPathMapTab extends AbstractLaunchConfigurationTab {

    private CheckboxTableViewer viewer;
    private Button button_add;
    private Button button_edit;
    private Button button_remove;
    private Button button_up;
    private Button button_down;
    private MenuItem item_add;
    private MenuItem item_edit;
    private MenuItem item_remove;
    private MenuItem item_up;
    private MenuItem item_down;

    protected static final int SIZING_TABLE_WIDTH = 500;
    protected static final int SIZING_TABLE_HEIGHT = 300;
    private static final int[] COL_WIDTH = { 30, 300, 300, 100 };

    private static final String[] column_ids = {
        "", //$NON-NLS-1$
        IPathMap.PROP_SOURCE,
        IPathMap.PROP_DESTINATION,
        IPathMap.PROP_CONTEXT_QUERY,
    };

    private static final String[] column_names = {
        "", //$NON-NLS-1$
        "Source", //$NON-NLS-1$
        "Destination", //$NON-NLS-1$
        "Context query", //$NON-NLS-1$
    };

    protected final static String PROP_ENABLED = "Enabled"; //$NON-NLS-1$
    protected final static String PROP_GENERATED = "Generated"; //$NON-NLS-1$

    private final static String ATTR_PATH_MAP_V1 = TCFLaunchDelegate.ATTR_PATH_MAP + "V1"; //$NON-NLS-1$

    private static final String TAB_ID = "org.eclipse.tcf.launch.pathMapTab"; //$NON-NLS-1$

    private List<IPathMap.PathMapRule> map;

    private class FileMapContentProvider implements IStructuredContentProvider  {

        public Object[] getElements(Object input) {
            return map.toArray(new IPathMap.PathMapRule[map.size()]);
        }

        public void inputChanged(Viewer viewer, Object old_input, Object new_input) {
        }

        public void dispose() {
        }
    }

    private class FileCheckStateProvider implements ICheckStateProvider {

        @Override
        public boolean isChecked(Object element) {
            IPathMap.PathMapRule e = (IPathMap.PathMapRule)element;
            if (e.getProperties().containsKey(PROP_ENABLED)) {
                return Boolean.parseBoolean(e.getProperties().get(PROP_ENABLED).toString());
            }
            return true;
        }

        @Override
        public boolean isGrayed(Object element) {
            return false;
        }
    }

    private class FileCheckStateListener implements ICheckStateListener {

        @Override
        public void checkStateChanged(CheckStateChangedEvent event) {
            IPathMap.PathMapRule rule = (IPathMap.PathMapRule)event.getElement();
            if (rule.getProperties().containsKey(PROP_GENERATED) && Boolean.parseBoolean(rule.getProperties().get(PROP_GENERATED).toString())) {
                viewer.refresh();
                return;
            }
            if (event.getChecked())
                rule.getProperties().remove(PROP_ENABLED);
            else
                rule.getProperties().put(PROP_ENABLED, Boolean.FALSE);
            viewer.refresh();
            updateLaunchConfigurationDialog();
        }
    }

    private class FileMapLabelProvider extends LabelProvider implements ITableLabelProvider, IColorProvider {

        public Image getColumnImage(Object element, int column) {
            return null;
        }

        public String getColumnText(Object element, int column) {
            IPathMap.PathMapRule e = (IPathMap.PathMapRule)element;
            Object o = e.getProperties().get(column_ids[column]);
            if (o == null) return ""; //$NON-NLS-1$
            return o.toString();
        }

        @Override
        public Color getForeground(Object element) {
            if (element instanceof IPathMap.PathMapRule) {
                IPathMap.PathMapRule rule = (IPathMap.PathMapRule)element;
                if (rule.getProperties().containsKey(PROP_GENERATED) && Boolean.parseBoolean(rule.getProperties().get(PROP_GENERATED).toString())) {
                    return PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_GRAY);
                }
            }
            return null;
        }

        @Override
        public Color getBackground(Object element) {
            return null;
        }
    }

    private final Preferences prefs;
    private Exception init_error;

    public TCFPathMapTab() {
        Preferences prefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        this.prefs = prefs.node(TCFPathMapTab.class.getCanonicalName());
    }

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
        createCustomControls(composite);
        setControl(composite);
    }

    /**
     * Hook to add custom controls below the path map rules table.
     *
     * @param parent The parent composite. Must not be <code>null</code>.
     */
    protected void createCustomControls(Composite parent) {
        // Nothing to do
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
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Table table = new Table(composite, SWT.CHECK | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
        table.setFont(font);

        configureTable(table);

        viewer = new CheckboxTableViewer(table);
        viewer.setContentProvider(new FileMapContentProvider());
        viewer.setLabelProvider(new FileMapLabelProvider());
        viewer.setCheckStateProvider(new FileCheckStateProvider());
        viewer.addCheckStateListener(new FileCheckStateListener());
        viewer.setColumnProperties(column_ids);
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                updateButtons();
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

        configureTableViewer(viewer);

        table.pack(true);

        createTableButtons(composite);
    }

    /**
     * Hook to configure the checkbox table viewer.
     *
     * @param viewer The checkbox table viewer. Must not be <code>null</code>.
     */
    protected void configureTableViewer(CheckboxTableViewer viewer) {
        // Nothing to do
    }

    protected void configureTable(final Table table) {
        GridData data = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING);
        data.widthHint = SIZING_TABLE_WIDTH;
        data.heightHint = SIZING_TABLE_HEIGHT;
        table.setLayoutData(data);

        TableLayout layout = new TableLayout();
        for (int i = 0; i < COL_WIDTH.length; i++) {
            if (i == 3 && !showContextQuery()) continue;
            final TableColumn col = new TableColumn(table, i);
            if (i == 0) {
                col.setResizable(false);
                col.setAlignment(SWT.CENTER);
            }
            else {
                col.setResizable(true);
                col.setAlignment(SWT.LEFT);
            }
            col.setText(getColumnText(i));
            final String id = "w" + i;
            layout.addColumnData(new ColumnPixelData(prefs.getInt(id, COL_WIDTH[i])));
            col.addListener(SWT.Resize, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    prefs.putInt(id, col.getWidth());
                }
            });
        }
        table.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                try {
                    prefs.flush();
                }
                catch (BackingStoreException x) {
                    Activator.log(x);
                }
            }
        });
        table.setLayout(layout);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
    }

    protected boolean showContextQuery() {
        return true;
    }

    protected String getColumnText(int c) {
        if (c < column_names.length && c >= 0) return column_names[c];
        return ""; //$NON-NLS-1$
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
        PixelConverter converter= new PixelConverter(button_add);
        gd.widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
        button_add.setLayoutData(gd);
        button_add.addSelectionListener(sel_adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onAdd();
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
                onRemove((IStructuredSelection)viewer.getSelection());
            }
        });
        item_remove = new MenuItem(menu, SWT.PUSH);
        item_remove.setText("&Remove"); //$NON-NLS-1$
        item_remove.addSelectionListener(sel_adapter);
        item_remove.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_DELETE));

        new MenuItem(menu, SWT.SEPARATOR);

        button_up = new Button(composite, SWT.PUSH);
        button_up.setText(" &Up "); //$NON-NLS-1$
        button_up.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        button_up.addSelectionListener(sel_adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onUp();
            }
        });
        item_up = new MenuItem(menu, SWT.PUSH);
        item_up.setText("&Up"); //$NON-NLS-1$
        item_up.addSelectionListener(sel_adapter);

        button_down = new Button(composite, SWT.PUSH);
        button_down.setText(" &Down "); //$NON-NLS-1$
        button_down.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        button_down.addSelectionListener(sel_adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onDown();
            }
        });
        item_down = new MenuItem(menu, SWT.PUSH);
        item_down.setText("&Down"); //$NON-NLS-1$
        item_down.addSelectionListener(sel_adapter);

        viewer.getTable().setMenu(menu);
    }

    private void onAdd() {
        // To guarantee a predictable path map properties iteration order,
        // we have to use a LinkedHashMap.
        int index = viewer.getTable().getSelectionIndex();
        final PathMapRule rule = new PathMapRule(new LinkedHashMap<String,Object>());
        PathMapRuleDialog dialog = new PathMapRuleDialog(getShell(), null, rule, true, showContextQuery());
        if (dialog.open() == Window.OK) {
            if (index >= 0)
                map.add(index, rule);
            else
                map.add(0, rule);
            viewer.refresh(true);
            updateLaunchConfigurationDialog();
            viewer.setSelection(new StructuredSelection(rule), true);
        }
    }

    private void onEdit(IStructuredSelection selection) {
        IPathMap.PathMapRule rule = (IPathMap.PathMapRule)selection.getFirstElement();
        PathMapRuleDialog dialog = new PathMapRuleDialog(getShell(), null, rule, true, showContextQuery());
        dialog.open();
        viewer.refresh(true);
        updateLaunchConfigurationDialog();
    }

    private void onRemove(IStructuredSelection selection) {
        for (Iterator<?> i = ((IStructuredSelection)viewer.getSelection()).iterator(); i.hasNext();) {
            IPathMap.PathMapRule rule = (IPathMap.PathMapRule)i.next();
            map.remove(rule);
        }
        viewer.refresh(true);
        updateLaunchConfigurationDialog();
    }

    private void onUp() {
        int index = viewer.getTable().getSelectionIndex();
        IPathMap.PathMapRule rule = map.remove(index);
        map.add(index-1, rule);
        viewer.refresh(true);
        updateLaunchConfigurationDialog();
        viewer.setSelection(new StructuredSelection(rule), true);
    }

    private void onDown() {
        int index = viewer.getTable().getSelectionIndex();
        IPathMap.PathMapRule rule = map.remove(index);
        map.add(index+1, rule);
        viewer.refresh(true);
        updateLaunchConfigurationDialog();
        viewer.setSelection(new StructuredSelection(rule), true);
    }

    protected final TableViewer getViewer() {
        return viewer;
    }

    List<IPathMap.PathMapRule> getPathMap() {
        return Collections.unmodifiableList(map);
    }

    public void initializeFrom(ILaunchConfiguration config) {
        setErrorMessage(null);
        setMessage(null);

        map = new ArrayList<IPathMap.PathMapRule>();
        initializePathMap(map, config);

        viewer.setInput(config);
        updateLaunchConfigurationDialog();
    }

    /**
     * Initialize the given path map.
     *
     * @param map The path map to initialize. Must not be <code>null</code>.
     * @param config The launch configuration. Must not be <code>null</code>.
     */
    protected void initializePathMap(List<IPathMap.PathMapRule> map, ILaunchConfiguration config) {
        Assert.isNotNull(map);
        Assert.isNotNull(config);

        try {
            String s = config.getAttribute(TCFLaunchDelegate.ATTR_PATH_MAP, ""); //$NON-NLS-1$
            String s1 = config.getAttribute(ATTR_PATH_MAP_V1, ""); //$NON-NLS-1$
            List<PathMapRule> m = TCFLaunchDelegate.parsePathMapAttribute(s);
            List<PathMapRule> m1 = TCFLaunchDelegate.parsePathMapAttribute(s1);
            for (IPathMap.PathMapRule rule : m1) {
                map.add(rule);
            }
            int i = -1;
            for (IPathMap.PathMapRule rule : m) {
                if (map.contains(rule))
                    i = map.indexOf(rule);
                else
                    map.add(++i, rule);
            }
        }
        catch (Exception e) {
            init_error = e;
            setErrorMessage("Cannot read launch configuration: " + e); //$NON-NLS-1$
            Activator.log(e);
        }
    }

    public void performApply(ILaunchConfigurationWorkingCopy config) {
        for (IPathMap.PathMapRule m : map)
            m.getProperties().remove(IPathMap.PROP_ID);
        StringBuffer bf = new StringBuffer();
        StringBuffer bf1 = new StringBuffer();
        for (IPathMap.PathMapRule m : map) {
            if (m.getProperties().containsKey(PROP_GENERATED)) {
                if (Boolean.parseBoolean(m.getProperties().get(PROP_GENERATED).toString())) {
                    continue;
                }
            }

            boolean enabled = true;
            if (m.getProperties().containsKey(PROP_ENABLED)) {
                enabled = Boolean.parseBoolean(m.getProperties().get(PROP_ENABLED).toString());
            }
            if (enabled) {
                m.getProperties().remove(PROP_ENABLED);
                bf.append(m.toString());
            }
            bf1.append(m.toString());
        }
        if (bf.length() == 0)
            config.removeAttribute(TCFLaunchDelegate.ATTR_PATH_MAP);
        else
            config.setAttribute(TCFLaunchDelegate.ATTR_PATH_MAP, bf.toString());

        if (bf1.length() == 0)
            config.removeAttribute(ATTR_PATH_MAP_V1);
        else
            config.setAttribute(ATTR_PATH_MAP_V1, bf1.toString());
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy config) {
        config.removeAttribute(TCFLaunchDelegate.ATTR_PATH_MAP);
        config.removeAttribute(ATTR_PATH_MAP_V1);
    }

    @Override
    protected void updateLaunchConfigurationDialog() {
        super.updateLaunchConfigurationDialog();
        updateButtons();
    }

    protected void updateButtons() {
        boolean singleSelection = ((IStructuredSelection)viewer.getSelection()).size() == 1;
        int index = viewer.getTable().getSelectionIndex();
        int count = viewer.getTable().getItemCount();

        IPathMap.PathMapRule selected = (IPathMap.PathMapRule)((IStructuredSelection)viewer.getSelection()).getFirstElement();
        boolean isGenerated = selected != null && selected.getProperties().containsKey(PROP_GENERATED) ? Boolean.parseBoolean(selected.getProperties().get(PROP_GENERATED).toString()) : false;

        button_remove.setEnabled(!viewer.getSelection().isEmpty() && !isGenerated);
        button_edit.setEnabled(singleSelection && !isGenerated);
        button_up.setEnabled(singleSelection && index > 0 && !isGenerated);
        button_down.setEnabled(singleSelection && index < count-1 && !isGenerated);

        item_remove.setEnabled(!viewer.getSelection().isEmpty() && !isGenerated);
        item_edit.setEnabled(singleSelection && !isGenerated);
        item_up.setEnabled(singleSelection && index > 0 && !isGenerated);
        item_down.setEnabled(singleSelection && index < count-1 && !isGenerated);
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
