package org.syncany.cli;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.commons.codec.binary.Base64;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.config.to.RepoTO;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.crypto.CipherUtil;

public class ConnectCommand extends AbstractInitCommand {
	public static final Pattern LINK_PATTERN = Pattern.compile("^syncany://storage/1/(not-encrypted/)?(.+)$");	
	
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
		
		String notEncryptedFlag = linkMatcher.group(1);
		String ciphertext = linkMatcher.group(2);
		String plaintext = null;
		
		boolean isEncryptedLink = !"".equals(notEncryptedFlag);
		
		if (isEncryptedLink) {
			password = askPassword();
			
			byte[] ciphertextBytes = Base64.decodeBase64(ciphertext);
			ByteArrayInputStream encryptedStorageConfig = new ByteArrayInputStream(ciphertextBytes);
			
			plaintext = CipherUtil.decryptToString(encryptedStorageConfig, password);					
		}
		else {
			plaintext = new String(Base64.decodeBase64(ciphertext));
		}
		
		//System.out.println(plaintext);

		Serializer serializer = new Persister();
		ConnectionTO connectionTO = serializer.read(ConnectionTO.class, plaintext);
		
		System.out.println(connectionTO);
		
		plugin = Plugins.get(connectionTO.getType());
		pluginSettings = connectionTO.getSettings();
		
		connection = plugin.createConnection();
		connection.init(pluginSettings);		
	}

	private void initPluginByAsking() throws StorageException {
		askPlugin();
		askPluginSettings();		
		
		password = askPassword(); // TODO [low] This is not necessary for non-encrypted repo files
	}

	protected File downloadEncryptedRepoFile() throws Exception {
		File tmpRepoFile = File.createTempFile("syncanyrepo", "tmp");
		
		try {
			out.print("Trying to connect ... ");
			TransferManager transferManager = connection.createTransferManager();
			Map<String, RemoteFile> repoFileList = transferManager.list("repo");
			
			if (repoFileList.containsKey("repo")) {
				transferManager.download(new RemoteFile("repo"), tmpRepoFile);
				return tmpRepoFile;
			}			
			else {
				return null;
			}
		}
		catch (Exception e) {
			throw new Exception("Unable to connect to repository.", e);
		}		
	}
	
	private void downloadAndInitRepo() throws Exception {
		File tmpEncryptedRepoFile = downloadEncryptedRepoFile();		
		FileInputStream encryptedRepoConfig = new FileInputStream(tmpEncryptedRepoFile);
		
		String repoFileStr = CipherUtil.decryptToString(encryptedRepoConfig, password);
		
		Serializer serializer = new Persister();
		RepoTO repoTO = serializer.read(RepoTO.class, repoFileStr);
		
		System.out.println(repoFileStr);
		System.out.println(repoTO);
		
	}	

	private String askPassword() { // TODO [low] Duplicate code in CommandLineClient
		String password = null;
		
		while (password == null) {
			char[] passwordChars = console.readPassword("Password: ");			
			password = new String(passwordChars);			
		}	
		
		return password;
	}
}
