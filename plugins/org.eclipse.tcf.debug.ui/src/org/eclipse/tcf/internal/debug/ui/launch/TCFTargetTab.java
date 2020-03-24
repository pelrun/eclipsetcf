/*******************************************************************************
 * Copyright (c) 2007-2020 Wind River Systems, Inc. and others.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.tcf.core.TransientPeer;
import org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate;
import org.eclipse.tcf.internal.debug.launch.TCFLocalAgent;
import org.eclipse.tcf.internal.debug.launch.TCFUserDefPeer;
import org.eclipse.tcf.internal.debug.tests.TCFTestSuite;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.internal.debug.ui.launch.PeerListControl.PeerInfo;
import org.eclipse.tcf.internal.debug.ui.launch.setup.SetupWizardDialog;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IMemoryMap;
import org.eclipse.tcf.services.IPathMap;
import org.eclipse.tcf.util.TCFTask;
import org.osgi.service.prefs.Preferences;


/**
 * Launch configuration dialog tab to specify the Target Communication Framework
 * configuration.
 */
public class TCFTargetTab extends AbstractLaunchConfigurationTab {

    private static final String TAB_ID = "org.eclipse.tcf.launch.targetTab";

    private Button run_local_server_button;
    private Button run_local_agent_button;
    private Button use_local_agent_button;
    private Text peer_id_text;
    private PeerListControl peer_list;
    private Tree peer_tree;
    private Runnable update_peer_buttons;
    private Display display;
    private Exception init_error;
    private String mem_map_cfg;

    public void createControl(Composite parent) {
        display = parent.getDisplay();
        assert display != null;

        Font font = parent.getFont();
        Composite comp = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, true);
        comp.setLayout(layout);
        comp.setFont(font);

