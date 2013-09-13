/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.internal.services;

import java.io.File;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Path;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IPathMap;
import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.utils.net.IPAddressUtil;
import org.eclipse.tcf.te.tcf.core.interfaces.IPathMapResolverService;
import org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService;

/**
 * Path map resolver service implementation.
 */
public class PathMapResolverService extends AbstractService implements IPathMapResolverService {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IPathMapResolverService#map(org.eclipse.tcf.services.IPathMap.PathMapRule, java.lang.String)
	 */
	@Override
	public String map(IPathMap.PathMapRule rule, String fnm) {
		Assert.isNotNull(rule);
		Assert.isNotNull(fnm);

		String src = rule.getSource();
		if (src == null) return null;
		if (!(new Path(src).isPrefixOf(new Path(fnm)))) return null;
		String host = rule.getHost();
		if (host != null && host.length() > 0) {
			if (!IPAddressUtil.getInstance().isLocalHost(host)) return null;
		}
		String dst = rule.getDestination();
		if (dst == null || dst.length() == 0) return null;
		int l = src.length();
		if (dst.endsWith("/") && l < fnm.length() && fnm.charAt(l) == '/') l++; //$NON-NLS-1$
		return new Path(dst + fnm.substring(l)).toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IPathMapResolverService#mapReverse(org.eclipse.tcf.services.IPathMap.PathMapRule, java.lang.String)
	 */
	@Override
	public String mapReverse(IPathMap.PathMapRule rule, String fnm) {
		Assert.isNotNull(rule);
		Assert.isNotNull(fnm);

		String dst = rule.getDestination();
		if (dst == null) return null;
		if (!(new Path(dst).isPrefixOf(new Path(fnm)))) return null;
		String host = rule.getHost();
		if (host != null && host.length() > 0) {
			if (!IPAddressUtil.getInstance().isLocalHost(host)) return null;
		}
		String src = rule.getSource();
		if (src == null || src.length() == 0) return null;
		int l = dst.length();
		if (src.endsWith("/") && l < fnm.length() && fnm.charAt(l) == '/') l++; //$NON-NLS-1$
		return new Path(src + fnm.substring(l)).toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IPathMapResolverService#findTargetPath(java.lang.Object, java.lang.String)
	 */
	@Override
	public String findTargetPath(Object context, String hostPath) {
    	Assert.isTrue(!Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(context);
		Assert.isNotNull(hostPath);

		IPathMapService svc = ServiceManager.getInstance().getService(context, IPathMapService.class);
		if (svc != null) {
			IPathMap.PathMapRule[] rules = svc.getPathMap(context);
			if (rules != null && rules.length > 0) {
				for (IPathMap.PathMapRule rule : rules) {
	                String query = rule.getContextQuery();
	                if (query != null && query.length() > 0 && !query.equals("*")) continue; //$NON-NLS-1$
					String targetPath = mapReverse(rule, hostPath);
					if (targetPath != null) {
						return targetPath;
					}
				}
			}
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.interfaces.IPathMapResolverService#findHostPath(java.lang.Object, java.lang.String)
	 */
	@Override
	public String findHostPath(Object context, String targetPath) {
    	Assert.isTrue(!Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(context);
		Assert.isNotNull(targetPath);

		IPathMapService svc = ServiceManager.getInstance().getService(context, IPathMapService.class);
		if (svc != null) {
			IPathMap.PathMapRule[] rules = svc.getPathMap(context);
			if (rules != null && rules.length > 0) {
				for (IPathMap.PathMapRule rule : rules) {
	                String query = rule.getContextQuery();
	                if (query != null && query.length() > 0 && !query.equals("*")) continue; //$NON-NLS-1$
					String hostPath = mapReverse(rule, targetPath);
					if (hostPath != null) {
			            if (hostPath.startsWith("/cygdrive/")) { //$NON-NLS-1$
			            	hostPath = hostPath.substring(10, 11) + ":" + hostPath.substring(11); //$NON-NLS-1$
			            }
						File f = new File(hostPath);
						if (f.exists() && f.canRead()) {
							return hostPath;
						}
					}
				}
			}
		}

		return null;
	}

}
