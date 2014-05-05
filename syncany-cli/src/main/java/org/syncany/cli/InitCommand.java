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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.chunk.Chunker;
import org.syncany.chunk.CipherTransformer;
import org.syncany.chunk.FixedChunker;
import org.syncany.chunk.GzipTransformer;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.ZipMultiChunker;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.config.to.RepoTO;
import org.syncany.config.to.RepoTO.ChunkerTO;
import org.syncany.config.to.RepoTO.MultiChunkerTO;
import org.syncany.config.to.RepoTO.TransformerTO;
import org.syncany.connection.plugins.StorageTestResult;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherSpecs;
import org.syncany.crypto.CipherUtil;
import org.syncany.operations.init.InitOperationOptions;
import org.syncany.operations.init.InitOperationResult;
import org.syncany.operations.init.InitOperationResult.InitResultCode;
import org.syncany.util.StringUtil;
import org.syncany.util.StringUtil.StringJoinListener;

public class InitCommand extends AbstractInitCommand {
	public static final int REPO_ID_LENGTH = 32;
	
	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.UNINITIALIZED_LOCALDIR;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		boolean retryNeeded = true;
		boolean performOperation = true;
		
		InitOperationOptions operationOptions = parseInitOptions(operationArgs);

		while (retryNeeded && performOperation) {
			InitOperationResult operationResult = client.init(operationOptions, this);			
			printResults(operationOptions, operationResult);
			
			retryNeeded = operationResult.getResultCode() != InitResultCode.OK;

			if (retryNeeded) {
				performOperation = isInteractive && askRetryConnection();
			
				if (performOperation) {
					updateConnectionTO(operationOptions.getConfigTO().getConnectionTO());
				}				
			}
		} 		
		
