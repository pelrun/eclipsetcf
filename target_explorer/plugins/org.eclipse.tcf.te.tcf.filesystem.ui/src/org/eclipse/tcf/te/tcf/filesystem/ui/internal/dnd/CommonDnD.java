/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.dnd;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IConfirmCallback;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.ImageConsts;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.handlers.MoveCopyCallback;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations.UiExecutor;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.ui.PlatformUI;
/**
 * Common DnD operations shared by File Explorer and Target Explorer.
 */
public class CommonDnD implements IConfirmCallback {

	/**
	 * If the current selection is draggable.
	 *
	 * @param selection The currently selected nodes.
	 * @return true if it is draggable.
	 */
	public boolean isDraggable(IStructuredSelection selection) {
		if (selection.isEmpty()) {
			return false;
		}
		Object[] objects = selection.toArray();
		for (Object object : objects) {
			if (!isDraggableObject(object)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * If the specified object is a draggable element.
	 *
	 * @param object The object to be dragged.
	 * @return true if it is draggable.
	 */
	private boolean isDraggableObject(Object object) {
		if (object instanceof IFSTreeNode) {
			IFSTreeNode node = (IFSTreeNode) object;
			if (node.isRootDirectory())
				return false;
			if (node.isWindowsNode())
				return !node.isReadOnly();
			return node.isWritable();
		}
		return false;
	}

	/**
	 * Perform the drop operation over dragged files to the specified target folder.
	 *
	 * @param viewer the tree viewer to be refreshed after dragging.
	 * @param files The files being dropped.
	 * @param operations the current dnd operations.
	 * @param target the target folder the files to be dropped to.
	 * @return true if the dropping is successful.
	 */
	public boolean dropFiles(TreeViewer viewer, String[] files, int operations, IFSTreeNode target) {
		boolean move = (operations & DND.DROP_MOVE) != 0;
		if (move) {
			String question;
			if (files.length == 1) {
				question = NLS.bind(Messages.FSDropTargetListener_MovingWarningSingle, files[0]);
			} else {
				question = NLS.bind(Messages.FSDropTargetListener_MovingWarningMultiple, Integer.valueOf(files.length));
			}
			Shell parent = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			if (!MessageDialog.openQuestion(parent, Messages.FSDropTargetListener_ConfirmMoveTitle, question)) {
				return false;
			}
		} else if ((operations & DND.DROP_COPY) == 0) {
			return false;
		}
		IStatus status = UiExecutor.execute(target.operationDropFiles(asList(files), this));
		if (move && status.isOK()) {
			for (String path : files) {
				File file = new File(path);
				if (!file.delete()) {
				}
			}
		}
		return status.isOK();
	}

//	private ICallback getCopyCallback(final TreeViewer viewer, final String[] files, final IFSTreeNode target) {
//		return new Callback() {
//			@Override
//			protected void internalDone(Object caller, IStatus status) {
//				// mstodo handle via notifications
//				if (status.isOK()) {
//					IOpExecutor executor = new JobExecutor(getSelectionCallback(viewer, files, target));
//					executor.execute(new OpRefresh(target));
//				}
//			}
//		};
//	}

//	private ICallback getMoveCallback(final TreeViewer viewer, final String[] files, final IFSTreeNode target) {
//		return new Callback() {
//			@Override
//			protected void internalDone(Object caller, IStatus status) {
//				if (status.isOK()) {
//					for (String path : files) {
//						File file = new File(path);
//						if (!file.delete()) {
//						}
//					}
					// mstodo handle via notifications
//					if (successful) {
//						IRuntimeModel model = ModelManager.getRuntimeModel(target.getPeerNode());
//						IOpExecutor executor = new JobExecutor(getSelectionCallback(viewer, files, target));
//						executor.execute(new OpRefresh(model.getRoot()));
//					}
//				}
//			}
//		};
//	}

//	ICallback getSelectionCallback(final TreeViewer viewer, final String[] paths, final IFSTreeNode target) {
//		return new Callback() {
//			@Override
//			protected void internalDone(Object caller, IStatus status) {
//				if(status.isOK()) {
//					List<IFSTreeNode> nodes = new ArrayList<IFSTreeNode>();
//					IFSTreeNode[] children = target.getChildren(true);
//					for (String path : paths) {
//						File file = new File(path);
//						String name = file.getName();
//						for (IFSTreeNode child : children) {
//							if (name.equals(child.getName())) {
//								nodes.add(child);
//								break;
//							}
//						}
//					}
//					if (viewer != null) {
//						updateViewer(viewer, target, nodes);
//					}
//				}
//			}
//		};
//	}

//	protected void updateViewer(final TreeViewer viewer, final IFSTreeNode target, final List<IFSTreeNode> nodes) {
//		if (Display.getCurrent() != null) {
//			viewer.refresh(target);
//			IStructuredSelection selection = new StructuredSelection(nodes.toArray());
//			viewer.setSelection(selection, true);
//		}
//		else {
//			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable(){
//				@Override
//                public void run() {
//					updateViewer(viewer, target, nodes);
//                }});
//		}
//    }

	/**
	 * Perform the drop operation over dragged selection.
	 *
	 * @param target the target Object to be moved to.
	 * @param operations the current dnd operations.
	 * @param selection The local selection being dropped.
	 * @return true if the dropping is successful.
	 */
	public boolean dropLocalSelection(IFSTreeNode target, int operations, IStructuredSelection selection) {
		List<IFSTreeNode> nodes = selection.toList();
		IOperation operation;
		boolean move = (operations & DND.DROP_MOVE) != 0;
		if (move) {
			operation = target.operationDropMove(nodes, new MoveCopyCallback());
		} else if ((operations & DND.DROP_COPY) != 0) {
			IFSTreeNode dest = getCopyDestination(target, nodes);
			boolean cpPerm = UIPlugin.isCopyPermission();
			boolean cpOwn = UIPlugin.isCopyOwnership();
			operation = dest.operationDropCopy(nodes, cpPerm, cpOwn, new MoveCopyCallback());
		} else {
			return false;
		}
		IStatus status = UiExecutor.execute(operation);
		if (move) {
			UIPlugin.getClipboard().clear();
		}
		return status.isOK();
	}

	/**
	 * Return an appropriate destination directory for copying according to the specified hovered
	 * node. If the hovered node is a file, then return its parent directory. If the hovered node is
	 * a directory, then return its self if it is not a node being copied. Return its parent
	 * directory if it is a node being copied.
	 *
	 * @param hovered
	 * @param nodes
	 * @return
	 */
	private IFSTreeNode getCopyDestination(IFSTreeNode hovered, List<IFSTreeNode> nodes) {
		if (hovered.isFile()) {
			return hovered.getParent();
		}
		else if (hovered.isDirectory()) {
			for (IFSTreeNode node : nodes) {
				if (node == hovered) {
					return hovered.getParent();
				}
			}
		}
		return hovered;
	}

	/**
	 * Validate dropping when the elements being dragged are files.
	 *
	 * @param target The target object.
	 * @param operation The DnD operation.
	 * @param transferType The transfered data simulator.
	 * @return true if it is valid for dropping.
	 */
	public boolean validateFilesDrop(Object target, int operation, TransferData transferType) {
		FileTransfer transfer = FileTransfer.getInstance();
		String[] elements = (String[]) transfer.nativeToJava(transferType);
		if (elements.length > 0) {
			boolean moving = (operation & DND.DROP_MOVE) != 0;
			boolean copying = (operation & DND.DROP_COPY) != 0;
			IFSTreeNode hovered = (IFSTreeNode) target;
			if (hovered.isFile() && copying) {
				hovered = hovered.getParent();
			}
			return hovered.isDirectory() && hovered.isWritable() && (moving || copying);
		}
		return false;
	}

	/**
	 * Validate dropping when the elements being dragged are local selection.
	 *
	 * @param target The target object.
	 * @param operation The DnD operation.
	 * @param transferType The transfered data simulator.
	 * @return true if it is valid for dropping.
	 */
	public boolean validateLocalSelectionDrop(Object target, int operation, TransferData transferType) {
		IFSTreeNode hovered = (IFSTreeNode) target;
		LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
		IStructuredSelection selection = (IStructuredSelection) transfer.getSelection();
		List<IFSTreeNode> nodes = selection.toList();
		boolean moving = (operation & DND.DROP_MOVE) != 0;
		boolean copying = (operation & DND.DROP_COPY) != 0;
		if (hovered.isDirectory() && hovered.isWritable() && (moving || copying)) {
			IFSTreeNode head = nodes.get(0);
			String hid = head.getPeerNode().getPeerId();
			String tid = hovered.getPeerNode().getPeerId();
			if (hid.equals(tid)) {
				for (IFSTreeNode node : nodes) {
					if (moving && node == hovered || node.getParent() == hovered || node.isAncestorOf(hovered)) {
						return false;
					}
				}
				return true;
			}
		}
		else if (hovered.isFile() && copying) {
			hovered = hovered.getParent();
			return validateLocalSelectionDrop(hovered, operation, transferType);
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.interfaces.IConfirmCallback#requires(java.lang.Object)
	 */
	@Override
    public boolean requires(Object object) {
		return true;
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.interfaces.IConfirmCallback#confirms(java.lang.Object)
	 */
	@Override
    public int confirms(final Object object) {
		final int[] results = new int[1];
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				Shell parent = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				String title = Messages.FSUpload_OverwriteTitle;
				String message = NLS.bind(Messages.FSUpload_OverwriteConfirmation, getName(object));
				final Image titleImage = UIPlugin.getImage(ImageConsts.DELETE_READONLY_CONFIRM);
				MessageDialog qDialog = new MessageDialog(parent, title, null, message,
								MessageDialog.QUESTION, new String[] {Messages.FSUpload_Yes,
								Messages.FSUpload_YesToAll, Messages.FSUpload_No, Messages.FSUpload_Cancel}, 0) {
					@Override
					public Image getQuestionImage() {
						return titleImage;
					}
				};
				results[0] = qDialog.open();
			}
		});
		return results[0];
    }

	protected String getName(Object object) {
		if (object instanceof File) {
			return ((File) object).getName();
		}
		if (object instanceof IFSTreeNode) {
			return ((IFSTreeNode) object).getName();
		}
		return String.valueOf(object);
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragSetData(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	public boolean setDragData(DragSourceEvent anEvent) {
	    if (LocalSelectionTransfer.getTransfer().isSupportedType(anEvent.dataType)) {
			anEvent.data = LocalSelectionTransfer.getTransfer().getSelection();
			return true;
		}
		else if (FileTransfer.getInstance().isSupportedType(anEvent.dataType)) {
			IStructuredSelection selection = (IStructuredSelection) LocalSelectionTransfer.getTransfer().getSelection();
			List<IFSTreeNode> nodes = selection.toList();
			List<String> paths = new ArrayList<String>();
			for(IFSTreeNode node : nodes) {
				File file = node.getCacheFile();
				if(file.exists()) {
					paths.add(file.getAbsolutePath());
				}
			}
			if (!paths.isEmpty()) anEvent.data = paths.toArray(new String[paths.size()]);
			return !paths.isEmpty();
		}
		return false;
    }
}
