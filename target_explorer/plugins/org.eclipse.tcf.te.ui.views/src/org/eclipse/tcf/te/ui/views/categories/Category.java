/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.categories;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.runtime.extensions.ExecutableExtension;
import org.eclipse.tcf.te.runtime.interfaces.IDisposable;
import org.eclipse.tcf.te.ui.views.Managers;
import org.eclipse.tcf.te.ui.views.activator.UIPlugin;
import org.eclipse.tcf.te.ui.views.interfaces.ICategory;
import org.eclipse.tcf.te.ui.views.interfaces.categories.ICategorizable;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Default category implementation.
 */
public class Category extends ExecutableExtension implements ICategory, IDisposable, IPersistableElement {
	// The category image / image descriptor
	private ImageDescriptor descriptor = null;
	private Image image = null;
	// The sorting rank
	private int rank = -1;
	// The converted expression
	private Expression expression;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.extensions.ExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		super.setInitializationData(config, propertyName, data);

		// Read the icon attribute and create the image
		String attrIcon = config.getAttribute("icon");//$NON-NLS-1$
		if (attrIcon != null) {
			descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(config.getNamespaceIdentifier(), attrIcon);
			if (descriptor != null) {
				image = JFaceResources.getResources().createImageWithDefault(descriptor);
			}
		}

		// Read the rank attribute
		String attrRank = config.getAttribute("rank"); //$NON-NLS-1$
		if (attrRank != null) {
			try {
				rank = Integer.valueOf(attrRank).intValue();
			} catch (NumberFormatException e) { /* ignored on purpose */ }
		}

		// Read the "enablement" sub element of the extension
		IConfigurationElement[] children = config.getChildren("enablement"); //$NON-NLS-1$
		// Only one "enablement" element is expected
		if (children != null && children.length > 0) {
			expression = ExpressionConverter.getDefault().perform(children[0]);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if(adapter == IPersistableElement.class) {
			return this;
		}
		return super.getAdapter(adapter);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IPersistable#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento memento) {
		memento.putString("id", this.getId()); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IPersistableElement#getFactoryId()
	 */
	@Override
	public String getFactoryId() {
		return "org.eclipse.tcf.te.ui.views.categoryFactory"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IDisposable#dispose()
	 */
	@Override
	public void dispose() {
		if (descriptor != null) {
			JFaceResources.getResources().destroyImage(descriptor);
			descriptor = null;
		}
		image = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.interfaces.ICategory#getImage()
	 */
	@Override
	public Image getImage() {
		return image;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.interfaces.ICategory#getChildren()
	 */
	@Override
	public Object[] getChildren() {
	    return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.interfaces.ICategory#getRank()
	 */
	@Override
	public int getRank() {
		return rank;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.interfaces.ICategory#belongsTo(java.lang.Object)
	 */
	@Override
	public boolean belongsTo(Object element) {
		ICategorizable categorizable = null;
		if (element instanceof IAdaptable) {
			categorizable = (ICategorizable)((IAdaptable)element).getAdapter(ICategorizable.class);
		}
		if (categorizable == null) {
			categorizable = (ICategorizable)Platform.getAdapterManager().getAdapter(element, ICategorizable.class);
		}
		return categorizable != null ? Managers.getCategoryManager().belongsTo(getId(), categorizable.getId()) : false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.interfaces.ICategory#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		// The category is enabled if no "enablement" expression is found
		boolean enabled = true;

		if (expression != null) {
			// Get the handler service
			IHandlerService handlerSvc = (IHandlerService)PlatformUI.getWorkbench().getService(IHandlerService.class);
			Assert.isNotNull(handlerSvc);

			// Get the current action
			IEvaluationContext currentState = handlerSvc.getCurrentState();

			// Construct the evaluation context to pass to the expression
			// The expressions default variable is the category itself.
			IEvaluationContext ctx = new EvaluationContext(currentState, this);
			try {
				enabled = expression.evaluate(ctx).equals(EvaluationResult.TRUE);
			} catch (CoreException e) {
				IStatus status = new Status(IStatus.ERROR, UIPlugin.getUniqueIdentifier(), e.getLocalizedMessage(), e);
				UIPlugin.getDefault().getLog().log(status);
			}
		}

	    return enabled;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer(getLabel());
		buffer.append(" ["); //$NON-NLS-1$
		buffer.append(getId());
		buffer.append("] {rank="); //$NON-NLS-1$
		buffer.append(getRank());
		buffer.append(", enabled="); //$NON-NLS-1$
		buffer.append(isEnabled());
		buffer.append("}"); //$NON-NLS-1$
		return buffer.toString();
	}
}
