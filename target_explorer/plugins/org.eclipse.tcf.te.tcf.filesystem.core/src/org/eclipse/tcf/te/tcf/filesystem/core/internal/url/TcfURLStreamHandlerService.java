/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.url;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.osgi.service.url.AbstractURLStreamHandlerService;

/**
 * The stream handler service used to parse tcf stream protocol.
 */
public class TcfURLStreamHandlerService extends AbstractURLStreamHandlerService {
	@Override
	public URLConnection openConnection(URL u) throws IOException {
		return new TcfURLConnection(u);
	}
}
