/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IResultOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.ui.views.editor.pages.AbstractTreeViewerExplorerEditorPage;
import org.eclipse.tcf.te.ui.wizards.AbstractWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.navigator.CommonNavigator;

/**
 * The base wizard class to create a new file/folder in the file system of Target Explorer.
 */
public abstract class NewNodeWizard extends AbstractWizard implements INewWizard {
	// The folder in which the new node is created.
	private IFSTreeNode folder;
	// The target peer where the new node is created.
	private IPeerNode peer;
	// The wizard page used to create the new node.
	private NewNodeWizardPage newPage;

	/**
	 * Create an instance.
	 */
	public NewNodeWizard() {
		setNeedsProgressMonitor(true);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);
		// Set the window title
		setWindowTitle(getTitle());
		if (!selection.isEmpty()) {
			Object element = selection.getFirstElement();
			if (element instanceof IFSTreeNode) {
				folder = (IFSTreeNode) element;
				if (folder.isFile()) {
					// If the selected is a file, then create the node in the parent folder.
					folder = folder.getParent();
				}
				peer = folder.getPeerNode();
			}
			else if (element instanceof IPeerNode) {
				if(hasFileSystem((IPeerNode) element)) {
					peer = (IPeerNode) element;
				}
			}
		}
	}

	/**
	 * Test if the specified target peer has a file system service.
	 *
	 * @param peer The target peer.
	 * @return true if it has a file system service.
	 */
	public boolean hasFileSystem(final IPeerNode peer) {
		if(Protocol.isDispatchThread()) {
			String services = null;
			services = peer.getStringProperty(IPeerNodeProperties.PROPERTY_REMOTE_SERVICES);
			if (services != null) {
				// Lookup each service individually
				for (String service : services.split(",")) { //$NON-NLS-1$
					if (service != null && service.trim().equals("FileSystem")) { //$NON-NLS-1$
						return true;
					}
				}
			}
		    return false;
		}
		final boolean[] result = new boolean[1];
		Protocol.invokeAndWait(new Runnable(){
			@Override
            public void run() {
				result[0] = hasFileSystem(peer);
            }});
		return result[0];
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		if (peer == null) {
			addPage(new TargetSelectionPage());
		}
		addPage(newPage = createWizardPage());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		if (newPage != null) {
			// Save the value so that next time it is used as the default input.
			newPage.saveWidgetValues();
			// Get the new name and create the node.
			String name = newPage.getNodeName();
			IFSTreeNode dest = newPage.getInputDir();
			final IResultOperation<? extends IFSTreeNode> create = getCreateOp(dest, name);
			final IStatus[] status = {Status.CANCEL_STATUS};
			IRunnableWithProgress runnable = new IRunnableWithProgress() {
				@Override
                public void run(IProgressMonitor monitor) {
					status[0] = create.run(monitor);
                }
			};
			try {
	            getContainer().run(false, false, runnable);
	            if (status[0].isOK()) {
					final IFSTreeNode newNode = create.getResult();
					getShell().getDisplay().asyncExec(new Runnable(){
						@Override
		                public void run() {
							selectNewNode(newNode);
		                }});
					return true;
	            }
	            newPage.setErrorMessage(status[0].getMessage());
            } catch (InvocationTargetException e) {
	            newPage.setErrorMessage(e.getMessage());
            } catch (InterruptedException e) {
            }
		}
		return false;
	}

	/**
	 * Select the specified node in the selection provider.
	 *
	 * @param node The node to be selected.
	 */
	void selectNewNode(IFSTreeNode node) {
		TreeViewer viewer = getFocusedViewer();
		if(viewer != null) {
			viewer.refresh(folder);
			ISelection selection = new StructuredSelection(node);
			viewer.setSelection(selection, true);
		}
    }

	/**
	 * Get currently focused tree viewer.
	 *
	 * @return currently focused tree viewer or null.
	 */
	TreeViewer getFocusedViewer() {
		IWorkbenchWindow window = getWorkbench() != null ? getWorkbench().getActiveWorkbenchWindow() : null;
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				IWorkbenchPart part = page.getActivePart();
				if (part instanceof FormEditor) {
					FormEditor formEditor = (FormEditor) part;
					IFormPage formPage = formEditor.getActivePageInstance();
					if (formPage instanceof AbstractTreeViewerExplorerEditorPage) {
						AbstractTreeViewerExplorerEditorPage viewerPage = (AbstractTreeViewerExplorerEditorPage) formPage;
						return (TreeViewer) viewerPage.getTreeControl().getViewer();
					}
				} else if (part instanceof CommonNavigator) {
					CommonNavigator navigator = (CommonNavigator) part;
					return navigator.getCommonViewer();
				}
			}
		}
		return null;
	}

	/**
	 * Create a wizard page to create a new node.
	 *
	 * @return The new wizard page.
	 */
	protected abstract NewNodeWizardPage createWizardPage();

	/**
	 * Create a Create operation instance using the specified folder and the new name.
	 *
	 * @param folder The folder in which the new node is created.
	 * @param name The name of the new node.
	 * @return a FSCreate instance to do the creation.
	 */
	protected abstract IResultOperation<? extends IFSTreeNode> getCreateOp(IFSTreeNode folder, String name);

	/**
	 * The wizard's title to be used.
	 *
	 * @return The wizard's title to be used.
	 */
	protected abstract String getTitle();

	/**
	 * Get the current target peer selected.
	 *
	 * @return The target peer selected.
	 */
	public IPeerNode getPeer(){
		return peer;
	}

	/**
	 * Set the currently selected target peer.
	 *
	 * @param peer The newly selected target peer.
	 */
	public void setPeer(IPeerNode peer) {
		this.peer = peer;
		newPage.setPeer(peer);
	}

	/**
	 * Get the current selected folder.
	 *
	 * @return the current selected folder.
	 */
	public IFSTreeNode getFolder() {
		return folder;
	}
}
