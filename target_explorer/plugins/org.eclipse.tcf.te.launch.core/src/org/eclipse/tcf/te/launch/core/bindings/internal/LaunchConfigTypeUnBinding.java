/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.core.bindings.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.te.launch.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.launch.core.selection.interfaces.ILaunchSelection;
import org.eclipse.tcf.te.launch.core.selection.interfaces.ISelectionContext;

/**
 * Launch configuration type unbinding implementation.
 */
public class LaunchConfigTypeUnBinding {
	// The launch configuration type id
	private final String typeId;

	// The list of enablement expressions
	private final List<Expression> expressions = new ArrayList<Expression>();

	/**
	 * Constructor.
	 *
	 * @param typeId The launch configuration type id the unbinding applies to. Must not be
	 *            <code>null</code>.
	 */
	public LaunchConfigTypeUnBinding(String typeId) {
		Assert.isNotNull(typeId);
		this.typeId = typeId;
	}

	/**
	 * Returns the launch configuration type id the unbinding applies to.
	 *
	 * @return The launch configuration type id.
	 */
	public String getTypeId() {
		return typeId;
	}

	/**
	 * Adds the given enablement expression.
	 *
	 * @param enablement The enablement expression. Must not be <code>null</code>.
	 */
	public void addEnablement(Expression expression) {
		Assert.isNotNull(expression);
		if (!expressions.contains(expression)) {
			expressions.add(expression);
		}
	}

	/**
	 * Evaluates the enablement expressions with the given launch selection.
	 *
	 * @param selection The launch selection. Must not be <code>null</code>.
	 * @return The result of the enablement expression evaluation.
	 */
	public EvaluationResult validate(ILaunchSelection selection) {
		Assert.isNotNull(selection);

		EvaluationResult result = EvaluationResult.NOT_LOADED;

		EvaluationResult valresult;
		for (ISelectionContext context : selection.getSelectedContexts()) {
			if (context.isPreferredContext()) {
				valresult = validate(selection.getLaunchMode(), context);
				if (valresult == EvaluationResult.FALSE) {
					return EvaluationResult.FALSE;
				}
				else if (valresult != EvaluationResult.NOT_LOADED) {
					result = valresult;
				}
			}
		}
		return result;
	}

	/**
	 * Evaluates the enablement expressions with the given launch mode and selection context.
	 *
	 * @param mode The launch mode. Must not be <code>null</code>.
	 * @param context The launch selection context or <code>null</code>.
	 *
	 * @return The result of the enablement expression evaluation.
	 */
	public EvaluationResult validate(String mode, ISelectionContext context) {
		EvaluationResult result = context.isPreferredContext() ? EvaluationResult.FALSE : EvaluationResult.NOT_LOADED;

		if (expressions.isEmpty()) {
			return EvaluationResult.TRUE;
		}
		EvaluationResult valresult;
		for (Expression expression : expressions) {
			// Set the default variable and "selection" is the selection context
			EvaluationContext evalContext = new EvaluationContext(null, context);
			evalContext.addVariable("context", context.getContext()); //$NON-NLS-1$
			evalContext.addVariable("selection", context.getSelections() != null ? Arrays.asList(context.getSelections()) : Collections.EMPTY_LIST); //$NON-NLS-1$
			evalContext.addVariable("type", context.getType() != null ? context.getType() : ""); //$NON-NLS-1$ //$NON-NLS-2$
			evalContext.addVariable("mode", mode != null ? mode : ""); //$NON-NLS-1$ //$NON-NLS-2$
			// Allow plugin activation
			evalContext.setAllowPluginActivation(true);
			// Evaluate the expression
			try {
				valresult = expression.evaluate(evalContext);
			} catch (CoreException e) {
				valresult = EvaluationResult.FALSE;

				if (Platform.inDebugMode()) {
					IStatus status = new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), e.getLocalizedMessage(), e);
					Platform.getLog(CoreBundleActivator.getContext().getBundle()).log(status);
				}
			}

			if (valresult == EvaluationResult.TRUE) {
				return EvaluationResult.TRUE;
			}
			if (valresult != EvaluationResult.NOT_LOADED) {
				result = valresult;
			}
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer toString = new StringBuffer();

		toString.append("LaunchConfigTypeUnBinding("); //$NON-NLS-1$
		toString.append(typeId);
		toString.append(")"); //$NON-NLS-1$

		return toString.toString();
	}
}
