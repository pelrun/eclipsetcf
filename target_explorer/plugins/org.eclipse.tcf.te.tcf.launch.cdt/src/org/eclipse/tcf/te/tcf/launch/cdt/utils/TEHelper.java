/*******************************************************************************
 * Copyright (c) 2012, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems          - initial API and implementation
 * Anna Dushistova (MontaVista)- adapted from org.eclipse.tcf.te.tcf.launch.core.steps.LaunchProcessStep
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.launch.cdt.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.te.core.utils.text.StringUtil;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.filetransfer.FileTransferItem;
import org.eclipse.tcf.te.runtime.services.interfaces.IDelegateService;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.services.interfaces.filetransfer.IFileTransferItem;
import org.eclipse.tcf.te.runtime.utils.Host;
import org.eclipse.tcf.te.runtime.utils.net.IPAddressUtil;
import org.eclipse.tcf.te.tcf.core.interfaces.ITransportTypes;
import org.eclipse.tcf.te.tcf.core.streams.StreamsDataReceiver;
import org.eclipse.tcf.te.tcf.core.streams.StreamsDataReceiver.Listener;
import org.eclipse.tcf.te.tcf.filesystem.core.services.FileTransferService;
import org.eclipse.tcf.te.tcf.launch.cdt.activator.Activator;
import org.eclipse.tcf.te.tcf.launch.cdt.interfaces.IRemoteLaunchDelegate;
import org.eclipse.tcf.te.tcf.launch.cdt.interfaces.IRemoteTEConfigurationConstants;
import org.eclipse.tcf.te.tcf.launch.cdt.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.launcher.IProcessLauncher;
import org.eclipse.tcf.te.tcf.processes.core.launcher.ProcessLauncher;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ILineSeparatorConstants;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;

public class TEHelper {

	private static final String LOCAL_TEMPLATE_ROOT = "templates"; //$NON-NLS-1$
	private static final String PRERUN_TEMPLATE_NAME = "prerun_template.sh"; //$NON-NLS-1$

	public static void remoteFileTransfer(IPeer peer, String localFilePath, String remoteFilePath, SubProgressMonitor monitor) throws IOException {
		// Copy the host side file to a temporary location first before copying to the target,
		// if the file size of the files are the same. If the remote file system is NFS mounted
		// from the host, we end up with a truncated file otherwise.
		boolean copyViaTemp = true;
		File tempFile = null;

		// Create the file transfer item
		FileTransferItem item = new FileTransferItem(new Path(localFilePath), remoteFilePath);
		item.setProperty(IFileTransferItem.PROPERTY_DIRECTION, "" + IFileTransferItem.HOST_TO_TARGET); //$NON-NLS-1$

		// Get the remote path file attributes
		IFileSystem.FileAttrs attrs = FileTransferService.getRemoteFileAttrs(peer, null, item);
		if (attrs != null) {
			IPath hostPath = item.getHostPath();
			if (hostPath.toFile().canRead()) {
				copyViaTemp = attrs.size == hostPath.toFile().length();
			}
		}

		// Copy the host file to a temporary location if needed
		if (copyViaTemp) {

			monitor.beginTask(Messages.TEGdbAbstractLaunchDelegate_downloading + " " + localFilePath + " to " + remoteFilePath, 200); //$NON-NLS-1$ //$NON-NLS-2$

			try {
				IPath hostPath = item.getHostPath();
				tempFile = File.createTempFile(Long.toString(System.nanoTime()), null);

				long tick = hostPath.toFile().length() / 100;

				FileInputStream in = new FileInputStream(hostPath.toFile());
				try {
					FileOutputStream out = new FileOutputStream(tempFile);
					try {
						int count;
						long tickCount = 0;
						byte[] buf = new byte[4096];
						while ((count = in.read(buf)) > 0) {
							out.write(buf, 0, count);
							tickCount += count;
							if (tickCount >= tick) {
								monitor.worked(1);
								tickCount = 0;
							}
						}
					}
					finally {
						out.close();
					}
				}
				finally {
					in.close();
				}
			}
			catch (IOException e) {
				// In case of an exception, make sure that the temporary file
				// is removed before re-throwing the exception
				if (tempFile != null) tempFile.delete();
				// Also the monitor needs to be marked done
				monitor.done();
				// Re-throw the exception finally
				throw e;
			}

			// Recreate the file transfer item to take the temporary file as input
			item = new FileTransferItem(new Path(tempFile.getAbsolutePath()), remoteFilePath);
			item.setProperty(IFileTransferItem.PROPERTY_DIRECTION, "" + IFileTransferItem.HOST_TO_TARGET); //$NON-NLS-1$
		} else {
			monitor.beginTask(Messages.TEGdbAbstractLaunchDelegate_downloading + " " + localFilePath + " to " + remoteFilePath, 100); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// Transfer the file to the target
		final Callback callback = new Callback();
		FileTransferService.transfer(peer, null, item, monitor, callback);
		// Wait till the step finished, an execution occurred or the
		// user hit cancel on the progress monitor.
		ExecutorsUtil.waitAndExecute(0, callback.getDoneConditionTester(null));

		// Remove the temporary file
		if (tempFile != null) tempFile.delete();
	}

	public static IPeerNode getPeerNode(final String peerId) {
		if (peerId != null) {
			final AtomicReference<IPeerNode> parent = new AtomicReference<IPeerNode>();
			final Runnable runnable = new Runnable() {
				@Override
				public void run() {
					parent.set(ModelManager.getPeerModel().getService(IPeerModelLookupService.class).lkupPeerModelById(peerId));
				}
			};
			Protocol.invokeAndWait(runnable);
			return parent.get();
		}
		return null;
	}

	public static IPeerNode getCurrentConnection(ILaunchConfiguration config) throws CoreException {
		String peerId = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_CONNECTION, ""); //$NON-NLS-1$
		IPeerNode connection = getPeerNode(peerId);
		if (connection == null) {
			abort(Messages.TEHelper_connection_not_found, null, ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
		}
		return connection;
	}

	public static ProcessLauncher launchCmd(final IPeer peer, String peerName, String command, Listener listener, SubProgressMonitor monitor, ICallback callback) throws CoreException {
		if (command != null && !command.trim().equals("")) { //$NON-NLS-1$
			String[] args = StringUtil.tokenize(command, 0, false);
			if (args.length > 0) {
				String cmd = args[0];
				String[] arguments = null;
				if (args.length > 1) {
					arguments = Arrays.copyOfRange(args, 1, args.length);
				}
				return launchCmdWithEnv(peer, peerName, cmd, arguments, null, listener, monitor, callback);
			}
		}
		return null;
	}

	public static ProcessLauncher launchCmd(final IPeer peer, String peerName, String remoteCommandPath, String arguments, Listener listener, SubProgressMonitor monitor, ICallback callback) throws CoreException {
		return launchCmdWithEnv(peer, peerName, remoteCommandPath, arguments, null, listener, monitor, callback);
	}

	public static ProcessLauncher launchCmd(final IPeer peer, String peerName, String remoteCommandPath, String[] args, Listener listener, SubProgressMonitor monitor, ICallback callback) throws CoreException {
		return launchCmdWithEnv(peer, peerName, remoteCommandPath, args, null, listener, monitor, callback);
	}

	public static ProcessLauncher launchCmdWithEnv(final IPeer peer, String peerName, String remoteCommandPath, String arguments, Map<String, String> env, Listener listener, SubProgressMonitor monitor, ICallback callback) throws CoreException {
		String[] args = arguments != null && !"".equals(arguments.trim()) ? StringUtil.tokenize(arguments, 0, false) : null; //$NON-NLS-1$
		return launchCmdWithEnv(peer, peerName, remoteCommandPath, args, env, listener, monitor, callback);
	}

	public static ProcessLauncher launchCmdWithEnv(final IPeer peer, String peerName, String remoteCommandPath, String[] args, Map<String, String> env, Listener listener, SubProgressMonitor monitor, ICallback callback) throws CoreException {
		if (remoteCommandPath != null && !remoteCommandPath.trim().equals("")) { //$NON-NLS-1$
			monitor.beginTask(NLS.bind(Messages.TEHelper_executing, remoteCommandPath, args), 10);

			// Construct the launcher object
			ProcessLauncher launcher = new ProcessLauncher();

			Map<String, Object> launchAttributes = new HashMap<String, Object>();

			// Compute the terminal title if possible
			if (args != null && args.length > 0) {
				StringBuilder title = new StringBuilder();
				IPath p = new Path(remoteCommandPath);
				// Avoid very long terminal title's by shortening the path if it has more than 3 segments
				if (p.segmentCount() > 3) {
					title.append(".../"); //$NON-NLS-1$
					title.append(p.lastSegment());
				} else {
					title.append(p.toString());
				}

				for (String arg : args) {
					if (arg.matches(":[0-9]+")) { //$NON-NLS-1$
						title.append(arg);
						break;
					}
				}

				String name = peerName != null ? peerName : peer.getName();
				if (name != null && !"".equals(name)) { //$NON-NLS-1$
					title.append(" [" + name + "]"); //$NON-NLS-1$ //$NON-NLS-2$
				}

				if (title.length() > 0) launchAttributes.put(ITerminalsConnectorConstants.PROP_TITLE, title.toString());
			}

			launchAttributes.put(IProcessLauncher.PROP_PROCESS_PATH, spaceEscapify(remoteCommandPath));
			launchAttributes.put(IProcessLauncher.PROP_PROCESS_ARGS, args);
			launchAttributes.put(IProcessLauncher.PROP_PROCESS_ENV, env);

			launchAttributes.put(ITerminalsConnectorConstants.PROP_LOCAL_ECHO, Boolean.FALSE);

			boolean outputConsole = true;
			if (outputConsole) {
				launchAttributes.put(IProcessLauncher.PROP_PROCESS_ASSOCIATE_CONSOLE, Boolean.TRUE);
			}

			// Fill in the launch attributes
			IPropertiesContainer container = new PropertiesContainer();
			container.setProperties(launchAttributes);

			// If the line separator setting is not set explicitly, try to
			// determine it automatically (local host only).
			if (container.getProperty(ITerminalsConnectorConstants.PROP_LINE_SEPARATOR) == null) {
				// Determine if the launch is on local host. If yes, we can
				// preset the line ending character.
				final AtomicBoolean isLocalhost = new AtomicBoolean();

				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						if (ITransportTypes.TRANSPORT_TYPE_TCP.equals(peer.getTransportName()) || ITransportTypes.TRANSPORT_TYPE_SSL.equals(peer.getTransportName())) {
							isLocalhost.set(IPAddressUtil.getInstance().isLocalHost(peer.getAttributes().get(IPeer.ATTR_IP_HOST)));
						}
					}
				};

				if (Protocol.isDispatchThread()) runnable.run();
				else Protocol.invokeAndWait(runnable);

				if (isLocalhost.get()) {
					container.setProperty(ITerminalsConnectorConstants.PROP_LINE_SEPARATOR, Host.isWindowsHost() ? ILineSeparatorConstants.LINE_SEPARATOR_CRLF : ILineSeparatorConstants.LINE_SEPARATOR_LF);
				}
			}

			if (listener != null) {
				container.setProperty(IProcessLauncher.PROP_PROCESS_OUTPUT_LISTENER, new StreamsDataReceiver.Listener[] { listener });
			}
			// Launch the process
			launcher.launch(peer, container, new Callback(callback) {
				@Override
				protected void internalDone(Object caller, IStatus status) {
					if (!status.isOK()) {
						System.out.println(status.getMessage());
					}
					super.internalDone(caller, status);
				}
			});
			monitor.done();
			return launcher;
		}
		return null;
	}

	/**
	 * Throws a core exception with an error status object built from the given message, lower level
	 * exception, and error code.
	 *
	 * @param message the status message
	 * @param exception lower level exception associated with the error, or <code>null</code> if
	 *            none
	 * @param code error code
	 */
	public static void abort(String message, Throwable exception, int code) throws CoreException {
		IStatus status;
		if (exception != null) {
			MultiStatus multiStatus = new MultiStatus(Activator.getUniqueIdentifier(), code, message, exception);
			multiStatus.add(new Status(IStatus.ERROR, Activator.getUniqueIdentifier(), code, exception
			                .getLocalizedMessage(), exception));
			status = multiStatus;
		}
		else {
			status = new Status(IStatus.ERROR, Activator.getUniqueIdentifier(), code, message, null);
		}
		throw new CoreException(status);
	}

	public static String spaceEscapify(String inputString) {
		if (inputString == null) return null;

		return inputString.replaceAll(" ", "\\\\ "); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Returns the string preference value for the given key. The method
	 * allows overwriting the preference value via a system property.
	 *
	 * @param key The preference key. Must not be <code>null</code>.
	 * @return The preference value or <code>null</code> if the preference key does not exist.
	 */
	public static String getStringPreferenceValue(String key) {
		Assert.isNotNull(key);

		// Try system properties first
		String value = System.getProperty(key, null);
		// If not set, try the preferences
		if (value == null || "".equals(value)) { //$NON-NLS-1$
			value = Activator.getScopedPreferences().getString(key);
		}
		return value;
	}

	/**
	 * Returns the list preference value for the given key. The method
	 * allows overwriting the preference value via a system property.
	 *
	 * @param key The preference key. Must not be <code>null</code>.
	 * @return The preference value or <code>null</code> if the preference key does not exist.
	 */
	public static List<String> getListPreferenceValue(String key) {
		Assert.isNotNull(key);

		List<String> list = null;
		String value = getStringPreferenceValue(key);
		if (value != null && !"".equals(value)) { //$NON-NLS-1$
			list = Arrays.asList(value.split("\\s*,\\s*")); //$NON-NLS-1$
		}

		return list;
	}

	/**
	 * @param peer
	 * @return the content of the script template used to run
	 * commands before the launch.
	 * @throws Exception
	 */
	public static String getPrerunTemplateContent(IPeer peer) throws Exception {
		File templateFile = null;
		BufferedReader reader = null;
		StringBuilder sb = new StringBuilder();
		try {
			IPath templatePath = null;
			IRemoteLaunchDelegate launchDelegate = null;
			IPeerNode peerNode = getPeerNode(peer.getID());
			IService[] services = ServiceManager.getInstance().getServices(peerNode, IDelegateService.class, false);
			for (IService service : services) {
		        if (service instanceof IDelegateService) {
		        	launchDelegate = ((IDelegateService)service).getDelegate(peerNode, IRemoteLaunchDelegate.class);
		        	if (launchDelegate != null) { break; }
		        }
	        }
			if (launchDelegate != null) {
				templatePath = launchDelegate.getPrerunTemplatePath();
			}
			if (templatePath != null) {
				templateFile = new File(templatePath.toOSString());
			}
			// Fallback - Use built-in template
			if (templateFile == null || !templateFile.exists()) {
				templateFile = new File(FileLocator.resolve(Activator.getDefault().getBundle().getEntry(LOCAL_TEMPLATE_ROOT + File.separator + PRERUN_TEMPLATE_NAME)).toURI());
			}

			reader = new BufferedReader(new FileReader(templateFile));
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
				sb.append('\n');
			}
		} catch (Exception e) {
			throw new Exception(NLS.bind(Messages.TEGdbAbstractLaunchDelegate_error_prerunScriptTemplate, templateFile), e);
		} finally {
			if (reader != null) {
				try { reader.close(); }	catch (IOException e) { /* Ignored on purpose. */ }
			}
		}
		return sb.toString();
	}
}
