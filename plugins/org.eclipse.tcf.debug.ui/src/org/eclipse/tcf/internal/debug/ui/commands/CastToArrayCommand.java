/*******************************************************************************
 * Copyright (c) 2009-2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.commands;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.internal.debug.ui.model.ICastToType;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExpression;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ISymbols;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;
import org.eclipse.ui.IWorkbenchWindow;

public class CastToArrayCommand extends AbstractActionDelegate {

    private String base_type_name;
    private String cur_length;
    private TCFNode cur_node;

    private static class CastToTypeInputValidator implements IInputValidator {

        public String isValid(String new_text) {
            try {
                if (new_text.length() == 0) return "";
                int i = Integer.parseInt(new_text);
                if (i < 1) return "Array length must be >= 1";
            }
            catch (Exception x) {
                return "Invalid number";
            }
            return null;
        }
    }

    private static class CastToTypeDialog extends InputDialog {

        public CastToTypeDialog(Shell shell, String initial_value) {
            super(shell, "Cast To Array", "Enter array length",
                    initial_value, new CastToTypeInputValidator() );
        }

        @Override
        protected void configureShell(Shell shell) {
            super.configureShell(shell);
            shell.setImage(ImageCache.getImage(ImageCache.IMG_TCF));
        }
    }

    private void getBaseTypeName() {
        base_type_name = null;
        cur_length = null;
        cur_node = getSelectedNode();
        if (cur_node == null) return;
        if (!(cur_node instanceof ICastToType)) return;
        if (cur_node instanceof TCFNodeExpression && !((TCFNodeExpression)cur_node).isEnabled()) return;
        try {
            new TCFTask<Boolean>(cur_node.getChannel()) {
                @Override
                public void run() {
                    String cast = cur_node.getModel().getCastToType(cur_node.getID());
                    if (cast != null) {
                        if (cast.endsWith("]")) {
                            int i = cast.lastIndexOf('[');
                            if (i > 0) {
                                base_type_name = cast.substring(0, i);
                                cur_length = cast.substring(i + 1, cast.length() - 1);
                                done(Boolean.TRUE);
                                return;
                            }
                        }
                        done(Boolean.FALSE);
                        return;
                    }
                    TCFDataCache<ISymbols.Symbol> type_cache = ((ICastToType)cur_node).getType();
                    if (!type_cache.validate(this)) return;
                    ISymbols.Symbol type_data = type_cache.getData();
                    if (type_data == null || type_data.getTypeClass() != ISymbols.TypeClass.pointer) {
                        done(Boolean.FALSE);
                    }
                    else {
                        for (int i = 0;; i++) {
                            TCFDataCache<ISymbols.Symbol> base_type_cache = cur_node.getModel().getSymbolInfoCache(type_data.getBaseTypeID());
                            if (!base_type_cache.validate(this)) return;
                            ISymbols.Symbol base_type_data = base_type_cache.getData();
                            if (base_type_data == null) {
                                done(Boolean.FALSE);
                                return;
                            }
                            else {
                                if (base_type_data.getName() != null) {
                                    base_type_name = makePtrTypeName(base_type_data.getName(), i);
                                    done(Boolean.TRUE);
                                    return;
                                }
                                else if (base_type_data.getTypeClass() == ISymbols.TypeClass.pointer) {
                                    type_data = base_type_data;
                                }
                                else if (!base_type_data.getID().equals(base_type_data.getTypeID())) {
                                    // modified type without name, like "volatile int"
                                    base_type_cache = cur_node.getModel().getSymbolInfoCache(base_type_data.getTypeID());
                                    if (!base_type_cache.validate(this)) return;
                                    base_type_data = base_type_cache.getData();
                                    if (base_type_data != null && base_type_data.getName() != null) {
                                        base_type_name = makePtrTypeName(base_type_data.getName(), i);
                                        done(Boolean.TRUE);
                                    }
                                    else {
                                        done(Boolean.FALSE);
                                    }
                                    return;
                                }
                                else {
                                    done(Boolean.FALSE);
                                    return;
                                }
                            }
                        }
                    }
                }
                private String makePtrTypeName(String base, int cnt) {
                    StringBuffer bf = new StringBuffer();
                    bf.append(base);
                    if (cnt > 0) {
                        bf.append(' ');
                        while (cnt > 0) {
                            bf.append('*');
                            cnt--;
                        }
                    }
                    return bf.toString();
                }
            }.get();
        }
        catch (Throwable x) {
            Activator.log(x);
        }
    }

    @Override
    protected void run() {
        IWorkbenchWindow window = Activator.getDefault().getWorkbench().getActiveWorkbenchWindow();
        if (window == null) return;
        getBaseTypeName();
        if (base_type_name == null) return;
        CastToTypeDialog dialog = new CastToTypeDialog(window.getShell(), cur_length);
        if (dialog.open() != Window.OK) return;
        final String new_type = dialog.getValue().trim();
        Protocol.invokeLater(new Runnable() {
            public void run() {
                cur_node.getModel().setCastToType(cur_node.getID(), base_type_name + "[" + new_type + "]");
            }
        });
    }

    @Override
    protected void selectionChanged() {
        getBaseTypeName();
        setEnabled(base_type_name != null);
    }
}
