/*******************************************************************************
 * Copyright (c) 2013 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui;

import java.math.BigInteger;

import org.eclipse.cdt.debug.internal.ui.disassembly.dsf.AddressRangePosition;
import org.eclipse.cdt.dsf.debug.internal.ui.disassembly.DisassemblyPart;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.tcf.internal.debug.ui.model.ITCFDisassemblyPart;

@SuppressWarnings({ "rawtypes", "restriction" })
public class DisassemblyPartAdapterFactory implements IAdapterFactory {

    private static final Class<?>[] CLASSES = {
        ITCFDisassemblyPart.class,
    };

    public Object getAdapter(Object obj, Class type) {
        if (obj instanceof DisassemblyPart) {
            final DisassemblyPart dpart = (DisassemblyPart)obj;
            if (type == ITCFDisassemblyPart.class) {
                return new ITCFDisassemblyPart() {
                    @Override
                    public IAnnotationModel getAnnotationModel() {
                        ISourceViewer viewer = dpart.getTextViewer();
                        if (viewer == null) return null;
                        return viewer.getAnnotationModel();
                    }

                    @Override
                    public Position getAddressPosition(BigInteger addr) {
                        AddressRangePosition pos = dpart.getPositionOfAddress(addr);
                        if (pos != null && pos.fValid) return new Position(pos.offset, Math.max(0, pos.length-1));
                        return null;
                    }
                };
            }
        }
        return null;
    }

    public Class[] getAdapterList() {
        return CLASSES;
    }
}
