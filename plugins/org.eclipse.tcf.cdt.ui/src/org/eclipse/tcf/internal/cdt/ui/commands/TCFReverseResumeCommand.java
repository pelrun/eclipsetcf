/*******************************************************************************
 * Copyright (c) 2010, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui.commands;

import org.eclipse.cdt.debug.core.model.IReverseResumeHandler;
import org.eclipse.tcf.internal.debug.ui.commands.BackResumeCommand;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;

/**
 * Debug command handler for reverse resume.
 */
public class TCFReverseResumeCommand extends BackResumeCommand
        implements IReverseResumeHandler {

    public TCFReverseResumeCommand(TCFModel model) {
        super(model);
    }
}
