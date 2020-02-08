/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.launchcontext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.launch.core.bindings.LaunchConfigTypeBindingsManager;
import org.eclipse.tcf.te.launch.core.lm.interfaces.ILaunchContextLaunchAttributes;
import org.eclipse.tcf.te.launch.core.persistence.launchcontext.LaunchContextsPersistenceDelegate;
import org.eclipse.tcf.te.launch.core.selection.RemoteSelectionContext;
import org.eclipse.tcf.te.launch.ui.interfaces.ILaunchConfigurationTabFormPart;
import org.eclipse.tcf.te.launch.ui.tabs.launchcontext.AbstractContextSelectorTab;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.ui.controls.ContextSelectorSectionControl;
import org.eclipse.tcf.te.tcf.ui.sections.AbstractContextSelectorSection;
import org.eclipse.tcf.te.ui.views.controls.AbstractContextSelectorControl;
import org.eclipse.ui.forms.IManagedForm;

/**
 * Launch context selection main launch tab implementation.
 */
public abstract class AbstractLaunchContextMainTab extends AbstractContextSelectorTab {
	/* default */ ILaunchConfiguration configuration = null;

	/**
	 * Context selector control filter filtering remote contexts which are not
	 * enabled for the launch configuration type.
	 */
	protected class MainTabContextSelectorViewerFilter extends ViewerFilter {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof IPeerNode) {
				String typeId = null;
				if (configuration != null) {
					try {
						typeId = configuration.getType().getIdentifier();
					}
					catch (CoreException e) { /* ignored on purpose */ }
				}

				String mode = getLaunchConfigurationDialog().getMode();

				if (typeId != null && mode != null) {
					return LaunchConfigTypeBindingsManager.getInstance().isValidLaunchConfigType(typeId, mode, new RemoteSelectionContext((IPeerNode)element, true));
				}
			}
			return true;
		}
	}

	/**
	 * Launch configuration main tab context selector control implementation.
	 */
	protected class MainTabContextSelectorControl extends ContextSelectorSectionControl {

		/**
		 * Constructor.
		 *
		 * @param section The parent context selector section. Must not be <code>null</code>.
		 * @param parentPage The parent target connection page this control is embedded in. Might be
		 *            <code>null</code> if the control is not associated with a page.
		 */
		public MainTabContextSelectorControl(MainTabContextSelectorSection section, IDialogPage parentPage) {
			super(section, parentPage);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.launch.ui.tabs.launchcontext.AbstractContextSelectorControl#doCreateViewerFilters()
		 */
		@Override
		protected ViewerFilter[] doCreateViewerFilters() {
			List<ViewerFilter> filters = new ArrayList<ViewerFilter>(Arrays.asList(super.doCreateViewerFilters()));
			filters.add(new MainTabContextSelectorViewerFilter());
			return filters.toArray(new ViewerFilter[filters.size()]);
		}
	}

	protected class MainTabContextSelectorSection extends AbstractContextSelectorSection implements ILaunchConfigurationTabFormPart {

		/**
		 * Constructor.
		 * @param form The managed form.
		 * @param parent The parent composite.
		 */
		public MainTabContextSelectorSection(IManagedForm form, Composite parent) {
			super(form, parent);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.tcf.launch.ui.launchcontext.MainTabContextSelectorSection#doCreateContextSelector()
		 */
		@Override
		protected AbstractContextSelectorControl doCreateContextSelector() {
			AbstractContextSelectorControl control = new MainTabContextSelectorControl(this, null);
			return control;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.tcf.ui.sections.AbstractContextSelectorSection#getContextListDataKey()
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

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.tcf.ui.sections.AbstractContextSelectorSection#dataChanged()
		 */
		@Override
		public void dataChanged() {
		    getManagedForm().dirtyStateChanged();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.views.sections.AbstractContextSelectorSection#doConfigureContextSelector(org.eclipse.tcf.te.ui.views.controls.AbstractContextSelectorControl)
		 */
		@Override
		protected void doConfigureContextSelector(AbstractContextSelectorControl contextSelector) {
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.tabs.AbstractFormsLaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		super.initializeFrom(configuration);
		this.configuration = configuration;

		if (getContextSelectorSection() != null) {
			AbstractContextSelectorControl control = (AbstractContextSelectorControl)getContextSelectorSection().getAdapter(AbstractContextSelectorControl.class);
			if (control != null && control.getViewer() != null) {
				control.getViewer().refresh();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.tabs.launchcontext.AbstractContextSelectorTab#doCreateContextSelectorSection(org.eclipse.ui.forms.IManagedForm, org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected MainTabContextSelectorSection doCreateContextSelectorSection(IManagedForm form, Composite panel) {
		return new MainTabContextSelectorSection(form, panel);
	}
}
