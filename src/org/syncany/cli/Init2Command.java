package org.syncany.cli;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.commons.io.FileUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Encryption;
import org.syncany.config.EncryptionException;
import org.syncany.config.to.RepoTO;
import org.syncany.config.to.RepoTO.ChunkerTO;
import org.syncany.config.to.RepoTO.MultiChunkerTO;
import org.syncany.config.to.RepoTO.TransformerTO;
import org.syncany.crypto.AdvancedCipherOutputStream;
import org.syncany.crypto.CipherSession;
import org.syncany.crypto.CipherSuite;
import org.syncany.crypto.CipherSuites;

public class Init2Command extends AbstractInitCommand {
	public static final int[] DEFAULT_CIPHER_SUITE_IDS = new int[] { 1, 2 }; 	
	
	@Override
	public boolean needConfigFile() {	
		return false;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		runInitOperation(operationArgs);
		return 0;
	}	

	private void runInitOperation(String[] operationArguments) throws OptionException, Exception {
		OptionParser parser = new OptionParser();	 
		OptionSpec<Void> optionAdvanced = parser.acceptsAll(asList("a", "advanced"));
		OptionSpec<Void> optionNoGzip = parser.acceptsAll(asList("g", "no-gzip"));
		OptionSpec<Void> optionNoEncryption = parser.acceptsAll(asList("e", "no-encryption"));
		
		OptionSet options = parser.parse(operationArguments);	
		//List<?> nonOptionArgs = options.nonOptionArguments();
	
		out.println("Interactive repository initialization started.");
		out.println("Default options are in brackets.");
		out.println();
		
		initAppDir();
		String machineName = getDefaultMachineName();
		
		// Ask for plugin, and plugin settings
		askPlugin();
		askPluginSettings();
		
		File tmpEncryptedRepoFile = downloadEncryptedRepoFile();
			
		if (tmpEncryptedRepoFile != null) {
			throw new Exception("Repo already exists. Use 'connect' command to connect to existing repository.");
		}		
		
		out.println("Successful. No repository found on the remote storage. ");			
		
		String password = null;
		List<CipherSuite> cipherSuites = new ArrayList<CipherSuite>();
		
		ChunkerTO chunkerTO = getDefaultChunkerTO();
		MultiChunkerTO multiChunkerTO = getDefaultMultiChunkerTO();
		List<TransformerTO> transformersTO = new ArrayList<TransformerTO>();
		
		// Compression
		if (!options.has(optionNoGzip)) { // not --no-gzip
			transformersTO.add(getGzipTransformerTO());
		}
		
		if (!options.has(optionAdvanced)) { // Easy			
			if (!options.has(optionNoEncryption)) { // not --no-encryption
				// Add default encryption AES 128 and Twofish 128
				for (int cipherSuiteId : DEFAULT_CIPHER_SUITE_IDS) {
					CipherSuite cipherSuite = CipherSuites.getCipherSuite(cipherSuiteId);
					
					cipherSuites.add(cipherSuite);
					transformersTO.add(getCipherTransformer(cipherSuite));
				}
			}
		}
		else { // --advanced
			if (!options.has(optionNoEncryption)) { // not --no-encryption
				cipherSuites = askCipherSuites();
				
				for (CipherSuite cipherSuite : cipherSuites) {
					transformersTO.add(getCipherTransformer(cipherSuite));
				}
			}
		}
		
		// Ask for password
		if (!options.has(optionNoEncryption)) {
			password = askPassword();
		}
		
		writeStorageFile();
		writeLocalFile(machineName, password);	

		writeRepoFile(chunkerTO, multiChunkerTO, transformersTO);
		
		if (!options.has(optionNoEncryption)) {
			writeEncryptedRepoFile(cipherSuites, password);
		}
	}

	private List<CipherSuite> askCipherSuites() throws Exception {
		List<CipherSuite> cipherSuites = new ArrayList<CipherSuite>();
		Map<Integer, CipherSuite> availableCipherSuites = CipherSuites.getAvailableCipherSuites();

		out.println();
		out.println("Please choose your encryption settings. If you're paranoid,");
		out.println("you can choose multiple cipher suites by separating with a comma.");
		out.println();
		out.println("Options:");
		
		for (CipherSuite cipherSuite : availableCipherSuites.values()) {
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
					CipherSuite cipherSuite = availableCipherSuites.get(cipherSuiteId);
					
					if (cipherSuite == null) {
						throw new Exception();
					}
					
					if (cipherSuite.isUnlimitedStrength()) {
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
							Encryption.enableUnlimitedCrypto();
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
	

	protected void writeRepoFile(ChunkerTO chunkerTO, MultiChunkerTO multiChunkerTO, List<TransformerTO> transformersTO) throws Exception {
		// Make transfer object
		RepoTO repoTO = new RepoTO();
				
		// Add to repo transfer object
		repoTO.setChunker(chunkerTO);
		repoTO.setMultichunker(multiChunkerTO);
		repoTO.setTransformers(transformersTO);
		
		// Write file
		File file = new File(appDir+"/repo.xml");
		out.println("- Writing "+file);		
		
		Serializer serializer = new Persister();
		serializer.write(repoTO, file);		
	}
	
	private void writeEncryptedRepoFile(List<CipherSuite> cipherSuites, String password) throws IOException, EncryptionException {
		File fileXml = new File(appDir+"/repo.xml");
		File file = new File(appDir+"/repo");
		out.println("- Writing "+file);		
		
		Encryption.init(); // TODO workaround
		OutputStream lastOutputStream = new FileOutputStream(file);
		
		for (CipherSuite cipherSuite : cipherSuites) {
			CipherSession cipherSession = new CipherSession(cipherSuite, password);
			lastOutputStream = new AdvancedCipherOutputStream(lastOutputStream, cipherSession);
		}
			
		FileUtils.copyFile(fileXml, lastOutputStream);
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
	
	private TransformerTO getCipherTransformer(CipherSuite cipherSuite) {		
		Map<String, String> cipherSettings = new HashMap<String, String>();
		cipherSettings.put("cipher", cipherSuite.getCipherStr());
		cipherSettings.put("keysize", Integer.toString(cipherSuite.getKeySize()));		
		cipherSettings.put("iv", Boolean.toString(cipherSuite.isIv()));
		cipherSettings.put("ivsize", Integer.toString(cipherSuite.getIvSize()));
		cipherSettings.put("unlimitedstrength", Boolean.toString(cipherSuite.isUnlimitedStrength()));
		
		TransformerTO cipherTransformerTO = new TransformerTO();
		cipherTransformerTO.setType("cipher");		
		cipherTransformerTO.setSettings(cipherSettings);
		
		return cipherTransformerTO;		
	}	
}
