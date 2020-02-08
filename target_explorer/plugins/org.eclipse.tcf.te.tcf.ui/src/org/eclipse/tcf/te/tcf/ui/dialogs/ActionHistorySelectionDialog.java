/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.persistence.history.HistoryManager;
import org.eclipse.tcf.te.runtime.persistence.utils.DataHelper;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IDelegateService;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepAttributes;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.ui.handler.images.ActionHistoryImageDescriptor;
import org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate;
import org.eclipse.tcf.te.tcf.ui.interfaces.IPreferenceKeys;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.jface.images.AbstractImageDescriptor;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * ActionHistorySelectionDialog
 */
public class ActionHistorySelectionDialog extends AbstractArraySelectionDialog {

	private static final int EDIT_ID = IDialogConstants.CLIENT_ID;
	private static final int COPY_ID = IDialogConstants.CLIENT_ID + 1;

	MenuItem menuExecute = null;
	MenuItem menuEdit = null;
	MenuItem menuCopy = null;
	MenuItem menuRemove = null;

	private int maxEntries = -1;

	protected static class Entry {
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
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractArraySelectionDialog#createDialogAreaContent(org.eclipse.swt.widgets.Composite)
	 */
	@SuppressWarnings("unused")
    @Override
	protected void createDialogAreaContent(Composite parent) {
	    super.createDialogAreaContent(parent);

        Menu menu = new Menu(getViewer().getTable());

        menuEdit = new MenuItem(menu, SWT.PUSH);
        menuEdit.setText(Messages.ActionHistorySelectionDialog_button_copy);
        menuEdit.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        	    editPressed();
        	}
		});

        menuCopy = new MenuItem(menu, SWT.PUSH);
        menuCopy.setText(Messages.ActionHistorySelectionDialog_button_edit);
        menuCopy.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        	    copyPressed();
        	}
		});

        menuExecute = new MenuItem(menu, SWT.PUSH);
        menuExecute.setText(Messages.ActionHistorySelectionDialog_button_execute);
        menuExecute.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        	    okPressed();
        	}
		});

        new MenuItem(menu, SWT.SEPARATOR);

        menuRemove = new MenuItem(menu, SWT.PUSH);
        menuRemove.setText(Messages.ActionHistorySelectionDialog_button_remove);
        menuRemove.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        	    removePressed();
        	}
		});
        menuRemove.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));

        getViewer().getTable().setMenu(menu);

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, COPY_ID, Messages.ActionHistorySelectionDialog_button_copy, false);
		createButton(parent, EDIT_ID, Messages.ActionHistorySelectionDialog_button_edit, false);
		createButton(parent, IDialogConstants.OK_ID, Messages.ActionHistorySelectionDialog_button_execute, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if (EDIT_ID == buttonId) {
			editPressed();
		}
		else if (COPY_ID == buttonId) {
			copyPressed();
		}
		else {
		    super.buttonPressed(buttonId);
		}
	}

	protected void removePressed() {
		Entry entry = getSelectedEntry();
		if (entry != null) {
			HistoryManager.getInstance().remove(IStepAttributes.PROP_LAST_RUN_HISTORY_ID + "@" + entry.peerNode.getPeerId(), entry.data); //$NON-NLS-1$
			getViewer().setInput(getInput());
			getViewer().refresh();
		}
	}

	protected void copyPressed() {
		Entry entry = getSelectedEntry();
		if (entry != null) {
			if (entry.delegate.execute(entry.peerNode, entry.data, true) != null) {
				close();
			}
		}
	}

	protected void editPressed() {
		Entry entry = getSelectedEntry();
		if (entry != null) {
			String newData = entry.delegate.execute(entry.peerNode, entry.data, true);
			if (newData != null) {
				if (!entry.data.equals(newData)) {
					HistoryManager.getInstance().remove(IStepAttributes.PROP_LAST_RUN_HISTORY_ID + "@" + entry.peerNode.getPeerId(), entry.data); //$NON-NLS-1$
				}
				close();
			}
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
			if (entry.delegate.execute(entry.peerNode, entry.data, false) != null) {
				close();
			}
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
		if (viewer.getTable().getSelectionCount() == 0) {
			viewer.getTable().setSelection(0);
		}

	    Entry entry = getSelectedEntry();

	    // Adjust the OK button enablement
	    Button okButton = getButton(IDialogConstants.OK_ID);
	    SWTControlUtil.setEnabled(okButton, entry != null && entry.delegate.validate(entry.peerNode, entry.data));
	    if (menuExecute != null) {
		    menuExecute.setEnabled(entry != null && entry.delegate.validate(entry.peerNode, entry.data));
	    }

	    // Adjust the edit button enablement
	    Button editButton = getButton(EDIT_ID);
	    SWTControlUtil.setEnabled(editButton, entry != null);
	    if (menuEdit != null) {
		    menuEdit.setEnabled(entry != null);
	    }

	    // Adjust the copy button enablement
	    Button copyButton = getButton(COPY_ID);
	    SWTControlUtil.setEnabled(copyButton, entry != null);
	    if (menuCopy != null) {
		    menuCopy.setEnabled(entry != null);
	    }

	    if (menuRemove != null) {
		    menuRemove.setEnabled(entry != null);
	    }
	}

	protected int getMaxEntries() {
		if (maxEntries <= 0) {
			maxEntries = UIPlugin.getScopedPreferences().getInt(IPreferenceKeys.PREF_MAX_RECENT_ACTION_ENTRIES);
		}
		return maxEntries;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractArraySelectionDialog#getInput()
	 */
	@Override
	protected Object[] getInput() {
		final IPeerNode peerNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);

		IService[] services = ServiceManager.getInstance().getServices(peerNode, IDelegateService.class, false);
		Map<String, IDefaultContextToolbarDelegate> delegates = new LinkedHashMap<String, IDefaultContextToolbarDelegate>();
		for (IService service : services) {
	        if (service instanceof IDelegateService) {
	        	IDefaultContextToolbarDelegate delegate = ((IDelegateService)service).getDelegate(peerNode, IDefaultContextToolbarDelegate.class);
	        	if (delegate != null) {
        			for (String stepGroupId : delegate.getHandledStepGroupIds(peerNode)) {
        				if (!delegates.containsKey(stepGroupId)) {
        					delegates.put(stepGroupId, delegate);
        				}
                    }
	        	}
	        }
        }

		List<Entry> actions = new ArrayList<Entry>();
    	String[] entries = HistoryManager.getInstance().getHistory(IStepAttributes.PROP_LAST_RUN_HISTORY_ID + "@" + peerNode.getPeerId()); //$NON-NLS-1$
    	if (entries != null && entries.length > 0) {
    		int count = 0;
    		for (final String entry : entries) {
    			if (count >= getMaxEntries()) {
    				break;
    			}
    			IPropertiesContainer decoded = DataHelper.decodePropertiesContainer(entry);
    			String stepGroupId = decoded.getStringProperty(IStepAttributes.ATTR_STEP_GROUP_ID);
    			if (stepGroupId != null && delegates.containsKey(stepGroupId)) {
    				Entry action = new Entry();
    				action.peerNode = peerNode;
    				action.delegate = delegates.get(stepGroupId);
    				action.data = entry;
         			actions.add(action);
         			count++;
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
	    	@Override
	    	public void update(ViewerCell cell) {
	    		cell.setText(" " + getText(cell.getElement())); //$NON-NLS-1$
	    		cell.setImage(getImage(cell.getElement()));
	    	}
	    	@Override
            public String getToolTipText(Object element) {
	    		Entry entry = (Entry)element;
    			return entry.delegate.getDescription(entry.peerNode, entry.data);
	    	}

	    	public String getText(Object element) {
	    		Entry entry = (Entry)element;
    			return entry.delegate.getLabel(entry.peerNode, entry.data);
	    	}

	    	public Image getImage(Object element) {
	    		Entry entry = (Entry)element;
    			AbstractImageDescriptor descriptor = new ActionHistoryImageDescriptor(
    							UIPlugin.getDefault().getImageRegistry(),
    							entry.delegate.getImage(entry.peerNode, entry.data),
    							entry.delegate.validate(entry.peerNode, entry.data));
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

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractArraySelectionDialog#updateSelection(org.eclipse.jface.viewers.ISelection)
	 */
    @Override
    protected void updateSelection(ISelection selection) {
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractArraySelectionDialog#createButtonAreaContent(org.eclipse.swt.widgets.Composite)
	 */
    @Override
    protected void createButtonAreaContent(Composite parent) {
    }

}
