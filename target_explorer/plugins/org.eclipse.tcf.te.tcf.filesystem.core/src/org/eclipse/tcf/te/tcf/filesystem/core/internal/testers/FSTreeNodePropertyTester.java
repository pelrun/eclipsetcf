/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * William Chen (Wind River) - [345387]Open the remote files with a proper editor
 * William Chen (Wind River) - [352302]Opening a file in an editor depending on
 *                             the client's permissions.
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.testers;

import java.io.File;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.CacheManager;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.ContentTypeHelper;
import org.eclipse.tcf.te.tcf.filesystem.core.model.CacheState;

/**
 * The property tester of an FSTreeNode.
 * <p>
 * The properties include
 * <ul>
 * <li>"isFile" if it is a file node,</li>
 * <li>"isDirectory" if it is a directory,</li>
 * <li>"isBinaryFile" if it is a binary file,</li>
 * <li>"isReadable" if it is readable,</li>
 * <li>"isWritable" if it is writable,</li>
 * <li>"isExecutable" if it is executable,</li>
 * <li>"isRoot" if it is a root directory,</li>
 * <li>"isWindows" if it is a windows file node,</li>
 * <li>"isReadOnly" if it is read only,</li>
 * <li>"isHidden" if it is hidden,</li>
 * <li>"getCacheState" to get a node's state.</li>
 * </ul>
 * <p>
 * "testParent" is a property by which the parent or even the grand parent
 * of a node can be tested. The arguments is a recursive list of the above
 * test property including "testParent".
 * <p>
 * The following is an example of how it is used.
 * <pre>
 *     &lt;test
 *         args="isWritable"
 *         property="org.eclipse.tcf.te.tcf.filesystem.propertytester.treenode.testParent"&gt;
 *     &lt;/test&gt;
 * </pre>
 * <p>
 * The above example tests if the parent node is writable.
 * <pre>
 *     &lt;test
 *         args="testParent,isWritable"
 *         property="org.eclipse.tcf.te.tcf.filesystem.propertytester.treenode.testParent"&gt;
 *     &lt;/test&gt;
 * </pre>
 * <p>
 * The above example tests if the grand parent node is writable.
 * <p>
 * And so on, you can test its ancestor recursively:
 * <pre>
 *     &lt;test
 *         args="testParent,testParent,testParent,...,isWritable"
 *         property="org.eclipse.tcf.te.tcf.filesystem.propertytester.treenode.testParent"&gt;
 *     &lt;/test&gt;
 * </pre>
 */
public class FSTreeNodePropertyTester extends PropertyTester {
	private enum Property {
		isFile, isDirectory, isBinaryFile, isReadable, isWritable, isExecutable, isRoot,
		isSystemRoot, isWindows, isReadOnly, isHidden, testParent, getCacheState, isRevealOnConnect
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if(receiver == null)
			return false;
		Property prop;
		try {
			prop = Property.valueOf(property);
		} catch (Exception e) {
			return false;
		}

		Assert.isTrue(receiver instanceof FSTreeNode);
		FSTreeNode node = (FSTreeNode) receiver;
		switch (prop) {
		case getCacheState:
			File file = CacheManager.getCacheFile(node);
			if(!file.exists())
				return false;
			CacheState state = node.getCacheState();
			return state.name().equals(expectedValue);
		case isBinaryFile:
			return ContentTypeHelper.isBinaryFile(node);
		case isDirectory:
			return node.isDirectory();
		case isExecutable:
			return node.isExecutable();
		case isRevealOnConnect:
			return node.isRevealOnConnect();
		case isFile:
			return node.isFile();
		case isHidden:
			return node.isHidden();
		case isReadOnly:
			return node.isReadOnly();
		case isReadable:
			return node.isReadable();
		case isRoot:
			return node.isRootDirectory();
		case isSystemRoot:
			return node.isFileSystem();
		case isWindows:
			return node.isWindowsNode();
		case isWritable:
			return node.isWritable();
		case testParent:
			return testParent(node, args, expectedValue);
		}
		return false;
	}

	private boolean testParent(FSTreeNode node, Object[] args, Object expectedValue) {
		if(args == null || args.length == 0)
			return false;
		String arg = (String) args[0];
		Object[] newArgs = new Object[args.length -1];
		System.arraycopy(args, 1, newArgs, 0, args.length - 1);
	    return test(node.getParent(), arg, newArgs, expectedValue);
    }
}
