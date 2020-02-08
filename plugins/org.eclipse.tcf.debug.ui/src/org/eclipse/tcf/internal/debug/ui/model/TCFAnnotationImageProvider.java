/*******************************************************************************
 * Copyright (c) 2009, 2016 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.ui.texteditor.IAnnotationImageProvider;

public class TCFAnnotationImageProvider implements IAnnotationImageProvider {

    public Image getManagedImage(Annotation annotation) {
        return ImageCache.getImage(((TCFAnnotationManager.TCFAnnotation)annotation).image);
    }

    public String getImageDescriptorId(Annotation annotation) {
        return null;
    }

    public ImageDescriptor getImageDescriptor(String id) {
        return null;
    }
}
