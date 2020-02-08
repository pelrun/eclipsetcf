/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.runtime.services.interfaces.delegates;

import org.eclipse.tcf.te.runtime.services.interfaces.IUIService;

/**
 * Label provider delegate for {@link IUIService} to enable
 * label provider access in core.
 */
public interface ILabelProviderDelegate {

    /**
     * Returns the text for the label of the given element.
     *
     * @param element the element for which to provide the label text
     * @return the text string used to label the element, or <code>null</code>
     *   if there is no text label for the given object
     */
	public String getText(Object element);

    /**
     * Returns a text label that is based on the given text label,
     * but decorated with additional information relating to the state
     * of the provided element.
     *
     * @param text the input text label to decorate
     * @param element the element whose image is being decorated
     * @return the decorated text label, or <code>null</code> if no decoration is to be applied
     */
    public String decorateText(String text, Object element);
}
