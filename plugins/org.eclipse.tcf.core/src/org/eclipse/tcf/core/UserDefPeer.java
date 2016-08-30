/*******************************************************************************
 * Copyright (c) 2016 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.core;

import java.util.Map;

/**
 * The class represents manually configured (user defined) TCF peers (targets).
 * Unlike auto-discovered peers, manually configured ones are persistent -
 * they exist until explicitly deleted by user.
 *
 * User defined peers info is not broadcasted over TCF discovery protocol.
 */
public class UserDefPeer extends AbstractPeer {

    public UserDefPeer(Map<String, String> attrs) {
        super(attrs);
    }
}
