/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.stepper.interfaces;

import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;

/**
 * Stepper operation service.
 */
public interface IStepperOperationService extends IService {

	/**
	 * Checks if this service can handle the given operation.
	 * @param context The context. Must not be <code>null</code>.
	 * @param operation The operation. Must not be <code>null</code>.
	 * @return <code>true</code> if this service handles the given operation.
	 */
	public boolean isHandledOperation(Object context, String operation);

	/**
	 * Get the step group id for the given context and operation
	 * or <code>null</code> if this operation is not available.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param operation The operation. Must not be <code>null</code>.
	 * @return The step group id or <code>null</code>.
	 */
	public String getStepGroupId(Object context, String operation);


	/**
	 * Get the step group name for the given context and operation
	 * or <code>null</code> if this operation is not available.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param operation The operation. Must not be <code>null</code>.
	 * @return The step group name or <code>null</code>.
	 */
	public String getStepGroupName(Object context, String operation);

	/**
	 * Get the step context for the given context and operation
	 * or <code>null</code> if this operation is not available.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param operation The operation. Must not be <code>null</code>.
	 * @return The step context or <code>null</code>.
	 */
	public IStepContext getStepContext(Object context, String operation);

	/**
	 * Get the enabled state for the given operation.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param operation The operation. Must not be <code>null</code>.
	 * @return <code>true</code> if the operation is enabled.
	 */
	public boolean isEnabled(Object context, String operation);

	/**
	 * Returns <code>true</code> if the given operation can be canceled.
	 * @param context The context. Must not be <code>null</code>.
	 * @param operation The operation. Must not be <code>null</code>.
	 * @return <code>true</code> if the operation can be canceled.
	 */
	public boolean isCancelable(Object context, String operation);

	/**
	 * Returns <code>true</code> if this stepper run should be added to the action history.
	 * @param context The context. Must not be <code>null</code>.
	 * @param operation The operation. Must not be <code>null</code>.
	 * @return <code>true</code> if the stepper run should be added to the action history.
	 */
	public boolean addToActionHistory(Object context, String operation);

	/**
	 * Returns special history data if for example some values should not be persisted
	 * (like ids that are only valid in the current session).
	 * @param context The context. Must not be <code>null</code>.
	 * @param operation The operation. Must not be <code>null</code>.
	 * @param data The step data to be validated.
	 * @return Special history data or <code>null</code> if the original data should be used.
	 */
	public IPropertiesContainer getSpecialHistoryData(Object context, String operation, IPropertiesContainer data);

	/**
	 * Validates the step data to be used for the given context and operation.
	 * @param context The context. Must not be <code>null</code>.
	 * @param operation The operation. Must not be <code>null</code>.
	 * @param data The step data to be validated.
	 * @return <code>true</code> if the step data is valid.
	 */
	public boolean validateStepData(Object context, String operation, IPropertiesContainer data);

	/**
	 * Get the stepper data to be used for the given context, operation and user data.
	 * @param context The context. Must not be <code>null</code>.
	 * @param operation The operation. Must not be <code>null</code>.
	 * @param data The step data to be validated.
	 * @return Stepper data to be useed.
	 */
	public IPropertiesContainer getStepGroupData(Object context, String operation, IPropertiesContainer data);

}
