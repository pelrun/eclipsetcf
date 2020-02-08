/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations;

import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tcf.te.core.utils.PropertyChangeProvider;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.ui.swt.DisplayUtil;
import org.eclipse.ui.PlatformUI;

/**
 * The clip board to which copy or cut files/folders.
 */
public class FsClipboard extends PropertyChangeProvider {

	/* default */ static class FsClipboardContent {
		// The constants to define the current operation type of the clip board.
		public static final int CUT = 0;
		public static final int COPY = 1;

		// The operation type, CUT, COPY or NONE.
		public final int operation;
		// The currently selected files/folders.
		public final List<IFSTreeNode> files;

		/**
         * Constructor
         */
        public FsClipboardContent(int operation, List<IFSTreeNode> files) {
        	Assert.isTrue(operation == CUT || operation == COPY);
        	this.operation = operation;
        	Assert.isNotNull(files);
        	this.files = files;
        }
	}

	private static class FsClipboardCache {
		private boolean cacheValid;
		private boolean clipboardEmpty;

		public FsClipboardCache() {
			super();
			cacheValid = false;
			clipboardEmpty = true;
		}

		public boolean isCacheValid() {
			return cacheValid;
		}

		public void invalidateCache() {
			cacheValid = false;
		}

		public boolean isClipboardEmpty() {
			return clipboardEmpty;
		}

		public void setClipboardEmpty(boolean empty) {
			clipboardEmpty = empty;
			cacheValid = true;
		}
	}

	/* default */ final Clipboard clipboard;
	/* default */ final FsClipboardCache clipboardCache;

	/**
	 * Create a clip board instance.
	 */
	public FsClipboard() {
		clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
		clipboardCache = new FsClipboardCache();
	}

	public boolean isCutOp() {
		final AtomicReference<Object> object = new AtomicReference<Object>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				object.set(clipboard.getContents(FsClipboardTransfer.getInstance()));
			}
		};

		exec(runnable);

		FsClipboardContent content = (FsClipboardContent) object.get();

		return content != null && content.operation == FsClipboardContent.CUT;
	}

	public boolean isCopyOp() {
		final AtomicReference<Object> object = new AtomicReference<Object>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				object.set(clipboard.getContents(FsClipboardTransfer.getInstance()));
			}
		};

		exec(runnable);

		FsClipboardContent content = (FsClipboardContent) object.get();

		return content != null && content.operation == FsClipboardContent.COPY;
	}

	public boolean isEmpty() {
		// Check cache before consulting SWT clipboard
		if (clipboardCache.isCacheValid()) {
			return clipboardCache.isClipboardEmpty();
		}

		final AtomicReference<Object> object = new AtomicReference<Object>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				object.set(clipboard.getContents(FsClipboardTransfer.getInstance()));
			}
		};

		exec(runnable);

		FsClipboardContent content = (FsClipboardContent) object.get();

		boolean empty = content == null || content.files.isEmpty();
		clipboardCache.setClipboardEmpty(empty);
		return empty;
	}

	/**
	 * Get the currently selected files/folders to operated.
	 */
	public List<IFSTreeNode> getFiles() {
		final AtomicReference<Object> object = new AtomicReference<Object>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				object.set(clipboard.getContents(FsClipboardTransfer.getInstance()));
			}
		};

		exec(runnable);

		FsClipboardContent content = (FsClipboardContent) object.get();

		return content.files;
	}

	/**
	 * Cut the specified files/folders to the clip board.
	 */
	public void cutFiles(List<IFSTreeNode> files) {
		Assert.isNotNull(files);

		final FsClipboardContent content = new FsClipboardContent(FsClipboardContent.CUT, files);
		final FsClipboardTransfer transfer = FsClipboardTransfer.getInstance();
		transfer.setContent(content);

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				clipboardCache.invalidateCache();
				clipboard.setContents(new Object[] { content }, new Transfer[] { transfer });

				PropertyChangeEvent event = new PropertyChangeEvent(this, "cut", null, null); //$NON-NLS-1$
				firePropertyChange(event);
			}
		};

		exec(runnable);
	}

	/**
	 * Copy the specified files/folders to the clip board.
	 *
	 * @param files The file/folder nodes.
	 */
	public void copyFiles(List<IFSTreeNode> files) {
		Assert.isNotNull(files);

		final FsClipboardContent content = new FsClipboardContent(FsClipboardContent.COPY, files);
		final FsClipboardTransfer transfer = FsClipboardTransfer.getInstance();
		transfer.setContent(content);

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				clipboardCache.invalidateCache();
				clipboard.setContents(new Object[] { content }, new Transfer[] { transfer });

				PropertyChangeEvent event = new PropertyChangeEvent(this, "copy", null, null); //$NON-NLS-1$
				firePropertyChange(event);
			}
		};

		exec(runnable);
	}

	/**
	 * Clear the clip board.
	 */
	public void clear() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				clipboardCache.invalidateCache();
				clipboard.clearContents();

				PropertyChangeEvent event = new PropertyChangeEvent(this, "clear", null, null); //$NON-NLS-1$
				firePropertyChange(event);
			}
		};

		exec(runnable);
	}

	/**
	 * Executes the given runnable in the UI thread synchronously.
	 *
	 * @param runnable The runnable. Must not be <code>null</code>.
	 */
	private void exec(Runnable runnable) {
		Assert.isNotNull(runnable);

		if (Display.getCurrent() != null) {
			runnable.run();
		} else {
			DisplayUtil.safeSyncExec(runnable);
		}
	}

	/**
	 * Dispose the clip board.
	 */
    public void dispose() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if (!clipboard.isDisposed()) {
					try { clipboard.dispose(); } catch (SWTException e) {}
				}
			}
		};

		exec(runnable);
	}

	/**
	 * Get the system clip board.
	 *
	 * @return The system clip board.
	 */
	public final Clipboard getSystemClipboard() {
		return clipboard;
	}
}
