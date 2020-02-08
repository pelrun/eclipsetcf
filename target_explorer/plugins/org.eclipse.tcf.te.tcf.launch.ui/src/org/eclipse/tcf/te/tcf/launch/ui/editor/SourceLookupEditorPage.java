/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.editor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupPanel;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.core.cdt.CdtUtils;
import org.eclipse.tcf.te.tcf.launch.ui.nls.Messages;

/**
 * Source lookup launch configuration tab container page implementation.
 */
@SuppressWarnings({ "restriction", "deprecation" })
public class SourceLookupEditorPage extends AbstractTcfLaunchTabContainerEditorPage {

	/**
	 * Extension to the standard source lookup panel to be embedded into an editor page
	 */
	protected static class EditorSourceLookupPanel extends SourceLookupPanel {
		private final AbstractTcfLaunchTabContainerEditorPage editorPage;
		private IPropertyChangeListener listener;

		// Set the flag to true if the page is initializing
		private boolean isInitializing = false;

		/**
         * Constructor
         *
         * @param editorPage The parent editor page. Must not be <code>null</code>.
         */
        public EditorSourceLookupPanel(AbstractTcfLaunchTabContainerEditorPage editorPage) {
        	super();

        	Assert.isNotNull(editorPage);
        	this.editorPage = editorPage;
        }

        /* (non-Javadoc)
         * @see org.eclipse.debug.internal.ui.sourcelookup.SourceLookupPanel#dispose()
         */
        @Override
        public void dispose() {
        	if (listener != null) {
    			Preferences prefs = CdtUtils.getDebugCorePluginPreferences();
    			if (prefs != null) prefs.removePropertyChangeListener(listener);
        		listener = null;
        	}
            super.dispose();
        }

		/* (non-Javadoc)
		 * @see org.eclipse.debug.internal.ui.sourcelookup.SourceLookupPanel#createControl(org.eclipse.swt.widgets.Composite)
		 */
		@Override
		public void createControl(Composite parent) {
			super.createControl(parent);
			((Composite)getControl()).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			// Create the preferences listener
			listener = new IPropertyChangeListener() {

				@SuppressWarnings("synthetic-access")
                @Override
				public void propertyChange(PropertyChangeEvent event) {
					if ("org.eclipse.cdt.debug.core.cDebug.default_source_containers".equals(event.getProperty())) { //$NON-NLS-1$
                        initializeFrom(getLaunchConfig(editorPage.getPeerModel(editorPage.getEditorInput())));
						//fPathViewer.refresh(true);
					}
				}
			};

			// Register preferences listener.
			// Note the the CDT debug core plugin still uses the deprecated plugin preferences.
			Preferences prefs = CdtUtils.getDebugCorePluginPreferences();
			if (prefs != null) prefs.addPropertyChangeListener(listener);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.debug.internal.ui.sourcelookup.SourceLookupPanel#getName()
		 */
		@Override
		public String getName() {
		    return Messages.SourceLookupEditorPage_name;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.debug.internal.ui.sourcelookup.SourceLookupPanel#updateLaunchConfigurationDialog()
		 */
		@Override
		protected void updateLaunchConfigurationDialog() {
			super.updateLaunchConfigurationDialog();
			if (!isInitializing) {
				performApply(getLaunchConfig(editorPage.getPeerModel(editorPage.getEditorInput())));
				editorPage.checkLaunchConfigDirty();
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.debug.internal.ui.sourcelookup.SourceLookupPanel#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
		 */
		@Override
		public void initializeFrom(ILaunchConfiguration configuration) {
			boolean oldDirty = editorPage.getEditor().isDirty() || editorPage.checkLaunchConfigDirty();
			isInitializing = true;
			super.initializeFrom(configuration);
			isInitializing = false;
			if (!oldDirty && editorPage.checkLaunchConfigDirty()) {
				editorPage.extractData();
			}
		}

		@Override
        public Image getImage() {
			return DebugPluginImages.getImage(IInternalDebugUIConstants.IMG_SRC_LOOKUP_TAB);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.editor.AbstractLaunchTabContainerEditorPage#createLaunchConfigurationTab()
	 */
	@Override
	protected AbstractLaunchConfigurationTab createLaunchConfigurationTab() {
		return new EditorSourceLookupPanel(this);
	}
}
