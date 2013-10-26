package org.syncany.cli;

import static java.util.Arrays.asList;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;
import org.syncany.config.to.ConfigTO;
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
		OptionSpec<String> optionFolder = parser.acceptsAll(asList("f", "folder")).withRequiredArg();
		OptionSpec<String> optionPlugin = parser.acceptsAll(asList("p", "plugin")).withRequiredArg();
		OptionSpec<String> optionPluginOpts = parser.acceptsAll(asList("P", "plugin-option")).withRequiredArg();
		
		OptionSet options = parser.parse(operationArguments);	
		List<?> nonOptionArgs = options.nonOptionArguments();
		
		// --folder=<local dir>
		File localDir = null;
        
        if (options.has(optionFolder)) {
            String locationStr = options.valueOf(optionFolder);
            localDir = new File(locationStr).getCanonicalFile();
        }
        else {
            localDir = new File(".").getCanonicalFile();                         
        }        
		
		if (nonOptionArgs.size() == 1) {
			initPluginWithLink((String) nonOptionArgs.get(0));			
		}
		else if (options.has(optionPlugin)) {
			if (options.has(optionPlugin)) {
				initPlugin(options.valueOf(optionPlugin));
			}
			else {
				askPlugin();
			}
			
			if (options.has(optionPluginOpts)) {
				initPluginSettings(options.valuesOf(optionPluginOpts));			
			}
			else {
				askPluginSettings();
			}		
		}
		else if (nonOptionArgs.size() == 0) {
			initPluginByAsking();
		}
		else {
			throw new Exception("Invalid syntax.");
		}
		
		File tmpRepoFile = downloadRepoFile();			
		RepoTO repoTO = createRepoTOFromFile(tmpRepoFile);
		ConfigTO configTO = createConfigTO(localDir, password);
		
		File appDir = new File(localDir+"/"+Config.DEFAULT_DIR_APPLICATION); // TODO [medium] Duplicate code in InitOperation
		File configFile = new File(appDir+"/"+Config.DEFAULT_FILE_CONFIG);
		File repoFile = new File(appDir+"/"+Config.DEFAULT_FILE_REPO);
		
		if (!appDir.exists()) {
			appDir.mkdir();
		}
		
		// Unset 'localdir' (it should not be saved)
		configTO.setLocalDir(null);
		
		writeXmlFile(configTO, configFile);
		FileUtils.copyFile(tmpRepoFile, repoFile);
	}
	
	private void initPluginWithLink(String link) throws Exception {
		Matcher linkMatcher = LINK_PATTERN.matcher(link);
		
		if (!linkMatcher.matches()) {
			throw new Exception("Invalid link provided, must start with syncany:// and match link pattern.");
		}
		
		String notEncryptedFlag = linkMatcher.group(1);
		String ciphertext = linkMatcher.group(2);
		String plaintext = null;
		
		boolean isEncryptedLink = notEncryptedFlag == null;
		
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
	}

	protected File downloadRepoFile() throws Exception {
		File tmpRepoFile = File.createTempFile("syncanyrepo", "tmp");
		
		try {
			out.print("Trying to connect ... ");
			
			TransferManager transferManager = connection.createTransferManager();
			transferManager.download(new RemoteFile("repo"), tmpRepoFile);
			
			return tmpRepoFile;			
		}
		catch (Exception e) {
			throw new Exception("Unable to connect to repository.", e);
		}		
	}
	
	private RepoTO createRepoTOFromFile(File file) throws Exception {
		if (CipherUtil.isEncrypted(file)) {
			if (password == null) {
				password = askPassword();
			}
			
			FileInputStream encryptedRepoConfig = new FileInputStream(file);
			String repoFileStr = CipherUtil.decryptToString(encryptedRepoConfig, password);
			
			Serializer serializer = new Persister();
			RepoTO repoTO = serializer.read(RepoTO.class, repoFileStr);

			return repoTO;
		}
		else {
			Serializer serializer = new Persister();
			RepoTO repoTO = serializer.read(RepoTO.class, file);

			return repoTO;
		}		
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