		return 0;		
	}

	private InitOperationOptions parseInitOptions(String[] operationArguments) throws Exception {
		InitOperationOptions operationOptions = new InitOperationOptions();

		OptionParser parser = new OptionParser();
		OptionSpec<Void> optionCreateTargetPath = parser.acceptsAll(asList("t", "create-target"));
		OptionSpec<Void> optionAdvanced = parser.acceptsAll(asList("a", "advanced"));
		OptionSpec<Void> optionNoCompression = parser.acceptsAll(asList("G", "no-compression"));
		OptionSpec<Void> optionNoEncryption = parser.acceptsAll(asList("E", "no-encryption"));
		OptionSpec<String> optionPlugin = parser.acceptsAll(asList("P", "plugin")).withRequiredArg();
		OptionSpec<String> optionPluginOpts = parser.acceptsAll(asList("o", "plugin-option")).withRequiredArg();
		OptionSpec<Void> optionNonInteractive = parser.acceptsAll(asList("I", "no-interaction"));
		
		OptionSet options = parser.parse(operationArguments);	
						
		ConnectionTO connectionTO = createConnectionTOFromOptions(options, optionPlugin, optionPluginOpts, optionNonInteractive);
		
		boolean createTargetPath = options.has(optionCreateTargetPath);
		boolean advancedModeEnabled = options.has(optionAdvanced);
		boolean encryptionEnabled = !options.has(optionNoEncryption);
		boolean compressionEnabled = !options.has(optionNoCompression);
		
		// --no-interaction
		isInteractive = !options.has(optionNonInteractive);

		// Cipher specs: --no-encryption, --advanced 
		List<CipherSpec> cipherSpecs = getCipherSpecs(encryptionEnabled, advancedModeEnabled);
		
		// Chunkers (not configurable)
		ChunkerTO chunkerTO = getDefaultChunkerTO();
		MultiChunkerTO multiChunkerTO = getDefaultMultiChunkerTO();

		// Compression: --no-compression
		List<TransformerTO> transformersTO = getTransformersTO(compressionEnabled, cipherSpecs);
				
		// Create configTO and repoTO
		ConfigTO configTO = createConfigTO(connectionTO);		
		RepoTO repoTO = createRepoTO(chunkerTO, multiChunkerTO, transformersTO);
		
		operationOptions.setLocalDir(localDir);
		operationOptions.setConfigTO(configTO);
		operationOptions.setRepoTO(repoTO); 
		
		operationOptions.setCreateTarget(createTargetPath);
		operationOptions.setEncryptionEnabled(encryptionEnabled);
		operationOptions.setCipherSpecs(cipherSpecs);
		operationOptions.setPassword(null); // set by callback in operation 
		
		return operationOptions;
	}		

	private void printResults(InitOperationOptions operationOptions, InitOperationResult operationResult) {
		if (operationResult.getResultCode() == InitResultCode.OK) {
			out.println();
			out.println("Repository created, and local folder initialized. To share the same repository");
			out.println("with others, you can share this link:");

			printLink(operationResult.getGenLinkResult(), false);
		}
		else if (operationResult.getResultCode() == InitResultCode.NOK_TEST_FAILED) {
			StorageTestResult testResult = operationResult.getTestResult();
			out.println();			
			
			if (testResult.isRepoFileExists()) {
				out.println("ERROR: Repository cannot be initialized, because it already exists ('syncany' file");
				out.println("       exists). Are you sure that you want to create a new repo?  Use 'sy connect'");
				out.println("       to connect to an existing repository.");
			}
			else if (!testResult.isTargetCanConnect()) {
				out.println("ERROR: Repository cannot be initialized, because the connection to the storage backend failed.");
				out.println("       Possible reasons for this could be connectivity issues (are you connect to the Internet?),");
				out.println("       or invalid user credentials (are username/password valid?).");
			}
			else if (!testResult.isTargetExists()) {
				if (!operationOptions.isCreateTarget()) {					
					out.println("ERROR: Repository cannot be initialized, because the target does not exist and");
					out.println("       the --create-target/-t option has not been enabled. Either create the target");
					out.println("       manually or retry with the --create-target/-t option.");
				}
				else {
					out.println("ERROR: Repository cannot be initialized, because the target does not exist and");
					out.println("       it cannot be created. Please check your permissions or create the target manually.");
				}
			}
			else if (!testResult.isTargetCanWrite()) {								
				out.println("ERROR: Repository cannot be initialized, because the target is not writable. This is probably");
				out.println("       a permission issue (does the user have write permissions to the target?).");
			}
			else {
				out.println("ERROR: Repository cannot be initialized.");
			}
			
			out.println();					
			printTestResult(testResult);
		}
		else {
			out.println();
			out.println("ERROR: Cannot connect to repository. Unknown error code: "+operationResult.getResultCode());
			out.println();
		}		
	}

	private List<TransformerTO> getTransformersTO(boolean gzipEnabled, List<CipherSpec> cipherSpecs) {
		List<TransformerTO> transformersTO = new ArrayList<TransformerTO>();
		
		if (gzipEnabled) { 
			transformersTO.add(getGzipTransformerTO());
		}

		if (cipherSpecs.size() > 0) {	
			TransformerTO cipherTransformerTO = getCipherTransformerTO(cipherSpecs);			
			transformersTO.add(cipherTransformerTO);
		}
		
		return transformersTO;
	}

	private List<CipherSpec> getCipherSpecs(boolean encryptionEnabled, boolean advancedModeEnabled) throws Exception {
		List<CipherSpec> cipherSpecs = new ArrayList<CipherSpec>();
		
		if (encryptionEnabled) {
			if (advancedModeEnabled) { 			
				cipherSpecs = askCipherSpecs();				
			}
			else { // Default
				cipherSpecs = CipherSpecs.getDefaultCipherSpecs();					
			}			
		}
		
		return cipherSpecs;
	}

	private List<CipherSpec> askCipherSpecs() throws Exception {
		List<CipherSpec> cipherSpecs = new ArrayList<CipherSpec>();
		Map<Integer, CipherSpec> availableCipherSpecs = CipherSpecs.getAvailableCipherSpecs();

		out.println();
		out.println("Please choose your encryption settings. If you're paranoid,");
		out.println("you can choose multiple cipher suites by separating with a comma.");
		out.println();
		out.println("Options:");
		
		for (CipherSpec cipherSuite : availableCipherSpecs.values()) {
			out.println(" ["+cipherSuite.getId()+"] "+cipherSuite);
		}
		
		out.println();
				
		boolean continueLoop = true;
		boolean unlimitedStrengthNeeded = false;
		
		while (continueLoop) {
			String commaSeparatedCipherIdStr = console.readLine("Cipher(s): ");			
			String[] cipherSpecIdStrs = commaSeparatedCipherIdStr.split(",");
			
			// Choose cipher
			try {
				// Add cipher suites
				for (String cipherSpecIdStr : cipherSpecIdStrs) {
					Integer cipherSpecId = Integer.parseInt(cipherSpecIdStr);				
					CipherSpec cipherSpec = availableCipherSpecs.get(cipherSpecId);
					
					if (cipherSpec == null) {
						throw new Exception();
					}
					
					if (cipherSpec.needsUnlimitedStrength()) {
						unlimitedStrengthNeeded = true;
					}
					
					cipherSpecs.add(cipherSpec);
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
							CipherUtil.enableUnlimitedStrength();
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

		return cipherSpecs;		
	}
	
	protected RepoTO createRepoTO(ChunkerTO chunkerTO, MultiChunkerTO multiChunkerTO, List<TransformerTO> transformersTO) throws Exception {
		// Make transfer object
		RepoTO repoTO = new RepoTO();
		
		// Create random repo identifier
		byte[] newRepoId = new byte[REPO_ID_LENGTH];
		new SecureRandom().nextBytes(newRepoId);
		
		repoTO.setRepoId(newRepoId);
				
		// Add to repo transfer object
		repoTO.setChunkerTO(chunkerTO);
		repoTO.setMultiChunker(multiChunkerTO);
		repoTO.setTransformers(transformersTO);
		
		return repoTO;
	}	

	protected ChunkerTO getDefaultChunkerTO() {
		ChunkerTO chunkerTO = new ChunkerTO();
		
		chunkerTO.setType(FixedChunker.TYPE);
		chunkerTO.setSettings(new HashMap<String, String>());
		chunkerTO.getSettings().put(Chunker.PROPERTY_SIZE, "16");
		
		return chunkerTO;
	}
	
	protected MultiChunkerTO getDefaultMultiChunkerTO() {
		MultiChunkerTO multichunkerTO = new MultiChunkerTO();
		
		multichunkerTO.setType(ZipMultiChunker.TYPE); 
		multichunkerTO.setSettings(new HashMap<String, String>());
		multichunkerTO.getSettings().put(MultiChunker.PROPERTY_SIZE, "4096");
		
		return multichunkerTO;		
	}
	
	protected TransformerTO getGzipTransformerTO() {		
		TransformerTO gzipTransformerTO = new TransformerTO();
		gzipTransformerTO.setType(GzipTransformer.TYPE);
		
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
