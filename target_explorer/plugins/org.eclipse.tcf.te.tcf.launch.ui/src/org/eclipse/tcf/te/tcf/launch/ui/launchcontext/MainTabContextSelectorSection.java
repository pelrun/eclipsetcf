/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.launchcontext;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.launch.core.lm.interfaces.ILaunchContextLaunchAttributes;
import org.eclipse.tcf.te.launch.core.persistence.launchcontext.LaunchContextsPersistenceDelegate;
import org.eclipse.tcf.te.launch.ui.interfaces.ILaunchConfigurationTabFormPart;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.tcf.ui.sections.ContextSelectorSection;
import org.eclipse.ui.forms.IManagedForm;

/**
 * Locator model context selector section implementation.
 */
public class MainTabContextSelectorSection extends ContextSelectorSection implements ILaunchConfigurationTabFormPart {

	/**
	 * Constructor.
	 * @param form The managed form.
	 * @param parent The parent composite.
	 */
	public MainTabContextSelectorSection(IManagedForm form, Composite parent) {
		super(form, parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.sections.ContextSelectorSection#getContextListDataKey()
	 */
	@Override
	protected String getContextListDataKey() {
	    return ILaunchContextLaunchAttributes.ATTR_LAUNCH_CONTEXTS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.interfaces.ILaunchConfigurationTabFormPart#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		Assert.isNotNull(configuration);

		if (selector != null) {
			IModelNode[] contexts = LaunchContextsPersistenceDelegate.getLaunchContexts(configuration);
			if (contexts != null && contexts.length > 0) {
				// Loop the contexts and create a list of nodes
				List<IModelNode> nodes = new ArrayList<IModelNode>();
				for (IModelNode node : contexts) {
					if (node != null && !nodes.contains(node)) {
						nodes.add(node);
					}
				}
				if (!nodes.isEmpty()) {
					selector.setCheckedModelContexts(nodes.toArray(new IModelNode[nodes.size()]));
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.interfaces.ILaunchConfigurationTabFormPart#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		Assert.isNotNull(configuration);

		if (selector != null) {
			IModelNode[] nodes = selector.getCheckedModelContexts();

			// Write the selected contexts to the launch configuration
			if (nodes != null && nodes.length > 0) {
				LaunchContextsPersistenceDelegate.setLaunchContexts(configuration, nodes);
			} else {
				LaunchContextsPersistenceDelegate.setLaunchContexts(configuration, null);
			}
		} else {
			LaunchContextsPersistenceDelegate.setLaunchContexts(configuration, null);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.interfaces.ILaunchConfigurationTabFormPart#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public boolean isValid(ILaunchConfiguration configuration) {
		return isValid();
	}
}
