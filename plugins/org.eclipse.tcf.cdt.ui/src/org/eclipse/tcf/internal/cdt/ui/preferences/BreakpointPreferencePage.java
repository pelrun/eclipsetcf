/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui.preferences;

import java.util.Set;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.tcf.internal.cdt.ui.Activator;
import org.eclipse.tcf.internal.cdt.ui.breakpoints.Messages;
import org.eclipse.tcf.internal.cdt.ui.breakpoints.TCFThreadFilterEditor;
import org.eclipse.tcf.internal.cdt.ui.breakpoints.TCFToggleBreakpointsTargetFactory;

public class BreakpointPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String PLUGIN_ID="org.eclipse.tcf.cdt.ui.preferences.BreakpointPreferencePage";

    private BooleanFieldEditor context_enabled;
    private ComboFieldEditor context_expression;

    private BooleanFieldEditor offset_enabled;
    private IntegerFieldEditor offset_limit;

    public BreakpointPreferencePage() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription(Messages.TCFBreakpointPreferencesDescription);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);
        Object source = event.getSource();
        if (source == context_enabled) {
            context_expression.setEnabled(
                    context_enabled.getBooleanValue(),
                    getFieldEditorParent());
        }
        if (source == offset_enabled) {
            offset_limit.setEnabled(
                    offset_enabled.getBooleanValue(),
                    getFieldEditorParent());
        }
    }

    public void createFieldEditors() {
        context_enabled = new BooleanFieldEditor(
                PreferenceConstants.PREF_DEFAULT_TRIGGER_SCOPE_ENABLED,
                Messages.TCFBreakpointPreferencesEnableDefaultTriggerScope,
                getFieldEditorParent());

        addField(context_enabled);

        String [] expressions = getTriggerExpressions();
        context_expression = new ComboFieldEditor(
                PreferenceConstants.PREF_DEFAULT_TRIGGER_SCOPE,
                Messages.TCFBreakpointPreferencesTriggerScopeExpression,
                joinToArray2D(expressions,expressions),
                getFieldEditorParent());

        addField(context_expression);

        offset_enabled = new BooleanFieldEditor(
                PreferenceConstants.PREF_LINE_OFFSET_LIMIT_ENABLED,
                Messages.TCFBreakpointPreferencesEnableLineOffsetLimit,
                getFieldEditorParent());

        addField(offset_enabled);

        offset_limit = new IntegerFieldEditor(
                PreferenceConstants.PREF_LINE_OFFSET_LIMIT,
                Messages.TCFBreakpointPreferencesLineOffsetLimit,
                getFieldEditorParent());

        addField(offset_limit);

        if (!checkTCFToggleBreakpointAdapter()) {
            context_enabled.setEnabled(false, getFieldEditorParent());
            context_expression.setEnabled(false, getFieldEditorParent());
            offset_enabled.setEnabled(false, getFieldEditorParent());
            offset_limit.setEnabled(false, getFieldEditorParent());
            setMessage(Messages.TCFBreakpointPrefrencesError, WARNING);
            setValid(false);
        }
        else {
            setErrorMessage(null);
            setValid(true);
        }
    }

    protected void checkState() {
        context_expression.setEnabled(
                context_enabled.getBooleanValue(),
                getFieldEditorParent());

        offset_limit.setEnabled(
                offset_enabled.getBooleanValue(),
                getFieldEditorParent());

        super.checkState();
    }

    public void init(IWorkbench workbench) {
    }

    private String[] getTriggerExpressions() {
        IDialogSettings dialogSettings = getBreakpointScopeDialogSettings();
        if (dialogSettings == null) return new String[0];

        String[] returnList = null;
        String[] expressionList = null;
        int index = 0;

        expressionList = dialogSettings.getArray(Messages.TCFThreadFilterQueryExpressionStore);
        // Find if there is a null entry.
        if ( expressionList != null ) {
            for(index = 0; index < expressionList.length; index++) {
                String member = expressionList[index];
                if (member == null || member.length() == 0) {
                    break;
                }
            }
        }
        returnList = new String[index + 1];
        returnList[0] = "";
        for (int loop = 0; loop < index; loop++) {
            returnList[loop+1] = expressionList[loop];
        }

        return returnList;
    }

    private IDialogSettings getBreakpointScopeDialogSettings() {
        String component = TCFThreadFilterEditor.PLUGIN_ID;
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        return settings.getSection(component);
    }

    private String[][] joinToArray2D(String[] labels, String[] values) {
        String[][] array2d = new String[labels.length][];
        for (int i = 0; i < labels.length; i++) {
            array2d[i] = new String[2];
            array2d[i][0] = labels[i];
            array2d[i][1] = values[i];
        }
        return array2d;
    }

    private boolean checkTCFToggleBreakpointAdapter() {
        IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        if (part == null) {
            part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
        }
        if (part != null) {
            ISelectionProvider provider = part.getSite().getSelectionProvider();
            if (provider != null) {
                ISelection selection = provider.getSelection();
                Set<?> enablers = DebugUITools.getToggleBreakpointsTargetManager().getEnabledToggleBreakpointsTargetIDs(part, selection);

                if (enablers != null &&
                    !enablers.contains(TCFToggleBreakpointsTargetFactory.ID_TCF_BREAKPOINT_TOGGLE_TARGET)) {
                    return true;
                }

                String preferred = DebugUITools.getToggleBreakpointsTargetManager().getPreferredToggleBreakpointsTargetID(part, selection);
                if (preferred != null &&
                    !preferred.equals(TCFToggleBreakpointsTargetFactory.ID_TCF_BREAKPOINT_TOGGLE_TARGET)) {
                    return false;
                }
            }
        }
        return true;
    }
}
