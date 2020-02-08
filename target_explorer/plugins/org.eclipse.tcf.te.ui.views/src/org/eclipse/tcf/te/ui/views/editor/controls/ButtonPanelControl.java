/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.editor.controls;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.ui.controls.BaseControl;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.tcf.te.ui.views.nls.Messages;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;

/**
 * A panel control holding a set of buttons.
 * <p>
 * The panel control is associated with a managed form. If the managed form is the
 * embedded within a form editor or a form page, <code>getEditor()</code> will return
 * the parent form editor instance.
 * <p>
 * If the panel control has the default "Apply" button, the apply button will be
 * enabled once the parent form editor is marked dirty. If the "Apply" button is
 * clicked the form editor will be saved.
 */
public class ButtonPanelControl extends BaseControl {
	// Reference to the parent managed form
	private final IManagedForm form;

	// The control sub-control
	private Composite panel;
	/* default */ Button applyButton;

	// Reference to the dirty action listener
	private IPropertyListener dirtyListener = null;

	/**
	 * Constructor
	 *
	 * @param form The parent managed form. Must not be <code>null</code>.
	 */
	public ButtonPanelControl(IManagedForm form) {
		super();

		Assert.isNotNull(form);
		this.form = form;
	}

	/**
	 * Returns the parent managed form instance.
	 *
	 * @return The parent managed form instance.
	 */
	protected final IManagedForm getManagedForm() {
		return form;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	@Override
	public void dispose() {
		// Dispose the dirty action listener
		if (dirtyListener != null && getEditor() != null) {
			getEditor().removePropertyListener(dirtyListener);
			dirtyListener = null;
		}

	    super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.BaseControl#setupPanel(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void setupPanel(Composite parent) {
	    super.setupPanel(parent);

	    // Create the buttons panel
	    panel = doCreatePanel(parent);
	    Assert.isNotNull(panel);

	    // Create the buttons within the buttons panel
	    doCreateButtons(panel);
	}

	/**
	 * Create the panel that holds the buttons.
	 *
	 * @param parent The parent composite. Must not be <code>null</code>.
	 * @return The panel.
	 */
	protected Composite doCreatePanel(Composite parent) {
		Assert.isNotNull(parent);

		Composite panel = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0; layout.marginWidth = 0;
		panel.setLayout(layout);
		GridData layoutData = new GridData(SWT.TRAIL, SWT.BEGINNING, true, false);
		if (parent.getLayout() instanceof GridLayout) layoutData.horizontalSpan = ((GridLayout)parent.getLayout()).numColumns;
		panel.setLayoutData(layoutData);
		panel.setBackground(parent.getBackground());

		return panel;
	}

	/**
	 * Returns if or of not the control has an apply button added to the buttons
	 * panel by default.
	 * <p>
	 * <b>Note:</b> The default return by this method is <code>true</code>.
	 *
	 * @return <code>true</code> if there should be an apply button.
	 */
	protected boolean hasApplyButton() {
		return true;
	}

	/**
	 * Creates the buttons within the given parent composite.
	 *
	 * @param parent The parent composite. Must not be <code>null</code>.
	 */
	protected void doCreateButtons(Composite parent) {
		Assert.isNotNull(parent);

		// Create a "Apply" button if requested
		if (hasApplyButton() && getEditor() != null) {
			applyButton = new Button(parent, SWT.PUSH);
			applyButton.setText(Messages.ButtonPanelControl_applyButton_label);
			applyButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_SAVE_EDIT));
			applyButton.setBackground(parent.getBackground());

			GridData layoutData = new GridData(SWT.TRAIL, SWT.CENTER, false, false);
			layoutData.widthHint = SWTControlUtil.convertWidthInCharsToPixels(applyButton, 15);
			applyButton.setLayoutData(layoutData);

			applyButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					FormEditor editor = getEditor();
					Assert.isNotNull(editor);
					if (editor.isDirty()) {
						editor.doSave(new NullProgressMonitor());
					}
				}
			});

			dirtyListener = new IPropertyListener() {
				@Override
				public void propertyChanged(Object source, int propId) {
					if (propId == IEditorPart.PROP_DIRTY) {
						boolean dirty = getEditor().isDirty();
						applyButton.setEnabled(dirty);
					}
				}
			};
			getEditor().addPropertyListener(dirtyListener);
			applyButton.setEnabled(getEditor().isDirty());
		}
	}

	/**
	 * Returns the parent form editor if the managed form is embedded
	 * in such an editor.
	 *
	 * @return The parent form editor or <code>null</code>.
	 */
	protected final FormEditor getEditor() {
		FormEditor editor = null;

		Object container = getManagedForm() != null ? getManagedForm().getContainer() : null;
		if (container  instanceof FormEditor) {
			editor = (FormEditor)container;
		} else if (container instanceof FormPage) {
			editor = ((FormPage)container).getEditor();
		}

		return editor;
	}
}
