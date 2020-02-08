/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.tcf.te.runtime.persistence.history.HistoryManager;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IDelegateService;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepAttributes;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.locator.utils.SimulatorUtils;
import org.eclipse.tcf.te.tcf.ui.handler.DeleteHandler;
import org.eclipse.tcf.te.tcf.ui.handler.RenameHandler;
import org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate;
import org.eclipse.tcf.te.ui.views.navigator.nodes.NewWizardNode;



/**
 * Property tester implementation.
 */
public class PropertyTester extends org.eclipse.core.expressions.PropertyTester {
	// References to the peer model delete/rename handlers (to determine "canDelete" and "canRename")
	private final DeleteHandler deleteHandler = new DeleteHandler();
	private final RenameHandler renameHandler = new RenameHandler();

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (receiver instanceof IStructuredSelection) {
			// Analyze the selection
			return testSelection((IStructuredSelection)receiver, property, args, expectedValue);
		}

		if ("canDelete".equals(property) || "canRename".equals(property)) { //$NON-NLS-1$ //$NON-NLS-2$
			return testSelection(new StructuredSelection(receiver), property, args, expectedValue);
		}

		if ("hasHistory".equals(property) && receiver instanceof IPeerNode) { //$NON-NLS-1$
			IPeerNode peerNode = (IPeerNode)receiver;
			IService[] services = ServiceManager.getInstance().getServices(peerNode, IDelegateService.class, false);
			for (IService service : services) {
		        if (service instanceof IDelegateService) {
		        	IDefaultContextToolbarDelegate delegate = ((IDelegateService)service).getDelegate(peerNode, IDefaultContextToolbarDelegate.class);
		        	if (delegate != null) {
        		    	String[] entries = HistoryManager.getInstance().getHistory(IStepAttributes.PROP_LAST_RUN_HISTORY_ID + "@" + peerNode.getPeerId()); //$NON-NLS-1$
        		    	if (entries != null && entries.length > 0) {
        		    		return true;
        		    	}
		        	}
		        }
	        }
			return false;
		}

		if ("isWizardId".equals(property) && receiver instanceof NewWizardNode) { //$NON-NLS-1$
			return ((NewWizardNode)receiver).getWizardId().equals(expectedValue);
		}

		if ("isValidSimulatorConfig".equals(property) && receiver instanceof IPeerNode && expectedValue instanceof Boolean) { //$NON-NLS-1$
			SimulatorUtils.Result simulator = SimulatorUtils.getSimulatorService((IPeerNode)receiver);
			boolean valid = simulator != null && simulator.service.isValidConfig(receiver, simulator.settings, true);
			return ((Boolean)expectedValue).booleanValue() == valid;
		}

		if ("canChangeDefaultContext".equals(property)) { //$NON-NLS-1$
			IPeerNode defaultPeer = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
			List<IPeerNode> peerNodes = Arrays.asList(ModelManager.getPeerModel().getPeerNodes());
			if (defaultPeer != null && peerNodes.contains(defaultPeer)) {
				return peerNodes.size() > 1;
			}
			return peerNodes.size() > 0;
		}


		return false;
	}

	/**
	 * Test the specific selection properties.
	 *
	 * @param selection The selection. Must not be <code>null</code>.
	 * @param property The property to test.
	 * @param args The property arguments.
	 * @param expectedValue The expected value.
	 *
	 * @return <code>True</code> if the property to test has the expected value, <code>false</code>
	 *         otherwise.
	 */
    protected boolean testSelection(IStructuredSelection selection, String property, Object[] args, Object expectedValue) {
		Assert.isNotNull(selection);

		if ("canDelete".equals(property)) { //$NON-NLS-1$
			return deleteHandler.canDelete(selection);
		} else if ("canRename".equals(property)) { //$NON-NLS-1$
			return renameHandler.canRename(selection);
		}

		return false;
    }
}
