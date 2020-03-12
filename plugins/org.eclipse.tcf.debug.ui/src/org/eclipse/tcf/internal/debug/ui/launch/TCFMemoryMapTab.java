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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.internal.debug.model.TCFLaunch;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.internal.debug.ui.commands.MemoryMapWidget;
import org.eclipse.tcf.internal.debug.ui.model.TCFModelManager;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.osgi.service.prefs.Preferences;

public class TCFMemoryMapTab extends AbstractLaunchConfigurationTab {

    private static final String TAB_ID = "org.eclipse.tcf.launch.memoryMapTab";

    private MemoryMapWidget widget;

    public void createControl(Composite parent) {
        TCFNode node = getSelectedNode();
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        composite.setFont(parent.getFont());
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 1, 1));
        widget = createWidget(composite, node);
        widget.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });
        setControl(composite);
    }

    /**
     * Create the memory map widget.
     *
     * @param composite The parent composite.
     * @param node The TCF node.
     *
     * @return The memory map widget.
     */
    protected MemoryMapWidget createWidget(Composite composite, TCFNode node) {
        Preferences prefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        prefs = prefs.node(TCFMemoryMapTab.class.getCanonicalName());
        return new MemoryMapWidget(composite, node, prefs);
    }

    /**
     * Returns the memory map widget.
     *
     * @return The memory map widget.
     */
    protected final MemoryMapWidget getWidget() {
        return widget;
    }

    /**
     * Update the context for {@link MemoryMapWidget}.
     * @return <code>true</code> if the widgets context combo was updated.
     */
    public boolean updateContext() {
        if (widget != null) {
            TCFNode node = getSelectedNode();
            if (node != null) {
                return widget.setTCFNode(node);
            }
        }
        return false;
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy cfg) {
    }

    public void initializeFrom(ILaunchConfiguration cfg) {
        setErrorMessage(null);
        setMessage(null);
        widget.loadData(cfg);
    }

    public void performApply(ILaunchConfigurationWorkingCopy cfg) {
        try {
            widget.saveData(cfg);
        }
        catch (Throwable x) {
            setErrorMessage("Cannot update memory map: " + x);
        }
    }

    @Override
    public void dispose() {
        if (widget != null) widget.dispose();
        super.dispose();
    }

    public String getName() {
        return "Symbol Files";
    }

    @Override
    public Image getImage() {
        return ImageCache.getImage(ImageCache.IMG_MEMORY_MAP);
    }

    @Override
    public String getId() {
        return TAB_ID;
    }

    private TCFNode getSelectedNode() {
        TCFNode node = null;
        IAdaptable adaptable = DebugUITools.getDebugContext();
        if (adaptable instanceof TCFLaunch) node = TCFModelManager.getRootNodeSync((Launch)adaptable);
        else if (adaptable != null) node = (TCFNode)adaptable.getAdapter(TCFNode.class);
        return node;
    }
}
