/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.ui.interfaces;

import org.eclipse.tcf.te.tcf.ui.handler.ContextSelectorToolbarContribution;

/**
 * Provides dynamic information for the {@link ContextSelectorToolbarContribution}
 */
public interface IContextSelectorToolbarDelegate {

	/**
	 * Returns a list of new configuration wizard ids to show
	 * in the context selector toolbar contribution.
	 * @param context
	 * @return
	 */
	public String[] getToolbarNewConfigWizardIds(Object context);
}
