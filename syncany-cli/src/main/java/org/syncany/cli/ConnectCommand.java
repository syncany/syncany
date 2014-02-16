/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.cli;

import static java.util.Arrays.asList;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.commons.codec.binary.Base64;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.SaltedSecretKey;
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
	
	public ConnectCommand() {
		super();
	}

	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.UNINITIALIZED_LOCALDIR;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		ConnectOperationOptions operationOptions = parseConnectOptions(operationArgs);
		ConnectOperationResult operationResult = client.connect(operationOptions, this);
		
		printResults(operationResult);
		
		return 0;		
	}

	private ConnectOperationOptions parseConnectOptions(String[] operationArguments) throws OptionException, Exception {
		ConnectOperationOptions operationOptions = new ConnectOperationOptions();

		OptionParser parser = new OptionParser();			
		OptionSpec<String> optionPlugin = parser.acceptsAll(asList("p", "plugin")).withRequiredArg();
		OptionSpec<String> optionPluginOpts = parser.acceptsAll(asList("P", "plugin-option")).withRequiredArg();
		
		OptionSet options = parser.parse(operationArguments);	
		List<?> nonOptionArgs = options.nonOptionArguments();		
		
		// Plugin
		ConnectionTO connectionTO = new ConnectionTO();
		
		if (nonOptionArgs.size() == 1) {
			connectionTO = initPluginWithLink((String) nonOptionArgs.get(0));			
		}
		else if (options.has(optionPlugin)) {
			connectionTO = initPluginWithOptions(options, optionPlugin, optionPluginOpts);
		}
		else if (nonOptionArgs.size() == 0) {
			String pluginStr = askPlugin();
			Map<String, String> pluginSettings = askPluginSettings(pluginStr);

			connectionTO.setType(pluginStr);
			connectionTO.setSettings(pluginSettings);
		}
		else {
			throw new Exception("Invalid syntax.");
		}
		
		ConfigTO configTO = createConfigTO(localDir, masterKey, connectionTO);
		
		operationOptions.setLocalDir(localDir);
		operationOptions.setConfigTO(configTO);
		
		return operationOptions;		
	}	

	private void printResults(ConnectOperationResult operationResult) {
		out.println();
		out.println("Repository connected, and local folder initialized.");
		out.println("You can now use the 'syncany' command to sync your files.");
		out.println();		
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

			String password = askPassword();
			
			notifyCreateMasterKey();
			masterKey = CipherUtil.createMasterKey(password, masterKeySalt);
			
			ByteArrayInputStream encryptedStorageConfig = new ByteArrayInputStream(ciphertextBytes);
			
			plaintext = new String(CipherUtil.decrypt(encryptedStorageConfig, masterKey));					
		}
		else {
			String encodedPlaintext = linkMatcher.group(LINK_PATTERN_GROUP_NOT_ENCRYPTED_ENCODED);
			plaintext = new String(Base64.decodeBase64(encodedPlaintext));
		}
		
		//System.out.println(plaintext);

		Serializer serializer = new Persister();
		ConnectionTO connectionTO = serializer.read(ConnectionTO.class, plaintext);		
		
		Plugin plugin = Plugins.get(connectionTO.getType());
		
		if (plugin == null) {
			throw new Exception("Link contains unknown connection type '"+connectionTO.getType()+"'. Corresponding plugin not found.");
		}
		
		return connectionTO;			
	}

	private String askPassword() { 
		out.println();
		
		String password = null;
		
		while (password == null) {
			char[] passwordChars = console.readPassword("Password: ");			
			password = new String(passwordChars);			
		}	
		
		return password;
	}

	@Override
	public String getPasswordCallback() {
		return askPassword();
	}

	@Override
	public void notifyCreateMasterKey() {
		out.println();
		out.println("Creating master key from password (this might take a while) ...");
	}
}
