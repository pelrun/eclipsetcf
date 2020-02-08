/*******************************************************************************
 * Copyright (c) 2008, 2016 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

public class ImageCache {

    /* Image ID prefix to indicate common Debug UI images */
    private static final String DebugUI = "DebugUI:";

    public static final String
        IMG_TCF = "icons/tcf",
        IMG_TARGET_TAB = "icons/target_tab",
        IMG_DOWNLOAD_TAB = "icons/download_tab",
        IMG_TARGET_WIZARD = "icons/full/wizban/debug_wiz",
        IMG_APPLICATION_TAB = "icons/application_tab",
        IMG_ARGUMENTS_TAB = "icons/arguments_tab",
        IMG_ATTRIBUTE = "icons/attribute",
        IMG_PATH = "icons/path",

        IMG_THREAD_TERMINATED = "icons/full/obj16/threadt_obj",
        IMG_THREAD_SUSPENDED = "icons/full/obj16/threads_obj",
        IMG_THREAD_RUNNNIG = "icons/full/obj16/thread_obj",
        IMG_THREAD_REVERSING = "icons/thread_reversing",
        IMG_THREAD_NOT_ACTIVE = "icons/thread_not_active",
        IMG_THREAD_UNKNOWN_STATE = "icons/thread_not_active",

        IMG_PROCESS_TERMINATED = "icons/full/obj16/debugtt_obj",
        IMG_PROCESS_SUSPENDED = "icons/full/obj16/debugts_obj",
        IMG_PROCESS_RUNNING = "icons/full/obj16/debugt_obj",

        IMG_REGISTER = "icons/full/obj16/genericregister_obj",

        IMG_VARIABLE = "icons/var_simple",
        IMG_VARIABLE_POINTER = "icons/var_pointer",
        IMG_VARIABLE_AGGREGATE = "icons/var_aggr",

        IMG_NEW_EXPRESSION = "icons/full/elcl16/monitorexpression_tsk",

        IMG_SIGNALS = "icons/signals",
        IMG_MEMORY_MAP = "icons/memory-map",
        IMG_PROFILER = "icons/profiler",

        IMG_ARRAY_PARTITION = "icons/full/obj16/arraypartition_obj",

        IMG_STACK_FRAME_SUSPENDED = "icons/full/obj16/stckframe_obj",
        IMG_STACK_FRAME_RUNNING = "icons/full/obj16/stckframe_running_obj",
        IMG_STACK_FRAME_REVERSING = "icons/stckframe_reversing",

        IMG_BREAKPOINT_ENABLED = "icons/full/obj16/brkp_obj",
        IMG_BREAKPOINT_DISABLED = "icons/full/obj16/brkpd_obj",
        IMG_BREAKPOINT_INSTALLED = "icons/ovr16/installed_ovr",
        IMG_BREAKPOINT_CONDITIONAL = "icons/ovr16/conditional_ovr",
        IMG_BREAKPOINT_MOVED = "icons/moved_ovr",
        IMG_BREAKPOINT_WARNING = "icons/ovr16/warning_ovr",
        IMG_BREAKPOINT_ERROR = "icons/ovr16/error_ovr",
        IMG_BREAKPOINT_OVERLAY = "icons/brkp_ovr",

        IMG_FOLDER = "icons/full/obj16/fldr_obj",
        IMG_FILE = "icons/full/obj16/file_obj",

        IMG_INSTRUCTION_POINTER_TOP = DebugUI + IDebugUIConstants.IMG_OBJS_INSTRUCTION_POINTER_TOP,
        IMG_INSTRUCTION_POINTER = DebugUI + IDebugUIConstants.IMG_OBJS_INSTRUCTION_POINTER;

    private static final Map<String,ImageDescriptor> desc_cache = new HashMap<String,ImageDescriptor>();
    private static final Map<ImageDescriptor,Image> image_cache = new HashMap<ImageDescriptor,Image>();
    private static final Map<String,Map<ImageDescriptor,ImageDescriptor>> overlay_cache =
        new HashMap<String,Map<ImageDescriptor,ImageDescriptor>>();

    public static synchronized ImageDescriptor getImageDescriptor(String name) {
        if (name == null) return null;
        ImageDescriptor descriptor = desc_cache.get(name);
        if (descriptor == null) {
            if (name.startsWith(DebugUI)) {
                descriptor = DebugUITools.getImageDescriptor(name.substring(DebugUI.length()));
            }
            else {
                String[] ext = { "png", "gif" };
                for (String e : ext) {
                    IPath path = new Path(name).removeFileExtension().addFileExtension(e);
                    Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
                    if (bundle != null) {
                        URL url = FileLocator.find(bundle, path, null);
                        if (url != null) {
                            descriptor = ImageDescriptor.createFromURL(url);
                            if (descriptor != null) break;
                        }
                    }
                    bundle = Platform.getBundle("org.eclipse.debug.ui");
                    if (bundle != null) {
                        URL url = FileLocator.find(bundle, path, null);
                        if (url != null) {
                            descriptor = ImageDescriptor.createFromURL(url);
                            if (descriptor != null) break;
                        }
                    }
                    bundle = Platform.getBundle("org.eclipse.cdt.debug.ui");
                    if (bundle != null) {
                        URL url = FileLocator.find(bundle, path, null);
                        if (url != null) {
                            descriptor = ImageDescriptor.createFromURL(url);
                            if (descriptor != null) break;
                        }
                    }
                }
                if (descriptor == null) {
                    descriptor = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(name);
                }
            }
            if (descriptor == null) {
                descriptor = ImageDescriptor.getMissingImageDescriptor();
            }
            desc_cache.put(name, descriptor);
        }
        return descriptor;
    }

    public static synchronized ImageDescriptor addOverlay(ImageDescriptor descriptor, String name) {
        return addOverlay(descriptor, name, 0, 0);
    }

    public static synchronized ImageDescriptor addOverlay(
            ImageDescriptor descriptor, String name, final int x, final int y) {
        if (descriptor == null || name == null) return descriptor;
        String key = name + ':' + x + ':' + y;
        Map<ImageDescriptor,ImageDescriptor> map = overlay_cache.get(key);
        if (map == null) overlay_cache.put(key, map = new HashMap<ImageDescriptor,ImageDescriptor>());
        ImageDescriptor res = map.get(descriptor);
        if (res != null) return res;
        final ImageData base = descriptor.getImageData();
        final ImageData overlay = getImageDescriptor(name).getImageData();
        res = new CompositeImageDescriptor() {
            @Override
            protected void drawCompositeImage(int width, int height) {
                drawImage(base, 0, 0);
                drawImage(overlay, x, y);
            }
            @Override
            protected Point getSize() {
                return new Point(base.width, base.height);
            }
        };
        map.put(descriptor, res);
        return res;
    }

    public static synchronized Image getImage(ImageDescriptor desc) {
        Image image = image_cache.get(desc);
        if (image == null) {
            image = desc.createImage();
            image_cache.put(desc, image);
        }
        return image;
    }

    public static synchronized Image getImage(String name) {
        return getImage(getImageDescriptor(name));
    }
}
