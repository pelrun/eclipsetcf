/*******************************************************************************
 * Copyright (c) 2010, 2015 Mentor Graphics Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Anna Dushistova (Mentor Graphics) - initial API and implementation
 * Anna Dushistova (Mentor Graphics) - moved to org.eclipse.cdt.launch.remote.launching
 * Anna Dushistova (MontaVista)      - [318051][remote debug] Terminating when "Remote shell" process is selected doesn't work
 * Anna Dushistova (MontaVista)      - [368597][remote debug] if gdbserver fails to launch on target, launch doesn't get terminated
 * Anna Dushistova (MontaVista)      - adapted from RemoteGdbLaunchDelegate
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.launch.cdt.launching;

/**
 * Launch and debug a remote application. The necessary gdbserver is launched via TCF/TE.
 */
public class TEGdbLaunchDelegate extends TEGdbAbstractLaunchDelegate {


}
