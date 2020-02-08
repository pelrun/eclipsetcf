/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.navigator.dnd;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.tcf.te.ui.views.editor.EditorInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.navigator.CommonDragAdapterAssistant;
import org.eclipse.ui.part.EditorInputTransfer;
import org.eclipse.ui.part.EditorInputTransfer.EditorInputData;

/**
 * Drag assistant implementation.
 */
public class DragAssistant extends CommonDragAdapterAssistant {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.CommonDragAdapterAssistant#dragStart(org.eclipse.swt.dnd.DragSourceEvent, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void dragStart(DragSourceEvent event, IStructuredSelection selection) {
		event.doit = CommonDnD.isDraggable(selection);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.CommonDragAdapterAssistant#getSupportedTransferTypes()
	 */
	@Override
	public Transfer[] getSupportedTransferTypes() {
		 return new Transfer[] {LocalSelectionTransfer.getTransfer(), EditorInputTransfer.getInstance()};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.CommonDragAdapterAssistant#setDragData(org.eclipse.swt.dnd.DragSourceEvent, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public boolean setDragData(DragSourceEvent event, IStructuredSelection selection) {
		if (EditorInputTransfer.getInstance().isSupportedType(event.dataType)) {
			List<EditorInputData> data = new ArrayList<EditorInputTransfer.EditorInputData>();
			Iterator<?> it = selection.iterator();
			while (it.hasNext()) {
				IEditorInput input = new EditorInput(it.next());
				data.add(EditorInputTransfer.createEditorInputData("org.eclipse.tcf.te.ui.views.Editor", input)); //$NON-NLS-1$
            }
			event.data = data.toArray(new EditorInputData[data.size()]);
			return true;
		}
		return false;
	}
}
