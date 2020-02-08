/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.tabbed;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IPropertyChangeProvider;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertyConstants;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * The base section that displays a title in a title bar.
 */
public abstract class BaseTitledSection extends AbstractPropertySection implements PropertyChangeListener {
	// The section
	protected Section section;

	// The main composite used to create the section content.
	protected Composite composite;

	protected IPropertyChangeProvider viewerInput;

	// The input node.
	protected IPeerNodeProvider provider;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#setInput(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
    public void setInput(IWorkbenchPart part, ISelection selection) {
        super.setInput(part, selection);
		if (this.viewerInput != null) {
			this.viewerInput.removePropertyChangeListener(this);
		}
        Assert.isTrue(selection instanceof IStructuredSelection);
        Object input = ((IStructuredSelection) selection).getFirstElement();

        IPeerNodeProvider provider = input instanceof IPeerNodeProvider ? (IPeerNodeProvider) input : null;
        if (provider == null) provider = input instanceof IAdaptable ? (IPeerNodeProvider)((IAdaptable)input).getAdapter(IPeerNodeProvider.class) : null;
        if (provider == null) provider = (IPeerNodeProvider)Platform.getAdapterManager().getAdapter(input, IPeerNodeProvider.class);

		if (provider != null) {
			this.provider = provider;
			IPeerNode peerNode = getPeerNode(provider);
			this.viewerInput = (IPropertyChangeProvider) peerNode.getAdapter(IPropertyChangeProvider.class);
			if (this.viewerInput != null) {
				this.viewerInput.addPropertyChangeListener(this);
			}
		} else {
			this.provider = null;
			this.viewerInput = null;
		}
		updateInput(provider);
    }

