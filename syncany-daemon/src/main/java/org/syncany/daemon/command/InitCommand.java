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

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.syncany.chunk.CipherTransformer;
import org.syncany.chunk.FixedChunker;
import org.syncany.chunk.GzipTransformer;
import org.syncany.chunk.ZipMultiChunker;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.config.to.RepoTO;
import org.syncany.config.to.RepoTO.ChunkerTO;
import org.syncany.config.to.RepoTO.MultiChunkerTO;
import org.syncany.config.to.RepoTO.TransformerTO;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherSpecs;
import org.syncany.operations.InitOperation;
import org.syncany.operations.InitOperation.InitOperationListener;
import org.syncany.operations.InitOperation.InitOperationOptions;
import org.syncany.operations.InitOperation.InitOperationResult;
import org.syncany.util.StringUtil;
import org.syncany.util.StringUtil.StringJoinListener;

public class InitCommand extends AbstractInitCommand implements InitOperationListener {
	public static final int REPO_ID_LENGTH = 32;
	public static final int[] DEFAULT_CIPHER_SUITE_IDS = new int[] { CipherSpecs.AES_128_GCM, CipherSpecs.TWOFISH_128_GCM };
	public static final int PASSWORD_MIN_LENGTH = 8;
	public static final int PASSWORD_WARN_LENGTH = 12;
	
	private String password;
	private boolean advancedModeEnabled;
	private boolean encryptionEnabled;
	private boolean gzipEnabled;
	private String localDir;
	private List<String> pluginArgs;
	private String pluginName;
	
	public InitCommand(String pluginName, List<String> pluginArgs, String localDir, String password, boolean advancedModeEnabled, boolean encryptionEnabled, boolean gzipEnabled){
		this.password = password;
		this.pluginName = pluginName;
		this.pluginArgs = pluginArgs;
		this.localDir = localDir;
		this.advancedModeEnabled = advancedModeEnabled;
		this.encryptionEnabled = encryptionEnabled;
		this.gzipEnabled = gzipEnabled;
	}
	
	public InitOperationResult execute() throws Exception {
		InitOperationOptions operationOptions = parseInitOptions();
		InitOperation io = new InitOperation(operationOptions, this);
		InitOperationResult result = io.execute();
		return result;		
	}

	private InitOperationOptions parseInitOptions() throws Exception {
		InitOperationOptions operationOptions = new InitOperationOptions();

		ConnectionTO connectionTO = initPluginWithOptions(pluginName, pluginArgs);
		
		List<CipherSpec> cipherSpecs = new ArrayList<>();
		
		ChunkerTO chunkerTO = getDefaultChunkerTO();
		MultiChunkerTO multiChunkerTO = getDefaultMultiChunkerTO();
		List<TransformerTO> transformersTO = getTransformersTO(gzipEnabled, cipherSpecs);
				
			
		ConfigTO configTO = createConfigTO(new File(localDir), null, connectionTO);		
		RepoTO repoTO = createRepoTO(chunkerTO, multiChunkerTO, transformersTO);
		
		operationOptions.setLocalDir(new File(localDir));
		operationOptions.setConfigTO(configTO);
		operationOptions.setRepoTO(repoTO); 
		
		operationOptions.setEncryptionEnabled(encryptionEnabled);
		operationOptions.setCipherSpecs(cipherSpecs);
		operationOptions.setPassword(password);
		
		return operationOptions;
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

	protected RepoTO createRepoTO(ChunkerTO chunkerTO, MultiChunkerTO multiChunkerTO, List<TransformerTO> transformersTO) throws Exception {
		// Make transfer object
		RepoTO repoTO = new RepoTO();
		
		// Create random repo identifier
		byte[] newRepoId = new byte[REPO_ID_LENGTH];
		new SecureRandom().nextBytes(newRepoId);
		
		repoTO.setRepoId(newRepoId);
				
		// Add to repo transfer object
		repoTO.setChunker(chunkerTO);
		repoTO.setMultiChunker(multiChunkerTO);
		repoTO.setTransformers(transformersTO);
		
		return repoTO;
	}	

	protected ChunkerTO getDefaultChunkerTO() {
		ChunkerTO chunkerTO = new ChunkerTO();
		
		chunkerTO.setType(FixedChunker.TYPE);
		chunkerTO.setSettings(new HashMap<String, String>());
		chunkerTO.getSettings().put(FixedChunker.PROPERTY_SIZE, "16");
		
		return chunkerTO;
	}
	
	protected MultiChunkerTO getDefaultMultiChunkerTO() {
		MultiChunkerTO multichunkerTO = new MultiChunkerTO();
		
		multichunkerTO.setType(ZipMultiChunker.TYPE); 
		multichunkerTO.setSettings(new HashMap<String, String>());
		multichunkerTO.getSettings().put(ZipMultiChunker.PROPERTY_SIZE, "512");
		
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
		//TODO
	}

	@Override
	public void disposeCommand() {
		
	}
}
