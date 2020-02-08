/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.sections;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.ui.forms.parts.AbstractSection;
import org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode;
import org.eclipse.tcf.te.ui.views.activator.UIPlugin;
import org.eclipse.tcf.te.ui.views.controls.AbstractContextSelectorControl;
import org.eclipse.tcf.te.ui.views.interfaces.ImageConsts;
import org.eclipse.tcf.te.ui.views.nls.Messages;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Context selector section implementation.
 */
public abstract class AbstractContextSelectorSection extends AbstractSection implements IDataExchangeNode {

	// Reference to the section sub controls
	protected AbstractContextSelectorControl selector;

	/**
	 * Context selector control refresh action implementation.
	 */
	protected class RefreshAction extends Action {

		/**
		 * Constructor.
		 */
		public RefreshAction() {
			super(null, IAction.AS_PUSH_BUTTON);
			setImageDescriptor(UIPlugin.getImageDescriptor(ImageConsts.ACTION_Refresh_Enabled));
			setToolTipText(Messages.AbstractContextSelectorSection_toolbar_refresh_tooltip);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.action.Action#run()
		 */
		@Override
		public void run() {
			if (selector != null && selector.getViewer() != null) {
				selector.getViewer().refresh();
			}
		}
	}

	protected boolean doShowRefreshAction() {
		return false;
	}

	/**
	 * Constructor.
	 *
	 * @param form The parent managed form. Must not be <code>null</code>.
	 * @param parent The parent composite. Must not be <code>null</code>.
	 * @param
	 */
	public AbstractContextSelectorSection(IManagedForm form, Composite parent, int style) {
		super(form, parent, style);
		getSection().setBackground(parent.getBackground());
		createClient(getSection(), form.getToolkit());
	}

	/**
	 * Constructor.
	 *
	 * @param form The parent managed form. Must not be <code>null</code>.
	 * @param parent The parent composite. Must not be <code>null</code>.
	 */
	public AbstractContextSelectorSection(IManagedForm form, Composite parent) {
		this(form, parent, ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		Assert.isNotNull(section);
		Assert.isNotNull(toolkit);

		// Configure the section
		section.setText(Messages.AbstractContextSelectorSection_title);
		if (section.getParent().getLayout() instanceof GridLayout) {
			section.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

		// Create the section client
		Composite client = createClientContainer(section, 1, toolkit);
		Assert.isNotNull(client);
		section.setClient(client);
		client.setBackground(section.getBackground());

		// Create a toolbar for the section
		createSectionToolbar(section, toolkit);

		// Create the section sub controls
		selector = doCreateContextSelector();
		doConfigureContextSelector(selector);
		selector.setFormToolkit(toolkit);
		selector.setupPanel(client);

		// Mark the control update as completed now
		setIsUpdating(false);
	}

	/**
	 * Create the context selector control.
	 * @return The context selector control.
	 */
	protected abstract AbstractContextSelectorControl doCreateContextSelector();

	protected abstract void doConfigureContextSelector(AbstractContextSelectorControl contextSelector);

	public AbstractContextSelectorControl getSelectorControl() {
		return selector;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (AbstractContextSelectorControl.class.isAssignableFrom(adapter)) {
			return selector;
		}
	    return super.getAdapter(adapter);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	@Override
	public void dispose() {
		if (selector != null) { selector.dispose(); selector = null; }
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#createSectionToolbarItems(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit, org.eclipse.jface.action.ToolBarManager)
	 */
	@Override
	protected void createSectionToolbarItems(Section section, FormToolkit toolkit, ToolBarManager tlbMgr) {
		super.createSectionToolbarItems(section, toolkit, tlbMgr);
		if (doShowRefreshAction()) {
			tlbMgr.add(new RefreshAction());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#isValid()
	 */
	@Override
	public boolean isValid() {
		boolean valid = super.isValid();

		if (valid) {
			valid = selector.isValid();
			if (!valid) {
				setMessage(selector.getMessage(), selector.getMessageType());
			}
		}

		return valid;
	}

	/**
	 * Called to signal that the data associated has been changed.
	 */
	public abstract void dataChanged();
}