	/**
	 * Get the peer node from the provider.
	 * @param provider
	 * @return
	 */
	protected IPeerNode getPeerNode(final IPeerNodeProvider provider) {
		Assert.isNotNull(provider);
		final AtomicReference<IPeerNode> peerNode = new AtomicReference<IPeerNode>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				peerNode.set(provider.getPeerNode());
			}
		});

		return peerNode.get();
	}

	/**
	 * Update the input node.
	 *
	 * @param input The input node.
	 */
	protected void updateInput(IPeerNodeProvider input) {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#aboutToBeHidden()
	 */
	@Override
    public void aboutToBeHidden() {
		if(this.viewerInput != null) {
			this.viewerInput.removePropertyChangeListener(this);
		}
    }

	/**
	 * Returns the standard label width of the properties section.
	 *
	 * @return The standard label width.
	 */
	protected int getStandardLabelWidth() {
		return STANDARD_LABEL_WIDTH;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#createControls(org.eclipse.swt.widgets.Composite, org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage)
	 */
	@Override
    public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);
		parent.setLayout(new FormLayout());

		section = getWidgetFactory().createSection(parent, ExpandableComposite.TITLE_BAR);
		section.setText(getText());
		FormData data = new FormData();
		data.left = new FormAttachment(0, ITabbedPropertyConstants.HMARGIN);
		data.right = new FormAttachment(100, -ITabbedPropertyConstants.HMARGIN);
		data.top = new FormAttachment(0, 2 * ITabbedPropertyConstants.VSPACE);
		section.setLayoutData(data);

		composite = getWidgetFactory().createComposite(parent);
		FormLayout layout = new FormLayout();
		layout.spacing = ITabbedPropertyConstants.HMARGIN;
		composite.setLayout(layout);

		data = new FormData();
		data.left = new FormAttachment(0, 2 * ITabbedPropertyConstants.HMARGIN);
		data.right = new FormAttachment(100, -2 * ITabbedPropertyConstants.HMARGIN);
		data.top = new FormAttachment(section, ITabbedPropertyConstants.VSPACE);
		data.bottom = new FormAttachment(100, 0);
		composite.setLayoutData(data);
	}

	/**
	 * Create a label for the control using the specified text.
	 *
	 * @param control The control for which the label is created.
	 * @param text The label text. Must not be <code>null</code>.
	 */
	protected CLabel createLabel(Control control, String text) {
		Assert.isNotNull(control);
		Assert.isNotNull(text);

		CLabel nameLabel = getWidgetFactory().createCLabel(composite, text);
		FormData data = new FormData();
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(control, -ITabbedPropertyConstants.HSPACE);
		data.top = new FormAttachment(control, 0, SWT.CENTER);
		data.width = SWTControlUtil.convertWidthInCharsToPixels(nameLabel, text.length() + 2);
		nameLabel.setLayoutData(data);
		return nameLabel;
	}

	/**
	 * Create a text field and a label with the specified label
	 * relative to the specified control.
	 *
	 * @param control The control relative to.
	 * @param label The text of the label.
	 * @return The new text created.
	 */
	protected Text createTextField(Control control, String label) {
		Text text = createText(control);
		createLabel(text, label);
		return text;
	}

	/**
	 * Create a checkbox with the specified label
	 * relative to the specified control.
	 *
	 * @param control The control relative to.
	 * @param label The text of the label.
	 * @return The new checkbox created.
	 */
	protected Button createCheckbox(Control control, String label) {
		Button check = getWidgetFactory().createButton(composite, label, SWT.CHECK);
		FormData data = new FormData();
		data.left = new FormAttachment(0, getStandardLabelWidth());
		data.right = new FormAttachment(100, 0);
		if (control == null) {
			data.top = new FormAttachment(0, ITabbedPropertyConstants.VSPACE);
		}
		else {
			data.top = new FormAttachment(control, ITabbedPropertyConstants.VSPACE);
		}
		check.setLayoutData(data);
		check.setEnabled(false);
		return check;
	}

	/**
	 * Create a wrap text field and a label with the specified label
	 * relative to the specified control.
	 *
	 * @param control The control relative to.
	 * @param label The text of the label.
	 * @return The new wrap text created.
	 */
	protected Text createWrapTextField(Control control, String label) {
		Text text = createWrapText(control);
		createLabel(text, label);
		return text;
	}

	/**
	 * Create a text field relative to the specified control.
	 *
	 * @param control The control to layout the new text field.
	 * @return The new text field created.
	 */
	protected Text createText(Control control) {
		Text text = getWidgetFactory().createText(composite, "", SWT.SINGLE | SWT.NO_FOCUS); //$NON-NLS-1$
		FormData data = new FormData();
		data.left = new FormAttachment(0, getStandardLabelWidth());
		data.right = new FormAttachment(100, 0);
		if (control == null) {
			data.top = new FormAttachment(0, ITabbedPropertyConstants.VSPACE);
		}
		else {
			data.top = new FormAttachment(control, ITabbedPropertyConstants.VSPACE);
		}
		text.setLayoutData(data);
		text.setEditable(false);
		return text;
	}

	/**
	 * Create a wrap text field relative to the specified control.
	 *
	 * @param control The control to layout the new wrap text field.
	 * @return The new wrap text field created.
	 */
	protected Text createWrapText(Control control) {
		Text text = getWidgetFactory().createText(composite, "", SWT.WRAP | SWT.NO_FOCUS); //$NON-NLS-1$
		FormData data = new FormData();
		data.left = new FormAttachment(0, getStandardLabelWidth());
		data.right = new FormAttachment(100, 0);
		if (control == null) {
			data.top = new FormAttachment(0, ITabbedPropertyConstants.VSPACE);
		}
		else {
			data.top = new FormAttachment(control, ITabbedPropertyConstants.VSPACE);
		}
		data.width = 200;
		text.setLayoutData(data);
		text.setEditable(false);
		return text;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#refresh()
	 */
	@Override
    public void refresh() {
		if (composite != null) {
			composite.layout();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	@Override
    public void propertyChange(PropertyChangeEvent event) {
		if (event.getSource() == provider) {
			updateInput(provider);
			Display display = getPart().getSite().getShell().getDisplay();
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					refresh();
				}
			});
		}
    }

	/**
	 * Get the text which is used as the title in the title bar of the section.
	 *
	 * @return A text string representing the section.
	 */
	protected abstract String getText();
}