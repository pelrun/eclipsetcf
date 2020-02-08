/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.interfaces;

import org.eclipse.tcf.te.ui.trees.AbstractTreeControl;

/**
 * Interface to be implemented by tree control input changed listeners.
 */
public interface ITreeControlInputChangedListener {

    /**
     * Notifies the listener that the input of the tree control has changed.
     *
     * @param control The tree control. Must not be <code>null</code>.
     * @param oldInput The old input element or <code>null</code>.
     * @param newInput The new input element or <code>null</code>.
     */
    public void inputChanged(AbstractTreeControl control, Object oldInput, Object newInput);

}
