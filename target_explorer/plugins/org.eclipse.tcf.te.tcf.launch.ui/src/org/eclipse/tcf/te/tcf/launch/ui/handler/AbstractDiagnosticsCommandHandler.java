/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate;
import org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate.PathMapRule;
import org.eclipse.tcf.internal.debug.tests.TCFTestSuite;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.internal.debug.ui.launch.TestErrorsDialog;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IDiagnostics;
import org.eclipse.tcf.services.IMemoryMap;
import org.eclipse.tcf.services.IPathMap;
import org.eclipse.tcf.te.tcf.launch.ui.editor.AbstractTcfLaunchTabContainerEditorPage;
import org.eclipse.tcf.te.tcf.launch.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelQueryService;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Abstract diagnostics command handler implementation.
 */
@SuppressWarnings("restriction")
public abstract class AbstractDiagnosticsCommandHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Get the active shell
		final Shell shell = HandlerUtil.getActiveShell(event);
		// Get the selection
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Iterator<?> iterator = ((IStructuredSelection)selection).iterator();
			while (iterator.hasNext()) {
				Object element = iterator.next();

				// The selected element must be of type IPeerNode
				if (element instanceof IPeerNode) {
					final IPeerNode node = (IPeerNode)element;

					IPeerModelQueryService service = node.getModel().getService(IPeerModelQueryService.class);
					if (service != null && service.hasRemoteService(node, IDiagnostics.NAME)) {
						runDiagnostics(node, shell);
					}
				}
			}
		}

		return null;
	}

	/**
	 * Returns if or if not the tests runs as tests loop.
	 *
	 * @return <code>True</code> to run the tests as loop, <code>false</code> otherwise.
	 */
	protected abstract boolean runAsLoop();

	/**
	 * Run the diagnostics tests.
	 *
	 * @param peer The peer. Must not be <code>null</code>.
	 * @param parentShell The parent shell.  Must not be <code>null</code>.
	 */
	/* default */ void runDiagnostics(IPeerNode node, Shell parentShell) {
		Assert.isNotNull(node);
		Assert.isNotNull(parentShell);

        final Shell shell = new Shell(parentShell, SWT.TITLE | SWT.PRIMARY_MODAL);
        GridLayout layout = new GridLayout();
        layout.verticalSpacing = 0;
        layout.numColumns = 2;
        shell.setLayout(layout);
        shell.setText(Messages.AbstractDiagnosticsCommandHandler_progress_title);
        CLabel label = new CLabel(shell, SWT.NONE);
        label.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        label.setText(Messages.AbstractDiagnosticsCommandHandler_progress_title);
        final TCFTestSuite[] test = new TCFTestSuite[1];
        Button button_cancel = new Button(shell, SWT.PUSH);
        button_cancel.setText(Messages.AbstractDiagnosticsCommandHandler_progress_button_cancel);
        button_cancel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        button_cancel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Protocol.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (test[0] != null) test[0].cancel();
                    }
                });
            }
        });
        SWTFactory.createVerticalSpacer(shell, 0);
        ProgressBar bar = new ProgressBar(shell, SWT.HORIZONTAL);
        bar.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1));
        shell.setDefaultButton(button_cancel);
        shell.pack();
        shell.setSize(483, shell.getSize().y);
        Rectangle rc0 = parentShell.getBounds();
        Rectangle rc1 = shell.getBounds();
        shell.setLocation(rc0.x + (rc0.width - rc1.width) / 2, rc0.y + (rc0.height - rc1.height) / 2);
        shell.setVisible(true);

        ILaunchConfigurationWorkingCopy wc = AbstractTcfLaunchTabContainerEditorPage.getLaunchConfig(node);
        runDiagnostics(node.getPeer(), runAsLoop(), test, shell, label, bar, wc);
	}

	/**
	 * Run the diagnostics tests.
	 *
	 * @param peer The peer. Must not be <code>null</code>.
	 * @param loop <code>True</code> to run the tests as tests loop, <code>false</code> otherwise.
	 * @param test The current test suite holder. Must not be <code>null</code>.
	 * @param shell The shell.  Must not be <code>null</code>.
	 * @param label The label. Must not be <code>null</code>.
	 * @param bar The progress bar. Must not be <code>null</code>.
	 * @param wc The launch configuration working copy to get the path map and memory map from. Must not be <code>null</code>.
	 */
	/* default */ void runDiagnostics(final IPeer peer, final boolean loop, final TCFTestSuite[] test,
									  final Shell shell, final CLabel label, final ProgressBar bar,
									  final ILaunchConfigurationWorkingCopy wc) {
		Assert.isNotNull(peer);
		Assert.isNotNull(test);
		Assert.isNotNull(shell);
		Assert.isNotNull(label);
		Assert.isNotNull(bar);

		final Display display = shell.getDisplay();

		final TCFTestSuite.TestListener done = new TCFTestSuite.TestListener() {
			/* default */ String last_text = ""; //$NON-NLS-1$
			/* default */ int last_count = 0;
			/* default */ int last_total = 0;
			@Override
            public void progress(final String label_text, final int count_done, final int count_total) {
				assert test[0] != null;
				if ((label_text == null || last_text.equals(label_text)) &&
								last_total == count_total &&
								(count_done - last_count) / (float)count_total < 0.02f) return;
				if (label_text != null) last_text = label_text;
				last_total = count_total;
				last_count = count_done;
				display.asyncExec(new Runnable() {
					@Override
                    public void run() {
						label.setText(last_text);
						bar.setMinimum(0);
						bar.setMaximum(last_total);
						bar.setSelection(last_count);
					}
				});
			}
			@Override
            public void done(final Collection<Throwable> errors) {
				assert test[0] != null;
				final boolean b = test[0].isCanceled();
				test[0] = null;
				display.asyncExec(new Runnable() {
					@Override
                    public void run() {
						if (errors.size() > 0) {
							shell.dispose();
							new TestErrorsDialog(shell,
											     ImageCache.getImage(ImageCache.IMG_TCF), errors).open();
						}
						else if (loop && !b) {
							runDiagnostics(peer, true, test, shell, label, bar, wc);
						}
						else {
							shell.dispose();
						}
					}
				});
			}
		};
		Protocol.invokeLater(new Runnable() {
			@Override
            public void run() {
				try {
					List<IPathMap.PathMapRule> path_map = new ArrayList<IPathMap.PathMapRule>();
		            String path_map_cfg = wc.getAttribute(TCFLaunchDelegate.ATTR_PATH_MAP, ""); //$NON-NLS-1$
		            ArrayList<PathMapRule> map = TCFLaunchDelegate.parsePathMapAttribute(path_map_cfg);
		            for (PathMapRule r : map) path_map.add(r);
		            if (path_map.isEmpty()) path_map = null;

					HashMap<String,ArrayList<IMemoryMap.MemoryRegion>> mem_map = null;
					String mem_map_cfg = wc.getAttribute(TCFLaunchDelegate.ATTR_MEMORY_MAP, (String)null);
					if (mem_map_cfg != null) {
						mem_map = new HashMap<String,ArrayList<IMemoryMap.MemoryRegion>>();
						TCFLaunchDelegate.parseMemMapsAttribute(mem_map, mem_map_cfg);
					}

					boolean enable_tracing =
									"true".equals(Platform.getDebugOption("org.eclipse.tcf.debug/debug")) && //$NON-NLS-1$ //$NON-NLS-2$
									"true".equals(Platform.getDebugOption("org.eclipse.tcf.debug/debug/tests/runcontrol")); //$NON-NLS-1$ //$NON-NLS-2$
					if (enable_tracing) System.setProperty("org.eclipse.tcf.debug.tracing.tests.runcontrol", "true"); //$NON-NLS-1$ //$NON-NLS-2$

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
