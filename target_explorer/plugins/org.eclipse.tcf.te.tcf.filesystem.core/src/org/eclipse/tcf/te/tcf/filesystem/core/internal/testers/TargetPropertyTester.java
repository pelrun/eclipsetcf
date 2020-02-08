/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.testers;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * The property tester to test if the target OS is a Windows OS.
 */
public class TargetPropertyTester extends PropertyTester {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if(receiver instanceof IPeerNode) {
			final IPeerNode peerNode = (IPeerNode) receiver;
			if(property.equals("isWindows")) { //$NON-NLS-1$
				return isWindows(peerNode);
			}
		}
		return false;
	}

	/**
	 * Test if the target represented by the peer model is a windows target.
	 *
	 * @param peerNode The peer model of the target.
	 * @return true if it is a windows target.
	 */
	public static boolean isWindows(final IPeerNode peerNode) {
		final String osName = getOSName(peerNode);
		return osName == null ? false : (osName.startsWith("Windows")); //$NON-NLS-1$
	}

	/**
	 * Get the OS name from the peer model.
	 *
	 * @param peerNode The peer model.
	 * @return OS name.
	 */
	public static String getOSName(final IPeerNode peerNode) {
	    final String[] osName = new String[1];
		if (Protocol.isDispatchThread()) {
			osName[0] = peerNode.getPeer().getOSName();
		}
		else {
			Protocol.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					osName[0] = peerNode.getPeer().getOSName();
				}
			});
		}
	    return osName[0];
    }
}
