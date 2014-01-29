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
package org.syncany.tests.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.syncany.chunk.CipherTransformer;
import org.syncany.chunk.GzipTransformer;
import org.syncany.chunk.ZipMultiChunker;
import org.syncany.config.Config;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.RepoTO;
import org.syncany.config.to.RepoTO.ChunkerTO;
import org.syncany.config.to.RepoTO.MultiChunkerTO;
import org.syncany.config.to.RepoTO.TransformerTO;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.connection.plugins.unreliable_local.UnreliableLocalConnection;
import org.syncany.connection.plugins.unreliable_local.UnreliableLocalPlugin;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.SaltedSecretKey;

import com.google.common.collect.Lists;

public class TestConfigUtil {
	private static final String RUNDATE = new SimpleDateFormat("yyMMddHHmmssSSS").format(new Date());
	private static boolean cryptoEnabled = false;
	private static SaltedSecretKey masterKey = null;
	
	public static Map<String, String> createTestLocalConnectionSettings() throws Exception {
		Map<String, String> pluginSettings = new HashMap<String, String>();

		File tempRepoDir = TestFileUtil.createTempDirectoryInSystemTemp(createUniqueName("repo", pluginSettings));		
		pluginSettings.put("path", tempRepoDir.getAbsolutePath());
		
		return pluginSettings;
	}
		
	public static Config createTestLocalConfig() throws Exception {
		return createTestLocalConfig("syncanyclient");
	}
	
	public static Config createTestLocalConfig(String machineName) throws Exception {
		return createTestLocalConfig(machineName, createTestLocalConnection());
	}
	
	private static MultiChunkerTO createZipMultichunkerTO(){
		final MultiChunkerTO multiChunkerTO = new MultiChunkerTO();
		multiChunkerTO.setType(ZipMultiChunker.TYPE);
		final Map<String,String> settings = new HashMap<>();
		settings.put(ZipMultiChunker.PROPERTY_SIZE, "4096");
		multiChunkerTO.setSettings(settings);
		return multiChunkerTO;
	}
	
	private static ChunkerTO createMimeChunkerTO(){
		ChunkerTO chunkerTO = new ChunkerTO();
		return chunkerTO;
	}
	
	private static List<TransformerTO> createTransformerTOs(){
		if(!cryptoEnabled){
			return null;
		}
		
		final TransformerTO gzipTransformerTO = new TransformerTO();
		gzipTransformerTO.setType(GzipTransformer.TYPE);
		
		final Map<String, String> cipherTransformerSettings = new HashMap<String, String>();
		cipherTransformerSettings.put(CipherTransformer.PROPERTY_CIPHER_SPECS, "1,2");
		
		final TransformerTO cipherTransformerTO = new TransformerTO();
		cipherTransformerTO.setType(CipherTransformer.TYPE);
		cipherTransformerTO.setSettings(cipherTransformerSettings);
		
		return Lists.newArrayList(gzipTransformerTO, cipherTransformerTO);
	}
	
	private static SaltedSecretKey getMasterKey() throws Exception{
		if(!cryptoEnabled){
			return null;
		}
		
		if (masterKey == null) {
			masterKey = CipherUtil.createMasterKey("some password");
		}
		
		return masterKey;
	}
	
	public static Config createDummyConfig() throws Exception{
		// All is dummy
		final ConfigTO configTO = new ConfigTO();
		configTO.setMachineName("dummymachine");
		
		final RepoTO repoTO = new RepoTO();
		repoTO.setTransformers(null);
		repoTO.setChunker(createMimeChunkerTO());
		repoTO.setMultiChunker(createZipMultichunkerTO());
		
		return new Config(new File("/dummy"), configTO, repoTO);
	}
	
	
	public static Config createTestLocalConfig(String machineName, Connection connection) throws Exception {
		final File tempLocalDir = TestFileUtil.createTempDirectoryInSystemTemp(createUniqueName("client-"+machineName, connection));		
		tempLocalDir.mkdirs();	
		
		// Create Repo TO
		final RepoTO repoTO = new RepoTO();
		
		// Create ChunkerTO and MultiChunkerTO
		MultiChunkerTO multiChunkerTO = createZipMultichunkerTO();
		ChunkerTO chunkerTO = createMimeChunkerTO();
		repoTO.setChunker(chunkerTO); // TODO [low] Chunker not configurable right now. Not used.
		repoTO.setMultiChunker(multiChunkerTO);
		
		// Create TransformerTO
		List<TransformerTO> transformerTOs = createTransformerTOs();
		repoTO.setTransformers(transformerTOs);
		
		// Create config TO
		final ConfigTO configTO = new ConfigTO();
		configTO.setMachineName(machineName+Math.abs(new Random().nextInt()));
		
		// Get Masterkey
		final SaltedSecretKey masterKey = getMasterKey();
		configTO.setMasterKey(masterKey);
						
		// Skip configTO.setConnection()		
		final Config config = new Config(tempLocalDir, configTO, repoTO);
		
		config.setConnection(connection);
		config.getAppDir().mkdirs();
		config.getCacheDir().mkdirs();
		config.getDatabaseDir().mkdirs();
		config.getLogDir().mkdirs();
		
		return config;
	}
	
	public static Connection createTestLocalConnection() throws Exception {
		Plugin plugin = Plugins.get("local");
		Connection conn = plugin.createConnection();
		
		File tempRepoDir = TestFileUtil.createTempDirectoryInSystemTemp(createUniqueName("repo", conn));
		
		Map<String, String> pluginSettings = new HashMap<String, String>();
		pluginSettings.put("path", tempRepoDir.getAbsolutePath());
		
		conn.init(pluginSettings);		
		conn.createTransferManager().init();
		
		return conn;
	}	
	
	public static UnreliableLocalConnection createTestUnreliableLocalConnection(List<String> failingOperationPatterns) throws Exception {
		UnreliableLocalPlugin unreliableLocalPlugin = new UnreliableLocalPlugin();
		UnreliableLocalConnection unreliableLocalConnection = (UnreliableLocalConnection) unreliableLocalPlugin.createConnection();
				
		File tempRepoDir = TestFileUtil.createTempDirectoryInSystemTemp(createUniqueName("repo", unreliableLocalConnection));
		
		unreliableLocalConnection.setRepositoryPath(tempRepoDir);
		unreliableLocalConnection.setFailingOperationPatterns(failingOperationPatterns);

		unreliableLocalConnection.createTransferManager().init();
		
		return unreliableLocalConnection;
	}	

	public static void deleteTestLocalConfigAndData(Config config) {
		TestFileUtil.deleteDirectory(config.getLocalDir());
		TestFileUtil.deleteDirectory(config.getCacheDir());
		TestFileUtil.deleteDirectory(config.getDatabaseDir());

		if (config.getAppDir() != null) {
			TestFileUtil.deleteDirectory(config.getAppDir());
		}
		
		// TODO [low] workaround: delete empty parent folder of getAppDir() --> ROOT/app/.. --> ROOT/
		config.getLocalDir().getParentFile().delete(); // if empty!
		
		deleteTestLocalConnection(config);
	}

	private static void deleteTestLocalConnection(Config config) {
		LocalConnection connection = (LocalConnection) config.getConnection();
		TestFileUtil.deleteDirectory(connection.getRepositoryPath());		
	}
	
	public static String createUniqueName(String name, Object uniqueHashObj) {
		return String.format("syncany-%s-%d-%s", RUNDATE, 100 + uniqueHashObj.hashCode() % 899, name);
	}

	public static void setCrypto(boolean cryptoEnabled) {
		TestConfigUtil.cryptoEnabled = cryptoEnabled;
	}
}
