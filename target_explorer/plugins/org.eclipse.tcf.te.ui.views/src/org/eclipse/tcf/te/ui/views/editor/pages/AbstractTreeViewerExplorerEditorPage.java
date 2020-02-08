/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.editor.pages;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.tcf.te.core.interfaces.IFilterable;
import org.eclipse.tcf.te.core.interfaces.IPropertyChangeProvider;
import org.eclipse.tcf.te.ui.forms.CustomFormToolkit;
import org.eclipse.tcf.te.ui.interfaces.ITreeControlInputChangedListener;
import org.eclipse.tcf.te.ui.trees.AbstractTreeControl;
import org.eclipse.tcf.te.ui.trees.TreeControl;
import org.eclipse.tcf.te.ui.utils.TreeViewerUtil;
import org.eclipse.tcf.te.ui.views.activator.UIPlugin;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.MultiPageSelectionProvider;
import org.osgi.framework.Bundle;

/**
 * Tree viewer based editor page implementation.
 */
public abstract class AbstractTreeViewerExplorerEditorPage extends AbstractCustomFormToolkitEditorPage implements IDoubleClickListener {
	// The references to the pages subcontrol's (needed for disposal)
	private TreeControl treeControl;
	private IToolBarManager toolbarMgr;
	private PropertyChangeListener pcListener;
	private ISelectionChangedListener scListener;
	private FocusListener fListener;
	private Image formImage;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormPage#dispose()
	 */
	@Override
	public void dispose() {
	    IPropertyChangeProvider provider = getPropertyChangeProvider();
		if(provider != null && pcListener != null) {
			provider.removePropertyChangeListener(pcListener);
		}
		if (treeControl != null && treeControl.getViewer() != null) {
			treeControl.getViewer().removeSelectionChangedListener(scListener);
			if (treeControl.getViewer() instanceof TreeViewer) {
				((TreeViewer)treeControl.getViewer()).removeDoubleClickListener(this);
				Tree tree = ((TreeViewer)treeControl.getViewer()).getTree();
				if (tree != null && !tree.isDisposed()) {
					tree.removeFocusListener(fListener);
				}
			}
		}
		if (treeControl != null) { treeControl.dispose(); treeControl = null; }
		super.dispose();
	}

