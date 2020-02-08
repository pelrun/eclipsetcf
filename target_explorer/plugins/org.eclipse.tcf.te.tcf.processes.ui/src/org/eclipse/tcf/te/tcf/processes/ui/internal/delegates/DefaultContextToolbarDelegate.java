/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.internal.delegates;

import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.persistence.utils.DataHelper;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService;
import org.eclipse.tcf.te.runtime.stepper.utils.StepperHelper;
import org.eclipse.tcf.te.tcf.core.interfaces.IContextDataProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.utils.PeerNodeDataHelper;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.IProcessContextItem;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.IProcessesDataProperties;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.services.IStepGroupIds;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.services.IStepperServiceOperations;
import org.eclipse.tcf.te.tcf.processes.core.util.ProcessDataHelper;
import org.eclipse.tcf.te.tcf.processes.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.processes.ui.internal.ImageConsts;
import org.eclipse.tcf.te.tcf.processes.ui.internal.dialogs.AttachContextSelectionDialog;
import org.eclipse.tcf.te.tcf.processes.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.ui.delegates.AbstractDefaultContextToolbarDelegate;
import org.eclipse.tcf.te.ui.interfaces.IDataExchangeDialog;

public class DefaultContextToolbarDelegate extends AbstractDefaultContextToolbarDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.delegates.AbstractDefaultContextToolbarDelegate#getHandledStepGroupIds(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode)
	 */
	@Override
	public String[] getHandledStepGroupIds(IPeerNode peerNode) {
		return new String[]{IStepGroupIds.ATTACH, IStepGroupIds.DETACH};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.delegates.AbstractDefaultContextToolbarDelegate#getLabel(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, java.lang.String)
	 */
	@Override
	public String getLabel(IPeerNode peerNode, String entry) {
		IPropertiesContainer data = DataHelper.decodePropertiesContainer(entry);
		String stepGroupId = getStepGroupId(entry);
		if (IStepGroupIds.ATTACH.equals(stepGroupId)) {
			IProcessContextItem[] items = ProcessDataHelper.decodeProcessContextItems(data.getStringProperty(IProcessesDataProperties.PROPERTY_CONTEXT_LIST));
			String contexts = null;
			int count = 0;
			for (IProcessContextItem item : items) {
				if (contexts == null) {
					contexts = ""; //$NON-NLS-1$
				}
				else {
					contexts += ","; //$NON-NLS-1$
				}
				if (count >= 2) {
					contexts += ".."; //$NON-NLS-1$
					break;
				}
				contexts += item.getName();
			}
			return NLS.bind(Messages.DefaultContextToolbarDelegate_attachContext_label, contexts);
		}
		return super.getLabel(peerNode, entry);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.delegates.AbstractDefaultContextToolbarDelegate#getDescription(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, java.lang.String)
	 */
	@Override
	public String getDescription(IPeerNode peerNode, String entry) {
		IPropertiesContainer data = DataHelper.decodePropertiesContainer(entry);
		String stepGroupId = getStepGroupId(entry);
		if (IStepGroupIds.ATTACH.equals(stepGroupId)) {
			IProcessContextItem[] items = ProcessDataHelper.decodeProcessContextItems(data.getStringProperty(IProcessesDataProperties.PROPERTY_CONTEXT_LIST));
			String contexts = null;
			for (IProcessContextItem item : items) {
				if (contexts == null) {
					contexts = "\t"; //$NON-NLS-1$
				}
				else {
					contexts += "\n\t"; //$NON-NLS-1$
				}
				String path = item.getPath();
				if (path != null && path.length() > 0) {
					contexts += path + IProcessContextItem.PATH_SEPARATOR;
				}
				contexts += item.getName();
			}
			return NLS.bind(Messages.DefaultContextToolbarDelegate_attachContext_description, contexts);
		}
	    return super.getDescription(peerNode, entry);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.delegates.AbstractDefaultContextToolbarDelegate#getImage(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, java.lang.String)
	 */
	@Override
	public Image getImage(IPeerNode peerNode, String entry) {
		String stepGroupId = getStepGroupId(entry);
		if (IStepGroupIds.ATTACH.equals(stepGroupId)) {
			return UIPlugin.getImage(ImageConsts.ATTACH);
		}
		if (IStepGroupIds.DETACH.equals(stepGroupId)) {
			return UIPlugin.getImage(ImageConsts.DETACH);
		}
		return super.getImage(peerNode, entry);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.delegates.AbstractDefaultContextToolbarDelegate#execute(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, java.lang.String, boolean)
	 */
	@Override
	public String execute(IPeerNode peerNode, String entry, boolean showDialog) {
		String operation = null;
		String stepGroupId = getStepGroupId(entry);
		if (IStepGroupIds.ATTACH.equals(stepGroupId)) {
			operation = IStepperServiceOperations.ATTACH;
		}
		if (IStepGroupIds.DETACH.equals(stepGroupId)) {
			operation = IStepperServiceOperations.DETACH;
		}
		IPropertiesContainer data = DataHelper.decodePropertiesContainer(entry);
		IStepperOperationService stepperOperationService = StepperHelper.getService(peerNode, operation);
		if (stepperOperationService != null) {
			if (!showDialog) {
				showDialog |= !stepperOperationService.validateStepData(peerNode, operation, data);
			}
		}
		else {
			return null;
		}

		if (showDialog) {
			IDataExchangeDialog dialog = null;
			if (IStepGroupIds.ATTACH.equals(stepGroupId)) {
				dialog = new AttachContextSelectionDialog(Display.getDefault().getActiveShell(), null);
			}
			if (dialog != null) {
				data.setProperty(IContextDataProperties.PROPERTY_CONTEXT_LIST, PeerNodeDataHelper.encodeContextList(new IPeerNode[]{peerNode}));
				dialog.setupData(data);
				if (dialog.open() == Window.OK) {
					data = new PropertiesContainer();
					dialog.extractData(data);
					data.setProperty(IContextDataProperties.PROPERTY_CONTEXT_LIST, null);
				}
				else {
					return null;
				}
			}
		}

		StepperHelper.scheduleStepperJob(peerNode, operation, stepperOperationService, data, null, null);
		return DataHelper.encodePropertiesContainer(data);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.delegates.AbstractDefaultContextToolbarDelegate#validate(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, java.lang.String)
	 */
	@Override
	public boolean validate(IPeerNode peerNode, String entry) {
		String operation = null;
		String stepGroupId = getStepGroupId(entry);
		if (IStepGroupIds.ATTACH.equals(stepGroupId)) {
			operation = IStepperServiceOperations.ATTACH;
		}
		if (IStepGroupIds.DETACH.equals(stepGroupId)) {
			operation = IStepperServiceOperations.DETACH;
		}
		if (operation != null) {
			IPropertiesContainer data = DataHelper.decodePropertiesContainer(entry);
			IStepperOperationService stepperOperationService = StepperHelper.getService(peerNode, operation);
			if (stepperOperationService != null) {
				return stepperOperationService.validateStepData(peerNode, operation, data);
			}
		}
		return super.validate(peerNode, entry);
	}
}
