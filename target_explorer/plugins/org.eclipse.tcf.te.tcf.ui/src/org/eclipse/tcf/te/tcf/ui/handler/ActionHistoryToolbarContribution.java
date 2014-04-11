package org.eclipse.tcf.te.tcf.ui.handler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.persistence.history.HistoryManager;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.services.interfaces.IUIService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

public class ActionHistoryToolbarContribution extends CompoundContributionItem implements IWorkbenchContribution {

    IServiceLocator serviceLocator;

    @Override
    public void initialize(IServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @Override
    public void dispose() {
    	super.dispose();
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

	@Override
	protected IContributionItem[] getContributionItems() {
		List<IContributionItem> items = new ArrayList<IContributionItem>();
		final IPeerNode peerNode = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
		boolean enabled = (peerNode != null && peerNode.getConnectState() == IConnectable.STATE_CONNECTED);

		IService[] services = ServiceManager.getInstance().getServices(peerNode, IUIService.class, false);
		Map<String, IDefaultContextToolbarDelegate> historyIds = new LinkedHashMap<String, IDefaultContextToolbarDelegate>();
		String[] ids = new String[0];
		for (IService service : services) {
	        if (service instanceof IUIService) {
	        	IDefaultContextToolbarDelegate delegate = ((IUIService)service).getDelegate(peerNode, IDefaultContextToolbarDelegate.class);
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

	    for (final String historyId : ids) {
	    	String[] entries = HistoryManager.getInstance().getHistory(historyId);
	    	final IDefaultContextToolbarDelegate delegate = historyIds.get(historyId);
	    	if (entries != null && entries.length > 0) {
	    		if (!items.isEmpty()) {
		    		items.add(new Separator());
	    		}
	    		List<String> labels = new ArrayList<String>();
	    		for (final String entry : entries) {
	    			String label = delegate.getLabel(peerNode, historyId, entry);
	    			if (labels.contains(label)) {
	    				label += " ..."; //$NON-NLS-1$
	    			}
	    			else {
	    				labels.add(label);
	    			}
	    			IAction action = new Action(label) {
	    				@Override
	    				public void runWithEvent(Event event) {
	    					delegate.execute(peerNode, historyId, entry, (event.stateMask & SWT.CTRL) == SWT.CTRL);
	    				}
	    			};
	    			action.setEnabled(enabled);
	    			action.setImageDescriptor(delegate.getImageDescriptor(peerNode, historyId, entry));
	    			IContributionItem item = new ActionContributionItem(action);
	    			items.add(item);
                }
	    	}
	    }
		return items.toArray(new IContributionItem[items.size()]);
	}

}
