/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.properties;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;

/**
 * The property tester for a process tree node.
 */
public class PropertyTester extends org.eclipse.core.expressions.PropertyTester {

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (receiver instanceof IProcessContextNode) {
			final IProcessContextNode node = (IProcessContextNode) receiver;
			if ("isAttached".equals(property) && expectedValue instanceof Boolean) { //$NON-NLS-1$
				final AtomicBoolean isAttached = new AtomicBoolean();
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						if (node.getProcessContext() != null) {
							isAttached.set(node.getProcessContext().isAttached());
						}
					}
				};
				if (Protocol.isDispatchThread()) runnable.run();
				else Protocol.invokeAndWait(runnable);

				return ((Boolean) expectedValue).booleanValue() == isAttached.get();
			}

			if ("canAttach".equals(property) && expectedValue instanceof Boolean) { //$NON-NLS-1$
				final AtomicBoolean canAttach = new AtomicBoolean();
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						if (node.getProcessContext() != null) {
							if (node.getProcessContext().getProperties().containsKey("CanAttach")) { //$NON-NLS-1$
								Boolean value = (Boolean)node.getProcessContext().getProperties().get("CanAttach"); //$NON-NLS-1$
								canAttach.set(value != null && value.booleanValue());
							} else {
								canAttach.set(true);
							}
						}
					}
				};
				if (Protocol.isDispatchThread()) runnable.run();
				else Protocol.invokeAndWait(runnable);

				return ((Boolean) expectedValue).booleanValue() == canAttach.get();
			}

			if ("hasProcessContext".equals(property) && expectedValue instanceof Boolean) { //$NON-NLS-1$
				final AtomicBoolean canAttach = new AtomicBoolean();
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						canAttach.set(node.getProcessContext() != null);
					}
				};
				if (Protocol.isDispatchThread()) runnable.run();
				else Protocol.invokeAndWait(runnable);

				return ((Boolean) expectedValue).booleanValue() == canAttach.get();
			}
		}
		return false;
	}
}
