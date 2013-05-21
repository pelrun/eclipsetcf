/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.editor.pages;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionManager;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.runtime.interfaces.IDisposable;
import org.eclipse.tcf.te.ui.forms.CustomFormToolkit;
import org.eclipse.tcf.te.ui.forms.FormLayoutFactory;
import org.eclipse.tcf.te.ui.views.activator.UIPlugin;
import org.eclipse.tcf.te.ui.views.interfaces.ImageConsts;
import org.eclipse.tcf.te.ui.views.nls.Messages;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IMenuService;

/**
 * Abstract details editor page implementation managing
 *                  an custom form toolkit instance.
 */
public abstract class AbstractCustomFormToolkitEditorPage extends AbstractEditorPage {
	// Reference to the form toolkit instance
	private CustomFormToolkit toolkit = null;
	// Reference to the toolbar manager to release menu contributions for
	private IToolBarManager manager = null;

	// The default help action class definition
	static protected class HelpAction extends Action {
		/* default */ final String helpID;

		/**
		 * Constructor.
		 *
		 * @param helpID The context help id. Must not be <code>null</code>.
		 */
		public HelpAction(String helpID) {
			super(Messages.AbstractCustomFormToolkitEditorPage_HelpAction_label, IAction.AS_PUSH_BUTTON);
			Assert.isNotNull(helpID);
			this.helpID = helpID;
			setToolTipText(Messages.AbstractCustomFormToolkitEditorPage_HelpAction_tooltip);
			setImageDescriptor(UIPlugin.getImageDescriptor(ImageConsts.HELP));
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.action.Action#run()
		 */
		@Override
		public void run() {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					PlatformUI.getWorkbench().getHelpSystem().displayHelp(helpID);
				}
			});
		}
	}

	// The default apply changes action class definition
	static protected class ApplyAction extends Action implements IPropertyListener, IDisposable {
		private final IEditorPart part;

		/**
         * Constructor
         */
        public ApplyAction(IEditorPart part) {
        	super(Messages.AbstractCustomFormToolkitEditorPage_ApplyAction_label, IAction.AS_PUSH_BUTTON);
        	Assert.isNotNull(part);
        	this.part = part;
        	setToolTipText(Messages.AbstractCustomFormToolkitEditorPage_ApplyAction_tooltip);
        	setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT));
        	setDisabledImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT_DISABLED));

        	part.addPropertyListener(this);
        }

        /* (non-Javadoc)
         * @see org.eclipse.tcf.te.runtime.interfaces.IDisposable#dispose()
         */
        @Override
        public void dispose() {
        	if (part != null) part.dispose();
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.action.Action#run()
         */
        @Override
        public void run() {
        	if (part != null && part.isDirty()) {
        		part.doSave(new NullProgressMonitor());
        	}
        }

        /* (non-Javadoc)
         * @see org.eclipse.ui.IPropertyListener#propertyChanged(java.lang.Object, int)
         */
        @Override
        public void propertyChanged(Object source, int propId) {
			if (propId == IEditorPart.PROP_DIRTY) {
				boolean dirty = part != null && part.isDirty();
				setEnabled(dirty);
			}
        }
	}

	/**
	 * Returns the custom form toolkit instance.
	 *
	 * @return The custom form toolkit instance or <code>null</code>.
	 */
	protected final CustomFormToolkit getFormToolkit() {
		return toolkit;
	}

	/**
	 * Sets the custom form toolkit instance.
	 *
	 * @param toolkit The custom form toolkit instance or <code>null</code>.
	 */
	protected final void setFormToolkit(CustomFormToolkit toolkit) {
		this.toolkit = toolkit;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormPage#dispose()
	 */
	@Override
	public void dispose() {
		// Get the menu service and release the toolbar manager
		if (manager instanceof ContributionManager) {
			IMenuService service = (IMenuService) getSite().getService(IMenuService.class);
			if (service != null) {
				service.releaseContributions((ContributionManager)manager);
			}
		}
		// Dispose the custom form toolkit
		if (toolkit != null) { toolkit.dispose(); toolkit = null; }
		// Dispose all the rest
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);

		Assert.isNotNull(managedForm);

		// Create the toolkit instance
		toolkit = new CustomFormToolkit(managedForm.getToolkit());

		// Configure the managed form
		configureManagedForm(managedForm);

		// Do create the content of the form now
		doCreateFormContent(managedForm.getForm().getBody(), getFormToolkit());

		// Re-arrange the controls
		managedForm.reflow(true);
	}

	/**
	 * Configure the managed form to be ready for usage.
	 *
	 * @param managedForm The managed form. Must not be <code>null</code>.
	 */
	protected void configureManagedForm(IManagedForm managedForm) {
		Assert.isNotNull(managedForm);

		// Configure main layout
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(false, 1));

		// Set context help id
		if (getContextHelpId() != null) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(managedForm.getForm(), getContextHelpId());
		}

		// Decorate the form header
		getFormToolkit().getFormToolkit().decorateFormHeading(managedForm.getForm().getForm());
		// And set the header text and image
		if (getFormTitle() != null) managedForm.getForm().getForm().setText(getFormTitle());
		managedForm.getForm().getForm().setImage(getFormImage());

		// Add the toolbar items which will appear in the form header
		manager = managedForm.getForm().getForm().getToolBarManager();
		// Add the default "additions" separator
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		// Create fixed toolbar contribution items
		createToolbarContributionItems(manager);
		// Get the menu service and populate contributed toolbar actions
		IMenuService service = (IMenuService) getSite().getService(IMenuService.class);
		if (service != null && manager instanceof ContributionManager) {
			service.populateContributionManager((ContributionManager)manager, "toolbar:" + getId()); //$NON-NLS-1$
		}
		// Trigger an update of the toolbar widget
		manager.update(true);
	}

	/**
	 * Returns the context help id to associate with the page form.
	 *
	 * @return The context help id.
	 */
	protected String getContextHelpId() {
		return null;
	}

	/**
	 * Returns the form title to set to the top form header.
	 *
	 * @return The form title.
	 */
	protected String getFormTitle() {
		return null;
	}

	/**
	 * Returns the image to be set to the top form header.
	 *
	 * @return The image or <code>null</code> to use no image.
	 */
	protected Image getFormImage() {
		return null;
	}

	/**
	 * Create the toolbar contribution items.
	 *
	 * @param manager The toolbar manager. Must not be <code>null</code>.
	 */
	protected void createToolbarContributionItems(IToolBarManager manager) {
		Assert.isNotNull(manager);

		manager.add(new Separator("group.connect")); //$NON-NLS-1$
		manager.add(new Separator("group.launch")); //$NON-NLS-1$
		manager.add(new GroupMarker("group.launch.rundebug")); //$NON-NLS-1$
		manager.add(new GroupMarker("group.launch.modes")); //$NON-NLS-1$
		manager.add(new GroupMarker("group.launch.additions")); //$NON-NLS-1$

		manager.add(new Separator("group.showIn")); //$NON-NLS-1$
		if (hasShowInSystemManagementAction()) {
			manager.add(new CommandContributionItem(new CommandContributionItemParameter(PlatformUI.getWorkbench(),
                        "org.eclipse.tcf.te.ui.views.command.showIn.systemManagement", //$NON-NLS-1$
                        "org.eclipse.tcf.te.ui.command.showIn.systemManagement", //$NON-NLS-1$
						  CommandContributionItem.STYLE_PUSH)));
		}

		manager.add(new Separator("group.save")); //$NON-NLS-1$
		// If the page should have an apply button, add one to the toolbar
		if (hasApplyAction()) {
			Action applyAction = doCreateApplyAction(getEditor());
			if (applyAction != null) manager.add(applyAction);
		}

		manager.add(new Separator("group.help")); //$NON-NLS-1$
		// If the page is associated with a context help id, add a default
		// help action button into the toolbar
		if (getContextHelpId() != null) {
			Action helpAction = doCreateHelpAction(getContextHelpId());
			if (helpAction != null) manager.add(helpAction);
		}
	}

	/**
	 * Create the help action.
	 *
	 * @param contextHelpId The context help id. Must not be <code>null</code>.
	 * @return The help action or <code>null</code>.
	 */
	protected Action doCreateHelpAction(String contextHelpId) {
		Assert.isNotNull(contextHelpId);
		return new HelpAction(contextHelpId);
	}

	/**
	 * Creates the apply action.
	 *
	 * @param part The editor part. Must not be <code>null</code>.
	 * @return The apply action or <code>null</code>.
	 */
	protected Action doCreateApplyAction(IEditorPart part) {
		Assert.isNotNull(part);
		return new ApplyAction(part);
	}

	/**
	 * Returns if or if not the page should have an
	 * ShowInSystemManagementView button in the toolbar.
	 * <p>
	 * The default implementation returns <code>true</code>.
	 *
	 * @return <code>True</code> if the page does have an ShowInSystemManagementView button, <code>false</code> otherwise.
	 */
	protected boolean hasShowInSystemManagementAction() {
		return true;
	}

	/**
	 * Returns if or if not the page should have an apply button in
	 * the toolbar.
	 * <p>
	 * The default implementation returns <code>false</code>.
	 *
	 * @return <code>True</code> if the page does have an apply button, <code>false</code> otherwise.
	 */
	protected boolean hasApplyAction() {
		return false;
	}

	/**
	 * Do create the managed form content.
	 *
	 * @param parent The parent composite. Must not be <code>null</code>
	 * @param toolkit The {@link CustomFormToolkit} instance. Must not be <code>null</code>.
	 */
	protected abstract void doCreateFormContent(Composite parent, CustomFormToolkit toolkit);
}
