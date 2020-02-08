/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.navigator;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.runtime.services.interfaces.delegates.ILabelProviderDelegate;
import org.eclipse.tcf.te.ui.views.extensions.LabelProviderDelegateExtensionPointManager;
import org.eclipse.ui.navigator.IDescriptionProvider;


/**
 * Label provider implementation.
 */
public class DelegatingLabelProvider extends LabelProvider implements ILabelDecorator, ILabelProviderDelegate, IDescriptionProvider, IColorProvider, IFontProvider {

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
					if (candidate != null && candidate != image) {
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

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.IDescriptionProvider#getDescription(java.lang.Object)
	 */
    @Override
    public String getDescription(Object element) {
		ILabelProvider[] delegates = LabelProviderDelegateExtensionPointManager.getInstance().getDelegates(element, false);

		String description = null;

		if (delegates != null && delegates.length > 0) {
			for (ILabelProvider delegate : delegates) {
				if (delegate instanceof IDescriptionProvider) {
					String candidate = ((IDescriptionProvider)delegate).getDescription(element);
					if (candidate != null) {
						description = candidate;
						break;
					}
				}
			}
		}

		return decorateText(getText(element), element) + (description != null ? " - " + description : ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IFontProvider#getFont(java.lang.Object)
	 */
    @Override
    public Font getFont(Object element) {
		ILabelProvider[] delegates = LabelProviderDelegateExtensionPointManager.getInstance().getDelegates(element, false);

		if (delegates != null && delegates.length > 0) {
			for (ILabelProvider delegate : delegates) {
				if (delegate instanceof IFontProvider) {
					Font candidate = ((IFontProvider)delegate).getFont(element);
					if (candidate != null) {
						return candidate;
					}
				}
			}
		}

	    return null;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
    @Override
    public Color getForeground(Object element) {
		ILabelProvider[] delegates = LabelProviderDelegateExtensionPointManager.getInstance().getDelegates(element, false);

		if (delegates != null && delegates.length > 0) {
			for (ILabelProvider delegate : delegates) {
				if (delegate instanceof IColorProvider) {
					Color candidate = ((IColorProvider)delegate).getForeground(element);
					if (candidate != null) {
						return candidate;
					}
				}
			}
		}

	    return null;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
	 */
    @Override
    public Color getBackground(Object element) {
		ILabelProvider[] delegates = LabelProviderDelegateExtensionPointManager.getInstance().getDelegates(element, false);

		if (delegates != null && delegates.length > 0) {
			for (ILabelProvider delegate : delegates) {
				if (delegate instanceof IColorProvider) {
					Color candidate = ((IColorProvider)delegate).getBackground(element);
					if (candidate != null) {
						return candidate;
					}
				}
			}
		}

	    return null;
    }
}
