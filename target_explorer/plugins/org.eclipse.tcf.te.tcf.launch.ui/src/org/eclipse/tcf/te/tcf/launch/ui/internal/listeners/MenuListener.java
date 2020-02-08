/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.internal.listeners;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener2;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.te.launch.core.persistence.launchcontext.LaunchContextsPersistenceDelegate;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.tcf.launch.core.interfaces.ILaunchTypes;
import org.eclipse.tcf.te.tcf.launch.ui.activator.UIPlugin;
import org.eclipse.tcf.te.ui.views.ViewsUtil;
import org.eclipse.tcf.te.ui.views.editor.EditorInput;
import org.eclipse.tcf.te.ui.views.extensions.EditorPageBindingExtensionPointManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * Menu listener implementation.
 */
@SuppressWarnings("restriction")
public class MenuListener implements IMenuListener2 {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	public void menuAboutToShow(IMenuManager manager) {
		if (!(manager instanceof MenuManager)) return;

		MenuManager m = (MenuManager)manager;
		IContributionItem editLaunchItem = getEditLaunchContributionItem();
		if (editLaunchItem != null) {
			int index = -1;
			IContributionItem[] items = m.getItems();
			for (int i = 0; i < items.length; i++) {
				IContributionItem item = items[i];
				if (item instanceof ActionContributionItem) {
					IAction action = ((ActionContributionItem)item).getAction();
					if (action.getClass().getSimpleName().equals("EditLaunchConfigurationAction")) { //$NON-NLS-1$
						index = i;
						m.remove(item);
						break;
					}
				}
			}
			if (index != -1) m.insert(index, editLaunchItem);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuListener2#menuAboutToHide(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	public void menuAboutToHide(IMenuManager manager) {
	}

	/**
	 * Get the edit launch configuration contribution item.
	 *
	 * @return The edit launch configuration contribution item or <code>null</code>.
	 */
	protected IContributionItem getEditLaunchContributionItem() {
		IContributionItem item = null;
		final IHandlerService service = (IHandlerService)PlatformUI.getWorkbench().getService(IHandlerService.class);

		// Get the current selection
		if (service != null) {
			IEvaluationContext state = service.getCurrentState();
			ISelection selection = (ISelection)state.getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
			if (selection instanceof IStructuredSelection && ((IStructuredSelection)selection).size() == 1) {
				// Get the selected element
				Object object = ((IStructuredSelection)selection).getFirstElement();

				// Determine the launch
				ILaunch launch = null;
				if (object instanceof IAdaptable) {
					launch = (ILaunch)((IAdaptable)object).getAdapter(ILaunch.class);
				}

				if (launch == null) {
					if (object instanceof ILaunch) {
						launch = (ILaunch)object;
					} else if (object instanceof IDebugElement) {
						launch = ((IDebugElement)object).getLaunch();
					} else if (object instanceof IProcess) {
						launch = ((IProcess)object).getLaunch();
					}
				}

				try {
					// Get the launch configuration of the launch
					ILaunchConfiguration lc = launch != null ? launch.getLaunchConfiguration() : null;
					if (lc != null) {
						// Get the launch configuration simulator
						ILaunchConfigurationType type = lc.getType();

						if (ILaunchTypes.ATTACH.equals(type.getIdentifier())) {
							IModelNode[] contexts = LaunchContextsPersistenceDelegate.getLaunchContexts(lc);
							if (contexts.length == 1) {
								final IModelNode context = contexts[0];

								// Create a fake editor input object
								IEditorInput input = new EditorInput(context);

								// Determine if we should use the configuration editor
								boolean useEditor = EditorPageBindingExtensionPointManager.getInstance().getApplicableEditorPageBindings(input).length > 0;

								if (useEditor) {
									IAction action = new Action() {
										@Override
										public void run() {
											ViewsUtil.openEditor(new StructuredSelection(context));
										}
									};
									action.setText(NLS.bind(ActionMessages.EditLaunchConfigurationAction_1, lc.getName()));
									action.setImageDescriptor(DebugPluginImages.getImageDescriptor(lc.getType().getIdentifier()));
									item = new ActionContributionItem(action);
								}
							}
						}
					}

				} catch (CoreException e) {
					if (Platform.inDebugMode()) UIPlugin.getDefault().getLog().log(e.getStatus());
				}
			}
		}

		return item;
	}
}
