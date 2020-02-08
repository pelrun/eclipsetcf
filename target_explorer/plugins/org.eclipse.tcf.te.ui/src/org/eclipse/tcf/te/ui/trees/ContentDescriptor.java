/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.trees;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

/**
 * A data structure to describe a content contribution.
 * <p>
 * A content contribution describes the content provider and the label provider.
 */
public class ContentDescriptor {
	// The content contribution id, which is unique in a tree viewer.
	private final String id;
	// The content contribution configuration element.
	private final IConfigurationElement element;
	// The content contribution
	private AbstractContentContribution contentContribution;
	// The content contribution rank
	private int rank = 100;

	/**
     * Constructor
     *
	 * @param id The content contribution id. Must not be <code>null</code>.
	 * @param element The content contribution configuration element. Must not be <code>null</code>.
     */
    public ContentDescriptor(String id, IConfigurationElement element) {
    	super();

    	Assert.isNotNull(id);
		this.id = id;
		Assert.isNotNull(element);
		this.element = element;

		// Determine the content contribution rank
		String value = element.getAttribute("rank"); //$NON-NLS-1$
		if (value != null) {
			try {
				rank = Integer.decode(value).intValue();
			} catch (NumberFormatException e) {
				/* ignored on purpose */
			}
		}
    }

	/**
	 * Get the content contribution id.
	 *
	 * @return The content contribution id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the rank of the content contribution.
	 *
	 * @return The rank of the content contribution.
	 */
	public int getRank() {
		return rank;
	}

	/**
	 * Get the content contribution instance.
	 *
	 * @return The content contribution instance or <code>null</code>.
	 */
	public AbstractContentContribution getContentContribution() {
		if (contentContribution == null) {
			try {
	            contentContribution = (AbstractContentContribution) element.createExecutableExtension("class"); //$NON-NLS-1$
            }
            catch (CoreException e) { /* ignored on purpose */ }
		}
		return contentContribution;
	}

	/**
	 * Dispose the content descriptor.
	 */
	public void dispose() {
		if (contentContribution != null) contentContribution.dispose();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
	    return id.hashCode() ^ element.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ContentDescriptor) {
			return id.equals(((ContentDescriptor)obj).id) && element.equals(((ContentDescriptor)obj).element);
		}
	    return super.equals(obj);
	}
}
