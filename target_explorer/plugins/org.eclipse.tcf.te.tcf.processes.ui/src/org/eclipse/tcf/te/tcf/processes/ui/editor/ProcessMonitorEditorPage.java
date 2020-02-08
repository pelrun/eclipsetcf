/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.editor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.services.ServiceUtils;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.processes.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.processes.ui.interfaces.IProcessMonitorUIDelegate;
import org.eclipse.tcf.te.tcf.processes.ui.navigator.events.TreeViewerListener;
import org.eclipse.tcf.te.tcf.processes.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.ui.editor.AbstractTreeViewerExplorerEditorPage;
import org.eclipse.tcf.te.ui.trees.AbstractTreeControl;
import org.eclipse.tcf.te.ui.trees.TreeControl;
import org.eclipse.tcf.te.ui.trees.TreeViewerHeaderMenu;
import org.eclipse.ui.IEditorInput;

/**
 * The editor page for Process Monitor.
 */
public class ProcessMonitorEditorPage extends AbstractTreeViewerExplorerEditorPage {
	// The decorator used to decorate the title bar.
	private ILabelDecorator decorator = new ProcessMonitorTitleDecorator();
	// The event listener instance
	private ProcessMonitorEventListener listener = null;
	// Reference to the tree listener
	/* default */ ITreeViewerListener treeListener = null;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractTreeViewerExplorerEditorPage#dispose()
	 */
	@Override
	public void dispose() {
		if (listener != null) {
			EventManager.getInstance().removeEventListener(listener);
			listener = null;
		}
	    super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractTreeViewerExplorerEditorPage#getViewerId()
	 */
	@Override
	protected String getViewerId() {
		return "org.eclipse.tcf.te.ui.controls.viewer.processes"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractCustomFormToolkitEditorPage#getFormTitle()
	 */
	@Override
    protected String getFormTitle() {
		String formTitle = Messages.getStringDelegated(getEditorInputNode(), "ProcessMonitorEditorPage_PageTitle"); //$NON-NLS-1$
	    return formTitle != null ? formTitle : Messages.ProcessMonitorEditorPage_PageTitle;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractEditorPage#setInput(org.eclipse.ui.IEditorInput)
     */
    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractCustomFormToolkitEditorPage#getContextHelpId()
	 */
	@Override
    protected String getContextHelpId() {
	    return "org.eclipse.tcf.te.tcf.processes.ui.ProcessExplorerEditorPage"; //$NON-NLS-1$
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#getPartName()
	 */
	@Override
	public String getPartName() {
		String partName = Messages.getStringDelegated(getEditorInputNode(), "ProcessMonitorEditorPage_PartName"); //$NON-NLS-1$
	    return partName != null ? partName : super.getPartName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractTreeViewerExplorerEditorPage#getTitleBarDecorator()
	 */
	@Override
    protected ILabelDecorator getTitleBarDecorator() {
	    return decorator;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractTreeViewerExplorerEditorPage#doCreateTreeControl()
	 */
	@Override
	protected TreeControl doCreateTreeControl() {
	    TreeControl treeControl = new TreeControl(getViewerId(), this) {
	    	/* (non-Javadoc)
	    	 * @see org.eclipse.tcf.te.ui.trees.AbstractTreeControl#configureTreeColumn(org.eclipse.swt.widgets.TreeColumn)
	    	 */
	    	@Override
	    	protected void configureTreeColumn(TreeColumn treeColumn) {
	    	    super.configureTreeColumn(treeColumn);

	    	    if (treeColumn.getText() != null) {
	    	    	String text = Messages.getStringDelegated(ProcessMonitorEditorPage.this.getEditorInputNode(), "ProcessMonitor_treeColum_" + treeColumn.getText().replace(' ', '_')); //$NON-NLS-1$
	    	    	if (text != null) treeColumn.setText(text);
	    	    }
	    	}

	    	/* (non-Javadoc)
	    	 * @see org.eclipse.tcf.te.ui.trees.AbstractTreeControl#createHeaderMenu(org.eclipse.tcf.te.ui.trees.AbstractTreeControl)
	    	 */
	    	@Override
	    	protected TreeViewerHeaderMenu createHeaderMenu(AbstractTreeControl control) {
	    		Assert.isNotNull(control);
	    	    return new TreeViewerHeaderMenu(control) {
	    	    	/* (non-Javadoc)
	    	    	 * @see org.eclipse.tcf.te.ui.trees.TreeViewerHeaderMenu#configureMenuItem(org.eclipse.swt.widgets.MenuItem)
	    	    	 */
	    	    	@Override
	    	    	protected void configureMenuItem(MenuItem item) {
	    	    	    super.configureMenuItem(item);

	    	    	    if (item.getText() != null) {
	    	    	    	String text = Messages.getStringDelegated(ProcessMonitorEditorPage.this.getEditorInputNode(), "ProcessMonitor_menuItem_" + item.getText().replace(' ', '_')); //$NON-NLS-1$
	    	    	    	if (text != null) item.setText(text);
	    	    	    }
	    	    	}
	    	    };
	    	}

	    	/* (non-Javadoc)
	    	 * @see org.eclipse.tcf.te.ui.trees.TreeControl#getAutoExpandLevel()
	    	 */
	    	@Override
	    	protected int getAutoExpandLevel() {
	    		if (ProcessMonitorEditorPage.this.getEditorInputNode() instanceof IPeerNode) {
	    			IPeerNode peerNode = (IPeerNode)ProcessMonitorEditorPage.this.getEditorInputNode();
	    			IProcessMonitorUIDelegate delegate = ServiceUtils.getUIServiceDelegate(peerNode, peerNode, IProcessMonitorUIDelegate.class);
					if (delegate != null) {
						return delegate.getAutoExpandLevel();
					}
	    		}
	    	    return super.getAutoExpandLevel();
	    	}
	    };
	    Assert.isNotNull(treeControl);

	    if (listener == null) {
	    	listener = new ProcessMonitorEventListener(treeControl);
	    	EventManager.getInstance().addEventListener(listener, ChangeEvent.class);
	    }

		if (treeListener == null && treeControl.getViewer() instanceof TreeViewer) {
			final TreeViewer treeViewer = (TreeViewer) treeControl.getViewer();

			treeListener = new TreeViewerListener();
			treeViewer.addTreeListener(treeListener);
			treeViewer.getTree().addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					if (treeListener != null) {
						treeViewer.removeTreeListener(treeListener);
						treeListener = null;
					}
				}
			});
		}

	    return treeControl;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractTreeViewerExplorerEditorPage#getViewerInput()
	 */
	@Override
    protected Object getViewerInput() {
		Object element = getEditorInputNode();
		IPeerNode peerNode = element instanceof IPeerNode ? (IPeerNode)element : null;
		if (peerNode == null && element instanceof IAdaptable) {
			peerNode = (IPeerNode)((IAdaptable)element).getAdapter(IPeerNode.class);
		}
		return peerNode != null ? ModelManager.getRuntimeModel(peerNode) : null;
    }
}
