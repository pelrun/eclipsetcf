/*******************************************************************************
 * Copyright (c) 2011, 2015, 2017 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.dialogs;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.filesystem.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.filesystem.ui.controls.FSTreeContentProvider;
import org.eclipse.tcf.te.tcf.filesystem.ui.controls.FSTreeViewerSorter;
import org.eclipse.tcf.te.tcf.filesystem.ui.interfaces.IFSConstants;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.columns.FSTreeElementLabelProvider;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.ui.trees.FilterDescriptor;
import org.eclipse.tcf.te.ui.trees.Pending;
import org.eclipse.tcf.te.ui.trees.ViewerStateManager;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;


/**
 * File system open file dialog.
 */
public class FSOpenFileDialog extends ElementTreeSelectionDialog {
	private String filterPath = null;
	/* default */ TreeViewer viewer = null;

	/**
	 * Create an FSFolderSelectionDialog using the specified shell as the parent.
	 *
	 * @param parentShell The parent shell.
	 */
	public FSOpenFileDialog(Shell parentShell) {
		this(parentShell, new FSTreeElementLabelProvider(), new FSTreeContentProvider());
	}

	/**
	 * Create an FSFolderSelectionDialog using the specified shell, an FSTreeLabelProvider, and a
	 * content provider that provides the tree nodes.
	 *
	 * @param parentShell The parent shell.
	 * @param labelProvider The label provider.
	 * @param contentProvider The content provider.
	 */
	private FSOpenFileDialog(Shell parentShell, ILabelProvider labelProvider, ITreeContentProvider contentProvider) {
		super(parentShell, createDecoratingLabelProvider(labelProvider), contentProvider);
		setTitle(Messages.FSOpenFileDialog_title);
		setMessage(Messages.FSOpenFileDialog_message);
		this.setAllowMultiple(false);
		this.setStatusLineAboveButtons(false);
		this.setComparator(new FSTreeViewerSorter());
		this.setValidator(new ISelectionStatusValidator() {
			@Override
			public IStatus validate(Object[] selection) {
				return isValidSelection(selection);
			}
		});
	}

