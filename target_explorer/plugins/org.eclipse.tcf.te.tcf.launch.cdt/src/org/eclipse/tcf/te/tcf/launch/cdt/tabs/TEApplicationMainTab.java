/*******************************************************************************
 * Copyright (c) 2006, 2015 PalmSource, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Ewa Matejska          (PalmSource) - initial API and implementation
 * Martin Oberhuber      (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 * Martin Oberhuber      (Wind River) - [196934] hide disabled system types in remotecdt combo
 * Yu-Fen Kuo            (MontaVista) - [190613] Fix NPE in Remotecdt when RSEUIPlugin has not been loaded
 * Martin Oberhuber      (Wind River) - [cleanup] Avoid using SystemStartHere in production code
 * Johann Draschwandtner (Wind River) - [231827][remotecdt]Auto-compute default for Remote path
 * Johann Draschwandtner (Wind River) - [233057][remotecdt]Fix button enablement
 * Anna Dushistova       (MontaVista) - [181517][usability] Specify commands to be run before remote application launch
 * Anna Dushistova       (MontaVista) - [223728] [remotecdt] connection combo is not populated until RSE is activated
 * Anna Dushistova       (MontaVista) - [267951] [remotecdt] Support systemTypes without files subsystem
 * Anna Dushistova  (Mentor Graphics) - adapted from RemoteCMainTab
 * Anna Dushistova  (Mentor Graphics) - moved to org.eclipse.cdt.launch.remote.tabs
 * Anna Dushistova  (Mentor Graphics) - [318052] [remote launch] Properties are not saved/used
 * Anna Dushistova       (MontaVista) - [375067] [remote] Automated remote launch does not support project-less debug
 * Anna Dushistova       (MontaVista) - adapted from RemoteCDSFMainTab
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.cdt.tabs;


public class TEApplicationMainTab extends TEAbstractMainTab {

	/**
     * Constructor
     */
	public TEApplicationMainTab() {
		super(0);
	}

	@Override
	public String getId() {
		return "org.eclipse.tcf.te.remotecdt.debug.mainTab"; //$NON-NLS-1$
	}


}