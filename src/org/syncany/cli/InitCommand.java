package org.syncany.cli;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.chunk.CipherTransformer;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.RepoTO;
import org.syncany.config.to.RepoTO.ChunkerTO;
import org.syncany.config.to.RepoTO.MultiChunkerTO;
import org.syncany.config.to.RepoTO.TransformerTO;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherSpecs;
import org.syncany.crypto.CipherUtil;
import org.syncany.operations.InitOperation.InitOperationOptions;
import org.syncany.operations.InitOperation.InitOperationResult;
import org.syncany.util.StringUtil;
import org.syncany.util.StringUtil.StringJoinListener;

public class InitCommand extends AbstractInitCommand {
	public static final int[] DEFAULT_CIPHER_SUITE_IDS = new int[] { 1, 2 };
	public static final int PASSWORD_MIN_LENGTH = 8;
	public static final int PASSWORD_WARN_LENGTH = 12;
	
	@Override
	public boolean needConfigFile() {	
		return false;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		InitOperationOptions operationOptions = parseInitOptions(operationArgs);
		InitOperationResult operationResult = client.init(operationOptions);
		
		printResults(operationResult);
		
		return 0;		
	}

	private InitOperationOptions parseInitOptions(String[] operationArguments) throws Exception {
		InitOperationOptions operationOptions = new InitOperationOptions();

		OptionParser parser = new OptionParser();
		OptionSpec<String> optionFolder = parser.acceptsAll(asList("f", "folder")).withRequiredArg();
		OptionSpec<Void> optionAdvanced = parser.acceptsAll(asList("a", "advanced"));
		OptionSpec<Void> optionNoGzip = parser.acceptsAll(asList("g", "no-gzip"));
		OptionSpec<Void> optionNoEncryption = parser.acceptsAll(asList("e", "no-encryption"));
		OptionSpec<String> optionPlugin = parser.acceptsAll(asList("p", "plugin")).withRequiredArg();
		OptionSpec<String> optionPluginOpts = parser.acceptsAll(asList("P", "plugin-option")).withRequiredArg();
		
		OptionSet options = parser.parse(operationArguments);	
	
		// --folder=<local dir>
		File localDir = null;
        
        if (options.has(optionFolder)) {
            String locationStr = options.valueOf(optionFolder);
            localDir = new File(locationStr).getCanonicalFile();
        }
        else {
            localDir = new File(".").getCanonicalFile();                         
        }        
		
		out.println("Interactive repository initialization started.");
		out.println();
						
		// Ask for plugin, and plugin settings
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
		
		out.print("Trying to connect ... ");

		if (encryptedRepoFileExistsOnRemoteStorage()) {
			throw new Exception("Repo already exists. Use 'connect' command to connect to existing repository.");
		}		
		
		out.println("Successful. No repository found on the remote storage. ");			
		
		boolean advancedModeEnabled = options.has(optionAdvanced);
		boolean encryptionEnabled = !options.has(optionNoEncryption);
		boolean gzipEnabled = !options.has(optionNoGzip);
		
		String password = null;
		List<CipherSpec> cipherSuites = getCipherSuites(encryptionEnabled, advancedModeEnabled);
		
		ChunkerTO chunkerTO = getDefaultChunkerTO();
		MultiChunkerTO multiChunkerTO = getDefaultMultiChunkerTO();
		List<TransformerTO> transformersTO = getTransformersTO(gzipEnabled, cipherSuites);
				
		if (encryptionEnabled) {			
			password = askPasswordAndConfirm();
		}
			
		ConfigTO configTO = createConfigTO(localDir, password);		
		RepoTO repoTO = createRepoTO(chunkerTO, multiChunkerTO, transformersTO);
		
		operationOptions.setConfigTO(configTO);
		operationOptions.setRepoTO(repoTO);
		
		operationOptions.setEncryptionEnabled(encryptionEnabled);
		operationOptions.setCipherSpecs(cipherSuites);
		operationOptions.setPassword(password);
		
		return operationOptions;
	}		

	private void printResults(InitOperationResult operationResult) {
		out.println("Share link: "+operationResult.getShareLink());
	}

	protected boolean encryptedRepoFileExistsOnRemoteStorage() throws Exception {
		try {
			TransferManager transferManager = connection.createTransferManager();
			Map<String, RemoteFile> repoFileList = transferManager.list("repo");
			
			if (repoFileList.containsKey("repo")) {
				return true;
			}			
			else {
				return false;
			}
		}
		catch (Exception e) {
			throw new Exception("Unable to connect to repository.", e);
		}		
	}

	private List<TransformerTO> getTransformersTO(boolean gzipEnabled, List<CipherSpec> cipherSuites) {
		List<TransformerTO> transformersTO = new ArrayList<TransformerTO>();
		
		if (gzipEnabled) { 
			transformersTO.add(getGzipTransformerTO());
		}

		if (cipherSuites.size() > 0) {	
			TransformerTO cipherTransformerTO = getCipherTransformerTO(cipherSuites);			
			transformersTO.add(cipherTransformerTO);
		}
		
		return transformersTO;
	}

	private List<CipherSpec> getCipherSuites(boolean encryptionEnabled, boolean advancedModeEnabled) throws Exception {
		List<CipherSpec> cipherSuites = new ArrayList<CipherSpec>();
		
		if (encryptionEnabled) {
			if (advancedModeEnabled) { 			
				cipherSuites = askCipherSuites();				
			}
			else { // Default
				for (int cipherSuiteId : DEFAULT_CIPHER_SUITE_IDS) { 
					cipherSuites.add(CipherSpecs.getCipherSpec(cipherSuiteId));
				}								
			}			
		}
		
		return cipherSuites;
	}

