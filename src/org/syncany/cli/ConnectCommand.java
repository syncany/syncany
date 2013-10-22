package org.syncany.cli;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.commons.codec.binary.Base64;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Encryption;
import org.syncany.config.to.RepoTO;
import org.syncany.config.to.StorageTO;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.StorageException;
import org.syncany.crypto.CipherUtil;

public class ConnectCommand extends AbstractInitCommand {
	public static final Pattern LINK_PATTERN = Pattern.compile("^syncany://storage/1/(e|d)/(.+)$");	
	
	static { // TODO [high] Workaround
		try {
			Encryption.init();
			Encryption.enableUnlimitedCrypto();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}
	
	private String password;
	
	public ConnectCommand() {
		super();
	}

	@Override
	public boolean needConfigFile() {	
		return false;
	}

	@Override
	public int execute(String[] operationArgs) throws Exception {
		runConnectOperation(operationArgs);
		return 0;
	}	

	private void runConnectOperation(String[] operationArguments) throws OptionException, Exception {
		OptionParser parser = new OptionParser();	
		
		OptionSet options = parser.parse(operationArguments);	
		List<?> nonOptionArgs = options.nonOptionArguments();
		
		if (nonOptionArgs.size() == 0) {
			initPluginByAsking();
			downloadAndInitRepo();
		}
		else if (nonOptionArgs.size() == 1) {
			initPluginWithLink((String) nonOptionArgs.get(0));
			downloadAndInitRepo();
		}
		else {
			throw new Exception("Invalid syntax.");
		}
	}
	
	private void initPluginWithLink(String link) throws Exception {
		Matcher linkMatcher = LINK_PATTERN.matcher(link);
		
		if (!linkMatcher.matches()) {
			throw new Exception("Invalid link provided, must start with syncany:// and match link pattern.");
		}
		
		String encryptionIndicator = linkMatcher.group(1);
		String ciphertext = linkMatcher.group(2);
		String plaintext = null;
		
		boolean isEncryptedLink = "e".equals(encryptionIndicator);
		
		if (isEncryptedLink) {
			password = askPassword();
			
			byte[] ciphertextBytes = Base64.decodeBase64(ciphertext);
			ByteArrayInputStream encryptedStorageConfig = new ByteArrayInputStream(ciphertextBytes);
			
			plaintext = CipherUtil.decryptToString(encryptedStorageConfig, password);					
		}
		else {
			plaintext = new String(Base64.decodeBase64(ciphertext));
		}
		
		System.out.println(plaintext);

		Serializer serializer = new Persister();
		StorageTO storageTO = serializer.read(StorageTO.class, plaintext);
		
		System.out.println(storageTO);
		
		plugin = Plugins.get(storageTO.getConnection().getType());
		pluginSettings = storageTO.getConnection().getSettings();
		
		connection = plugin.createConnection();
		connection.init(pluginSettings);		
	}

	private void initPluginByAsking() throws StorageException {
		askPlugin();
		askPluginSettings();		
		
		password = askPassword(); // TODO [low] This is not necessary for non-encrypted repo files
	}

	private void downloadAndInitRepo() throws Exception {
		File tmpEncryptedRepoFile = downloadEncryptedRepoFile();		
		FileInputStream encryptedRepoConfig = new FileInputStream(tmpEncryptedRepoFile);
		
		String repoFileStr = CipherUtil.decryptToString(encryptedRepoConfig, password);
		System.out.println("repotfilestr = "+repoFileStr);
		Serializer serializer = new Persister();
		RepoTO repoTO = serializer.read(RepoTO.class, repoFileStr);
		
		System.out.println(repoFileStr);
		System.out.println(repoTO);
		
	}	

	private String askPassword() {
		String password = null;
		
		while (password == null) {
			char[] passwordChars = console.readPassword("Password: ");			
			password = new String(passwordChars);			
		}	
		
		return password;
	}
}
