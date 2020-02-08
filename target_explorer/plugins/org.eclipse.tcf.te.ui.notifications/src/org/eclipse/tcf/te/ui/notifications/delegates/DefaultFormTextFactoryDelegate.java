/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.notifications.delegates;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.runtime.events.NotifyEvent;
import org.eclipse.tcf.te.ui.notifications.activator.UIPlugin;
import org.eclipse.tcf.te.ui.notifications.interfaces.IFormTextFactoryDelegate;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Default notification form text factory delegate implementation.
 */
public class DefaultFormTextFactoryDelegate implements IFormTextFactoryDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.notifications.interfaces.IFormTextFactoryDelegate#populateFormText(org.eclipse.ui.forms.widgets.FormToolkit, org.eclipse.ui.forms.widgets.FormText, org.eclipse.tcf.te.runtime.events.NotifyEvent)
	 */
	@Override
	public void populateFormText(FormToolkit toolkit, FormText widget, NotifyEvent event) {
		Assert.isNotNull(toolkit);
		Assert.isNotNull(widget);
		Assert.isNotNull(event);

		// Get properties
		String titleText = event.getProperties().getStringProperty(NotifyEvent.PROP_TITLE_TEXT);
		String titleImageId = event.getProperties().getStringProperty(NotifyEvent.PROP_TITLE_IMAGE_ID);
		String description = event.getProperties().getStringProperty(NotifyEvent.PROP_DESCRIPTION_TEXT);

		// At least the title text and the description must be not null
		if (titleText != null && description != null) {
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

			// Add the description
			buffer.append("<p>"); //$NON-NLS-1$
			buffer.append("<span font=\"text\">"); //$NON-NLS-1$
			buffer.append(description);
			buffer.append("</span>"); //$NON-NLS-1$
			buffer.append("</p>"); //$NON-NLS-1$

			buffer.append("</form>"); //$NON-NLS-1$

			// Set colors
			setFormTextColors(toolkit, widget);
			// Set fonts
			setFormTextFonts(toolkit, widget);

			// Set the form text to the widget
			widget.setText(buffer.toString(), true, false);
		}
	}

	/**
	 * Set the colors used by the form.
	 *
	 * @param toolkit The form toolkit. Must not be <code>null</code>.
	 * @param widget The form text widget. Must not be <code>null</code>.
	 */
	protected void setFormTextColors(FormToolkit toolkit, FormText widget) {
		Assert.isNotNull(toolkit);
		Assert.isNotNull(widget);

		widget.setColor("header", toolkit.getColors().getColor(IFormColors.TITLE)); //$NON-NLS-1$
	}

	/**
	 * Set the fonts used by the form.
	 *
	 * @param toolkit The form toolkit. Must not be <code>null</code>.
	 * @param widget The form text widget. Must not be <code>null</code>.
	 */
	protected void setFormTextFonts(FormToolkit toolkit, FormText widget) {
		Assert.isNotNull(toolkit);
		Assert.isNotNull(widget);

		widget.setFont("header", JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT)); //$NON-NLS-1$
		widget.setFont("text", JFaceResources.getTextFont()); //$NON-NLS-1$
	}

	/**
	 * Returns the image for the given image key.
	 *
	 * @param key The image key. Must not be <code>null</code>.
	 * @return The image or <code>null</code>.
	 */
	protected Image getImage(String key) {
		return UIPlugin.getImage(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.notifications.interfaces.IFormTextFactoryDelegate#getNotificationCloseDelay()
	 */
	@Override
	public long getNotificationCloseDelay() {
	    return -1;
	}
}