	private List<CipherSpec> askCipherSuites() throws Exception {
		List<CipherSpec> cipherSuites = new ArrayList<CipherSpec>();
		Map<Integer, CipherSpec> availableCipherSuites = CipherSpecs.getAvailableCipherSpecs();

		out.println();
		out.println("Please choose your encryption settings. If you're paranoid,");
		out.println("you can choose multiple cipher suites by separating with a comma.");
		out.println();
		out.println("Options:");
		
		for (CipherSpec cipherSuite : availableCipherSuites.values()) {
			out.println(" ["+cipherSuite.getId()+"] "+cipherSuite);
		}
		
		out.println();
				
		boolean continueLoop = true;
		boolean unlimitedStrengthNeeded = false;
		
		while (continueLoop) {
			String commaSeparatedCipherIdStr = console.readLine("Cipher Suite: ");			
			String[] cipherSuiteIdStrs = commaSeparatedCipherIdStr.split(",");
			
			// Choose cipher
			try {
				// Add cipher suites
				for (String cipherSuiteIdStr : cipherSuiteIdStrs) {
					Integer cipherSuiteId = Integer.parseInt(cipherSuiteIdStr);				
					CipherSpec cipherSuite = availableCipherSuites.get(cipherSuiteId);
					
					if (cipherSuite == null) {
						throw new Exception();
					}
					
					if (cipherSuite.needsUnlimitedStrength()) {
						unlimitedStrengthNeeded = true;
					}
					
					cipherSuites.add(cipherSuite);
				}
				
				// Unlimited strength
				if (unlimitedStrengthNeeded) {
					out.println();
					out.println("At least one of the chosen ciphers or key sizes might");
					out.println("not be allowed in your country.");
					out.println();
					
					String yesno = console.readLine("Are you sure you want to use it (y/n)? ");
					
					if (yesno.toLowerCase().startsWith("y")) {
						try {
							CipherUtil.enableUnlimitedCrypto();
						}
						catch (Exception e) {
							throw new Exception("Unable to enable unlimited crypto. Check out: http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html");
						}
					}
					else {
						continue;
					}
				}
				
				continueLoop = false;
				break;
			}
			catch (Exception e) {
				out.println("ERROR: Please choose at least one valid option.");
				out.println();
				
				continue;
			}
		}

		return cipherSuites;		
	}
	
	protected String askPasswordAndConfirm() {
		out.println();
		out.println("The password is used to encrypt data on the remote storage.");
		out.println("Please choose it wisely.");
		out.println();
		
		String password = null;
		
		while (password == null) {
			char[] passwordChars = console.readPassword("Password: ");
			char[] confirmPasswordChars = console.readPassword("Confirm: ");
			
			if (!Arrays.equals(passwordChars, confirmPasswordChars)) {
				out.println("ERROR: Passwords do not match.");
				out.println();
				
				continue;
			} 
			
			if (passwordChars.length < PASSWORD_MIN_LENGTH) {
				out.println("ERROR: This password is not allowed (too short, min.  chars)");
				out.println();
				
				continue;
			}
			
			if (passwordChars.length < PASSWORD_WARN_LENGTH) {
				out.println("WARNING: The password is a bit short. Less than 12 chars are not future-proof!");
				String yesno = console.readLine("Are you sure you want to use it (y/n)? ");
				
				if (!yesno.toLowerCase().startsWith("y")) {
					out.println();
					continue;
				}
			}
			
			password = new String(passwordChars);			
		}	
		
		return password;
	}
	
	protected RepoTO createRepoTO(ChunkerTO chunkerTO, MultiChunkerTO multiChunkerTO, List<TransformerTO> transformersTO) throws Exception {
		// Make transfer object
		RepoTO repoTO = new RepoTO();
				
		// Add to repo transfer object
		repoTO.setChunker(chunkerTO);
		repoTO.setMultiChunker(multiChunkerTO);
		repoTO.setTransformers(transformersTO);
		
		return repoTO;
	}	

	protected ChunkerTO getDefaultChunkerTO() {
		ChunkerTO chunkerTO = new ChunkerTO();
		
		chunkerTO.setType("fixed");
		chunkerTO.setSettings(new HashMap<String, String>());
		chunkerTO.getSettings().put("size", "16");
		
		return chunkerTO;
	}
	
	protected MultiChunkerTO getDefaultMultiChunkerTO() {
		MultiChunkerTO multichunkerTO = new MultiChunkerTO();
		
		multichunkerTO.setType("zip"); 
		multichunkerTO.setSettings(new HashMap<String, String>());
		multichunkerTO.getSettings().put("size", "512");
		
		return multichunkerTO;		
	}
	
	protected TransformerTO getGzipTransformerTO() {		
		TransformerTO gzipTransformerTO = new TransformerTO();
		gzipTransformerTO.setType("gzip");
		
		return gzipTransformerTO;				
	}
	
	private TransformerTO getCipherTransformerTO(List<CipherSpec> cipherSpec) {
		String cipherSuitesIdStr = StringUtil.join(cipherSpec, ",", new StringJoinListener<CipherSpec>() {
			@Override
			public String getString(CipherSpec cipherSpec) {
				return ""+cipherSpec.getId();
			}			
		});
		
		Map<String, String> cipherTransformerSettings = new HashMap<String, String>();
		cipherTransformerSettings.put(CipherTransformer.PROPERTY_CIPHER_SPECS, cipherSuitesIdStr);
		// Note: Property 'password' is added dynamically by CommandLineClient

		TransformerTO cipherTransformerTO = new TransformerTO();
		cipherTransformerTO.setType(CipherTransformer.TYPE);
		cipherTransformerTO.setSettings(cipherTransformerSettings);

		return cipherTransformerTO;
	}
}
