/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.editor.pages;

import org.eclipse.core.runtime.Assert;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionManager;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.tcf.te.ui.forms.CustomFormToolkit;
import org.eclipse.tcf.te.ui.forms.FormLayoutFactory;
import org.eclipse.tcf.te.ui.views.activator.UIPlugin;
import org.eclipse.tcf.te.ui.views.editor.Editor;
import org.eclipse.tcf.te.ui.views.extensions.LabelProviderDelegateExtensionPointManager;
import org.eclipse.tcf.te.ui.views.interfaces.ImageConsts;
import org.eclipse.tcf.te.ui.views.nls.Messages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.menus.IMenuService;

/**
 * Abstract details editor page implementation managing
 *                  an custom form toolkit instance.
 */
public abstract class AbstractCustomFormToolkitEditorPage extends AbstractEditorPage {
	// Reference to the form toolkit instance
	private CustomFormToolkit toolkit = null;
	// Reference to the toolbar toolBarManager to release menu contributions for
	private IToolBarManager toolBarManager = null;
	// Reference to the toolbar MenuManager to release menu contributions for
//	private IMenuManager menuManager = null;

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
					IContext context = HelpSystem.getContext(helpID);
					if (context != null) {
						PlatformUI.getWorkbench().getHelpSystem().displayHelp(context);
					}
					else {
						PlatformUI.getWorkbench().getHelpSystem().displayHelp();
					}
				}
			});
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
		IMenuService service = (IMenuService) getSite().getService(IMenuService.class);
		// Get the menu service and release the toolbar toolBarManager
		if (service != null) {
			if (toolBarManager instanceof ContributionManager) {
				service.releaseContributions((ContributionManager)toolBarManager);
			}
//			if (menuManager instanceof ContributionManager) {
//				service.releaseContributions((ContributionManager)menuManager);
//			}
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

		// Set the header text
		String title = getFormTitle();
		if (title != null) setFormTitle(title);

		// Set the header image
		managedForm.getForm().getForm().setImage(getFormImage());

		// Add the toolbar items which will appear in the form header
		toolBarManager = managedForm.getForm().getForm().getToolBarManager();
		// Add the default "additions" separator
		toolBarManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		// Create fixed toolbar contribution items
		createToolbarContributionItems(toolBarManager);
		// Get the menu service and populate contributed toolbar actions
		IMenuService service = (IMenuService) getSite().getService(IMenuService.class);
		if (service != null && toolBarManager instanceof ContributionManager) {
			service.populateContributionManager((ContributionManager)toolBarManager, "toolbar:" + getId()); //$NON-NLS-1$
		}

//		// Add the menu items which will appear in the form header
//		menuManager = managedForm.getForm().getForm().getMenuManager();
//		// Get the menu service and populate contributed menu actions
//		if (service != null && menuManager instanceof ContributionManager) {
//			service.populateContributionManager((ContributionManager)menuManager, "menu:" + getId()); //$NON-NLS-1$
//		}
//		// Trigger an update of the menu widget
//		menuManager.update(true);

		// Trigger an update of the toolbar widget
		toolBarManager.update(true);
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
	 * @return The form title or <code>null</code>.
	 */
	protected String getFormTitle() {
		return null;
	}

	/**
	 * Returns the form title state decoration.
	 *
	 * @return The form title state decoration or <code>null</code>.
	 */
	public String getFormTitleStateDecoration() {
		return null;
	}

	/**
	 * Returns the image to be set to the top form header.
	 *
	 * @return The image or <code>null</code> to use no image.
	 */
	protected Image getFormImage() {
		Image image = null;
		ILabelProvider[] delegates = LabelProviderDelegateExtensionPointManager.getInstance().getDelegates(getEditorInputNode(), false);
		if (delegates != null && delegates.length > 0) {
			image = delegates[0].getImage(getEditorInputNode());
			if (image != null && delegates[0] instanceof ILabelDecorator) {
				image = ((ILabelDecorator)delegates[0]).decorateImage(image, getEditorInputNode());
			}
		}
		return image;
	}

	/**
	 * Update the form header title.
	 *
	 * @param title The title text. Must not be <code>null</code>:
	 */
	public void setFormTitle(String title) {
		Assert.isNotNull(title);

		String fullTitle = title;
		String titleStateDecoration = getFormTitleStateDecoration();
		if (titleStateDecoration != null) fullTitle += " " + titleStateDecoration; //$NON-NLS-1$

		String oldTitle = getManagedForm().getForm().getText();
		if (!fullTitle.equals(oldTitle)) {
			getManagedForm().getForm().setText(fullTitle);
		}
	}

	/**
	 * Update the form header image.
	 *
	 * @param image The image or <code>null</code>.
	 */
	public void setFormImage(Image image) {
		ScrolledForm form = getManagedForm().getForm();
		if (form.getImage() != image) form.setImage(image);
		if (getEditor() instanceof Editor) {
			Editor editor = (Editor)getEditor();
			if (editor.getTitleImage() != image) editor.setTitleImage(image);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractEditorPage#setActive(boolean)
	 */
//	@Override
//	public void setActive(boolean active) {
//		if (active) setFormTitle(getFormTitle());
//	    super.setActive(active);
//	}

	/**
	 * Create the toolbar contribution items.
	 *
	 * @param toolBarManager The toolbar toolBarManager. Must not be <code>null</code>.
	 */
	protected void createToolbarContributionItems(IToolBarManager manager) {
		Assert.isNotNull(manager);

		manager.add(new Separator("group.connect")); //$NON-NLS-1$
		manager.add(new Separator("group.launch")); //$NON-NLS-1$
		manager.add(new GroupMarker("group.launch.rundebug")); //$NON-NLS-1$
		manager.add(new GroupMarker("group.launch.additions")); //$NON-NLS-1$
		manager.add(new Separator("group.additions")); //$NON-NLS-1$
		manager.add(new Separator("group.additions.control")); //$NON-NLS-1$
		manager.add(new Separator("group.help")); //$NON-NLS-1$
		IContributionItem linkContribution = doCreateLinkContribution(manager);
		if (linkContribution != null) manager.add(linkContribution);
		// If the page is associated with a context help id, add a default
		// help action button into the toolbar
		if (getContextHelpId() != null) {
			Action helpAction = doCreateHelpAction(getContextHelpId());
			if (helpAction != null) manager.add(helpAction);
		}

		final MenuManager mgr = new MenuManager();
		mgr.add(new Separator("group.launch")); //$NON-NLS-1$
		mgr.add(new GroupMarker("group.launch.rundebug")); //$NON-NLS-1$
		mgr.add(new Separator("group.launch.additions")); //$NON-NLS-1$
		mgr.add(new Separator("group.delete")); //$NON-NLS-1$
		mgr.add(new Separator("group.additions")); //$NON-NLS-1$
		final IMenuService service = (IMenuService) getSite().getService(IMenuService.class);
		if (service != null) {
			service.populateContributionManager(mgr, "menu:" + AbstractCustomFormToolkitEditorPage.this.getId()); //$NON-NLS-1$
		}
		if (mgr.getSize() > 5) {
			toolBarManager.add(new ControlContribution("toolbarmenu") { //$NON-NLS-1$
				@Override
				protected Control createControl(Composite parent) {
					final ToolBar tb = new ToolBar(parent, SWT.FLAT);
					final ToolItem item = new ToolItem(tb, SWT.PUSH);
					item.setImage(UIPlugin.getImage(ImageConsts.MENU));
					item.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							MenuManager mgr = new MenuManager();
							mgr.add(new Separator("group.launch")); //$NON-NLS-1$
							mgr.add(new GroupMarker("group.launch.rundebug")); //$NON-NLS-1$
							mgr.add(new Separator("group.launch.additions")); //$NON-NLS-1$
							mgr.add(new Separator("group.delete")); //$NON-NLS-1$
							mgr.add(new Separator("group.additions")); //$NON-NLS-1$
							service.populateContributionManager(mgr, "menu:" + AbstractCustomFormToolkitEditorPage.this.getId()); //$NON-NLS-1$
							Menu menu = mgr.createContextMenu(tb);
							menu.setVisible(true);
						}
					});
					return tb;
				}
			});
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

	protected IContributionItem doCreateLinkContribution(IToolBarManager tbManager) {
		return null;
	}

	/**
	 * Do create the managed form content.
	 *
	 * @param parent The parent composite. Must not be <code>null</code>
	 * @param toolkit The {@link CustomFormToolkit} instance. Must not be <code>null</code>.
	 */
	protected abstract void doCreateFormContent(Composite parent, CustomFormToolkit toolkit);
}
