/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.daemon.command;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.operations.ConnectOperation;
import org.syncany.operations.ConnectOperation.ConnectOperationListener;
import org.syncany.operations.ConnectOperation.ConnectOperationOptions;
import org.syncany.operations.ConnectOperation.ConnectOperationResult;

public class ConnectCommand extends AbstractInitCommand implements ConnectOperationListener {
	private static final Pattern LINK_PATTERN = Pattern.compile("^syncany://storage/1/(?:(not-encrypted/)(.+)|([^-]+-(.+)))$");
	private static final int LINK_PATTERN_GROUP_NOT_ENCRYPTED_FLAG = 1;
	private static final int LINK_PATTERN_GROUP_NOT_ENCRYPTED_ENCODED = 2;
	private static final int LINK_PATTERN_GROUP_ENCRYPTED_MASTER_KEY_SALT = 3;
	private static final int LINK_PATTERN_GROUP_ENCRYPTED_ENCODED = 4;
	
	private SaltedSecretKey masterKey;
	private String url;
	private String localDir;
	private String password;
	private String pluginName;
	private List<String> pluginArgs;
	
	public ConnectCommand(String url, String pluginName, List<String> pluginArgs, String localDir, String password){
		this.url = url;
		this.localDir = localDir;
		this.pluginArgs = pluginArgs;
		this.password = password;
		this.pluginName = pluginName;
	}
	
	public ConnectOperationResult execute() throws Exception {
		ConnectOperationOptions operationOptions = parseConnectOptions();
		ConnectOperation co = new ConnectOperation(operationOptions, this);
		ConnectOperationResult operationResult = co.execute();
		return operationResult;
	}

	private ConnectOperationOptions parseConnectOptions() throws Exception {
		ConnectOperationOptions operationOptions = new ConnectOperationOptions();

		// Plugin
		ConnectionTO connectionTO;
		
		if (url != null) {
			connectionTO  = initPluginWithLink(url);
		}
		else {
			connectionTO = initPluginWithOptions(pluginName, pluginArgs);
		}
		
		ConfigTO configTO = createConfigTO(new File(localDir), masterKey, connectionTO);
		
		operationOptions.setLocalDir(new File(localDir));
		operationOptions.setConfigTO(configTO);
		
		return operationOptions;		
	}	

	private ConnectionTO initPluginWithLink(String link) throws Exception {
		Matcher linkMatcher = LINK_PATTERN.matcher(link);
		
		if (!linkMatcher.matches()) {
			throw new Exception("Invalid link provided, must start with syncany:// and match link pattern.");
		}
		
		String notEncryptedFlag = linkMatcher.group(LINK_PATTERN_GROUP_NOT_ENCRYPTED_FLAG);
		
		String plaintext = null;
		boolean isEncryptedLink = notEncryptedFlag == null;
		
		if (isEncryptedLink) {
			String masterKeySaltStr = linkMatcher.group(LINK_PATTERN_GROUP_ENCRYPTED_MASTER_KEY_SALT);
			String ciphertext = linkMatcher.group(LINK_PATTERN_GROUP_ENCRYPTED_ENCODED);
			
			byte[] masterKeySalt = Base64.decodeBase64(masterKeySaltStr);
			byte[] ciphertextBytes = Base64.decodeBase64(ciphertext);

			notifyCreateMasterKey();
			masterKey = CipherUtil.createMasterKey(password, masterKeySalt);
			
			ByteArrayInputStream encryptedStorageConfig = new ByteArrayInputStream(ciphertextBytes);
			
			plaintext = new String(CipherUtil.decrypt(encryptedStorageConfig, masterKey));					
		}
		else {
			String encodedPlaintext = linkMatcher.group(LINK_PATTERN_GROUP_NOT_ENCRYPTED_ENCODED);
			plaintext = new String(Base64.decodeBase64(encodedPlaintext));
		}
		
		Serializer serializer = new Persister();
		ConnectionTO connectionTO = serializer.read(ConnectionTO.class, plaintext);		
		
		Plugin plugin = Plugins.get(connectionTO.getType());
		
		if (plugin == null) {
			throw new Exception("Link contains unknown connection type '"+connectionTO.getType()+"'. Corresponding plugin not found.");
		}
		
		return connectionTO;			
	}


	@Override
	public void disposeCommand() {
		
	}

	@Override
	public void notifyCreateMasterKey() {
		
	}

	@Override
	public String askPassword() {
		return password;
	}

	@Override
	public boolean askRetryPassword() {
		// TODO throw something
		return false;
	}
}
