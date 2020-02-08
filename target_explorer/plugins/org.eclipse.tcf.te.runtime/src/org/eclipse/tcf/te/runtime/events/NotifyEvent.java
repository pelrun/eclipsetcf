/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.events;

import java.util.EventObject;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.runtime.activator.CoreBundleActivator;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.interfaces.tracing.ITraceIds;

/**
 * A notification event.
 * <p>
 * A notification is displayed in a desktop popup and is rendered as form
 * text. The form text is created by an associated form text factory delegate.
 * <p>
 * The main listener for this event is registered by the <code>org.eclipse.tcf.te.ui.notifications</code>
 * bundle. In headless environments, this event is ignored.
 * <p>
 * On construction time, the notification event remembers the current system time.
 * This time stamp is used by the <code>compare</code> method to provide an natural
 * ordering of the notifications.
 */
public class NotifyEvent extends EventObject implements Comparable<NotifyEvent> {
    private static final long serialVersionUID = -7099295102694857196L;

    /**
     * Property defining the title text of the notification. The title text
     * is typically the first line of the notification to be displayed.
     * <p>
     * <b>Note:</b> The location and the rendering of the title text inside
     * the notification to display can be customized by contributing your
     * own notification form text factory delegate.
     */
    public static final String PROP_TITLE_TEXT = "titleText"; //$NON-NLS-1$

    /**
     * Property defining the title image id of the notification. The title image
     * is typically shown left of the title text in the first line of the
     * notification to display.
     * <p>
     * <b>Note:</b> The location and the rendering of the title text inside
     * the notification to display can be customized by contributing your
     * own notification form text factory delegate.
     */
    public static final String PROP_TITLE_IMAGE_ID = "titleImageId"; //$NON-NLS-1$

    /**
     * Property defining the description text of the notification. The description
     * text is typically shown as multi line text block below the title text.
     * <p>
     * <b>Note:</b> The location and the rendering of the title text inside
     * the notification to display can be customized by contributing your
     * own notification form text factory delegate.
     */
    public static final String PROP_DESCRIPTION_TEXT = "descriptionText"; //$NON-NLS-1$

	// The creation time time stamp.
	private final long creationTime = System.nanoTime();

    private final String factoryId;
    private final IPropertiesContainer properties;

    /**
     * Constructor
	 *
	 * @param source The event source. Must not be <code>null</code>.
	 * @param properties The properties to be consumed by the form text factory delegate. Must not be <code>null</code>.
     */
    public NotifyEvent(Object source, IPropertiesContainer properties) {
    	this(source, null, properties);
    }

	/**
	 * Constructor
	 *
	 * @param source The event source. Must not be <code>null</code>.
	 * @param factoryId The unique id of the form text factory delegate or <code>null</code>.
	 * @param properties The properties to be consumed by the form text factory delegate. Must not be <code>null</code>.
	 */
	public NotifyEvent(Object source, String factoryId, IPropertiesContainer properties) {
		super(source);

		this.factoryId = factoryId;

		Assert.isNotNull(properties);
		this.properties = properties;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
    public int compareTo(NotifyEvent o) {
		// If the other object is null, this object is always
		// greater than the null object
		if (o == null) return 1;
		// Compare the creation times
		return Long.valueOf(creationTime).compareTo(Long.valueOf(o.creationTime));
	}

	/**
	 * Returns the form text factory delegate id.
	 *
	 * @return The form text factory delegate id or <code>null<code>.
	 */
	public String getFactoryId() {
		return factoryId;
	}

	/**
	 * Returns the properties to be consumed by the form text factory delegate.
	 *
	 * @return The properties.
	 */
	public IPropertiesContainer getProperties() {
		return properties;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int hashCode = Long.valueOf(creationTime).hashCode();
		if (factoryId != null) hashCode ^= factoryId.hashCode();
		hashCode ^= properties.hashCode();
		return hashCode;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NotifyEvent) {
			return creationTime == ((NotifyEvent)obj).creationTime
							&& (factoryId != null ? factoryId.equals(((NotifyEvent)obj).factoryId) : ((NotifyEvent)obj).factoryId == null)
							&& properties.equals(((NotifyEvent)obj).properties);
		}
	    return super.equals(obj);
	}

	/* (non-Javadoc)
	 * @see com.windriver.ide.common.core.event.WRAbstractNotificationEvent#toString()
	 */
	@Override
	public String toString() {
		StringBuffer toString = new StringBuffer(getClass().getName());

		String prefix = ""; //$NON-NLS-1$
		// if tracing the event, formating them a little bit better readable.
		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_EVENTS)) {
			prefix = "\n\t\t"; //$NON-NLS-1$
		}

		toString.append(prefix + "{creationTime="); //$NON-NLS-1$
		toString.append(creationTime);
		toString.append("," + prefix + "{factoryId="); //$NON-NLS-1$ //$NON-NLS-2$
		toString.append(factoryId);
		toString.append("," + prefix + "properties="); //$NON-NLS-1$ //$NON-NLS-2$
		toString.append(properties);
		toString.append("," + prefix + "source="); //$NON-NLS-1$ //$NON-NLS-2$
		toString.append(source);
		toString.append("}"); //$NON-NLS-1$

		return toString.toString();
	}

}