	/**
	 * Sets the filter path.
	 *
	 * @param filterPath The filter path or <code>null</code>.
	 */
	public void setFilterPath(String filterPath) {
		this.filterPath = filterPath;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.ElementTreeSelectionDialog#setInput(java.lang.Object)
	 */
	@Override
    public void setInput(Object input) {
		super.setInput(input);
		FilterDescriptor[] filterDescriptors = ViewerStateManager.getInstance().getFilterDescriptors(IFSConstants.ID_TREE_VIEWER_FS, input);
		Assert.isNotNull(filterDescriptors);
		for (FilterDescriptor descriptor : filterDescriptors) {
			if (descriptor.isEnabled()) {
				addFilter(descriptor.getFilter());
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.ElementTreeSelectionDialog#create()
	 */
	@Override
	public void create() {
	    super.create();

	    if (filterPath != null && !"".equals(filterPath.trim())) { //$NON-NLS-1$
	    	IPath path = new Path(filterPath);
	    	if (viewer.getInput() instanceof IPeerNode) {
	    		Object element = null;
				IRuntimeModel model = ModelManager.getRuntimeModel((IPeerNode)viewer.getInput());
	    		if (model != null) {
	    			IFSTreeNode root = model.getRoot();
	    			ITreeContentProvider contentProvider = (ITreeContentProvider)viewer.getContentProvider();
	    			Object[] elements = contentProvider.getElements(root);
	    			String segment = path.getDevice() != null ? path.getDevice() : path.segmentCount() > 0 ? path.segment(0) : null;
	    			if (segment != null) {
	    				for (Object elem : elements) {
	    					if (!(elem instanceof IFSTreeNode)) break;
	    					IFSTreeNode child = (IFSTreeNode)elem;
	    					String name = child.getName();
	    					if (name.endsWith("\\") || name.endsWith("/")) name = name.substring(0, name.length() - 1); //$NON-NLS-1$ //$NON-NLS-2$
	    					boolean matches = child.isWindowsNode() ? name.equalsIgnoreCase(segment) : name.equals(segment);
	    					if (matches) {
	    						if (path.segmentCount() > (path.getDevice() != null ? 0 : 1)) {
	    							// Have to drill down a bit further
	    							element = findRecursive(child, path, path.getDevice() != null ? 0 : 1);
	    							if (element != null) break;
	    						} else {
	    							element = child;
	    							break;
	    						}
	    					}
	    				}
	    			}
	    		}

	    		if (element != null) {
	    			final ISelection selection = new StructuredSelection(element);
	    			final AtomicInteger counter = new AtomicInteger();

	    			Runnable runnable = new Runnable() {
						@Override
						public void run() {
			    			viewer.setSelection(selection, true);
			    			if (!selection.equals(viewer.getSelection())) {
			    				if (counter.incrementAndGet() <= 10) {
			    					viewer.getControl().getDisplay().asyncExec(this);
			    				}
			    			}
						}
					};

	    			viewer.getControl().getDisplay().asyncExec(runnable);
	    		}
	    	}
	    }
	}

	/**
	 * Finds the given path within the file system hierarchy.
	 *
	 * @param parent The parent file system node. Must not be <code>null</code>.
	 * @param path The path. Must not be <code>null</code>.
	 * @param index The segment index.
	 *
	 * @return The matching file system node or <code>null</code>.
	 */
	private IFSTreeNode findRecursive(IFSTreeNode parent, IPath path, int index) {
		Assert.isNotNull(parent);
		Assert.isNotNull(path);

		IFSTreeNode node = null;

		ITreeContentProvider contentProvider = (ITreeContentProvider)viewer.getContentProvider();
		Object[] elements = contentProvider.getElements(parent);
		while (elements.length == 1 && elements[0] instanceof Pending) {
			try {
	            Thread.sleep(100);
            } catch (InterruptedException e) {}
			elements = contentProvider.getElements(parent);
		}

		String segment = path.segment(index);

		for (Object element : elements) {
			if (!(element instanceof IFSTreeNode)) break;
			IFSTreeNode child = (IFSTreeNode)element;
			String name = child.getName();
			if (name.endsWith("\\") || name.endsWith("/")) name = name.substring(0, name.length() - 1); //$NON-NLS-1$ //$NON-NLS-2$
			boolean matches = child.isWindowsNode() ? name.equalsIgnoreCase(segment) : name.equals(segment);
			if (matches) {
				if (path.segmentCount() > index + 1) {
					// Have to drill down a bit further
					node = findRecursive(child, path, index + 1);
					if (node != null) break;
				} else {
					node = child;
					break;
				}
			}
		}

		return node;
	}

	/**
	 * Create a decorating label provider using the specified label provider.
	 *
	 * @param labelProvider The label provider that actually provides labels and images.
	 * @return The decorating label provider.
	 */
	private static ILabelProvider createDecoratingLabelProvider(ILabelProvider labelProvider) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IDecoratorManager manager = workbench.getDecoratorManager();
		ILabelDecorator decorator = manager.getLabelDecorator();
		return new DecoratingLabelProvider(labelProvider,decorator);
	}

	/**
	 * Create the tree viewer and set it to the label provider.
	 */
	@Override
	protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
		viewer = super.doCreateTreeViewer(parent, style);
		viewer.getTree().setLinesVisible(false);

		FSFolderSelectionDialog.createContextMenuRefresh(viewer);

		return viewer;
	}

	/**
	 * If the specified selection is a valid folder to be selected.
	 *
	 * @param selection The selected folders.
	 * @return An error status if it is invalid or an OK status indicating it is valid.
	 */
	IStatus isValidSelection(Object[] selection) {
		String pluginId = UIPlugin.getUniqueIdentifier();
		IStatus error = new Status(IStatus.ERROR, pluginId, null);
		if (selection == null || selection.length == 0) {
			return error;
		}
		if (!(selection[0] instanceof IFSTreeNode)) {
			return error;
		}
		IFSTreeNode target = (IFSTreeNode) selection[0];
		if(!target.isFile()) {
			return error;
		}
		return new Status(IStatus.OK, pluginId, null);
	}
}
