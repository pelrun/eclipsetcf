/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.tcf.te.runtime.activator.CoreBundleActivator;

/**
 * Environment handling utility methods.
 */
public class Env {

	// Reference to the monitor to lock if determining the native environment
	private final static Object ENV_GET_MONITOR = new Object();

	// Reference to the native environment once retrieved
	private static Map<String, String> nativeEnvironment = null;
	// Reference to the native environment with the case of the variable names preserved
	private static Map<String, String> nativeEnvironmentCasePreserved = null;

	/**
	 * Returns the merged environment of the native environment and the passed
	 * in environment. Passed in variables will overwrite the native environment
	 * if the same variables are set there.
	 * <p>
	 * For use with terminals, the parameter <code>terminal</code> should be set to
	 * <code>true</code>. In this case, the method will assure that the <code>TERM</code>
	 * environment variable is always set to <code>ANSI</code> and is not overwritten
	 * by the passed in environment.
	 *
	 * @param envp The environment to set on top of the native environment or <code>null</code>.
	 * @param terminal <code>True</code> if used with an terminal, <code>false</code> otherwise.
	 *
	 * @return The merged environment.
	 */
	public static String[] getEnvironment(String[] envp, boolean terminal) {
		Map<String, String> env = getNativeEnvironment();

		if (terminal) env.put("TERM", "ansi"); //$NON-NLS-1$ //$NON-NLS-2$

		Iterator<Map.Entry<String, String>> iter = env.entrySet().iterator();
		List<String> strings = new ArrayList<String>(env.size());
		StringBuffer buffer = null;
		while (iter.hasNext()) {
			Map.Entry<String, String> entry = iter.next();
			buffer = new StringBuffer(entry.getKey());
			buffer.append('=').append(entry.getValue());
			strings.add(buffer.toString());
		}

		// if "local" environment is provided - append
		if (envp != null) {
			// add provided
			for (int i = 0; i < envp.length; i++) {
				String envpPart = envp[i];
				// don't override TERM
				String[] parts = envpPart.split("=");//$NON-NLS-1$
				if (!terminal || !parts[0].trim().equals("TERM")) {//$NON-NLS-1$
					strings.add(envpPart);
				}
			}
		}

		return strings.toArray(new String[strings.size()]);
	}

	/**
	 * Determine the native environment, but returns all environment variable
	 * names in upper case.
	 *
	 * @return The native environment with upper case variable names, or an empty map.
	 */
	private static Map<String, String> getNativeEnvironment() {
		synchronized (ENV_GET_MONITOR) {
			if (nativeEnvironment == null) {
				Map<String, String> casePreserved = getNativeEnvironmentCasePreserved();
				if (Platform.getOS().equals(org.eclipse.osgi.service.environment.Constants.OS_WIN32)) {
					nativeEnvironment = new HashMap<String, String>();
					Iterator<Map.Entry<String, String>> entries = casePreserved.entrySet().iterator();
					while (entries.hasNext()) {
						Map.Entry<String, String> entry = entries.next();
						nativeEnvironment.put(entry.getKey().toUpperCase(), entry.getValue());
					}
				} else {
					nativeEnvironment = new HashMap<String, String>(casePreserved);
				}
			}
			return new HashMap<String, String>(nativeEnvironment);
		}
	}

	/**
	 * Determine the native environment.
	 *
	 * @return The native environment, or an empty map.
	 */
	private static Map<String, String> getNativeEnvironmentCasePreserved() {
		synchronized (ENV_GET_MONITOR) {
			if (nativeEnvironmentCasePreserved == null) {
				nativeEnvironmentCasePreserved= new HashMap<String, String>();
				cacheNativeEnvironment(nativeEnvironmentCasePreserved);
			}
			return new HashMap<String, String>(nativeEnvironmentCasePreserved);
		}
	}

