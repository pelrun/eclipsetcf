/*******************************************************************************
 * Copyright (c) 2013 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import java.math.BigInteger;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;

public interface ITCFDisassemblyPart {

    IAnnotationModel getAnnotationModel();
    Position getAddressPosition(BigInteger addr);
}
