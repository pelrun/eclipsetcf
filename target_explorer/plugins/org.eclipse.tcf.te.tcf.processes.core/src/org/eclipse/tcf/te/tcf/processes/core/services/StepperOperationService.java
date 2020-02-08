/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.processes.core.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.services.AbstractStepperOperationService;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.IProcessContextItem;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.IProcessesDataProperties;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.services.IStepGroupIds;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.services.IStepperServiceOperations;
import org.eclipse.tcf.te.tcf.processes.core.util.ProcessDataHelper;

/**
 * Processes stepper operation service implementation.
 */
public class StepperOperationService extends AbstractStepperOperationService {
	/**
	 * Constructor.
	 */
	public StepperOperationService() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.services.StepperOperationService#isHandledOperation(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isHandledOperation(Object context, String operation) {
		return IStepperServiceOperations.ATTACH.equals(operation) ||
						IStepperServiceOperations.DETACH.equals(operation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.services.StepperOperationService#addToActionHistory(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean addToActionHistory(Object context, String operation) {
		return IStepperServiceOperations.ATTACH.equals(operation) ||
						IStepperServiceOperations.DETACH.equals(operation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.services.AbstractStepperOperationService#getSpecialHistoryData(java.lang.Object, java.lang.String, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public IPropertiesContainer getSpecialHistoryData(Object context, String operation, IPropertiesContainer data) {
		String encoded = data.getStringProperty(IProcessesDataProperties.PROPERTY_CONTEXT_LIST);
		IProcessContextItem[] items = ProcessDataHelper.decodeProcessContextItems(encoded);
		for (IProcessContextItem item : items) {
	        item.setProperty(IProcessContextItem.PROPERTY_ID, null);
        }

		IPropertiesContainer histData = new PropertiesContainer();
		histData.setProperties(data.getProperties());
		// sort the data
		Arrays.sort(items);
		// remove duplicate items
		List<IProcessContextItem> histItems = new ArrayList<IProcessContextItem>();
		int i=0;
		while (i < items.length) {
			if (i == items.length-1) {
				histItems.add(items[i]);
			}
			else if (!items[i].equals(items[i+1])) {
				histItems.add(items[i]);
			}
			i++;
        }
		histData.setProperty(IProcessesDataProperties.PROPERTY_CONTEXT_LIST, ProcessDataHelper.encodeProcessContextItems(histItems.toArray(new IProcessContextItem[histItems.size()])));
	    return histData;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.services.StepperOperationService#getStepGroupId(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getStepGroupId(Object context, String operation) {
		if (IStepperServiceOperations.ATTACH.equals(operation)) {
			return IStepGroupIds.ATTACH;
		}
		if (IStepperServiceOperations.DETACH.equals(operation)) {
			return IStepGroupIds.DETACH;
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.services.StepperOperationService#getStepGroupName(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getStepGroupName(Object context, String operation) {
		if (IStepperServiceOperations.ATTACH.equals(operation)) {
			return "Attach"; //$NON-NLS-1$
		}
		if (IStepperServiceOperations.DETACH.equals(operation)) {
			return "Detach"; //$NON-NLS-1$
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#isEnabled(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isEnabled(Object context, String operation) {
		return IStepperServiceOperations.ATTACH.equals(operation) ||
						IStepperServiceOperations.DETACH.equals(operation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.services.StepperOperationService#isCancelable(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isCancelable(Object context, String operation) {
		return IStepperServiceOperations.ATTACH.equals(operation) ||
						IStepperServiceOperations.DETACH.equals(operation);
	}
}
