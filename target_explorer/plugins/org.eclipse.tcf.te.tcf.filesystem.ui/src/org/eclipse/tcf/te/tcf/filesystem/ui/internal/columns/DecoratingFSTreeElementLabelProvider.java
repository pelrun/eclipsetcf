/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.columns;

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.ui.PlatformUI;

/**
 * The label provider for the tree column "name".
 */
public class DecoratingFSTreeElementLabelProvider extends DecoratingLabelProvider {

	public DecoratingFSTreeElementLabelProvider() {
		super(new FSTreeElementLabelProvider(), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator());
		DecorationContext context = new DecorationContext();
		context.putProperty(IDecoration.ENABLE_REPLACE, Boolean.TRUE);
		setDecorationContext(context);
	}
}
