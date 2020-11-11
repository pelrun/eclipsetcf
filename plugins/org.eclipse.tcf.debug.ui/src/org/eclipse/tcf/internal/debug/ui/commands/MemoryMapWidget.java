/*******************************************************************************
 * Copyright (c) 2014-2020 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.commands;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.tcf.core.ErrorReport;
import org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate;
import org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.PathMapRule;
import org.eclipse.tcf.internal.debug.launch.TCFSourceLookupDirector;
import org.eclipse.tcf.internal.debug.model.TCFLaunch;
import org.eclipse.tcf.internal.debug.model.TCFMemoryRegion;
import org.eclipse.tcf.internal.debug.model.TCFSymFileRef;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.internal.debug.ui.model.TCFChildren;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeLaunch;
import org.eclipse.tcf.internal.services.local.LocatorService;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IMemory;
import org.eclipse.tcf.services.IMemoryMap;
import org.eclipse.tcf.services.IPathMap;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class MemoryMapWidget {

    private static final int TABLE_WIDTH = 800;
    private static final int TABLE_HEIGHT = 300;
    private static final int[] COL_WIDTH = { 300, 100, 80, 50, 140, 140 };

    private static final String[] column_names = {
        "File", //$NON-NLS-1$
        "Address", //$NON-NLS-1$
        "Size", //$NON-NLS-1$
        "Flags", //$NON-NLS-1$
        "File offset/section", //$NON-NLS-1$
        "Context query", //$NON-NLS-1$
    };

    private final Display display;
    private final Preferences prefs;

    private TCFModel model;
    private IChannel channel;
    private TCFNode selection;
    private Combo ctx_text;
    private Tree map_table;
    private TreeViewer table_viewer;
    private Runnable update_map_buttons;
    private final Map<String, ArrayList<IMemoryMap.MemoryRegion>> org_maps = new HashMap<String, ArrayList<IMemoryMap.MemoryRegion>>();
    private final Map<String, ArrayList<IMemoryMap.MemoryRegion>> cur_maps = new HashMap<String, ArrayList<IMemoryMap.MemoryRegion>>();
    private final ArrayList<IMemoryMap.MemoryRegion> target_map = new ArrayList<IMemoryMap.MemoryRegion>();
    private final HashMap<String, TCFNodeExecContext> target_map_nodes = new HashMap<String, TCFNodeExecContext>();
    private TCFNodeExecContext selected_mem_map_node;
    private IMemory.MemoryContext mem_ctx;
    private ILaunchConfiguration cfg;
    private final HashSet<IMemoryMap.MemoryRegion> loaded_regions = new HashSet<IMemoryMap.MemoryRegion>();
    private final HashMap<String, String> path_map = new HashMap<String, String>();
    private String selected_mem_map_id;
    private final ArrayList<ModifyListener> modify_listeners = new ArrayList<ModifyListener>();
    private boolean editing;
    private boolean changed;
    private boolean disposed;

    private static class RegionList extends TCFMemoryRegion {
        ArrayList<IMemoryMap.MemoryRegion> children;
        RegionList(Map<String,Object> props) {
            super(props);
        }
    }

    private static class ForeignRegion extends TCFMemoryRegion {
        ForeignRegion(Map<String,Object> props) {
            super(props);
        }
    }

    private final ITreeContentProvider content_provider = new ITreeContentProvider() {

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public boolean hasChildren(Object element) {
            if (element instanceof IMemoryMap.MemoryRegion) {
                IMemoryMap.MemoryRegion region = (IMemoryMap.MemoryRegion)element;
                return region instanceof RegionList;
            }
            return false;
        }

        @Override
        public Object getParent(Object element) {
            return null;
        }

        @Override
        public Object[] getElements(Object input) {
            ArrayList<IMemoryMap.MemoryRegion> all = new ArrayList<IMemoryMap.MemoryRegion>();
            ArrayList<IMemoryMap.MemoryRegion> lst = cur_maps.get(input);
            if (lst != null) all.addAll(lst);
            all.addAll(target_map);
            ArrayList<IMemoryMap.MemoryRegion> roots = new ArrayList<IMemoryMap.MemoryRegion>();
            HashSet<IMemoryMap.MemoryRegion> removed = new HashSet<IMemoryMap.MemoryRegion>();
            for (int i = 0; i < all.size(); i++) {
                IMemoryMap.MemoryRegion region = all.get(i);
                if (removed.contains(region)) continue;
                String file_name = region.getFileName();
                if (file_name != null) {
                    ArrayList<IMemoryMap.MemoryRegion> children = new ArrayList<IMemoryMap.MemoryRegion>();
                    for (int j = i + 1; j < all.size(); j++) {
                        IMemoryMap.MemoryRegion child = all.get(j);
                        if (!region.equals(child) && file_name.equals(child.getFileName())) {
                            children.add(child);
                            removed.add(child);
                        }
                    }
                    if (children.size() > 0) {
                        removed.add(region);
                        children.add(0, region);
                        Map<String, Object> props = new HashMap<String, Object>();
                        props.put(IMemoryMap.PROP_FILE_NAME, file_name);
                        RegionList root = new RegionList(props);
                        root.children = children;
                        roots.add(root);
                        continue;
                    }
                }
                roots.add(region);
            }
            return roots.toArray();
        }

        @Override
        public Object[] getChildren(Object parent) {
            if (parent instanceof RegionList) {
                RegionList region = (RegionList)parent;
                return region.children.toArray();
            }
            return null;
        }
    };

    private final MapLabelProvider label_provider = new MapLabelProvider();
    private class MapLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider, ITableFontProvider {

        public Image getColumnImage(Object element, int column) {
            return null;
        }

        public String getColumnText(Object element, int column) {
            TCFMemoryRegion r = (TCFMemoryRegion) element;
            if (r instanceof RegionList && column != 0) {
                return ""; //$NON-NLS-1$
            }
            switch (column) {
            case 0:
                return r.getFileName();
            case 1:
            case 2: {
                BigInteger x = column == 1 ? r.addr : r.size;
                if (x == null) return ""; //$NON-NLS-1$
                String s = x.toString(16);
                int sz = 0;
                if (mem_ctx != null) sz = mem_ctx.getAddressSize() * 2;
                int l = sz - s.length();
                if (l < 0) l = 0;
                if (l > 16) l = 16;
                return "0x0000000000000000".substring(0, 2 + l) + s; //$NON-NLS-1$
            }
            case 3: {
                int n = r.getFlags();
                char[] bf = new char[3];
                bf[0] = bf[1] = bf[2] = '-';
                if ((n & IMemoryMap.FLAG_READ) != 0) bf[0] = 'r';
                if ((n & IMemoryMap.FLAG_WRITE) != 0) bf[1] = 'w';
                if ((n & IMemoryMap.FLAG_EXECUTE) != 0) bf[2] = 'x';
                return new String(bf);
            }
            case 4: {
                Number n = r.getOffset();
                if (n != null) {
                    BigInteger x = JSON.toBigInteger(n);
                    String s = x.toString(16);
                    int l = 16 - s.length();
                    if (l < 0) l = 0;
                    if (l > 16) l = 16;
                    return "0x0000000000000000".substring(0, 2 + l) + s; //$NON-NLS-1$
                }
                String s = r.getSectionName();
                if (s != null) return s;
                return ""; //$NON-NLS-1$
            }
            case 5: {
                String s = r.getContextQuery();
                if (s == null) s = "";
                return s;
            }
            }
            return ""; //$NON-NLS-1$
        }

        public Color getBackground(Object element, int columnIndex) {
            return map_table.getBackground();
        }

        public Color getForeground(Object element, int columnIndex) {
            TCFMemoryRegion region = (TCFMemoryRegion)element;
            if (region instanceof RegionList) return map_table.getForeground();
            if (region instanceof ForeignRegion) return map_table.getForeground();
            if (loaded_regions.contains(region)) return display.getSystemColor(SWT.COLOR_DARK_GREEN);
            return display.getSystemColor(SWT.COLOR_DARK_BLUE);
        }

        @Override
        public Font getFont(Object element, int columnIndex) {
            switch (columnIndex) {
            case 1:
            case 2:
            case 4:
                return JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT);
            }
            return null;
        }

        public String getText(Object element) {
            return element.toString();
        }
    }

    private final IMemoryMap.MemoryMapListener listener = new IMemoryMap.MemoryMapListener() {
        @Override
        public void changed(final String context_id) {
            asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (disposed) return;
                    if (mem_ctx == null) return;
                    if (mem_ctx.getID() == null) return;
                    if (!mem_ctx.getID().equals(context_id)) return;
                    if (editing) {
                        changed = true;
                    }
                    else if (cfg != null) {
                        loadData(cfg);
                    }
                }
            });
        }
    };

    public MemoryMapWidget(Composite composite, TCFNode node, Preferences prefs) {
        display = composite.getDisplay();
        this.prefs = prefs;
        setTCFNode(node);
        createContextText(composite);
        createMemoryMapTable(composite);
    }

    public MemoryMapWidget(Composite composite, TCFNode node) {
        this(composite, node, null);
    }

    /**
     * Dispose the widget and cleanup the created resources and listeners.
     */
    public void dispose() {
        if (disposed) return;
        disposed = true;

        if (prefs != null) {
            try {
                prefs.flush();
            }
            catch (BackingStoreException x) {
                Activator.log(x);
            }
        }

        // Remove the memory map listener
        if (channel != null) {
            Protocol.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    IMemoryMap svc = channel.getRemoteService(IMemoryMap.class);
                    if (svc != null) svc.removeListener(listener);
                }
            });
        }
        model = null;
        channel = null;
        selection = null;
    }

    public boolean setTCFNode(TCFNode node) {
        if (node == selection) return false;

        // Remove the memory map listener
        if (channel != null) {
            Protocol.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    IMemoryMap svc = channel.getRemoteService(IMemoryMap.class);
                    if (svc != null) svc.removeListener(listener);
                }
            });
        }

        if (node != null) {
            model = node.getModel();
            channel = node.getChannel();
            selection = node;

            // Register the memory map listener
            if (channel != null) {
                Protocol.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        IMemoryMap svc = channel.getRemoteService(IMemoryMap.class);
                        if (svc != null) svc.addListener(listener);
                    }
                });
            }
        }
        else {
            model = null;
            channel = null;
            selection = null;
        }
        return true;
    }

    public String getMemoryMapID() {
        return selected_mem_map_id;
    }

    private void createContextText(Composite parent) {
        Font font = parent.getFont();
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setFont(font);
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label props_label = new Label(composite, SWT.WRAP);
        props_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        props_label.setFont(font);
        props_label.setText("&Debug context:"); //$NON-NLS-1$

        ctx_text = new Combo(composite, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        ctx_text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ctx_text.setFont(font);
        ctx_text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                selected_mem_map_id = ctx_text.getText();
                selected_mem_map_node = target_map_nodes.get(selected_mem_map_id);
                loadTargetMemoryMap();
                table_viewer.setInput(selected_mem_map_id);
                if (selected_mem_map_id.length() == 0) selected_mem_map_id = null;
                update_map_buttons.run();
            }
        });
    }

    private void createMemoryMapTable(Composite parent) {
        Font font = parent.getFont();

        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setFont(font);
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        map_table = new Tree(composite, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);

        map_table.setFont(font);
        configureTable(map_table);

        map_table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                final TreeItem item = (TreeItem)e.item;
                final boolean expanded = item.getExpanded();
                final IMemoryMap.MemoryRegion r = (IMemoryMap.MemoryRegion)item.getData();
                if (r == null) return;
                // Async exec is used to workaround exception in jface
                asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (r instanceof RegionList) {
                            table_viewer.setExpandedState(r, !expanded);
                        }
                        else {
                            editRegion(r);
                        }
                    }
                });
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                update_map_buttons.run();
            }
        });

        table_viewer = new TreeViewer(map_table);
        table_viewer.setUseHashlookup(true);
        table_viewer.setColumnProperties(column_names);
        table_viewer.setContentProvider(content_provider);
        table_viewer.setLabelProvider(label_provider);

        map_table.pack();

        createMapButtons(composite);
    }

    protected String getColumnText(int column) {
        if (column < column_names.length && column >= 0) return column_names[column];
        return ""; //$NON-NLS-1$
    }

    protected void configureTable(final Tree table) {
        GridData data = new GridData(GridData.FILL_BOTH);
        data.widthHint = TABLE_WIDTH;
        data.heightHint = TABLE_HEIGHT;
        table.setLayoutData(data);

        TableLayout layout = new TableLayout();
        for (int i = 0; i < COL_WIDTH.length; i++) {
            final TreeColumn col = new TreeColumn(table, i);
            col.setResizable(true);
            col.setAlignment(SWT.LEFT);
            col.setText(getColumnText(i));
            int w = COL_WIDTH[i];
            if (prefs != null) {
                final String id = "w" + i;
                w = prefs.getInt(id, w);
                col.addListener(SWT.Resize, new Listener() {
                    @Override
                    public void handleEvent(Event event) {
                        prefs.putInt(id, col.getWidth());
                    }
                });
            }
            layout.addColumnData(new ColumnPixelData(w));
        }

        table.setLayout(layout);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
    }

    protected final TreeViewer getViewer() {
        return table_viewer;
    }

    private void createMapButtons(Composite parent) {
        Font font = parent.getFont();
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setFont(font);
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
        Menu menu = new Menu(map_table);
        SelectionAdapter sel_adapter = null;

        final Button button_add = new Button(composite, SWT.PUSH);
        button_add.setText(" &Add... "); //$NON-NLS-1$
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        PixelConverter converter = new PixelConverter(button_add);
        gd.widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
        button_add.setLayoutData(gd);
        button_add.addSelectionListener(sel_adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String id = ctx_text.getText();
                if (id == null || id.length() == 0) return;
                Map<String, Object> props = new HashMap<String, Object>();
                if (new MemoryMapItemDialog(map_table.getShell(), props, true).open() == Window.OK) {
                    props.put(IMemoryMap.PROP_ID, id); // Context ID
                    props.put("UUID", UUID.randomUUID().toString()); // Unique ID of this region
                    ArrayList<IMemoryMap.MemoryRegion> lst = cur_maps.get(id);
                    if (lst == null) cur_maps.put(id, lst = new ArrayList<IMemoryMap.MemoryRegion>());
                    lst.add(new TCFMemoryRegion(props));
                    table_viewer.refresh();
                    notifyModifyListeners();
                }
            }
        });
        final MenuItem item_add = new MenuItem(menu, SWT.PUSH);
        item_add.setText("&Add..."); //$NON-NLS-1$
        item_add.addSelectionListener(sel_adapter);
        item_add.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));

        final Button button_edit = new Button(composite, SWT.PUSH);
        button_edit.setText(" E&dit... "); //$NON-NLS-1$
        button_edit.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        button_edit.addSelectionListener(sel_adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IMemoryMap.MemoryRegion r = getSelectedRegion();
                if (r == null) return;
                editRegion(r);
            }
        });
        final MenuItem item_edit = new MenuItem(menu, SWT.PUSH);
        item_edit.setText("E&dit..."); //$NON-NLS-1$
        item_edit.addSelectionListener(sel_adapter);

        final Button button_remove = new Button(composite, SWT.PUSH);
        button_remove.setText(" &Remove "); //$NON-NLS-1$
        button_remove.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        button_remove.addSelectionListener(sel_adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String id = ctx_text.getText();
                if (id == null || id.length() == 0) return;
                IMemoryMap.MemoryRegion r = getSelectedRegion();
                if (r == null) return;
                ArrayList<IMemoryMap.MemoryRegion> lst = cur_maps.get(id);
                if (lst != null && lst.remove(r)) table_viewer.refresh();
                notifyModifyListeners();
            }
        });
        final MenuItem item_remove = new MenuItem(menu, SWT.PUSH);
        item_remove.setText("&Remove"); //$NON-NLS-1$
        item_remove.addSelectionListener(sel_adapter);
        item_remove.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_DELETE));

        map_table.setMenu(menu);

        update_map_buttons = new Runnable() {
            public void run() {
                IMemoryMap.MemoryRegion r = getSelectedRegion();
                boolean can_edit = r != null && !(r instanceof RegionList);
                boolean can_remove = can_edit && !(r instanceof ForeignRegion);
                button_add.setEnabled(selected_mem_map_id != null);
                button_edit.setEnabled(can_edit);
                button_remove.setEnabled(can_remove);
                item_add.setEnabled(selected_mem_map_id != null);
                item_edit.setEnabled(can_edit);
                item_remove.setEnabled(can_remove);
            }
        };
        update_map_buttons.run();
    }

    private IMemoryMap.MemoryRegion getSelectedRegion() {
        return (IMemoryMap.MemoryRegion)((IStructuredSelection)table_viewer.getSelection()).getFirstElement();
    }

    private void editRegion(final IMemoryMap.MemoryRegion region) {
        try {
            editing = true;
            String id = ctx_text.getText();
            if (id == null || id.length() == 0) return;
            assert !(region instanceof RegionList);
            final TCFNodeExecContext mem_node = selected_mem_map_node;
            Map<String, Object> props = region.getProperties();
            final boolean enable_editing = !(region instanceof ForeignRegion);
            if (enable_editing) props = new HashMap<String, Object>(props);
            MemoryMapItemDialog dlg = new MemoryMapItemDialog(map_table.getShell(), props, enable_editing) {
                boolean symbols_ok;
                protected void createStatusFields(Composite parent) {
                    try {
                        if (mem_node != null && region != null) {
                            Label label = new Label(parent, SWT.WRAP);
                            label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
                            label.setFont(parent.getFont());
                            if (region instanceof ForeignRegion) {
                                label.setText("The file info was repoted by the target agent: cannot edit");
                            }
                            else if (loaded_regions.contains(region)) {
                                label.setForeground(display.getSystemColor(SWT.COLOR_DARK_GREEN));
                                label.setText("The file info was sent to the target agent");
                            }
                            else {
                                label.setForeground(display.getSystemColor(SWT.COLOR_DARK_BLUE));
                                label.setText("The file info is not sent to the target agent yet");
                            }
                        }

                        if (mem_node != null && region != null && region.getAddress() != null) {
                            String file_info = new TCFTask<String>(channel) {
                                public void run() {
                                    StringBuilder buf = new StringBuilder();
                                    TCFDataCache<TCFSymFileRef> sym_cache = mem_node.getSymFileInfo(JSON.toBigInteger(region.getAddress()));
                                    if (sym_cache != null) {
                                        if (!sym_cache.validate(this)) return;
                                        TCFSymFileRef sym_data = sym_cache.getData();
                                        if (sym_data != null) {
                                            if (sym_data.props != null) {
                                                String sym_file_name = (String)sym_data.props.get("FileName"); //$NON-NLS-1$
                                                if (sym_file_name != null && !sym_file_name.equals(region.getFileName())) {
                                                    buf.append("Symbol file: "); //$NON-NLS-1$
                                                    buf.append(sym_file_name);
                                                    symbols_ok = true;
                                                }

                                                @SuppressWarnings("unchecked")
                                                Map<String,Object> map = (Map<String,Object>)sym_data.props.get("FileError"); //$NON-NLS-1$
                                                if (map != null) {
                                                    if (buf.length() > 0) buf.append("\n"); //$NON-NLS-1$
                                                    buf.append("Symbol file error: "); //$NON-NLS-1$
                                                    buf.append(TCFModel.getErrorMessage(new ErrorReport("", map), false)); //$NON-NLS-1$
                                                }
                                            }
                                            else if (sym_data.error != null) {
                                                buf.append("Symbol file error: "); //$NON-NLS-1$
                                                buf.append(TCFModel.getErrorMessage(sym_data.error, false));
                                            }
                                        }
                                    }
                                    done(buf.length() > 0 ? buf.toString() : null);
                                }
                            }.get();
                            if (file_info != null) {
                                Label label = new Label(parent, SWT.WRAP);
                                label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
                                label.setFont(parent.getFont());
                                label.setText(file_info);
                            }
                        }

                        if (!symbols_ok && mem_node != null && region != null && region.getFileName() != null) {
                            TCFLaunch launch = model.getLaunch();
                            IStorage mapped = TCFSourceLookupDirector.lookupByTargetPathMap(launch, mem_node.getID(), region.getFileName());
                            if (mapped != null) {
                                String file_name = mapped.getFullPath().toOSString();
                                if (!file_name.equals(region.getFileName())) {
                                    Label label = new Label(parent, SWT.WRAP);
                                    label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
                                    label.setFont(parent.getFont());
                                    label.setText("Mapped to: " + file_name +
                                            "\nMapping can be edited in the Path Map tab of the launch configuration");
                                }
                            }
                            boolean local_host_peer = new TCFTask<Boolean>(channel) {
                                @Override
                                public void run() {
                                    List<IPeer> peers = channel.getRemotePeerList();
                                    for (IPeer peer : peers) {
                                        if (LocatorService.isLocalHostPeer(peer)) {
                                            done(true);
                                            return;
                                        }
                                    }
                                    done(false);
                                }
                            }.get();
                            if (local_host_peer) {
                                boolean exists =
                                        mapped != null && mapped.getFullPath().toFile().exists() ||
                                        new File(region.getFileName()).exists();
                                if (!exists) {
                                    Label label = new Label(parent, SWT.WRAP);
                                    label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
                                    label.setFont(parent.getFont());
                                    label.setText("The symbol file not found on the local host");
                                    if (!enable_editing) {
                                        final Button button_locate = new Button(parent, SWT.PUSH | SWT.WRAP);
                                        button_locate.setText("Locate local copy of the file... "); //$NON-NLS-1$
                                        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
                                        button_locate.setLayoutData(layoutData);
                                        button_locate.addSelectionListener(new SelectionAdapter() {
                                            @Override
                                            public void widgetSelected(SelectionEvent e) {
                                                locateSymbolFile(region);
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    }
                    catch (Throwable x) {
                        Label label = new Label(parent, SWT.WRAP);
                        label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
                        label.setForeground(display.getSystemColor(SWT.COLOR_RED));
                        label.setFont(parent.getFont());
                        String s = "Cannot display region status: " + x.getClass().getName();
                        String m = x.getMessage();
                        if (m != null) s += " " + m;
                        label.setText(s);
                    }
                }
            };
            if (dlg.open() == Window.OK && enable_editing) {
                ArrayList<IMemoryMap.MemoryRegion> lst = cur_maps.get(id);
                if (lst != null) {
                    for (int n = 0; n < lst.size(); n++) {
                        if (lst.get(n) == region) {
                            lst.set(n, new TCFMemoryRegion(props));
                            table_viewer.refresh();
                            notifyModifyListeners();
                        }
                    }
                }
            }
        }
        finally {
            editing = false;
            if (changed) {
                loadData(cfg);
                changed = false;
            }
        }
    }

    private void locateSymbolFile(IMemoryMap.MemoryRegion r) {
        FileDialog dialog = new FileDialog(map_table.getShell(), SWT.OPEN);
        IPath workspace_path = ResourcesPlugin.getWorkspace().getRoot().getLocation();
        dialog.setFilterPath(workspace_path.toString());
        dialog.setText("Locate Symbol File"); //$NON-NLS-1$
        String symbol_file = dialog.open();
        if (symbol_file != null && new File(symbol_file).exists()) {
            path_map.put(r.getFileName(), symbol_file);
        }
    }

    private void readMemoryMapAttribute() {
        cur_maps.clear();
        try {
            new TCFTask<Boolean>() {
                public void run() {
                    try {
                        TCFLaunchDelegate.getMemMapsAttribute(cur_maps, cfg);
                        done(true);
                    }
                    catch (Exception e) {
                        error(e);
                    }
                }
            }.get();
        }
        catch (Exception x) {
            Activator.log("Invalid launch cofiguration attribute", x); //$NON-NLS-1$
        }
    }

    private void writeMemoryMapAttribute(ILaunchConfigurationWorkingCopy copy) throws Exception {
        String s = null;
        final ArrayList<Map<String, Object>> lst = new ArrayList<Map<String, Object>>();
        for (ArrayList<IMemoryMap.MemoryRegion> x : cur_maps.values()) {
            for (IMemoryMap.MemoryRegion r : x)
                lst.add(r.getProperties());
        }
        if (lst.size() > 0) {
            s = new TCFTask<String>() {
                public void run() {
                    try {
                        done(JSON.toJSON(lst));
                    }
                    catch (IOException e) {
                        error(e);
                    }
                }
            }.getIO();
        }
        copy.setAttribute(TCFLaunchDelegate.ATTR_MEMORY_MAP, s);
    }

    public void loadData(ILaunchConfiguration cfg) {
        this.cfg = cfg;
        cur_maps.clear();
        org_maps.clear();
        loadTargetMemoryNodes();
        readMemoryMapAttribute();
        for (String id : cur_maps.keySet()) {
            org_maps.put(id, new ArrayList<IMemoryMap.MemoryRegion>(cur_maps.get(id)));
        }
        // Update controls
        String map_id = getSelectedMemoryNode();
        HashSet<String> ids = new HashSet<String>(target_map_nodes.keySet());
        if (map_id != null) ids.add(map_id);
        ids.addAll(cur_maps.keySet());
        String[] arr = ids.toArray(new String[ids.size()]);
        Arrays.sort(arr);
        ctx_text.removeAll();
        for (String id : arr) ctx_text.add(id);
        if (map_id == null && arr.length > 0) map_id = arr[0];
        if (map_id == null) map_id = ""; //$NON-NLS-1$
        ctx_text.setText(map_id);
    }

    private String getSelectedMemoryNode() {
        if (channel == null || channel.getState() != IChannel.STATE_OPEN) return null;
        try {
            return new TCFTask<String>(channel) {
                public void run() {
                    TCFDataCache<TCFNodeExecContext> mem_cache = model.searchMemoryContext(selection);
                    if (mem_cache == null) {
                        error(new Exception("Context does not provide memory access")); //$NON-NLS-1$
                        return;
                    }
                    if (!mem_cache.validate(this)) return;
                    if (mem_cache.getError() != null) {
                        error(mem_cache.getError());
                        return;
                    }
                    String id = null;
                    TCFNodeExecContext mem_node = mem_cache.getData();
                    if (mem_node != null) {
                        TCFDataCache<TCFNodeExecContext> syms_cache = mem_node.getSymbolsNode();
                        if (!syms_cache.validate(this)) return;
                        TCFNodeExecContext syms_node = syms_cache.getData();
                        if (syms_node != null) {
                            TCFDataCache<IMemory.MemoryContext> mem_ctx = syms_node.getMemoryContext();
                            if (!mem_ctx.validate(this)) return;
                            if (mem_ctx.getData() != null) {
                                if (model.getLaunch().isMemoryMapPreloadingSupported()) {
                                    TCFDataCache<String> name_cache = syms_node.getFullName();
                                    if (!name_cache.validate(this)) return;
                                    id = name_cache.getData();
                                }
                                else {
                                    id = mem_ctx.getData().getName();
                                }
                                if (id == null) id = syms_node.getID();
                            }
                        }
                    }
                    done(id);
                }
            }.get();
        }
        catch (Exception x) {
            // if (channel.getState() != IChannel.STATE_OPEN) return null;
            // Don't log error. This is expected if the selected node has no containing memory context
            // Activator.log("Cannot get selected memory node", x);
            return null;
        }
    }

    protected TCFLaunch findLaunch() {
        for (ILaunch launch : DebugPlugin.getDefault().getLaunchManager().getLaunches()) {
            if (launch instanceof TCFLaunch &&
                    launch.getLaunchConfiguration().equals(cfg instanceof ILaunchConfigurationWorkingCopy ? ((ILaunchConfigurationWorkingCopy)cfg).getOriginal() : cfg)) {
                return (TCFLaunch)launch;
            }
        }
        return null;
    }

    private void loadTargetMemoryNodes() {
        target_map_nodes.clear();
        if (channel == null || channel.getState() != IChannel.STATE_OPEN) return;
        try {
            new TCFTask<Boolean>(channel) {
                public void run() {
                    TCFNodeLaunch n = model.getRootNode();
                    if (!collectMemoryNodes(n.getFilteredChildren())) return;
                    done(true);
                }

                private boolean collectMemoryNodes(TCFChildren children) {
                    if (!children.validate(this)) return false;
                    Map<String, TCFNode> m = children.getData();
                    if (m != null) {
                        for (TCFNode n : m.values()) {
                            if (n instanceof TCFNodeExecContext) {
                                TCFNodeExecContext exe = (TCFNodeExecContext) n;
                                if (!collectMemoryNodes(exe.getChildren())) return false;
                                TCFDataCache<TCFNodeExecContext> syms_cache = exe.getSymbolsNode();
                                if (!syms_cache.validate(this)) return false;
                                TCFNodeExecContext syms_node = syms_cache.getData();
                                if (syms_node != null) {
                                    TCFDataCache<IMemory.MemoryContext> mem_ctx = syms_node.getMemoryContext();
                                    if (!mem_ctx.validate(this)) return false;
                                    if (mem_ctx.getData() != null) {
                                        String id = null;
                                        if (model.getLaunch().isMemoryMapPreloadingSupported()) {
                                            TCFDataCache<String> name_cache = syms_node.getFullName();
                                            if (!name_cache.validate(this)) return false;
                                            id = name_cache.getData();
                                        }
                                        else {
                                            id = mem_ctx.getData().getName();
                                        }
                                        if (id == null) id = syms_node.getID();
                                        target_map_nodes.put(id, syms_node);
                                    }
                                }
                            }
                        }
                    }
                    return true;
                }
            }.get();
        }
        catch (Exception x) {
            if (channel.getState() != IChannel.STATE_OPEN) return;
            Activator.log("Cannot load target memory context info", x); //$NON-NLS-1$
        }
    }

    private void loadTargetMemoryMap() {
        loaded_regions.clear();
        target_map.clear();
        mem_ctx = null;
        if (channel == null || channel.getState() != IChannel.STATE_OPEN) return;
        try {
            new TCFTask<Boolean>(channel) {
                public void run() {
                    if (selected_mem_map_node != null && !selected_mem_map_node.isDisposed()) {
                        TCFDataCache<IMemory.MemoryContext> mem_cache = selected_mem_map_node.getMemoryContext();
                        if (!mem_cache.validate(this)) return;
                        if (mem_cache.getError() != null) {
                            error(mem_cache.getError());
                            return;
                        }
                        mem_ctx = mem_cache.getData();
                        TCFDataCache<TCFNodeExecContext.MemoryRegion[]> map_cache = selected_mem_map_node.getMemoryMap();
                        if (!map_cache.validate(this)) return;
                        if (map_cache.getError() != null) {
                            error(map_cache.getError());
                            return;
                        }
                        if (map_cache.getData() != null) {
                            ArrayList<IMemoryMap.MemoryRegion> map = cur_maps.get(selected_mem_map_id);
                            for (TCFNodeExecContext.MemoryRegion m : map_cache.getData()) {
                                // Create comparable region object
                                TCFMemoryRegion region = new TCFMemoryRegion(m.region.getProperties());
                                if (map != null && map.contains(region)) {
                                    loaded_regions.add(region);
                                }
                                else {
                                    /* Foreign entry, added by another client or by the target itself */
                                    target_map.add(new ForeignRegion(m.region.getProperties()));
                                }
                            }
                        }
                    }
                    done(true);
                }
            }.get();
        }
        catch (Exception x) {
            if (channel.getState() != IChannel.STATE_OPEN) return;
            Activator.log("Cannot load target memory map", x); //$NON-NLS-1$
        }
    }

    public boolean saveData(ILaunchConfigurationWorkingCopy copy) throws Exception {
        boolean loaded_regions_ok = true;
        if (selected_mem_map_id != null) {
            ArrayList<IMemoryMap.MemoryRegion> lst = cur_maps.get(selected_mem_map_id);
            if (lst != null) {
                for (IMemoryMap.MemoryRegion r : lst) {
                    assert !(r instanceof ForeignRegion);
                    if (loaded_regions.contains(r)) continue;
                    loaded_regions_ok = false;
                    break;
                }
                if (lst.size() == 0) cur_maps.remove(selected_mem_map_id);
            }
        }
        if (path_map.size() > 0) {
            String s = copy.getAttribute(TCFLaunchDelegate.ATTR_PATH_MAP, ""); //$NON-NLS-1$
            List<PathMapRule> map = TCFLaunchDelegate.parsePathMapAttribute(s);
            for (String fnm : path_map.keySet()) {
                // Create the new path map rule
                Map<String, Object> props = new LinkedHashMap<String, Object>();
                props.put(IPathMap.PROP_SOURCE, fnm);
                props.put(IPathMap.PROP_DESTINATION, path_map.get(fnm));
                PathMapRule rule = new PathMapRule(props);
                if (!map.contains(rule)) map.add(rule);
            }
            StringBuilder bf = new StringBuilder();
            for (IPathMap.PathMapRule m : map) bf.append(m.toString());
            copy.setAttribute(TCFLaunchDelegate.ATTR_PATH_MAP, bf.toString());
        }
        if (!loaded_regions_ok || !org_maps.equals(cur_maps) || path_map.size() > 0) {
            writeMemoryMapAttribute(copy);
            return true;
        }
        return false;
    }

    public void addModifyListener(ModifyListener l) {
        modify_listeners.add(l);
    }

    private void notifyModifyListeners() {
        for (ModifyListener l : modify_listeners) {
            l.modifyText(null);
        }
    }

    private void asyncExec(Runnable r) {
        synchronized (Device.class) {
            if (display.isDisposed()) return;
            display.asyncExec(r);
        }
    }
}
