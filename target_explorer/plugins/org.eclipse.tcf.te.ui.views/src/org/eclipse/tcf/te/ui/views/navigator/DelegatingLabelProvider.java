/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.navigator;

import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.runtime.services.interfaces.delegates.ILabelProviderDelegate;
import org.eclipse.tcf.te.ui.views.extensions.LabelProviderDelegateExtensionPointManager;


/**
 * Label provider implementation.
 */
public class DelegatingLabelProvider extends LabelProvider implements ILabelDecorator, ILabelProviderDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(final Object element) {
		ILabelProvider[] delegates = LabelProviderDelegateExtensionPointManager.getInstance().getDelegates(element, false);

		if (delegates != null && delegates.length > 0) {
			for (ILabelProvider delegate : delegates) {
				String text = delegate.getText(element);
	            if (text != null) {
	    			return text;
				}
            }
		}

		return super.getText(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(final Object element) {
		ILabelProvider[] delegates = LabelProviderDelegateExtensionPointManager.getInstance().getDelegates(element, false);

		if (delegates != null && delegates.length > 0) {
			for (ILabelProvider delegate : delegates) {
				Image image = delegate.getImage(element);
				if (image != null) {
					return image;
				}
			}
		}

		return super.getImage(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelDecorator#decorateImage(org.eclipse.swt.graphics.Image, java.lang.Object)
	 */
	@Override
	public Image decorateImage(Image image, Object element) {
		ILabelProvider[] delegates = LabelProviderDelegateExtensionPointManager.getInstance().getDelegates(element, false);

		if (delegates != null && delegates.length > 0) {
			for (ILabelProvider delegate : delegates) {
				if (delegate instanceof ILabelDecorator) {
					Image candidate = ((ILabelDecorator)delegate).decorateImage(image, element);
					if (candidate != null) {
						return candidate;
					}
				}
			}
		}

		return image;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelDecorator#decorateText(java.lang.String, java.lang.Object)
	 */
	@Override
	public String decorateText(final String text, final Object element) {
		ILabelProvider[] delegates = LabelProviderDelegateExtensionPointManager.getInstance().getDelegates(element, false);

		if (delegates != null && delegates.length > 0) {
			for (ILabelProvider delegate : delegates) {
				if (delegate instanceof ILabelDecorator) {
					String candidate = ((ILabelDecorator)delegate).decorateText(text, element);
					if (candidate != null) {
						return candidate;
					}
				}
			}
		}

		return text;
	}
}