	/**
	 * Query the native environment and store it to the specified cache.
	 *
	 * @param cache The environment cache. Must not be <code>null</code>.
	 */
	private static void cacheNativeEnvironment(Map<String, String> cache) {
		Assert.isNotNull(cache);

		try {
			String nativeCommand = null;
			boolean isWin9xME = false; // see bug 50567
			String fileName = null;
			if (Platform.getOS().equals(Constants.OS_WIN32)) {
				String osName = System.getProperty("os.name"); //$NON-NLS-1$
				isWin9xME = osName != null && (osName.startsWith("Windows 9") || osName.startsWith("Windows ME")); //$NON-NLS-1$ //$NON-NLS-2$
				if (isWin9xME) {
					// Win 95, 98, and ME
					// SET might not return therefore we pipe into a file
					IPath stateLocation = Platform.getStateLocation(CoreBundleActivator.getContext().getBundle());
					fileName = stateLocation.toOSString() + File.separator + "env.txt"; //$NON-NLS-1$
					nativeCommand = "command.com /C set > " + fileName; //$NON-NLS-1$
				} else {
					// Win NT, 2K, XP
					nativeCommand = "cmd.exe /C set"; //$NON-NLS-1$
				}
			} else if (!Platform.getOS().equals(Constants.OS_UNKNOWN)) {
				nativeCommand = "env"; //$NON-NLS-1$
			}
			if (nativeCommand == null) { return; }
			Process process = Runtime.getRuntime().exec(nativeCommand);
			if (isWin9xME) {
				// read piped data on Win 95, 98, and ME
				Properties p = new Properties();
				File file = new File(fileName);
				InputStream stream = null;
				try {
					stream = new BufferedInputStream(new FileInputStream(file));
					p.load(stream);
				} finally {
					if (stream != null) stream.close();
				}
				if (!file.delete()) {
					file.deleteOnExit(); // if delete() fails try again on VM close
				}
				for (Enumeration<Object> enumeration = p.keys(); enumeration.hasMoreElements();) {
					// Win32's environment variables are case insensitive. Put everything
					// to upper case so that (for example) the "PATH" variable will match
					// "pAtH" correctly on Windows.
					String key = (String)enumeration.nextElement();
					cache.put(key, (String)p.get(key));
				}
			} else {
				// read process directly on other platforms
				// we need to parse out matching '{' and '}' for function declarations in .bash environments
				// pattern is [function name]=() { and we must find the '}' on its own line with no trailing ';'
				InputStream stream = process.getInputStream();
				InputStreamReader isreader = new InputStreamReader(stream);
				BufferedReader reader = new BufferedReader(isreader);
				try {
					String line = reader.readLine();
					String key = null;
					String value = null;
					while (line != null) {
						int func = line.indexOf("=()"); //$NON-NLS-1$
						if (func > 0) {
							key = line.substring(0, func);
							// scan until we find the closing '}' with no following chars
							value = line.substring(func + 1);
							while (line != null && !line.equals("}")) { //$NON-NLS-1$
								line = reader.readLine();
								if (line != null) {
									value += line;
								}
							}
							line = reader.readLine();
						} else {
							int separator = line.indexOf('=');
							if (separator > 0) {
								key = line.substring(0, separator);
								value = line.substring(separator + 1);
								StringBuilder bufValue = new StringBuilder(value);
								line = reader.readLine();
								if (line != null) {
									// this line has a '=' read ahead to check next line for '=', might be broken on more
									// than one line
									separator = line.indexOf('=');
									while (separator < 0) {
										bufValue.append(line.trim());
										line = reader.readLine();
										if (line == null) {
											// if next line read is the end of the file quit the loop
											break;
										}
										separator = line.indexOf('=');
									}
								}
								value = bufValue.toString();
							}
						}
						if (key != null) {
							cache.put(key, value);
							key = null;
							value = null;
						} else {
							line = reader.readLine();
						}
					}
				} finally {
					reader.close();
				}
			}
		} catch (IOException e) {
			// Native environment-fetching code failed.
			// This can easily happen and is not useful to log.
		}
	}

}
