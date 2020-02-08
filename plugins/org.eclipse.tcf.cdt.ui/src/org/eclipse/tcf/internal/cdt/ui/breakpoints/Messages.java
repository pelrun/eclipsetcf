/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui.breakpoints;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tcf.internal.cdt.ui.breakpoints.messages"; //$NON-NLS-1$

    public static String TCFThreadFilterQueryExpressionStore;
    public static String TCFThreadFilterQueryButtonEdit;
    public static String TCFThreadFilterQueryAdvancedLabel;
    public static String TCFThreadFilterQueryTreeViewLabel;
    public static String TCFThreadFilterEditor_cannotEditExpr;

    public static String TCFThreadFilterEditor_cannotRetrieveAttrs;

    public static String TCFThreadFilterEditor_cannotValidate;

    public static String TCFThreadFilterEditor_defaultScopePrefsLink;

    public static String TCFThreadFilterEditorFormatError;
    public static String TCFThreadFilterEditorUnbalancedParameters;
    public static String TCFThreadFilterEditorNoOpenChannel;
    public static String TCFBreakpointPreferencesEnableDefaultTriggerScope;
    public static String TCFBreakpointPreferencesTriggerScopeExpression;
    public static String TCFBreakpointPreferencesEnableLineOffsetLimit;
    public static String TCFBreakpointPreferencesLineOffsetLimit;
    public static String TCFBreakpointPreferencesDescription;
    public static String TCFBreakpointToggle;
    public static String TCFBreakpointPrefrencesError;
    public static String BreakpointScopeCategory_filter_label;
    public static String BreakpointScopeCategory_contexts_label;
    public static String BreakpointScopeCategory_filter_and_contexts_label;
    public static String BreakpointScopeCategory_global_label;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
