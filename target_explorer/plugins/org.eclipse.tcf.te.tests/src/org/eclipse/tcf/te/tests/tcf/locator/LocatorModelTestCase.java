/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf.locator;

import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tests.tcf.TcfTestCase;
import org.eclipse.tcf.te.tests.tcf.model.ModelTestCase;

/**
 * Locator and peer model test cases.
 */
public class LocatorModelTestCase extends TcfTestCase {

	/**
	 * Provides a test suite to the caller which combines all single
	 * test bundled within this category.
	 *
	 * @return Test suite containing all test for this test category.
	 */
	public static Test getTestSuite() {
		TestSuite testSuite = new TestSuite("Test Locator and Peer model"); //$NON-NLS-1$

			// add ourself to the test suite
			testSuite.addTestSuite(ModelTestCase.class);

		return testSuite;
	}

	//***** BEGIN SECTION: Single test methods *****
	//NOTE: All method which represents a single test case must
	//      start with 'test'!

	public void testLocatorModel() {
		assertNotNull("Test peer missing.", peer); //$NON-NLS-1$
		assertNotNull("Test peer node missing.", peerNode); //$NON-NLS-1$

		ILocatorModel model = ModelManager.getLocatorModel();
		assertNotNull("Failed to instantiate locator model.", model); //$NON-NLS-1$

		// Get the refresh service and force a refresh of the model
		final ILocatorModelRefreshService refreshSvc = model.getService(ILocatorModelRefreshService.class);
		assertNotNull("Failed to get locator model refresh service.", refreshSvc); //$NON-NLS-1$

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				refreshSvc.refresh(new Callback());
			}
		};
		Protocol.invokeAndWait(runnable);

		// As there is the local test agent started, give it time to discover the test agent
		// and return with at least one peer.
		IPeer[] peers = model.getPeers();
		int counter = 20;
		while (peers.length == 0 && counter > 0) {
			try { Thread.sleep(200); } catch (InterruptedException e) {}
			peers = model.getPeers();
			counter--;
		}
		assertTrue("Locator model failed to discover any peer.", peers.length > 0); //$NON-NLS-1$

		// Lookup the test peer
		final ILocatorModelLookupService lkupSvc = model.getService(ILocatorModelLookupService.class);
		assertNotNull("Failed to get locator model lookup service.", lkupSvc); //$NON-NLS-1$

		final AtomicReference<IPeer> p = new AtomicReference<IPeer>();

		runnable = new Runnable() {

			@SuppressWarnings("synthetic-access")
            @Override
			public void run() {
				p.set(lkupSvc.lkupPeerById(peer.getID()));
				int counter = 20;
				while (p.get() == null && counter > 0) {
					try { Thread.sleep(200); } catch (InterruptedException e) {}
					p.set(lkupSvc.lkupPeerById(peer.getID()));
					counter--;
				}
			}
		};
		Protocol.invokeAndWait(runnable);

		assertNotNull("Failed to lookup test peer by ID in locator model", p.get()); //$NON-NLS-1$

		// There must be just one peer in the model per agent id

		final AtomicReference<IPeer[]> ps = new AtomicReference<IPeer[]>();

		runnable = new Runnable() {
			@SuppressWarnings("synthetic-access")
            @Override
			public void run() {
				ps.set(lkupSvc.lkupPeerByAgentId(peer.getAgentID()));
			}
		};
		Protocol.invokeAndWait(runnable);

		assertNotNull("Failed to lookup test peers by agent ID in locator model", ps.get()); //$NON-NLS-1$
		assertEquals("Invalid number of test peers in the locator model.", 1, ps.get().length); //$NON-NLS-1$
	}



	//***** END SECTION: Single test methods *****
}
