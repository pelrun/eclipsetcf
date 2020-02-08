/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.launch.core.lm.delegates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate;
import org.eclipse.tcf.te.launch.core.lm.delegates.DefaultLaunchManagerDelegate;
import org.eclipse.tcf.te.launch.core.lm.interfaces.ILaunchContextLaunchAttributes;
import org.eclipse.tcf.te.launch.core.lm.interfaces.ILaunchSpecification;
import org.eclipse.tcf.te.launch.core.persistence.DefaultPersistenceDelegate;
import org.eclipse.tcf.te.launch.core.persistence.launchcontext.LaunchContextsPersistenceDelegate;
import org.eclipse.tcf.te.launch.core.selection.interfaces.IRemoteSelectionContext;
import org.eclipse.tcf.te.launch.core.selection.interfaces.ISelectionContext;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.runtime.services.ServiceUtils;
import org.eclipse.tcf.te.runtime.services.interfaces.delegates.ILabelProviderDelegate;
import org.eclipse.tcf.te.tcf.launch.core.interfaces.steps.ITcfLaunchStepAttributes;

/**
 * RemoteAppLaunchManagerDelegate
 */
public class AttachLaunchManagerDelegate extends DefaultLaunchManagerDelegate {

	// mandatory attributes for attach launch configurations
	private static final String[] MANDATORY_CONFIG_ATTRIBUTES = new String[] {
		ILaunchContextLaunchAttributes.ATTR_LAUNCH_CONTEXTS
	};

	/**
	 * Constructor.
	 */
	public AttachLaunchManagerDelegate() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.core.lm.delegates.DefaultLaunchManagerDelegate#updateLaunchConfigAttributes(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy, org.eclipse.tcf.te.launch.core.lm.interfaces.ILaunchSpecification)
	 */
	@Override
	public void updateLaunchConfigAttributes(ILaunchConfigurationWorkingCopy wc, ILaunchSpecification launchSpec) {
		super.updateLaunchConfigAttributes(wc, launchSpec);

		DefaultPersistenceDelegate.setAttribute(wc, ITcfLaunchStepAttributes.ATTR_ATTACH_SERVICES, (List<String>)null);
		DefaultPersistenceDelegate.setAttribute(wc, TCFLaunchDelegate.ATTR_DISCONNECT_ON_CTX_EXIT, false);

		copySpecToConfig(launchSpec, wc);

		wc.rename(getDefaultLaunchName(wc));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.core.lm.delegates.DefaultLaunchManagerDelegate#initLaunchConfigAttributes(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy, org.eclipse.tcf.te.launch.core.lm.interfaces.ILaunchSpecification)
	 */
	@Override
	public void initLaunchConfigAttributes(ILaunchConfigurationWorkingCopy wc, ILaunchSpecification launchSpec) {
		super.initLaunchConfigAttributes(wc, launchSpec);

		DefaultPersistenceDelegate.setAttribute(wc, ITcfLaunchStepAttributes.ATTR_ATTACH_SERVICES, (List<String>)null);
		DefaultPersistenceDelegate.setAttribute(wc, TCFLaunchDelegate.ATTR_DISCONNECT_ON_CTX_EXIT, false);

		copySpecToConfig(launchSpec, wc);

		wc.rename(getDefaultLaunchName(wc));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.core.lm.delegates.DefaultLaunchManagerDelegate#updateLaunchConfig(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy, org.eclipse.tcf.te.launch.core.selection.interfaces.ISelectionContext, boolean)
	 */
	@Override
	public void updateLaunchConfig(ILaunchConfigurationWorkingCopy wc, ISelectionContext selContext, boolean replace) {
		super.updateLaunchConfig(wc, selContext, replace);

		if (selContext instanceof IRemoteSelectionContext) {
			IRemoteSelectionContext remoteCtx = (IRemoteSelectionContext)selContext;
			LaunchContextsPersistenceDelegate.setLaunchContexts(wc, new IModelNode[]{remoteCtx.getRemoteCtx()});
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.core.lm.delegates.DefaultLaunchManagerDelegate#addLaunchSpecAttributes(org.eclipse.tcf.te.launch.core.lm.interfaces.ILaunchSpecification, java.lang.String, org.eclipse.tcf.te.launch.core.selection.interfaces.ISelectionContext)
	 */
	@Override
	protected ILaunchSpecification addLaunchSpecAttributes(ILaunchSpecification launchSpec, String launchConfigTypeId, ISelectionContext selectionContext) {
		launchSpec = super.addLaunchSpecAttributes(launchSpec, launchConfigTypeId, selectionContext);

		if (selectionContext instanceof IRemoteSelectionContext) {
			List<IModelNode> launchContexts = new ArrayList<IModelNode>(Arrays.asList(LaunchContextsPersistenceDelegate.getLaunchContexts(launchSpec)));
			IModelNode remoteCtx = ((IRemoteSelectionContext)selectionContext).getRemoteCtx();
			if (!launchContexts.contains(remoteCtx)) {
				launchContexts.add(remoteCtx);
				LaunchContextsPersistenceDelegate.setLaunchContexts(launchSpec, launchContexts.toArray(new IModelNode[launchContexts.size()]));
			}

			launchSpec.setLaunchConfigName(getDefaultLaunchName(launchSpec));
		}

		return launchSpec;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.core.lm.delegates.DefaultLaunchManagerDelegate#getDefaultLaunchName(org.eclipse.tcf.te.launch.core.lm.interfaces.ILaunchSpecification)
	 */
	@Override
	public String getDefaultLaunchName(ILaunchSpecification launchSpec) {
		IModelNode[] contexts = LaunchContextsPersistenceDelegate.getLaunchContexts(launchSpec);
		String name = getDefaultLaunchName((contexts != null && contexts.length > 0 ? contexts[0] : null));
		return name.trim().length() > 0 ? name.trim() : super.getDefaultLaunchName(launchSpec);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.core.lm.delegates.DefaultLaunchManagerDelegate#getDefaultLaunchName(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public String getDefaultLaunchName(ILaunchConfiguration launchConfig) {
		IModelNode[] contexts = LaunchContextsPersistenceDelegate.getLaunchContexts(launchConfig);
		String name = getDefaultLaunchName((contexts != null && contexts.length > 0 ? contexts[0] : null));
		return name.trim().length() > 0 ? name.trim() : super.getDefaultLaunchName(launchConfig);
	}

	private String getDefaultLaunchName(IModelNode context) {
		if (context != null) {
			ILabelProviderDelegate delegate = ServiceUtils.getUIServiceDelegate(context, context, ILabelProviderDelegate.class);
			String name = delegate != null ? delegate.getText(context) : null;
			return name != null ? name : context.getName();
		}
		return ""; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.core.lm.delegates.DefaultLaunchManagerDelegate#getMandatoryAttributes()
	 */
	@Override
	protected List<String> getMandatoryAttributes() {
		return Arrays.asList(MANDATORY_CONFIG_ATTRIBUTES);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.core.lm.delegates.DefaultLaunchManagerDelegate#getNumAttributes()
	 */
	@Override
	protected int getNumAttributes() {
		return 1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.core.lm.delegates.DefaultLaunchManagerDelegate#getAttributeRanking(java.lang.String)
	 */
	@Override
	protected int getAttributeRanking(String attributeKey) {
		if (ILaunchContextLaunchAttributes.ATTR_LAUNCH_CONTEXTS.equals(attributeKey)) {
			return getNumAttributes() * 2;
		}
		return 1;
	}
}
