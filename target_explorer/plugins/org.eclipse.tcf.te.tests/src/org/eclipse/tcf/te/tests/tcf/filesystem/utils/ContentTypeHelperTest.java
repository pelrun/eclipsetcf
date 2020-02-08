/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf.filesystem.utils;

import static java.util.Collections.singletonList;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.ContentTypeHelper;

public class ContentTypeHelperTest extends UtilsTestBase {
	private FSTreeNode agentNode;
	@Override
    protected void setUp() throws Exception {
	    super.setUp();
		uploadAgent();
		prepareXmlFile();
    }

	private void prepareXmlFile() throws IOException {
	    StringBuilder content = new StringBuilder();
		content.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); //$NON-NLS-1$
		content.append("<root></root>\n"); //$NON-NLS-1$
		writeFileContent(content.toString());
    }

    private void uploadAgent() throws Exception {
	    File agentFile = getAgentFile().toFile();
	    String agentPath = getTestRoot() + getPathSep() + agentFile.getName();
	    agentNode = getFSNode(agentPath);
		if (agentNode == null) {
			IOperation upload = testRoot.operationDropFiles(singletonList(agentFile.getPath()), null);

			upload.run(new NullProgressMonitor());
			agentNode = getFSNode(agentPath);
			assertNotNull(agentNode);
		}
    }

	public void testBinaryFile() {
		printMessage("The agent's location is: "+agentNode.getLocation()); //$NON-NLS-1$
		IContentType contentType = ContentTypeHelper.getContentType(agentNode);
		if (contentType != null) {
			IContentType binaryFile = Platform.getContentTypeManager().getContentType("org.eclipse.cdt.core.binaryFile"); //$NON-NLS-1$
			if (binaryFile != null) {
				 if(contentType.isKindOf(binaryFile)){
					 printMessage("Successful of testing binary file"); //$NON-NLS-1$
				 }
				 else{
					 printMessage("Failure of testing binary file"); //$NON-NLS-1$
				 }
			}
			else {
				printMessage("The content type for binary file cannot be found!"); //$NON-NLS-1$
			}
		}
		else {
			printMessage("The content type of the agent node is null!"); //$NON-NLS-1$
		}
	}

	public void testContentType() {
		IContentType contentType = ContentTypeHelper.getContentType(testFile);
		assertEquals("Text", contentType.getName()); //$NON-NLS-1$
	}
}
