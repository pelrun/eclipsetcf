/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.expressions;

import org.eclipse.tcf.te.runtime.services.ServiceUtils;
import org.eclipse.tcf.te.ui.interfaces.handler.IEditorHandlerDelegate;
import org.eclipse.tcf.te.ui.views.editor.Editor;
import org.eclipse.tcf.te.ui.views.extensions.EditorPageBindingExtensionPointManager;
import org.eclipse.ui.IEditorInput;


/**
 * Property tester implementation.
 */
public class PropertyTester extends org.eclipse.core.expressions.PropertyTester {

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {

		if ("hasApplicableEditorBindings".equals(property)) { //$NON-NLS-1$
			IEditorHandlerDelegate delegate = ServiceUtils.getUIServiceDelegate(receiver, receiver, IEditorHandlerDelegate.class);
			IEditorInput input = delegate != null ? delegate.getEditorInput(receiver) : null;

			return (expectedValue != null ? expectedValue : Boolean.TRUE).equals(
							input != null ? Boolean.valueOf(EditorPageBindingExtensionPointManager.getInstance().getApplicableEditorPageBindings(input).length > 0) : Boolean.FALSE);
		}

		if ("isDirty".equals(property) && receiver instanceof Editor && expectedValue instanceof Boolean) { //$NON-NLS-1$
			Editor editor = (Editor) receiver;
			return ((Boolean)expectedValue).booleanValue() == editor.isDirty();
		}

		return false;
	}

}
