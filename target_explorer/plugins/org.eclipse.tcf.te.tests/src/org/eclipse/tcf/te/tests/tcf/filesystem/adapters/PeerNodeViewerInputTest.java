/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf.filesystem.adapters;

import org.eclipse.core.runtime.Platform;
import org.eclipse.tcf.te.core.interfaces.IViewerInput;
import org.eclipse.tcf.te.tests.tcf.filesystem.FSPeerTestCase;

public class PeerNodeViewerInputTest extends FSPeerTestCase {
	public void testViewerInputId() {
		IViewerInput input = (IViewerInput) Platform.getAdapterManager().getAdapter(peerNode, IViewerInput.class);
		assertNotNull(input);
		String id = input.getInputId();
		assertEquals(peerNode.getPeerId(), id);
	}
}
