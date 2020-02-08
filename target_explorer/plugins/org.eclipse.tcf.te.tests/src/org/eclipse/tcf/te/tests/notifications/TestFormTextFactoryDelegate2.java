/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.notifications;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.runtime.events.NotifyEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;


/**
 * Test notification form text factory delegate implementation.
 */
public class TestFormTextFactoryDelegate2 extends TestFormTextFactoryDelegate {

	 /* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.notifications.delegates.DefaultFormTextFactoryDelegate#populateFormText(org.eclipse.ui.forms.widgets.FormToolkit, org.eclipse.ui.forms.widgets.FormText, org.eclipse.tcf.te.runtime.events.NotifyEvent)
	 */
	@Override
	public void populateFormText(FormToolkit toolkit, FormText widget, NotifyEvent event) {
		Assert.isNotNull(toolkit);
		Assert.isNotNull(widget);
		Assert.isNotNull(event);

		// Get properties
		String titleText = event.getProperties().getStringProperty(NotifyEvent.PROP_TITLE_TEXT);
		String titleImageId = event.getProperties().getStringProperty(NotifyEvent.PROP_TITLE_IMAGE_ID);
		String moduleName = event.getProperties().getStringProperty("moduleName"); //$NON-NLS-1$
		boolean success = event.getProperties().getBooleanProperty("success"); //$NON-NLS-1$

		if (titleText != null && moduleName != null) {
			StringBuilder buffer = new StringBuilder();

			buffer.append("<form>"); //$NON-NLS-1$

			// The title paragraph
			buffer.append("<p>"); //$NON-NLS-1$

			// If the title image id is set, try to load the image
			if (titleImageId != null) {
				Image image = getImage(titleImageId);
				if (image != null) {
					buffer.append("<img href=\"titleImage\"/> "); //$NON-NLS-1$
					widget.setImage("titleImage", image); //$NON-NLS-1$
				}
			}

			// Set the title using the default header font
			buffer.append("<span color=\"header\" font=\"header\">"); //$NON-NLS-1$
			buffer.append(titleText);
			buffer.append("</span>"); //$NON-NLS-1$

			buffer.append("</p>"); //$NON-NLS-1$

			if (success) {
				buffer.append("<p>"); //$NON-NLS-1$
				buffer.append("<span color=\"green\" font=\"text\">"); //$NON-NLS-1$
				buffer.append("Successfully"); //$NON-NLS-1$
				buffer.append("</span>"); //$NON-NLS-1$
				buffer.append(" "); //$NON-NLS-1$
				buffer.append("<span font=\"text\">"); //$NON-NLS-1$
				buffer.append("loaded module " + moduleName + "."); //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append("</span>"); //$NON-NLS-1$
				buffer.append("</p>"); //$NON-NLS-1$

				buffer.append("<p>"); //$NON-NLS-1$
				buffer.append("<a href=\"showModule\" nowrap=\"true\">Show Module</a>"); //$NON-NLS-1$
				buffer.append("</p>"); //$NON-NLS-1$
			} else {
				buffer.append("<p>"); //$NON-NLS-1$
				buffer.append("<span color=\"red\" font=\"text\">"); //$NON-NLS-1$
				buffer.append("Failed"); //$NON-NLS-1$
				buffer.append("</span>"); //$NON-NLS-1$
				buffer.append(" "); //$NON-NLS-1$
				buffer.append("<span font=\"text\">"); //$NON-NLS-1$
				buffer.append("to load module " + moduleName + "."); //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append("</span>"); //$NON-NLS-1$
				buffer.append("</p>"); //$NON-NLS-1$

				buffer.append("<p>"); //$NON-NLS-1$
				buffer.append("<a href=\"showError\" nowrap=\"true\">Show Error</a>"); //$NON-NLS-1$
				buffer.append("</p>"); //$NON-NLS-1$
			}

			buffer.append("</form>"); //$NON-NLS-1$

			// Set colors
			setFormTextColors(toolkit, widget);
			// Set fonts
			setFormTextFonts(toolkit, widget);

			// Set the form text to the widget
			widget.setText(buffer.toString(), true, false);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.notifications.delegates.DefaultFormTextFactoryDelegate#setFormTextColors(org.eclipse.ui.forms.widgets.FormToolkit, org.eclipse.ui.forms.widgets.FormText)
	 */
	@Override
	protected void setFormTextColors(FormToolkit toolkit, FormText widget) {
	    super.setFormTextColors(toolkit, widget);

		widget.setColor("green", PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN)); //$NON-NLS-1$
		widget.setColor("red", PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_RED)); //$NON-NLS-1$
	}
}
