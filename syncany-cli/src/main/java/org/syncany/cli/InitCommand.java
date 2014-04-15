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
import java.util.Arrays;
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
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherSpecs;
import org.syncany.crypto.CipherUtil;
import org.syncany.operations.init.InitOperationListener;
import org.syncany.operations.init.InitOperationOptions;
import org.syncany.operations.init.InitOperationResult;
import org.syncany.operations.init.InitOperationResult.InitResultCode;
import org.syncany.util.StringUtil;
import org.syncany.util.StringUtil.StringJoinListener;

public class InitCommand extends AbstractInitCommand implements InitOperationListener {
	public static final int REPO_ID_LENGTH = 32;
	public static final int PASSWORD_MIN_LENGTH = 10;
	public static final int PASSWORD_WARN_LENGTH = 12;
	
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
			printResults(operationResult);
			
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
		
		operationOptions.setCreateTargetPath(createTargetPath);
		operationOptions.setEncryptionEnabled(encryptionEnabled);
		operationOptions.setCipherSpecs(cipherSpecs);
		operationOptions.setPassword(null); // set by callback in operation 
		
		return operationOptions;
	}		

	private void printResults(InitOperationResult operationResult) {
		if (operationResult.getResultCode() == InitResultCode.OK) {
			out.println();
			out.println("Repository created, and local folder initialized. To share the same repository");
			out.println("with others, you can share this link:");

			printLink(operationResult.getGenLinkResult(), false);
		}
		else if (operationResult.getResultCode() == InitResultCode.NOK_NO_REPO_CANNOT_CREATE) {
			out.println();
			out.println("ERROR: Cannot initialize repository (not writable, or -t not set).");
			out.println();
			out.println("Make sure that the repository path is writable by the connection user");
			out.println("and that the --create-target/-t option is set if you want to create");
			out.println("the remote repository folder/path.");
			out.println();
		}
		else if (operationResult.getResultCode() == InitResultCode.NOK_NO_CONNECTION) {
			out.println();
			out.println("ERROR: Cannot initialize repository (broken connection).");
			out.println();
			out.println("Make sure that you have a working Internet connection and that ");
			out.println("the connection details (esp. the hostname/IP) are correct.");				
			out.println();
		}
		else if (operationResult.getResultCode() == InitResultCode.NOK_REPO_EXISTS) {
			out.println();
			out.println("ERROR: Cannot initialize repository (already exists).");
			out.println();
			out.println("If you want to connect to an existing repository, please");
			out.println("use the 'connect' command.");
			out.println();
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
	
	protected String askPasswordAndConfirm() {
		out.println();
		out.println("The password is used to encrypt data on the remote storage.");
		out.println("Choose wisely!");
		out.println();
		
		String password = null;
		
		while (password == null) {
			char[] passwordChars = console.readPassword("Password (min. "+PASSWORD_MIN_LENGTH+" chars): ");
			
			if (passwordChars.length < PASSWORD_MIN_LENGTH) {
				out.println("ERROR: This password is not allowed (too short, min. "+PASSWORD_MIN_LENGTH+" chars)");
				out.println();
				
				continue;
			}
			
			char[] confirmPasswordChars = console.readPassword("Confirm: ");
			
			if (!Arrays.equals(passwordChars, confirmPasswordChars)) {
				out.println("ERROR: Passwords do not match.");
				out.println();
				
				continue;
			} 
			
			if (passwordChars.length < PASSWORD_WARN_LENGTH) {
				out.println();
				out.println("WARNING: The password is a bit short. Less than "+PASSWORD_WARN_LENGTH+" chars are not future-proof!");
				String yesno = console.readLine("Are you sure you want to use it (y/n)? ");
				
				if (!yesno.toLowerCase().startsWith("y") && !"".equals(yesno)) {
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

	@Override
	public void notifyGenerateMasterKey() {
		out.println();
		out.println("Generating master key from password (this might take a while) ...");
	}

	@Override
	public String getPasswordCallback() {
		return askPasswordAndConfirm();
	}
}
