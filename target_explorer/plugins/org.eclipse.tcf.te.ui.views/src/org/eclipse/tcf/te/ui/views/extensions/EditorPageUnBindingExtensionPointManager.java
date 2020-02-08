/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.extensions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.te.runtime.extensions.AbstractExtensionPointManager;
import org.eclipse.tcf.te.runtime.extensions.ExecutableExtensionProxy;
import org.eclipse.tcf.te.ui.views.activator.UIPlugin;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;


/**
 * Editor page unbinding extension point manager implementation.
 */
public class EditorPageUnBindingExtensionPointManager extends AbstractExtensionPointManager<EditorPageUnBinding> {

	/*
	 * Thread save singleton instance creation.
	 */
	private static class LazyInstance {
		public static EditorPageUnBindingExtensionPointManager instance = new EditorPageUnBindingExtensionPointManager();
	}

	/**
	 * Constructor.
	 */
	EditorPageUnBindingExtensionPointManager() {
		super();
	}

	/**
	 * Returns the singleton instance of the extension point manager.
	 */
	public static EditorPageUnBindingExtensionPointManager getInstance() {
		return LazyInstance.instance;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.extensions.AbstractExtensionPointManager#getExtensionPointId()
	 */
	@Override
	protected String getExtensionPointId() {
		return "org.eclipse.tcf.te.ui.views.editorPageBindings"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.extensions.AbstractExtensionPointManager#getConfigurationElementName()
	 */
	@Override
	protected String getConfigurationElementName() {
		return "editorPageUnBinding"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.extensions.AbstractExtensionPointManager#doCreateExtensionProxy(org.eclipse.core.runtime.IConfigurationElement)
	 */
	@Override
	protected ExecutableExtensionProxy<EditorPageUnBinding> doCreateExtensionProxy(IConfigurationElement element) throws CoreException {
		return new ExecutableExtensionProxy<EditorPageUnBinding>(element) {
			/* (non-Javadoc)
			 * @see org.eclipse.tcf.te.runtime.extensions.ExecutableExtensionProxy#newInstance()
			 */
			@Override
			public EditorPageUnBinding newInstance() {
				EditorPageUnBinding instance = new EditorPageUnBinding();
				try {
					instance.setInitializationData(getConfigurationElement(), null, null);
				} catch (CoreException e) {
					IStatus status = new Status(IStatus.ERROR, UIPlugin.getUniqueIdentifier(),
							e.getLocalizedMessage(), e);
					UIPlugin.getDefault().getLog().log(status);
				}
				return instance;
			}
		};
	}

	/**
	 * Returns the applicable editor page unbindings for the given data source model node..
	 *
	 * @param input The active editor input or <code>null</code>.
	 * @return The list of applicable editor page unbindings or an empty array.
	 */
	public EditorPageUnBinding[] getApplicableEditorPageUnBindings(IEditorInput input) {
		List<EditorPageUnBinding> applicable = new ArrayList<EditorPageUnBinding>();

		for (EditorPageUnBinding binding : getEditorPageUnBindings()) {
			Expression enablement = binding.getEnablement();

			// The page binding is applicable by default if no expression
			// is specified.
			boolean isApplicable = enablement == null;

			if (enablement != null && input != null) {
				// Extract the node from the editor input
				Object node = input.getAdapter(Object.class);
				if (node != null) {
					// Set the default variable to the data source model node instance.
					IEvaluationContext currentState = ((IHandlerService)PlatformUI.getWorkbench().getService(IHandlerService.class)).getCurrentState();
					EvaluationContext context = new EvaluationContext(currentState, node);
					// Set the "activeEditorInput" variable to the data source model node instance.
					context.addVariable(ISources.ACTIVE_EDITOR_INPUT_NAME, node);
					// Allow plugin activation
					context.setAllowPluginActivation(true);
					// Evaluate the expression
					try {
						isApplicable = enablement.evaluate(context).equals(EvaluationResult.TRUE);
					} catch (CoreException e) {
						IStatus status = new Status(IStatus.ERROR, UIPlugin.getUniqueIdentifier(), e.getLocalizedMessage(), e);
						UIPlugin.getDefault().getLog().log(status);
					}
				} else {
					// The enablement is false by definition if we cannot
					// determine the data source model node.
					isApplicable = false;
				}
			}

			// Add the page if applicable
			if (isApplicable) applicable.add(binding);
		}

		return applicable.toArray(new EditorPageUnBinding[applicable.size()]);
	}

	/**
	 * Returns the list of all contributed editor page unbindings.
	 *
	 * @return The list of contributed editor page unbindings, or an empty array.
	 */
	public EditorPageUnBinding[] getEditorPageUnBindings() {
		List<EditorPageUnBinding> contributions = new ArrayList<EditorPageUnBinding>();
		Collection<ExecutableExtensionProxy<EditorPageUnBinding>> editorPageUnBindings = getExtensions().values();
		for (ExecutableExtensionProxy<EditorPageUnBinding> editorPageUnBinding : editorPageUnBindings) {
			EditorPageUnBinding instance = editorPageUnBinding.getInstance();
			if (instance != null && !contributions.contains(instance)) {
				contributions.add(instance);
			}
		}

		return contributions.toArray(new EditorPageUnBinding[contributions.size()]);
	}
}
