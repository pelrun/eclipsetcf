/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.properties;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IProcesses.ProcessContext;
import org.eclipse.tcf.services.ISysMonitor;
import org.eclipse.tcf.services.ISysMonitor.SysMonitorContext;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNodeProperties;

/**
 * The property tester for a process tree node.
 */
public class PropertyTester extends org.eclipse.core.expressions.PropertyTester {

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, final Object expectedValue) {
		if (receiver instanceof IProcessContextNode) {
			final IProcessContextNode node = (IProcessContextNode) receiver;
			if ("isAttached".equals(property) && expectedValue instanceof Boolean) { //$NON-NLS-1$
				final AtomicBoolean isAttached = new AtomicBoolean();
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						ProcessContext pc = node.getProcessContext();
						SysMonitorContext sc = node.getSysMonitorContext();
						boolean attached = false;
						if (pc != null)
							attached = pc.isAttached();
						if (!attached && sc != null)
							attached = sc.getTracerPID() > 0 || "t".equals(sc.getState()); //$NON-NLS-1$
						isAttached.set(attached);
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
								SysMonitorContext sc = node.getSysMonitorContext();
								if (sc != null && sc.getProperties().containsKey(ISysMonitor.PROP_EXETYPE)) {
									Object exeType = sc.getProperties().get(ISysMonitor.PROP_EXETYPE);
									canAttach.set(!Integer.valueOf(ISysMonitor.EXETYPE_KERNEL).equals(exeType));
								} else {
									canAttach.set(true);
								}
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

			if ("canTerminate".equals(property) && expectedValue instanceof Boolean) { //$NON-NLS-1$
				final AtomicBoolean canTerminate = new AtomicBoolean();
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						if (node.getProcessContext() != null) {
							canTerminate.set(node.getProcessContext().canTerminate());
						}
					}
				};
				if (Protocol.isDispatchThread()) runnable.run();
				else Protocol.invokeAndWait(runnable);

				return ((Boolean) expectedValue).booleanValue() == canTerminate.get();
			}

			if ("hasCapability".equals(property) && expectedValue instanceof String) { //$NON-NLS-1$
				final AtomicBoolean hasCapability = new AtomicBoolean();
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						@SuppressWarnings("unchecked")
                        Map<String, Object> caps = (Map<String, Object>)node.getProperty(IProcessContextNodeProperties.PROPERTY_CAPABILITIES);
						if (caps != null) {
							hasCapability.set(caps.containsKey(expectedValue) && Boolean.parseBoolean(caps.get(expectedValue).toString()));
						}
					}
				};
				if (Protocol.isDispatchThread()) runnable.run();
				else Protocol.invokeAndWait(runnable);

				return hasCapability.get();
			}
		}
		return false;
	}
}
