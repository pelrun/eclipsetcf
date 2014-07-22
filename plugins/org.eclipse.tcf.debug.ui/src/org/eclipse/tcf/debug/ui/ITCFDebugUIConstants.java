/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.debug.ui;

public interface ITCFDebugUIConstants {

    /**
     * View ID for a view that shows contexts filtered according to view properties.
     */
    final static String ID_CONTEXT_QUERY_VIEW = "org.eclipse.tcf.debug.ui.ContextQueryView";

    /**
     * Property of a view presentation context which holds a query string, that is
     * used to select a sub-set of contexts.
     */
    final static String PROP_CONTEXT_QUERY = "query";

    /**
     * Property of a view presentation context which holds a set of strings,
     * that are used to select a sub-set of contexts.
     */
    final static String PROP_FILTER_CONTEXTS = "contexts";
}
