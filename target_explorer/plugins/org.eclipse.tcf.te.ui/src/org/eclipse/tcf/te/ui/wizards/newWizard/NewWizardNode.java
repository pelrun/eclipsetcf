/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.wizards.newWizard;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.swt.graphics.Point;
import org.eclipse.tcf.te.ui.activator.UIPlugin;
import org.eclipse.tcf.te.ui.nls.Messages;
import org.eclipse.tcf.te.ui.wizards.interfaces.INewTargetWizard;
import org.eclipse.ui.IPluginContribution;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.wizards.IWizardDescriptor;

/**
 * New wizard node implementation.
 */
/* default */ class NewWizardNode implements IWizardNode, IPluginContribution {
	// The associated wizard descriptor
	private final IWizardDescriptor descriptor;
	// The parent wizard selection page
	private final NewWizardSelectionPage page;

	// The wizard instance, once created
	private IWizard wizard;

	/**
	 * Constructor.
	 *
	 * @param page The parent wizard selection page. Must not be <code>null</code>.
	 * @param descriptor The wizard descriptor. Must not be <code>null</code>.
	 */
	public NewWizardNode(NewWizardSelectionPage page, IWizardDescriptor descriptor) {
		Assert.isNotNull(page);
		Assert.isNotNull(descriptor);

		this.page = page;
		this.descriptor = descriptor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizardNode#dispose()
	 */
	@Override
	public void dispose() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizardNode#getExtent()
	 */
	@Override
	public Point getExtent() {
        return new Point(-1, -1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPluginContribution#getLocalId()
	 */
	@Override
	public String getLocalId() {
    	IPluginContribution contribution = getAdapter(descriptor, IPluginContribution.class);
		if (contribution != null) {
			return contribution.getLocalId();
		}
		return descriptor.getId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPluginContribution#getPluginId()
	 */
	@Override
	public String getPluginId() {
       	IPluginContribution contribution = getAdapter(descriptor, IPluginContribution.class);
		if (contribution != null) {
			return contribution.getPluginId();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizardNode#isContentCreated()
	 */
	@Override
	public boolean isContentCreated() {
		return wizard != null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizardNode#getWizard()
	 */
	@Override
	public IWizard getWizard() {
		if (wizard == null) {
			wizard = createWizard(descriptor);
		}
		return wizard;
	}

	/**
	 * Create the wizard associated with the specified wizard descriptor.
	 *
	 * @param descriptor The wizard descriptor. Must not be <code>null</code>.
	 * @return The wizard or <code>null</code> if the creation fails.
	 */
	private final IWorkbenchWizard createWizard(IWizardDescriptor descriptor) {
		Assert.isNotNull(descriptor);

		IWorkbenchWizard wizard = null;

		try {
			// Create the wizard instance
			wizard = descriptor.createWizard();

			// If the wizard is a INewTargetWizard, associate the wizard descriptor
			if (wizard instanceof INewTargetWizard) ((INewTargetWizard)wizard).setWizardDescriptor(descriptor);

			// Initialize the wizard
			IStructuredSelection wizardSelection = descriptor.adaptedSelection(page.getSelection());
			wizard.init(page.getWorkbench(), wizardSelection);
		} catch (CoreException e) {
			page.setErrorMessage(Messages.NewWizardSelectionPage_createWizardFailed);

			IStatus status = new Status(IStatus.ERROR, UIPlugin.getUniqueIdentifier(),
										Messages.NewWizardSelectionPage_createWizardFailed, e);
			UIPlugin.getDefault().getLog().log(status);
		}

		return wizard;
	}

	/**
	 * Bug# 480444: NoSuchMethodError with Neon M3
	 *
	 * Instead of referring to the internal <code>org.eclipse.ui.internal.util.Util</code> class,
	 * copy the <code>getAdapter</code> method here, so it can work with every platform.
	 */
	@SuppressWarnings("cast")
    private final <T> T getAdapter(Object sourceObject, Class<T> adapterType) {
    	Assert.isNotNull(adapterType);
        if (sourceObject == null) {
            return null;
        }
        if (adapterType.isInstance(sourceObject)) {
			return adapterType.cast(sourceObject);
        }

        if (sourceObject instanceof IAdaptable) {
            IAdaptable adaptable = (IAdaptable) sourceObject;

			T result = (T) adaptable.getAdapter(adapterType);
            if (result != null) {
                // Sanity-check
                Assert.isTrue(adapterType.isInstance(result));
                return result;
            }
        }

        if (!(sourceObject instanceof PlatformObject)) {
			T result = (T) Platform.getAdapterManager().getAdapter(sourceObject, adapterType);
            if (result != null) {
                return result;
            }
        }

        return null;
    }
}