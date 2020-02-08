/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.events;

import java.util.EventObject;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.runtime.activator.CoreBundleActivator;
import org.eclipse.tcf.te.runtime.interfaces.tracing.ITraceIds;

/**
 * Event used to trigger a command execution from a non-UI plugin.
 */
public class TriggerCommandEvent extends EventObject {
    private static final long serialVersionUID = -1685836126878415742L;

    private final String commandId;

	/**
	 * Constructor
	 *
	 * @param source The event source. Must not be <code>null</code>.
	 * @param commandId The command id. Must not be <code>null</code>.
	 */
	public TriggerCommandEvent(Object source, String commandId) {
		super(source);

		Assert.isNotNull(commandId);
		this.commandId = commandId;
	}
	/**
	 * Returns the command id.
	 *
	 * @return The command id.
	 */
	public final String getCommandId() {
		return commandId;
	}

	/* (non-Javadoc)
	 * @see com.windriver.ide.common.core.event.WRAbstractNotificationEvent#toString()
	 */
	@Override
	public String toString() {
		StringBuffer toString = new StringBuffer(getClass().getName());

		String prefix = ""; //$NON-NLS-1$
		// if tracing the event, formating them a little bit better readable.
		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_EVENTS)) {
			prefix = "\n\t\t"; //$NON-NLS-1$
		}

		toString.append(prefix + "{commandId="); //$NON-NLS-1$
		toString.append(commandId);
		toString.append("," + prefix + "source="); //$NON-NLS-1$ //$NON-NLS-2$
		toString.append(source);
		toString.append("}"); //$NON-NLS-1$

		return toString.toString();
	}


}
