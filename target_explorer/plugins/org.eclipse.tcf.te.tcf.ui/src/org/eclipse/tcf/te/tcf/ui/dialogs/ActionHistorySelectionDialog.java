/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.ui.dialogs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.runtime.persistence.history.HistoryManager;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IDelegateService;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.ui.handler.images.ActionHistoryImageDescriptor;
import org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.jface.images.AbstractImageDescriptor;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;

/**
 * ActionHistorySelectionDialog
 */
public class ActionHistorySelectionDialog extends AbstractArraySelectionDialog {

	protected static class Entry {
		String historyId;
		IPeerNode peerNode;
		IDefaultContextToolbarDelegate delegate;
		String data;
	}

	/**
	 * Constructor.
	 * @param shell
	 * @param contextHelpId
	 */
	public ActionHistorySelectionDialog(Shell shell, String contextHelpId) {
		super(shell, contextHelpId);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CLIENT_ID, Messages.ActionHistorySelectionDialog_button_edit,	true);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,	true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.CLIENT_ID == buttonId) {
			editPressed();
		}
		else {
		    super.buttonPressed(buttonId);
		}
	}

	protected void editPressed() {
		Entry entry = getSelectedEntry();
		if (entry != null) {
			entry.delegate.execute(entry.peerNode, entry.historyId, entry.data, true);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractArraySelectionDialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		Entry entry = getSelectedEntry();
	    super.okPressed();
		if (entry != null) {
			entry.delegate.execute(entry.peerNode, entry.historyId, entry.data, true);
		}
	}

	protected Entry getSelectedEntry() {
		ISelection sel = viewer.getSelection();
		if (sel instanceof IStructuredSelection && !((IStructuredSelection)sel).isEmpty()) {
			Object first = ((IStructuredSelection)sel).getFirstElement();
			if (first instanceof Entry) {
				return (Entry)first;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractArraySelectionDialog#updateEnablement(org.eclipse.jface.viewers.TableViewer)
	 */
	@Override
	protected void updateEnablement(TableViewer viewer) {
	    Entry entry = getSelectedEntry();

	    // Adjust the OK button enablement
	    Button okButton = getButton(IDialogConstants.OK_ID);
	    SWTControlUtil.setEnabled(okButton, entry != null && entry.delegate.validate(entry.peerNode, entry.historyId, entry.data));

	    // Adjust the edit button enablement
	    Button editButton = getButton(IDialogConstants.CLIENT_ID);
	    SWTControlUtil.setEnabled(editButton, entry != null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractArraySelectionDialog#getInput()
	 */
	@Override
	protected Object[] getInput() {
		final IPeerNode peerNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);

		IService[] services = ServiceManager.getInstance().getServices(peerNode, IDelegateService.class, false);
		Map<String, IDefaultContextToolbarDelegate> historyIds = new LinkedHashMap<String, IDefaultContextToolbarDelegate>();
		String[] ids = new String[0];
		for (IService service : services) {
	        if (service instanceof IDelegateService) {
	        	IDefaultContextToolbarDelegate delegate = ((IDelegateService)service).getDelegate(peerNode, IDefaultContextToolbarDelegate.class);
	        	if (delegate != null) {
	        		ids = delegate.getToolbarHistoryIds(peerNode, ids);
        			for (String newId : ids) {
        				if (!historyIds.containsKey(newId)) {
        					historyIds.put(newId, delegate);
        				}
                    }
	        	}
	        }
        }

		List<Entry> actions = new ArrayList<Entry>();
	    for (final String historyId : ids) {
	    	String[] entries = HistoryManager.getInstance().getHistory(historyId);
	    	final IDefaultContextToolbarDelegate delegate = historyIds.get(historyId);
	    	if (entries != null && entries.length > 0) {
	    		for (final String entry : entries) {
	    			Entry action = new Entry();
	    			action.peerNode = peerNode;
	    			action.historyId = historyId;
	    			action.delegate = delegate;
	    			action.data = entry;

	    			actions.add(action);
                }
	    	}
	    }
		return actions.toArray();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractArraySelectionDialog#getLabelProvider()
	 */
	@Override
	protected IBaseLabelProvider getLabelProvider() {
	    return new CellLabelProvider() {
	    	/* (non-Javadoc)
	    	 * @see org.eclipse.jface.viewers.CellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
	    	 */
	    	@Override
	    	public void update(ViewerCell cell) {
	    		cell.setText(getText(cell.getElement()));
	    		cell.setImage(getImage(cell.getElement()));
	    	}

	    	/* (non-Javadoc)
	    	 * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipText(java.lang.Object)
	    	 */
	    	@Override
            public String getToolTipText(Object element) {
	    		Entry entry = (Entry)element;
    			return entry.delegate.getDescription(entry.peerNode, entry.historyId, entry.data);
	    	}

	    	public String getText(Object element) {
	    		Entry entry = (Entry)element;
    			return entry.delegate.getLabel(entry.peerNode, entry.historyId, entry.data);
	    	}

	    	public Image getImage(Object element) {
	    		Entry entry = (Entry)element;
    			AbstractImageDescriptor descriptor = new ActionHistoryImageDescriptor(
    							UIPlugin.getDefault().getImageRegistry(),
    							entry.delegate.getImage(entry.peerNode, entry.historyId, entry.data),
    							entry.delegate.validate(entry.peerNode, entry.historyId, entry.data));
    			return UIPlugin.getSharedImage(descriptor);
	    	}
	    };
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractArraySelectionDialog#getDialogTitle()
	 */
	@Override
	protected String getDialogTitle() {
		return Messages.ActionHistorySelectionDialog_dialogTitle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractArraySelectionDialog#getTitle()
	 */
	@Override
	protected String getTitle() {
		IPeerNode peerNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
		return NLS.bind(Messages.ActionHistorySelectionDialog_title, peerNode.getName());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractArraySelectionDialog#getDefaultMessage()
	 */
	@Override
	protected String getDefaultMessage() {
		return Messages.ActionHistorySelectionDialog_message;
	}

}
