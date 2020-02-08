/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.stepper.interfaces;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.tcf.te.runtime.interfaces.extensions.IExecutableExtension;


/**
 * A step group.
 * <p>
 * A step group is a set of single steps or other step groups.
 */
public interface IStepGroup extends IExecutableExtension {

	/**
	 * Returns if or if not the step group is locked for user modifications.
	 *
	 * @return <code>True</code> if locked for user modifications, <code>false</code> otherwise.
	 */
	public boolean isLocked();

	/**
	 * Returns the list of steps or step groups enlisted and enabled in the group for the
	 * given context.
	 *
	 * @param context The step context object. Must not be <code>null</code>.
	 * @return The list of steps and step groups or an empty array.
	 *
	 * @throws CoreException If the steps cannot be determined.
	 */
	public IStepGroupable[] getSteps(IStepContext context) throws CoreException;

	/**
	 * Return the step group iterator or <code>null</code>. The step group iterator can be used to
	 * generate loops and conditions for a step group.
	 */
	public IStepGroupIterator getStepGroupIterator();


	/**
	 * Set additional parameters for this step
	 * @param parameters
	 */
	public void setParameters(Map<String,String> parameters);

	/**
	 * Returns a map of additional parameters given through the parameters section in the Reference section of a StepGroups.
	 * @return The parameters of an empty Map.
	 */
	public Map<String,String> getParameters();
}
