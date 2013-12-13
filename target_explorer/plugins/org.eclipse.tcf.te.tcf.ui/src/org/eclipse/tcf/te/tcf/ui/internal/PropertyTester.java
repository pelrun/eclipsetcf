/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.utils.SimulatorUtils;
import org.eclipse.tcf.te.tcf.ui.handler.DeleteHandler;
import org.eclipse.tcf.te.ui.views.navigator.nodes.NewWizardNode;



/**
 * Property tester implementation.
 */
public class PropertyTester extends org.eclipse.core.expressions.PropertyTester {
	// Reference to the peer model delete handler (to determine "canDelete")
	private final DeleteHandler deleteHandler = new DeleteHandler();

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (receiver instanceof IStructuredSelection) {
			// Analyze the selection
			return testSelection((IStructuredSelection)receiver, property, args, expectedValue);
		}

		if ("isWizardId".equals(property) && receiver instanceof NewWizardNode) { //$NON-NLS-1$
			return ((NewWizardNode)receiver).getWizardId().equals(expectedValue);
		}

		if ("isValidSimulatorConfig".equals(property) && receiver instanceof IPeerNode && expectedValue instanceof Boolean) { //$NON-NLS-1$
			SimulatorUtils.Result simulator = SimulatorUtils.getSimulatorService((IPeerNode)receiver);
			boolean valid = simulator != null && simulator.service.isValidConfig(receiver, simulator.settings);
			return ((Boolean)expectedValue).booleanValue() == valid;
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
		}

		return false;
    }
}
