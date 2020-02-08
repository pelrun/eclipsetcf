/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.interfaces;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;

/**
 * A class that implement this interface represents an file system operation,
 * which is an abstract of the action operated over files/folders.
 */
public interface IOperation {

	/**
	 * The algorithm of calculating the message digest of a file.
	 */
	public static final String MD_ALG = "MD5";  //$NON-NLS-1$

	/**
	 * Returns the name of the operation
	 */
	public String getName();

	/**
     * Runs this operation.
     */
    public IStatus run(IProgressMonitor monitor);

    /**
     * Runs the operation in a job and calls the callback after the job has been
     * completed or cancelled.
     */
	public void runInJob(ICallback callback);

    /**
     * Runs the operation in a job with user interaction set to true
     */
	public void runInUserJob(ICallback object);
}
