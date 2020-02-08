/*******************************************************************************
 * Copyright (c) 2007, 2015 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.commands;

import org.eclipse.debug.core.commands.IDebugCommandRequest;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.services.IRunControl;

public class BackIntoCommand extends StepCommand {

    public BackIntoCommand(TCFModel model) {
        super(model);
    }

    @Override
    protected boolean canExecute(IRunControl.RunControlContext ctx) {
        if (ctx == null) return false;
        if (ctx.canResume(IRunControl.RM_REVERSE_STEP_INTO_LINE)) return true;
        if (ctx.canResume(IRunControl.RM_REVERSE_STEP_INTO)) return true;
        return false;
    }

    @Override
    protected void execute(final IDebugCommandRequest monitor,
            final IRunControl.RunControlContext ctx,
            boolean src_step, final Runnable done) {
        TCFNodeExecContext node = (TCFNodeExecContext)model.getNode(ctx.getID());
        new ActionStepInto(node, src_step, true, monitor, done);
    }
}