        GridData gd = new GridData(GridData.FILL_BOTH);
        comp.setLayoutData(gd);
        setControl(comp);
        createVerticalSpacer(comp, 1);
        createLocalAgentButtons(comp);
        createVerticalSpacer(comp, 1);
        createTargetGroup(comp);
    }

    private void createLocalAgentButtons(Composite parent) {
        Composite local_agent_comp = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        local_agent_comp.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        local_agent_comp.setLayoutData(gd);

        run_local_server_button = createCheckButton(local_agent_comp, "Run TCF symbols server on the local host");
        run_local_server_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });
        run_local_server_button.setEnabled(true);

        run_local_agent_button = createCheckButton(local_agent_comp, "Run instance of TCF agent on the local host");
        run_local_agent_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });
        run_local_agent_button.setEnabled(true);

        use_local_agent_button = createCheckButton(local_agent_comp, "Use local host as the target");
        use_local_agent_button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });
        use_local_agent_button.setEnabled(true);
    }

    private void createTargetGroup(Composite parent) {
        Font font = parent.getFont();

        Group group = new Group(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.verticalSpacing = 0;
        layout.numColumns = 2;
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        group.setFont(font);
        group.setText("Target");

        createVerticalSpacer(group, layout.numColumns);

        Label host_label = new Label(group, SWT.NONE);
        host_label.setText("Target ID:");
        host_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        host_label.setFont(font);

        peer_id_text = new Text(group, SWT.SINGLE | SWT.BORDER);
        peer_id_text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        peer_id_text.setFont(font);
        peer_id_text.setEditable(false);
        peer_id_text.setBackground(parent.getBackground());

        createVerticalSpacer(group, layout.numColumns);

        Label peer_label = new Label(group, SWT.NONE);
        peer_label.setText("&Available targets:");
        peer_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        peer_label.setFont(font);

        Preferences prefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        prefs = prefs.node(TCFTargetTab.class.getCanonicalName());
        peer_list  = new PeerListControl(group, prefs) {
            @Override
            protected void onPeerListChanged() {
                updateLaunchConfigurationDialog();
            }
            @Override
            protected void onPeerSelected(PeerInfo info) {
                peer_id_text.setText(peer_list.getPath(info));
            }
        };
        peer_tree = peer_list.getTree();
        createPeerButtons(peer_tree.getParent());
    }

    private void createPeerButtons(Composite parent) {
        Font font = parent.getFont();
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        composite.setFont(font);
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
        Menu menu = new Menu(peer_tree);
        SelectionAdapter sel_adapter = null;

        final Button button_new = new Button(composite, SWT.PUSH);
        button_new.setText("N&ew...");
        button_new.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        button_new.addSelectionListener(sel_adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final Map<String,String> attrs = new HashMap<String,String>();
                attrs.put("TransportName", "TCP");
                attrs.put("Host", "127.0.0.1");
                attrs.put("Port", "1534");
                SetupWizardDialog wizard = new SetupWizardDialog(attrs);
                WizardDialog dialog = new WizardDialog(getShell(), wizard);
                dialog.create();
                if (dialog.open() != Window.OK) return;
                if (attrs.isEmpty()) return;
                Protocol.invokeLater(new Runnable() {
                    public void run() {
                        new TCFUserDefPeer(attrs);
                        TCFUserDefPeer.savePeers();
                    }
                });
            }
        });
        final MenuItem item_new = new MenuItem(menu, SWT.PUSH);
        item_new.setText("N&ew...");
        item_new.addSelectionListener(sel_adapter);

        final Button button_edit = new Button(composite, SWT.PUSH);
        button_edit.setText("E&dit...");
        button_edit.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        button_edit.addSelectionListener(sel_adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final PeerInfo info = peer_list.findPeerInfo(peer_id_text.getText());
                if (info == null) return;
                if (new PeerPropsDialog(getShell(), getImage(), info.attrs,
                        info.peer instanceof TCFUserDefPeer).open() != Window.OK) return;
                if (!(info.peer instanceof TCFUserDefPeer)) return;
                Protocol.invokeLater(new Runnable() {
                    public void run() {
                        ((TCFUserDefPeer)info.peer).updateAttributes(info.attrs);
                        TCFUserDefPeer.savePeers();
                    }
                });
            }
        });
        final MenuItem item_edit = new MenuItem(menu, SWT.PUSH);
        item_edit.setText("E&dit...");
        item_edit.addSelectionListener(sel_adapter);

        final Button button_remove = new Button(composite, SWT.PUSH);
        button_remove.setText("&Remove");
        button_remove.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        button_remove.addSelectionListener(sel_adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final PeerInfo info = peer_list.findPeerInfo(peer_id_text.getText());
                if (info == null) return;
                if (!(info.peer instanceof TCFUserDefPeer)) return;
                peer_id_text.setText("");
                updateLaunchConfigurationDialog();
                Protocol.invokeAndWait(new Runnable() {
                    public void run() {
                        ((TCFUserDefPeer)info.peer).dispose();
                        TCFUserDefPeer.savePeers();
                    }
                });
            }
        });
        final MenuItem item_remove = new MenuItem(menu, SWT.PUSH);
        item_remove.setText("&Remove");
        item_remove.addSelectionListener(sel_adapter);

        createVerticalSpacer(composite, 20);
        new MenuItem(menu, SWT.SEPARATOR);

        final Button button_test = new Button(composite, SWT.PUSH);
        button_test.setText("Run &Tests");
        button_test.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        button_test.addSelectionListener(sel_adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                runDiagnostics(false);
            }
        });
        final MenuItem item_test = new MenuItem(menu, SWT.PUSH);
        item_test.setText("Run &Tests");
        item_test.addSelectionListener(sel_adapter);

        final Button button_loop = new Button(composite, SWT.PUSH);
        button_loop.setText("Tests &Loop");
        button_loop.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        button_loop.addSelectionListener(sel_adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                runDiagnostics(true);
            }
        });
        final MenuItem item_loop = new MenuItem(menu, SWT.PUSH);
        item_loop.setText("Tests &Loop");
        item_loop.addSelectionListener(sel_adapter);

        peer_tree.setMenu(menu);

        update_peer_buttons = new Runnable() {

            public void run() {
                boolean local = use_local_agent_button.getSelection();
                PeerInfo info = peer_list.findPeerInfo(peer_id_text.getText());
                button_new.setEnabled(!local);
                button_edit.setEnabled(info != null && !local);
                button_remove.setEnabled(info != null && info.peer instanceof TCFUserDefPeer && !local);
                button_test.setEnabled(local || info != null);
                button_loop.setEnabled(local || info != null);
                item_new.setEnabled(!local);
                item_edit.setEnabled(info != null && !local);
                item_remove.setEnabled(info != null && info.peer instanceof TCFUserDefPeer && !local);
                item_test.setEnabled(info != null);
                item_loop.setEnabled(info != null);
            }
        };
        update_peer_buttons.run();
    }

    @Override
    protected void updateLaunchConfigurationDialog() {
        if (use_local_agent_button.getSelection()) {
            peer_tree.setEnabled(false);
            peer_tree.deselectAll();
            String id = TCFLocalAgent.getLocalAgentID(TCFLocalAgent.AGENT_NAME);
            if (id == null) id = "";
            peer_id_text.setText(id);
            peer_id_text.setEnabled(false);
        }
        else {
            peer_tree.setEnabled(true);
            peer_id_text.setEnabled(true);
            String id = peer_id_text.getText();
            TreeItem item = peer_list.findItem(id);
            if (item != null) peer_tree.setSelection(item);
            else peer_tree.deselectAll();
        }
        update_peer_buttons.run();
        super.updateLaunchConfigurationDialog();
    }

    public String getName() {
        return "Target";
    }

    @Override
    public Image getImage() {
        return ImageCache.getImage(ImageCache.IMG_TARGET_TAB);
    }

    @Override
    public String getId() {
        return TAB_ID;
    }

    public void initializeFrom(ILaunchConfiguration configuration) {
        setErrorMessage(null);
        setMessage(null);
        try {
            String id = configuration.getAttribute(TCFLaunchDelegate.ATTR_PEER_ID, "");
            peer_id_text.setText(id);
            peer_list.setInitialSelection(id);
            run_local_server_button.setSelection(configuration.getAttribute(TCFLaunchDelegate.ATTR_RUN_LOCAL_SERVER, false));
            run_local_agent_button.setSelection(configuration.getAttribute(TCFLaunchDelegate.ATTR_RUN_LOCAL_AGENT, false));
            use_local_agent_button.setSelection(configuration.getAttribute(TCFLaunchDelegate.ATTR_USE_LOCAL_AGENT, true));
            mem_map_cfg = configuration.getAttribute(TCFLaunchDelegate.ATTR_MEMORY_MAP, "null");
        }
        catch (CoreException e) {
            init_error = e;
            setErrorMessage("Cannot read launch configuration: " + e);
            Activator.log(e);
        }
        updateLaunchConfigurationDialog();
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        if (use_local_agent_button.getSelection()) {
            configuration.removeAttribute(TCFLaunchDelegate.ATTR_PEER_ID);
        }
        else {
            configuration.setAttribute(TCFLaunchDelegate.ATTR_PEER_ID, peer_id_text.getText());
        }
        configuration.setAttribute(TCFLaunchDelegate.ATTR_RUN_LOCAL_SERVER, run_local_server_button.getSelection());
        configuration.setAttribute(TCFLaunchDelegate.ATTR_RUN_LOCAL_AGENT, run_local_agent_button.getSelection());
        configuration.setAttribute(TCFLaunchDelegate.ATTR_USE_LOCAL_AGENT, use_local_agent_button.getSelection());
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(TCFLaunchDelegate.ATTR_RUN_LOCAL_SERVER, false);
        configuration.setAttribute(TCFLaunchDelegate.ATTR_RUN_LOCAL_AGENT, false);
        configuration.setAttribute(TCFLaunchDelegate.ATTR_USE_LOCAL_AGENT, true);
        configuration.removeAttribute(TCFLaunchDelegate.ATTR_PEER_ID);
    }

    @Override
    public boolean isValid(ILaunchConfiguration config) {
        setErrorMessage(null);
        setMessage(null);

        if (init_error != null) {
            setErrorMessage("Cannot read launch configuration: " + init_error);
            return false;
        }

        return true;
    }

    public String getPeerID() {
        return peer_id_text.getText();
    }

    private void runDiagnostics(boolean loop) {
        IPeer peer = null;
        if (use_local_agent_button.getSelection()) {
            try {
                if (run_local_agent_button.getSelection()) TCFLocalAgent.runLocalAgent(TCFLocalAgent.AGENT_NAME);
                final String id = TCFLocalAgent.getLocalAgentID(TCFLocalAgent.AGENT_NAME);
                peer = new TCFTask<IPeer>() {
                    public void run() {
                        done(Protocol.getLocator().getPeers().get(id));
                    }
                }.get();
            }
            catch (Throwable err) {
                String msg = err.getLocalizedMessage();
                if (msg == null || msg.length() == 0) msg = err.getClass().getName();
                MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
                mb.setText("Error");
                mb.setMessage("Cannot start agent:\n" + msg);
                mb.open();
            }
        }
        else {
            PeerInfo info = peer_list.findPeerInfo(peer_id_text.getText());
            if (info == null) return;
            peer = info.peer;
        }
        if (peer == null) return;
        if (run_local_server_button.getSelection()) {
            try {
                final IPeer agent = peer;
                final String id = TCFLocalAgent.runLocalAgent(TCFLocalAgent.SERVER_NAME);
                IPeer server = new TCFTask<IPeer>() {
                    public void run() {
                        done(Protocol.getLocator().getPeers().get(id));
                    }
                }.get();
                peer = new TransientPeer(server.getAttributes()) {
                    public IChannel openChannel() {
                        assert Protocol.isDispatchThread();
                        IChannel c = super.openChannel();
                        c.redirect(agent.getAttributes());
                        return c;
                    }
                };
            }
            catch (Throwable err) {
                String msg = err.getLocalizedMessage();
                if (msg == null || msg.length() == 0) msg = err.getClass().getName();
                MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
                mb.setText("Error");
                mb.setMessage("Cannot start symbols server:\n" + msg);
                mb.open();
            }
        }
        final Shell shell = new Shell(getShell(), SWT.TITLE | SWT.PRIMARY_MODAL);
        GridLayout layout = new GridLayout();
        layout.verticalSpacing = 0;
        layout.numColumns = 2;
        shell.setLayout(layout);
        shell.setText("Running Diagnostics...");
        CLabel label = new CLabel(shell, SWT.NONE);
        label.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        label.setText("Running Diagnostics...");
        final TCFTestSuite[] test = new TCFTestSuite[1];
        Button button_cancel = new Button(shell, SWT.PUSH);
        button_cancel.setText("&Cancel");
        button_cancel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        button_cancel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Protocol.invokeLater(new Runnable() {
                    public void run() {
                        if (test[0] != null) test[0].cancel();
                    }
                });
            }
        });
        createVerticalSpacer(shell, 0);
        ProgressBar bar = new ProgressBar(shell, SWT.HORIZONTAL);
        bar.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1));
        shell.setDefaultButton(button_cancel);
        shell.pack();
        shell.setSize(483, shell.getSize().y);
        Rectangle rc0 = getShell().getBounds();
        Rectangle rc1 = shell.getBounds();
        shell.setLocation(rc0.x + (rc0.width - rc1.width) / 2, rc0.y + (rc0.height - rc1.height) / 2);
        shell.setVisible(true);
        runDiagnostics(peer, loop, test, shell, label, bar);
    }

    private void runDiagnostics(final IPeer peer, final boolean loop, final TCFTestSuite[] test,
            final Shell shell, final CLabel label, final ProgressBar bar) {
        final TCFTestSuite.TestListener done = new TCFTestSuite.TestListener() {
            private String last_text = "";
            private int last_count = 0;
            private int last_total = 0;
            public void progress(final String label_text, final int count_done, final int count_total) {
                assert test[0] != null;
                if ((label_text == null || last_text.equals(label_text)) &&
                        last_total == count_total &&
                        (count_done - last_count) / (float)count_total < 0.02f) return;
                if (label_text != null) last_text = label_text;
                last_total = count_total;
                last_count = count_done;
                display.asyncExec(new Runnable() {
                    public void run() {
                        label.setText(last_text);
                        bar.setMinimum(0);
                        bar.setMaximum(last_total);
                        bar.setSelection(last_count);
                    }
                });
            }
            public void done(final Collection<Throwable> errors) {
                assert test[0] != null;
                final boolean b = test[0].isCanceled();
                test[0] = null;
                display.asyncExec(new Runnable() {
                    public void run() {
                        if (errors.size() > 0) {
                            shell.dispose();
                            new TestErrorsDialog(getControl().getShell(),
                                    ImageCache.getImage(ImageCache.IMG_TCF), errors).open();
                        }
                        else if (loop && !b && display != null) {
                            runDiagnostics(peer, true, test, shell, label, bar);
                        }
                        else {
                            shell.dispose();
                        }
                    }
                });
            }
        };
        Protocol.invokeLater(new Runnable() {
            public void run() {
                try {
                    List<IPathMap.PathMapRule> path_map = null;
                    for (ILaunchConfigurationTab t : getLaunchConfigurationDialog().getTabs()) {
                        if (t instanceof TCFPathMapTab) path_map = ((TCFPathMapTab)t).getPathMap();
                    }
                    HashMap<String,ArrayList<IMemoryMap.MemoryRegion>> mem_map = null;
                    if (mem_map_cfg != null) {
                        mem_map = new HashMap<String,ArrayList<IMemoryMap.MemoryRegion>>();
                        TCFLaunchDelegate.parseMemMapsAttribute(mem_map, mem_map_cfg);
                    }
                    boolean enable_tracing =
                            "true".equals(Platform.getDebugOption("org.eclipse.tcf.debug/debug")) &&
                            "true".equals(Platform.getDebugOption("org.eclipse.tcf.debug/debug/tests/runcontrol"));
                    if (enable_tracing) System.setProperty("org.eclipse.tcf.debug.tracing.tests.runcontrol", "true");
                    test[0] = new TCFTestSuite(peer, done, path_map, mem_map);
                }
                catch (Throwable x) {
                    ArrayList<Throwable> errors = new ArrayList<Throwable>();
                    errors.add(x);
                    done.done(errors);
                }
            }
        });
    }
}