	/**
	 * Set the initial focus to the tree.
	 */
	@Override
    public void setFocus() {
		Control control = treeControl.getViewer().getControl();
		if(control != null && !control.isDisposed()) {
			control.setFocus();
		}
		else {
			super.setFocus();
		}
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractEditorPage#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	@Override
    public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
	    super.setInitializationData(config, propertyName, data);
	    String iconPath = config.getAttribute("icon"); //$NON-NLS-1$
	    if(iconPath != null) {
	    	String bundleId = config.getContributor().getName();
	    	Bundle bundle = Platform.getBundle(bundleId);
	    	if(bundle != null) {
	    		URL iconURL = bundle.getEntry(iconPath);
	    		if(iconURL != null) {
	    			formImage = UIPlugin.getImage(iconURL.toString());
	    			if (formImage == null) {
	    				ImageDescriptor iconDesc = ImageDescriptor.createFromURL(iconURL);
	    				if(iconDesc != null) {
	    					formImage = iconDesc.createImage();
	    					UIPlugin.getDefault().getImageRegistry().put(iconURL.toString(), formImage);
	    				}
	    			}
	    		}
	    	}
	    }
		treeControl = doCreateTreeControl();
		Assert.isNotNull(treeControl);
		treeControl.addInputChangedListener(new ITreeControlInputChangedListener() {
			@Override
			public void inputChanged(AbstractTreeControl control, Object oldInput, Object newInput) {
				Assert.isNotNull(control);

				if (getTreeControl() != control) return;
				// Adjust the table column width after an input element change
				if (control.getViewer() instanceof TreeViewer && ((TreeViewer)control.getViewer()).getTree() != null) {
					Tree tree = ((TreeViewer)control.getViewer()).getTree();
					if (!tree.isDisposed()) adjustTreeColumnWidth(tree);
				}
			}
		});
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractCustomFormToolkitEditorPage#getFormImage()
	 */
	@Override
    protected Image getFormImage() {
	    return formImage;
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractCustomFormToolkitEditorPage#createToolbarContributionItems(org.eclipse.jface.action.IToolBarManager)
	 */
	@Override
    protected void createToolbarContributionItems(IToolBarManager manager) {
	    this.toolbarMgr = manager;
	    super.createToolbarContributionItems(manager);
	    treeControl.createToolbarContributionItems(manager);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractCustomFormToolkitEditorPage#doCreateFormContent(org.eclipse.swt.widgets.Composite, org.eclipse.tcf.te.ui.forms.CustomFormToolkit)
	 */
	@Override
    protected void doCreateFormContent(Composite parent, CustomFormToolkit toolkit) {
		Assert.isNotNull(parent);
		Assert.isNotNull(toolkit);

		// Setup the tree control
		Assert.isNotNull(treeControl);
		treeControl.setupFormPanel(parent, toolkit);

		// Register the context menu at the parent workbench part site.
		getSite().registerContextMenu(getId(), treeControl.getContextMenuManager(), treeControl.getViewer());

		// Set the initial input
		Object input = getViewerInput();
		treeControl.getViewer().setInput(input);

	    addViewerListeners();

	    // adjust the tree column width initially to take the full size of control
	    adjustTreeColumnWidth(treeControl.getViewer());

	    updateUI();
	}

	/**
	 * Determines the current visible width of the tree control and
	 * adjust the column width according to there relative weight.
	 *
	 * @param viewer The viewer or <code>null</code>.
	 */
	protected void adjustTreeColumnWidth(Viewer viewer) {
		if (!(viewer instanceof TreeViewer)) return;

		final TreeViewer treeViewer = (TreeViewer)viewer;
		final Tree tree = treeViewer.getTree();
		tree.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {
				adjustTreeColumnWidth(tree);
				tree.removeControlListener(this);
			}

			@Override
			public void controlMoved(ControlEvent e) {
			}
		});
	}

	/**
	 * Adjust the width of the tree columns of the given tree to fill in the
	 * available screen area.
	 *
	 * @param tree The tree. Must not be <code>null</code>.
	 */
	protected void adjustTreeColumnWidth(Tree tree) {
		Assert.isNotNull(tree);

		int sumColumnWidth = 0;
		int treeWidth = tree.getSize().x - tree.getVerticalBar().getSize().x;

		TreeColumn[] columns = tree.getColumns();

		// Summarize the tree column width
		for (TreeColumn column : columns) {
			Object widthHint = column.getData("widthHint"); //$NON-NLS-1$
			sumColumnWidth += widthHint instanceof Integer ? ((Integer)widthHint).intValue() : column.getWidth();
		}

		// Calculate the new width for each column
		int sumColumnWidth2 = 0;
		TreeColumn maxColumn = null;
		for (TreeColumn column : columns) {
			Object widthHint = column.getData("widthHint"); //$NON-NLS-1$
			int width = widthHint instanceof Integer ? ((Integer)widthHint).intValue() : column.getWidth();
			int weight = (width * 100) / sumColumnWidth;
			int newWidth = (weight * treeWidth) / 100;
			sumColumnWidth2 += newWidth;
			column.setWidth(newWidth);
			if (maxColumn == null || maxColumn.getWidth() < column.getWidth()) {
				maxColumn = column;
			}
		}

		// If we end up with a slighter larger width of all columns than
		// the tree widget is, reduce the size of the largest column
		if (sumColumnWidth2 > treeWidth && maxColumn != null) {
			int delta = sumColumnWidth2 - treeWidth + 2;
			maxColumn.setWidth(maxColumn.getWidth() - delta);
		}
	}

	/**
	 * Add tree viewer listeners to the tree control.
	 */
	private void addViewerListeners() {
		TreeViewer viewer = (TreeViewer) treeControl.getViewer();
		scListener = new ISelectionChangedListener(){
			@Override
            public void selectionChanged(SelectionChangedEvent event) {
				propagateSelection();
            }
		};
        viewer.addSelectionChangedListener(scListener);
        fListener = new FocusAdapter() {
			@Override
            public void focusGained(FocusEvent e) {
				propagateSelection();
            }
		};
        viewer.getTree().addFocusListener(fListener);
		viewer.addDoubleClickListener(this);

	    IPropertyChangeProvider provider = getPropertyChangeProvider();
		if(provider != null) {
			pcListener = new PropertyChangeListener() {
				@Override
				public void propertyChange(final PropertyChangeEvent event) {
					Object object = event.getSource();
					Object input = getTreeViewerInput();
					if (object == input) {
						if (Display.getCurrent() != null) {
							updateUI();
						}
						else {
							Display display = getSite().getShell().getDisplay();
							display.asyncExec(new Runnable() {
								@Override
								public void run() {
									updateUI();
								}
							});
						}
					}
				}
			};
			provider.addPropertyChangeListener(pcListener);
		}
    }

	@Override
    public Object getAdapter(Class adapter) {
		if(TreeViewer.class.equals(adapter)) {
			return treeControl.getViewer();
		}
	    return super.getAdapter(adapter);
    }

	/**
	 * Get an adapter instance from the adaptable with the specified
	 * adapter interface.
	 *
	 * @param adaptable The adaptable to get the adapter.
	 * @param adapter The adapter interface class.
	 * @return An adapter or null if it does not adapt to this type.
	 */
	@SuppressWarnings("rawtypes")
	private Object getAdapter(Object adaptable, Class adapter) {
		Object adapted = null;
		if(adapter.isInstance(adaptable)) {
			adapted = adaptable;
		}
		if(adapted == null && adaptable instanceof IAdaptable) {
			adapted = ((IAdaptable)adaptable).getAdapter(adapter);
		}
		if(adapted == null && adaptable != null) {
			adapted = Platform.getAdapterManager().getAdapter(adaptable, adapter);
		}
		return adapted;
	}

	/**
	 * Get an adapter of IFilteringLabelDecorator.
	 *
	 * @return an IFilteringLabelDecorator adapter or null if it does not adapt to IFilteringLabelDecorator.
	 */
	private IFilterable adaptFilterable() {
		Object input = getTreeViewerInput();
		if (input != null) {
			return (IFilterable) getAdapter(input, IFilterable.class);
		}
		return null;
	}

	protected abstract Object getViewerInput();

	/**
	 * Get the viewer input adapter for the input.
	 *
	 * @param input the input of the tree viewer.
	 * @return The adapter.
	 */
	private IPropertyChangeProvider getPropertyChangeProvider() {
		Object input = getTreeViewerInput();
		if (input != null) {
			return (IPropertyChangeProvider) getAdapter(input, IPropertyChangeProvider.class);
		}
		return null;
    }

	Object getTreeViewerInput() {
		if (treeControl != null && treeControl.getViewer() != null) {
			return treeControl.getViewer().getInput();
		}
		return null;
	}

	/**
	 * Creates and returns a tree control.
	 *
	 * @return The new tree control.
	 */
	protected TreeControl doCreateTreeControl() {
		return new TreeControl(getViewerId(), this);
	}

	/**
	 * Returns the associated tree control.
	 *
	 * @return The associated tree control or <code>null</code>.
	 */
	public final TreeControl getTreeControl() {
		return treeControl;
	}

	/**
	 * Update the page's ui including its toolbar and title text and image.
	 */
	protected void updateUI() {
		toolbarMgr.update(true);
		IManagedForm managedForm = getManagedForm();
		Form form = managedForm.getForm().getForm();
		Object element = getTreeViewerInput();
		boolean filterEnabled = false;
		IFilterable filterDecorator = adaptFilterable();
		if (filterDecorator != null) {
			TreeViewer viewer = (TreeViewer) treeControl.getViewer();
			filterEnabled = TreeViewerUtil.isFiltering(viewer, TreePath.EMPTY);
		}
		ILabelDecorator titleDecorator = getTitleBarDecorator();
		String text = getFormTitle();
		if (text != null) {
			if (titleDecorator != null) {
				text = titleDecorator.decorateText(text, element);
			}
			if (filterEnabled) {
				TreeViewer viewer = (TreeViewer) treeControl.getViewer();
				text = TreeViewerUtil.getDecoratedText(text, viewer, TreePath.EMPTY);
			}
		}
		Image image = getFormImage();
		if (image != null) {
			if (titleDecorator != null) {
				image = titleDecorator.decorateImage(image, element);
			}
			if (filterEnabled) {
				TreeViewer viewer = (TreeViewer) treeControl.getViewer();
				image = TreeViewerUtil.getDecoratedImage(image, viewer, TreePath.EMPTY);
			}
		}
		if (text != null) {
			try {
				form.setText(text);
			}
			catch (Exception e) {
				// Ignore any disposed exception
			}
		}
		if (image != null) {
			try {
				form.setImage(image);
			}
			catch (Exception e) {
				// Ignore any disposed exception
			}
		}
	}

	/**
	 * Get the title bar's decorator or null if there's no decorator for it.
	 */
	protected ILabelDecorator getTitleBarDecorator() {
	    return null;
    }

	/**
	 * Propagate the current selection to the editor's selection provider.
	 */
	protected void propagateSelection() {
		ISelection selection = treeControl.getViewer().getSelection();
		ISelectionProvider selectionProvider = getSite().getSelectionProvider();
		// If the parent control is already disposed, we have no real chance of
		// testing for it. Catch the SWT exception here just in case.
		try {
			selectionProvider.setSelection(selection.isEmpty() ? new StructuredSelection(getEditorInputNode()) : selection);
			if (selectionProvider instanceof MultiPageSelectionProvider) {
				SelectionChangedEvent changedEvent = new SelectionChangedEvent(selectionProvider, selection);
				((MultiPageSelectionProvider) selectionProvider).fireSelectionChanged(changedEvent);
			}
		}
		catch (SWTException e) {
			/* ignored on purpose */
		}
	}

	/**
	 * Get the id of the command invoked when the tree is double-clicked.
	 * If the id is null, then no command is invoked.
	 *
	 * @return The double-click command id.
	 */
	protected String getDoubleClickCommandId() {
		return null;
	}

	/**
	 * Get the tree viewer's id. This viewer id is used by
	 * viewer extension to define columns and filters.
	 *
	 * @return This viewer's id or null.
	 */
	protected abstract String getViewerId();

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IDoubleClickListener#doubleClick(org.eclipse.jface.viewers.DoubleClickEvent)
	 */
	@Override
    public void doubleClick(final DoubleClickEvent event) {
		// If an handled and enabled command is registered for the ICommonActionConstants.OPEN
		// retargetable action id, redirect the double click handling to the command handler.
		//
		// Note: The default tree node expansion must be re-implemented in the active handler!
		String commandId = getDoubleClickCommandId();
		Command cmd = null;
		if(commandId != null) {
			ICommandService service = (ICommandService)PlatformUI.getWorkbench().getService(ICommandService.class);
			cmd = service != null ? service.getCommand(commandId) : null;
		}
		if (cmd != null && cmd.isDefined() && cmd.isEnabled()) {
			final Command command = cmd;
			SafeRunner.run(new SafeRunnable(){
				@Override
                public void handleException(Throwable e) {
					// Ignore exception
                }
				@Override
                public void run() throws Exception {
					IHandlerService handlerSvc = (IHandlerService)PlatformUI.getWorkbench().getService(IHandlerService.class);
					Assert.isNotNull(handlerSvc);

					ISelection selection = event.getSelection();
					EvaluationContext ctx = new EvaluationContext(handlerSvc.getCurrentState(), selection);
					ctx.addVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME, selection);
					ctx.addVariable(ISources.ACTIVE_MENU_SELECTION_NAME, selection);
					ctx.setAllowPluginActivation(true);

					ParameterizedCommand pCmd = ParameterizedCommand.generateCommand(command, null);
					Assert.isNotNull(pCmd);

					handlerSvc.executeCommandInContext(pCmd, null, ctx);
                }});
		} else {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			Object element = selection.getFirstElement();
			TreeViewer viewer = (TreeViewer) treeControl.getViewer();
			if (viewer.isExpandable(element)) {
				viewer.setExpandedState(element, !viewer.getExpandedState(element));
			}
		}
    }
}
