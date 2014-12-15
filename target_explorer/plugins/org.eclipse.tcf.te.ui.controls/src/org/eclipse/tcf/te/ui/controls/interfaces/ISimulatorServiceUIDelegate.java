/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.controls.interfaces;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService;
import org.eclipse.tcf.te.ui.controls.BaseControl;
import org.eclipse.tcf.te.ui.controls.BaseWizardConfigurationPanelControl;

/**
 * Simulator service UI delegate.
 */
public interface ISimulatorServiceUIDelegate {

	public static final String PROP_NAME_NEW = "NameNew"; //$NON-NLS-1$
	public static final String PROP_MODES = "Modes"; //$NON-NLS-1$
	public static final String PROP_MODE_LABEL_X = "ModeLabel"; //$NON-NLS-1$
	public static final String PROP_MODE_DESCRIPTION_X = "ModeDescription"; //$NON-NLS-1$
	public static final String PROP_BSPS = "BSPs"; //$NON-NLS-1$

	/**
	 * Get the simulator service the UI delegate is associated with.
	 *
	 * @return The simulator service.
	 */
	public ISimulatorService getService();

	/**
	 * Get the name of the simulator service to identify the simulator (type)
	 * to the user in the UI.
	 *
	 * @return The simulator service name.
	 */
	public String getName();

	/**
	 * Get the image for the simulator service.
	 * @return The simulator image.
	 */
	public Image getImage();

	/**
	 * Get a description fo rthe given config.
	 * This description is shown i.e. as tooltip of the configure button.
     * @param context The context for which the simulator should be configured.
     * @param config The configuration or <code>null</code>.
	 * @return The description of the given config.
	 */
	public String getDescription(Object context, String config);

	/**
	 * Get properties for ui configuration.
	 * @param context The conetxt.
	 * @param config The current config.
	 * @return The properties for ui configuartion
	 */
	public IPropertiesContainer getProperties(Object context, String config);

	/**
	 * Get the configuration panel for the given context and mode.
	 * @param context The context to configure.
	 * @param parentControl The parent control.
	 * @param mode The connection mode.
	 * @return
	 */
	public IWizardConfigurationPanel getConfigPanel(Object context, BaseWizardConfigurationPanelControl parentControl, String mode);

	/**
	 * Check if a project can be used to get the kernel image.
	 * @param project The project to check.
	 * @return
	 */
	public boolean isValidProjectForKernelImage(IProject project);

	/**
	 * Check if a projects build target path is a valid kernel image.
	 * @param path The path to check.
	 * @return
	 */
	public boolean isValidBuildTargetPathForKernelImage(IPath path);

	/**
	 * Do additional validation for a given valid kernel image path.
	 * I.e. check for further needed files and set message to the messageProvider
	 * @param path
	 * @param messageProvider
	 * @return
	 */
	public boolean validateKernelImage(IPath path, BaseControl messageProvider);
}
