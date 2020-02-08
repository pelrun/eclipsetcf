/*******************************************************************************
 * Copyright (c) 2016 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
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
 * @since 1.5
 */
public class UserDefPeer extends AbstractPeer {

    public UserDefPeer(Map<String, String> attrs) {
        super(attrs);
    }
}
