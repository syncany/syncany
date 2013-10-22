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
	public static final Pattern LINK_PATTERN = Pattern.compile("^syncany://storage/1/([^/]*)/(.+)$");	
	
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
		
		String salt = linkMatcher.group(1);
		String ciphertext = linkMatcher.group(2);
		
		boolean isEncryptedLink = !"".equals(salt);
		
		if (isEncryptedLink) {
			password = askPassword();
			
			ByteArrayInputStream encryptedStorageConfig = new ByteArrayInputStream(Base64.decodeBase64(ciphertext));
			String storageConfigStr = CipherUtil.decryptToString(encryptedStorageConfig, password);
			
			Serializer serializer = new Persister();
			StorageTO storageTO = serializer.read(StorageTO.class, storageConfigStr);
			
			System.out.println(storageConfigStr);
			System.out.println(storageTO);
			// deserialize, read in StorageTO
			
			plugin = Plugins.get(storageTO.getConnection().getType());
			pluginSettings = storageTO.getConnection().getSettings();
			
			
			throw new Exception("this should decrypt and read the storage.xml from the string ...");
		}
		else {
			
		}
		
	}

	private void initPluginByAsking() throws StorageException {
		askPlugin();
		askPluginSettings();		
		
		password = askPassword(); // TODO [low] This is not necessary for non-encrypted repo files
	}

	private void downloadAndInitRepo() throws Exception {
		Encryption.init(); // TODO [high] Workaround!
		
		File tmpEncryptedRepoFile = downloadEncryptedRepoFile();		
		FileInputStream encryptedRepoConfig = new FileInputStream(tmpEncryptedRepoFile);
		
		String repoFileStr = CipherUtil.decryptToString(encryptedRepoConfig, password);
		System.out.println("repotfilestr = "+repoFileStr);
		Serializer serializer = new Persister();
		RepoTO repoTO = serializer.read(RepoTO.class, repoFileStr);
		
		System.out.println(repoFileStr);
		System.out.println(repoTO);
		
	}
}
