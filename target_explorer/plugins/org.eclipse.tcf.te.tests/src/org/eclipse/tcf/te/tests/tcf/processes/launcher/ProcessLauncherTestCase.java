/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf.processes.launcher;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tcf.services.IProcesses.ProcessesListener;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.launcher.IProcessLauncher;
import org.eclipse.tcf.te.tcf.processes.core.launcher.ProcessLauncher;
import org.eclipse.tcf.te.tcf.processes.core.launcher.ProcessProcessesListener;
import org.eclipse.tcf.te.tests.tcf.TcfTestCase;

/**
 * Process launcher test cases.
 */
public class ProcessLauncherTestCase extends TcfTestCase {

	/**
	 * Provides a test suite to the caller which combines all single
	 * test bundled within this category.
	 *
	 * @return Test suite containing all test for this test category.
	 */
	public static Test getTestSuite() {
		TestSuite testSuite = new TestSuite("Test TCF process launcher framework"); //$NON-NLS-1$

			// add ourself to the test suite
			testSuite.addTestSuite(ProcessLauncherTestCase.class);

		return testSuite;
	}

	//***** BEGIN SECTION: Single test methods *****
	//NOTE: All method which represents a single test case must
	//      start with 'test'!

	public void testProcessLauncher() {
		assertNotNull("Test peer missing.", peer); //$NON-NLS-1$

		// Determine the location of the "HelloWorld" executable
		IPath helloWorldLocation = getHelloWorldLocation();
		assertTrue("Missing hello world example for current OS and Arch:" + Platform.getOS() + "/" + Platform.getOSArch(), //$NON-NLS-1$ //$NON-NLS-2$
						helloWorldLocation != null &&
						helloWorldLocation.toFile().exists() &&
						helloWorldLocation.toFile().canRead());

		String temp = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$
		IPath tempDir = temp != null ? new Path(temp) : null;
		assertNotNull("Missing java temp directory", tempDir); //$NON-NLS-1$

		// If the temporary directory is not writable for whatever reason to us,
		// fallback to the users home directory
		if (!tempDir.toFile().canWrite()) {
			temp = System.getProperty("user.home"); //$NON-NLS-1$
			tempDir = temp != null ? new Path(temp) : null;
			assertNotNull("Missing user home directory", tempDir); //$NON-NLS-1$
		}

		tempDir = tempDir.append(ProcessLauncherTestCase.class.getSimpleName());
		assertNotNull("Cannot append test case specific temp directory", tempDir); //$NON-NLS-1$
		if (!tempDir.toFile().exists()) {
			assertTrue("Failed to create path " + tempDir.toString(), tempDir.toFile().mkdirs()); //$NON-NLS-1$
		}
		assertTrue("Temporary file location is not writable (" + tempDir.toOSString() + ")", tempDir.toFile().canWrite()); //$NON-NLS-1$ //$NON-NLS-2$

		IPath tempHelloWorld = tempDir.append(helloWorldLocation.lastSegment());
		if (tempHelloWorld.toFile().exists()) {
			tempHelloWorld.toFile().setWritable(true, false);
			tempHelloWorld.toFile().delete();
		}
		assertFalse("Cannot delete process image " + tempHelloWorld.toOSString(), tempHelloWorld.toFile().exists()); //$NON-NLS-1$

		// Copy the file manually. Using file transfer leads to an assertion in the agent
		try {
			copyFile(helloWorldLocation.toFile(), tempHelloWorld.toFile());
			tempHelloWorld.toFile().setExecutable(true);
		} catch (IOException e) {
			assertNull("Failed to copy file from " + helloWorldLocation.toOSString() + " to " + tempHelloWorld.toOSString() + ": " + e, e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		final AtomicBoolean processExited = new AtomicBoolean();

		// Create the process streams proxy
		ProcessStreamsProxy proxy = new ProcessStreamsProxy();
		// Create the process launcher
		ProcessLauncher launcher = new ProcessLauncher(proxy) {
			@Override
			protected ProcessesListener createProcessesListener() {
			    return new ProcessProcessesListener(this) {
			    	@Override
			    	public void exited(String processId, int exitCode) {
			    	    super.exited(processId, exitCode);
			    	    processExited.set(true);
			    	}
			    };
			}
		};

		// Create the launch properties
		IPropertiesContainer properties = new PropertiesContainer();
		properties.setProperty(IProcessLauncher.PROP_PROCESS_PATH, tempHelloWorld.toString());
		properties.setProperty(IProcessLauncher.PROP_PROCESS_ASSOCIATE_CONSOLE, true);

		// Launch the process
		launcher.launch(peer, properties, new Callback() {
			@Override
			protected void internalDone(Object caller, IStatus status) {
				if (status.getSeverity() != IStatus.OK && status.getSeverity() != IStatus.INFO) {
					System.out.println("ProcessLauncherTestCase: launch returned with status:\n" + status.toString()); //$NON-NLS-1$
				}
			}
		});

		// Wait for the process to terminate
		int counter = 50;
		while (counter > 0 && !processExited.get()) {
			waitAndDispatch(100);
			counter--;
		}
		assertTrue("Test application did not exit in time.", processExited.get()); //$NON-NLS-1$

		// Bug 431347: ProcessOutputReaderThread does not terminate itself on EOF
		proxy.getOutputReader().interrupt();

		// Wait for the output reader to finish
		counter = 240;
		while (counter > 0) {
			if (proxy.getOutputReader() != null && proxy.getOutputReader().isFinished()) break;
			waitAndDispatch(500);
			counter--;
		}
		assertTrue("Process output reader thread not finished.", proxy.getOutputReader() != null ? proxy.getOutputReader().isFinished() : true); //$NON-NLS-1$

		// Read the output from the reader
		String output = proxy.getOutputReader() != null ? proxy.getOutputReader().getOutput() : null;
		assertEquals("Unexpected output from helloWorls test application.", "Hello World", output != null ? output.trim() : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// Dispose the launcher at the end
		launcher.dispose();
	}

	//***** END SECTION: Single test methods *****

}
