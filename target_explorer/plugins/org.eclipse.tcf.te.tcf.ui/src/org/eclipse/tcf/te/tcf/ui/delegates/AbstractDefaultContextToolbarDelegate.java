/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.ui.delegates;

import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.persistence.utils.DataHelper;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepAttributes;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate;

/**
 * AbstractDefaultContextToolbarDelegate
 */
public abstract class AbstractDefaultContextToolbarDelegate implements IDefaultContextToolbarDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate#getToolbarNewConfigWizardIds(java.lang.Object)
	 */
	@Override
	public String[] getToolbarNewConfigWizardIds(Object context) {
		return new String[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate#getHandledStepGroupIds(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode)
	 */
	@Override
	public String[] getHandledStepGroupIds(IPeerNode peerNode) {
	    return new String[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate#getLabel(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, java.lang.String)
	 */
	@Override
	public String getLabel(IPeerNode peerNode, String entry) {
	    return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate#getImage(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, java.lang.String)
	 */
	@Override
	public Image getImage(IPeerNode peerNode, String entry) {
	    return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate#getDescription(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, java.lang.String)
	 */
	@Override
	public String getDescription(IPeerNode peerNode, String entry) {
	    return getLabel(peerNode, entry);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate#validate(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, java.lang.String)
	 */
	@Override
	public boolean validate(IPeerNode peerNode, String entry) {
	    return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate#execute(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, java.lang.String, boolean)
	 */
	@Override
	public String execute(IPeerNode peerNode, String entry, boolean showDialog) {
		return null;
	}

	protected String getStepGroupId(String entry) {
		IPropertiesContainer data = DataHelper.decodePropertiesContainer(entry);
		return data.getStringProperty(IStepAttributes.ATTR_STEP_GROUP_ID);
	}
}
