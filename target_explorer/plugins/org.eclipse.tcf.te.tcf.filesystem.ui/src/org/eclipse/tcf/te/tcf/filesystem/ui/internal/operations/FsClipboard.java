/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations;

import java.beans.PropertyChangeEvent;
import java.util.List;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tcf.te.core.utils.PropertyChangeProvider;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.ui.PlatformUI;

/**
 * The clip board to which copy or cut files/folders.
 */
public class FsClipboard extends PropertyChangeProvider {
	// The constants to define the current operation type of the clip board.
	private static final int NONE = -1;
	private static final int CUT = 0;
	private static final int COPY = 1;
	// The operation type, CUT, COPY or NONE.
	private int operation;
	// The currently selected files/folders.
	private List<IFSTreeNode> files;

	private Clipboard clipboard;

	/**
	 * Create a clip board instance.
	 */
	public FsClipboard() {
		clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
		operation = NONE;
	}

	public boolean isCutOp() {
		return operation == CUT;
	}

	public boolean isCopyOp() {
		return operation == COPY;
	}

	public boolean isEmpty() {
		return operation == NONE && (files == null || files.isEmpty());
	}

	/**
	 * Get the currently selected files/folders to operated.
	 */
	public List<IFSTreeNode> getFiles() {
		return files;
	}

	/**
	 * Cut the specified files/folders to the clip board.
	 */
	public void cutFiles(List<IFSTreeNode> files) {
		operation = CUT;
		this.files = files;
		PropertyChangeEvent event = new PropertyChangeEvent(this, "cut", null, null); //$NON-NLS-1$
		firePropertyChange(event);

		clearSystemClipboard();
	}

	/**
	 * Copy the specified files/folders to the clip board.
	 *
	 * @param files The file/folder nodes.
	 */
	public void copyFiles(List<IFSTreeNode> files) {
		operation = COPY;
		this.files = files;
		PropertyChangeEvent event = new PropertyChangeEvent(this, "copy", null, null); //$NON-NLS-1$
		firePropertyChange(event);

		clearSystemClipboard();
	}

	/**
	 * Clear the clip board.
	 */
	public void clear() {
		operation = NONE;
		this.files = null;
		PropertyChangeEvent event = new PropertyChangeEvent(this, "clear", null, null); //$NON-NLS-1$
		firePropertyChange(event);

		clearSystemClipboard();
	}

	/**
	 * Make sure the system clip board is cleared in a UI thread.
	 */
	void clearSystemClipboard() {
		if (Display.getCurrent() != null) {
			clipboard.clearContents();
		}
		else {
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable(){
				@Override
                public void run() {
					clearSystemClipboard();
                }});
		}
	}

	/**
	 * Dispose the clipboard.
	 */
    public void dispose() {
		if(Display.getCurrent() != null) {
			if (!clipboard.isDisposed()) {
				try {
					clipboard.dispose();
				}
				catch (SWTException e) {
				}
			}
		}
		else {
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable(){
				@Override
                public void run() {
					dispose();
                }});
		}
	}

	/**
	 * Get the system clipboard.
	 *
	 * @return The system clipboard.
	 */
	public Clipboard getSystemClipboard() {
		return clipboard;
	}
}
