/**
 * AbstractSimulatorService.java
 * Created on Mar 22, 2013
 *
 * Copyright (c) 2013 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.tcf.locator.services;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Abstract simulator service implementation.
 */
public abstract class AbstractSimulatorService extends AbstractService implements ISimulatorService {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService#isValidContext(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isValidContext(final Object context, String config) {
		if (context instanceof IPeerNode) {
			final AtomicBoolean complete = new AtomicBoolean(false);
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					complete.set(((IPeerNode)context).isComplete());
				}
			};
			Protocol.invokeAndWait(runnable);
			return complete.get();
		}
	    return false;
	}
}
