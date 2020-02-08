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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.extensions.IExecutableExtension;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;

/**
 * A single step.
 * <p>
 * Steps are assumed to be asynchronous. If the step execution finished, the passed in <b>callback
 * must be invoked</b>. The parent stepper suspend the step sequence execution till the callback is
 * invoked.
 * <p>
 * Steps signals the execution state to the parent stepper via the <code>IStatus</code> object
 * passed to the callback as first argument. The status object is mandatory and cannot be
 * <code>null</code>. If the step execution succeeds, an status with severity {@link IStatus#OK} is
 * expected.
 */
public interface IStep extends IExecutableExtension {

	/**
	 * Additional data property for ICallback.
	 */
	public static final String CALLBACK_PROPERTY_DATA = "data"; //$NON-NLS-1$

	/**
	 * Executes the context step logic.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param data The data giving object. Must not be <code>null</code>.
	 * @param fullQualifiedId The full qualified id for this step. Must not be <code>null</code>.
	 * @param monitor The progress monitor. Must not be <code>null</code>.
	 * @param callback The callback to invoke if finished. Must not be <code>null</code>.
	 */
	public void execute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor, ICallback callback);

	/**
	 * Returns if or if not this step can have multiple references within step groups. If
	 * <code>true</code> is returned, the step can occur exactly once per step group. This method
	 * effects all defined step groups and overwrite individual step settings.
	 * <p>
	 * The default implementation returns <code>false</code>.
	 *
	 * @return <code>True</code> if the step can be referenced only ones per step group,
	 *         <code>false</code> otherwise.
	 */
	public boolean isSingleton();

	/**
	 * Initialize the step from the given data.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param data The data. Must not be <code>null</code>.
	 * @param fullQualifiedId The full qualified id for this step. Must not be <code>null</code>.
	 * @param monitor The progress monitor. Must not be <code>null</code>.
	 */
	public void initializeFrom(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor);

	/**
	 * Validate execution conditions.
	 * <p>
	 * This method is called from
	 * {@link #execute(IAdaptable, IPropertiesContainer, IFullQualifiedId, IProgressMonitor, ICallback)}
	 * after the step initialization. If any execution condition is not fulfilled, the method should
	 * throw an {@link CoreException} to signal the failure.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param data The data. Must not be <code>null</code>.
	 * @param fullQualifiedId The full qualified id for this step. Must not be <code>null</code>.
	 * @param monitor The progress monitor. Must not be <code>null</code>.
	 *
	 * @throws CoreException if the execution cannot be continue. The associated status should
	 *             			 describe the failure cause.
	 */
	public void validateExecute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException;

	/**
	 * Cleanup intermediate data of the step.
	 * <p>
	 * This method will be called at the end of each step execution.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param data The data. Must not be <code>null</code>.
	 * @param fullQualifiedId The full qualified id for this step. Must not be <code>null</code>.
	 * @param monitor The progress monitor. Must not be <code>null</code>.
	 */
	public void cleanup(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor);

	/**
	 * Cancel the current execute.
	 * <p>
	 * This method will be called when the stepper gets canceled.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param data The data. Must not be <code>null</code>.
	 * @param fullQualifiedId The full qualified id for this step. Must not be <code>null</code>.
	 * @param monitor The progress monitor. Must not be <code>null</code>.
	 */
	public void cancel(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor);

	/**
	 * Called from the stepper engine once an error occurred during the stepping. Gives each step,
	 * completed previously to the error, the possibility to rollback whatever the step did.
	 * <p>
	 * <b>Note:</b> It is not guaranteed that the shared step data hasn't been overwritten in the
	 * meanwhile by multiple invocation of the same step. If a step supports multiple invocations,
	 * the implementer of the step is required to identify all the step data to rollback by himself.
	 * <p>
	 * IProgressMonitor.worked(int) should not be used for the given progress monitor. Setting sub task labels is ok.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param data The data. Must not be <code>null</code>.
	 * @param status The status of the last step executed and that caused the rollback.
	 * @param fullQualifiedId The full qualified id for this step. Must not be <code>null</code>.
	 * @param monitor The progress monitor. Must not be <code>null</code>.
	 * @param callback The callback to invoke if finished. Must not be <code>null</code>.
	 */
	public void rollback(IStepContext context, IPropertiesContainer data, IStatus status, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor, ICallback callback);

	/**
	 * Get the timeout in milliseconds to wait for finish
	 * when the progress monitor was canceled.
	 * If a step should not have a timeout and handles the cancel itself return -1.
	 * @return Timeout in milliseconds.
	 */
	public int getCancelTimeout();

	/**
	 * Returns the number of total work the step is consuming.
	 *
	 * @return The number of total work or {@link IProgressMonitor#UNKNOWN}.
	 */
	public int getTotalWork(IStepContext context, IPropertiesContainer data);

	/**
	 * Returns the list of required context step or context step group id's. The execution of a
	 * context step fails if not all of the required steps are available or have not been executed
	 * before.
	 * <p>
	 * If the listed required steps have dependencies on their own, these dependencies are
	 * implicitly inherited.
	 *
	 * @return The list of required context step or context step group id's or an empty list.
	 */
	public String[] getDependencies();

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
