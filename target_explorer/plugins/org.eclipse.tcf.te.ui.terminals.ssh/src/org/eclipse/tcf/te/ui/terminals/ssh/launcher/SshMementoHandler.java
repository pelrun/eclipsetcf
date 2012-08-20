/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.ssh.launcher;

import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tcf.te.ui.terminals.interfaces.IMementoHandler;
import org.eclipse.ui.IMemento;

/**
 * SSH terminal connection memento handler implementation.
 */
public class SshMementoHandler implements IMementoHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.IMementoHandler#saveState(org.eclipse.ui.IMemento, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void saveState(IMemento memento, IPropertiesContainer properties) {
		Assert.isNotNull(memento);
		Assert.isNotNull(properties);

		// Do not write the terminal title to the memento -> needs to
		// be recreated at the time of restoration.
		memento.putString(ITerminalsConnectorConstants.PROP_IP_HOST, properties.getStringProperty(ITerminalsConnectorConstants.PROP_IP_HOST));
		memento.putString(ITerminalsConnectorConstants.PROP_IP_PORT, properties.getStringProperty(ITerminalsConnectorConstants.PROP_IP_PORT));
		memento.putInteger(ITerminalsConnectorConstants.PROP_TIMEOUT, properties.getIntProperty(ITerminalsConnectorConstants.PROP_TIMEOUT));
		memento.putInteger(ITerminalsConnectorConstants.PROP_SSH_KEEP_ALIVE, properties.getIntProperty(ITerminalsConnectorConstants.PROP_SSH_KEEP_ALIVE));
		memento.putString(ITerminalsConnectorConstants.PROP_SSH_USER, properties.getStringProperty(ITerminalsConnectorConstants.PROP_SSH_USER));
		memento.putString(ITerminalsConnectorConstants.PROP_ENCODING, properties.getStringProperty(ITerminalsConnectorConstants.PROP_ENCODING));

		// The password is stored within the Eclipse secure preferences -> no need to store it to the memento
		//
		// If ever needed, this is an example on how to encrypt the password using 3DES. Do not remove!

		/*
		String password = properties.getStringProperty(ITerminalsConnectorConstants.PROP_SSH_PASSWORD);
		if (password != null) {
			try {
    			// Generate a temporary key. In practice, you would save this key.
    			// See also Encrypting with DES Using a Pass Phrase.
    			// SecretKey key = KeyGenerator.getInstance("DESede").generateKey();

				SecretKeyFactory factory = SecretKeyFactory.getInstance("DESede"); //$NON-NLS-1$
				SecretKey key = factory.generateSecret(new DESKeySpec((ITerminalsConnectorConstants.PROP_SSH_PASSWORD + ".SshMementoHandler").getBytes("UTF-8"))); //$NON-NLS-1$ //$NON-NLS-2$

	            Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding"); //$NON-NLS-1$
	            cipher.init(Cipher.ENCRYPT_MODE, key);

	            String encrypedPwd = new String(Base64.encode(cipher.doFinal(password.getBytes("UTF-8")))); //$NON-NLS-1$
	            memento.putString(ITerminalsConnectorConstants.PROP_SSH_PASSWORD, encrypedPwd);
            }
            catch (Exception e) {
            	if (Platform.inDebugMode()) e.printStackTrace();
            }
		}
		*/
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.IMementoHandler#restoreState(org.eclipse.ui.IMemento, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void restoreState(IMemento memento, IPropertiesContainer properties) {
		Assert.isNotNull(memento);
		Assert.isNotNull(properties);

		// Restore the terminal properties from the memento
		properties.setProperty(ITerminalsConnectorConstants.PROP_IP_HOST, memento.getString(ITerminalsConnectorConstants.PROP_IP_HOST));
		properties.setProperty(ITerminalsConnectorConstants.PROP_IP_PORT, memento.getString(ITerminalsConnectorConstants.PROP_IP_PORT));
		properties.setProperty(ITerminalsConnectorConstants.PROP_TIMEOUT, memento.getInteger(ITerminalsConnectorConstants.PROP_TIMEOUT));
		properties.setProperty(ITerminalsConnectorConstants.PROP_SSH_KEEP_ALIVE, memento.getInteger(ITerminalsConnectorConstants.PROP_SSH_KEEP_ALIVE));
		properties.setProperty(ITerminalsConnectorConstants.PROP_SSH_USER, memento.getString(ITerminalsConnectorConstants.PROP_SSH_USER));
		properties.setProperty(ITerminalsConnectorConstants.PROP_ENCODING, memento.getString(ITerminalsConnectorConstants.PROP_ENCODING));

		// The password is stored within the Eclipse secure preferences -> restore it from there
		// To access the secure storage, we need the preference instance
		String password = null;
		ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
		if (preferences != null && properties.getStringProperty(ITerminalsConnectorConstants.PROP_IP_HOST) != null) {
			// Construct the secure preferences node key
			String nodeKey = "/Target Explorer SSH Password/" + properties.getStringProperty(ITerminalsConnectorConstants.PROP_IP_HOST); //$NON-NLS-1$
			ISecurePreferences node = preferences.node(nodeKey);
			if (node != null) {
				try {
					password = node.get("password", null); //$NON-NLS-1$
				}
				catch (StorageException ex) { /* ignored on purpose */ }
			}
		}

		// Example of restoring the password from an 3DES encrypted string. Do not remove!
		/*
        String encrypedPwd = memento.getString(ITerminalsConnectorConstants.PROP_SSH_PASSWORD);
        if (encrypedPwd != null) {
        	try {
        		SecretKeyFactory factory = SecretKeyFactory.getInstance("DESede"); //$NON-NLS-1$
        		SecretKey key = factory.generateSecret(new DESKeySpec((ITerminalsConnectorConstants.PROP_SSH_PASSWORD + ".SshMementoHandler").getBytes("UTF-8"))); //$NON-NLS-1$ //$NON-NLS-2$

        		Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding"); //$NON-NLS-1$
        		cipher.init(Cipher.DECRYPT_MODE, key);

        		byte[] encBytes = Base64.decode(encrypedPwd.getBytes("UTF-8")); //$NON-NLS-1$
        		byte[] decBytes = cipher.doFinal(encBytes);

        		password = new String(decBytes);
        	}
        	catch (Exception e) {
        		if (Platform.inDebugMode()) e.printStackTrace();
        	}
        }
        */

		properties.setProperty(ITerminalsConnectorConstants.PROP_SSH_PASSWORD, password);
	}

}
