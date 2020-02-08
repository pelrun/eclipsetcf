/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.runtime.stepper.job;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

public abstract class AbstractStepperJobSchedulingRule implements ISchedulingRule {

	private final Object context;
	private final String operation;

	/**
     * Constructor.
     */
    public AbstractStepperJobSchedulingRule(Object context, String operation) {
    	this.context = context;
    	this.operation = operation;
    }

    public Object getContext() {
    	return context;
    }

    public String getOperation() {
    	return operation;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 */
    @Override
    public final boolean contains(ISchedulingRule rule) {
    	if (rule instanceof AbstractStepperJobSchedulingRule &&
    					context instanceof ISchedulingRule &&
    					((AbstractStepperJobSchedulingRule)rule).context instanceof ISchedulingRule) {
    		return ((ISchedulingRule)context).contains((ISchedulingRule)((AbstractStepperJobSchedulingRule)rule).context);
    	}
        return false;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#isConflicting(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 */
    @Override
    public final boolean isConflicting(ISchedulingRule rule) {
    	if (this == rule) {
    		return true;
    	}
    	if (rule instanceof AbstractStepperJobSchedulingRule) {
    		boolean ctxConflicts = context.equals(((AbstractStepperJobSchedulingRule)rule).getContext());
    		return ctxConflicts && isConflicting(this, (AbstractStepperJobSchedulingRule)rule);
    	}
    	if (context instanceof ISchedulingRule) {
    		// use the conflicting rule of parent for all other operations.
    		return ((ISchedulingRule)context).isConflicting(rule);
    	}
        return true;
    }

    protected abstract boolean isConflicting(AbstractStepperJobSchedulingRule rule1, AbstractStepperJobSchedulingRule rule2);
}