/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com>
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

import org.simpleframework.xml.core.Persister;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.CipherTransformer;
import org.syncany.chunk.GzipTransformer;
import org.syncany.chunk.ZipMultiChunker;
import org.syncany.config.Config;
import org.syncany.config.UserConfig;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.RepoTO;
import org.syncany.config.to.RepoTO.ChunkerTO;
import org.syncany.config.to.RepoTO.MultiChunkerTO;
import org.syncany.config.to.RepoTO.TransformerTO;
import org.syncany.crypto.CipherSpecs;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.operations.init.InitOperationOptions;
import org.syncany.plugins.Plugins;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.plugins.unreliable_local.UnreliableLocalTransferPlugin;
import org.syncany.plugins.unreliable_local.UnreliableLocalTransferSettings;
import org.syncany.tests.unit.util.TestFileUtil;

import com.google.common.collect.Lists;

public class TestConfigUtil {
	private static final String RUNDATE = new SimpleDateFormat("yyMMddHHmmssSSS").format(new Date());
	private static boolean cryptoEnabled = false;
	private static SaltedSecretKey masterKey = null;

	static {
		try {
			UserConfig.init(); // Load userconfig (include system properties, e.g. org.syncany.test.tmpdir)
			TestConfigUtil.cryptoEnabled = Boolean.parseBoolean(System.getProperty("crypto.enable"));
		}
		catch (Exception e) {
			TestConfigUtil.cryptoEnabled = false;
		}
	}

	public static Map<String, String> createTestLocalConnectionSettings() throws Exception {
		Map<String, String> pluginSettings = new HashMap<String, String>();

		File tempRepoDir = TestFileUtil.createTempDirectoryInSystemTemp(createUniqueName("repo", new Random().nextFloat()));
		pluginSettings.put("path", tempRepoDir.getAbsolutePath());

		return pluginSettings;
	}

	public static Config createTestLocalConfig() throws Exception {
		return createTestLocalConfig("syncanyclient");
	}

	public static Config createTestLocalConfig(String machineName) throws Exception {
		return createTestLocalConfig(machineName, createTestLocalConnection());
	}

	public static MultiChunkerTO createZipMultiChunkerTO() {
		Map<String, String> settings = new HashMap<String, String>();
		settings.put(ZipMultiChunker.PROPERTY_SIZE, "4096");

		MultiChunkerTO multiChunkerTO = new MultiChunkerTO();
		multiChunkerTO.setType(ZipMultiChunker.TYPE);
		multiChunkerTO.setSettings(settings);

		return multiChunkerTO;
	}

	public static ChunkerTO createFixedChunkerTO() {
		Map<String, String> settings = new HashMap<String, String>();
		settings.put(Chunker.PROPERTY_SIZE, "32768");

		ChunkerTO chunkerTO = new ChunkerTO();
		chunkerTO.setType("fixed");
		chunkerTO.setSettings(settings);

		return chunkerTO;
	}

	public static RepoTO createRepoTO() {
		// Create Repo TO
		RepoTO repoTO = new RepoTO();
		repoTO.setRepoId(new byte[] { 0x01, 0x02, 0x03 });

		// Create ChunkerTO and MultiChunkerTO
		MultiChunkerTO multiChunkerTO = createZipMultiChunkerTO();
		ChunkerTO chunkerTO = createFixedChunkerTO();
		repoTO.setChunkerTO(chunkerTO); // TODO [low] Chunker not configurable right now. Not used.
		repoTO.setMultiChunker(multiChunkerTO);

		// Create TransformerTO
		List<TransformerTO> transformerTOs = createTransformerTOs();
		repoTO.setTransformers(transformerTOs);
		return repoTO;
	}

	public static List<TransformerTO> createTransformerTOs() {
		if (!cryptoEnabled) {
			return null;
		}
		else {
			TransformerTO gzipTransformerTO = new TransformerTO();
			gzipTransformerTO.setType(GzipTransformer.TYPE);

			Map<String, String> cipherTransformerSettings = new HashMap<String, String>();
			cipherTransformerSettings.put(CipherTransformer.PROPERTY_CIPHER_SPECS, "1,2");

			TransformerTO cipherTransformerTO = new TransformerTO();
			cipherTransformerTO.setType(CipherTransformer.TYPE);
			cipherTransformerTO.setSettings(cipherTransformerSettings);

			return Lists.newArrayList(gzipTransformerTO, cipherTransformerTO);
		}
	}

	private static SaltedSecretKey getMasterKey() throws Exception {
		if (!cryptoEnabled) {
			return null;
		}
		else {
			if (masterKey == null) {
				masterKey = CipherUtil.createMasterKey("some password");
			}

			return masterKey;
		}
	}

	public static Config createDummyConfig() throws Exception {
		ConfigTO configTO = new ConfigTO();
		configTO.setMachineName("dummymachine");

		RepoTO repoTO = new RepoTO();
		repoTO.setTransformers(null);
		repoTO.setChunkerTO(createFixedChunkerTO());
		repoTO.setMultiChunker(createZipMultiChunkerTO());

		return new Config(new File("/dummy"), configTO, repoTO);
	}

	public static Config createTestLocalConfig(String machineName, TransferSettings connection) throws Exception {
		File tempLocalDir = TestFileUtil.createTempDirectoryInSystemTemp(createUniqueName("client-" + machineName, connection));
		tempLocalDir.mkdirs();

		RepoTO repoTO = createRepoTO();

		// Create config TO
		ConfigTO configTO = new ConfigTO();
		configTO.setMachineName(machineName + CipherUtil.createRandomAlphabeticString(20));

		// Get Masterkey
		SaltedSecretKey masterKey = getMasterKey();
		configTO.setMasterKey(masterKey);

		LocalTransferSettings localConnection = (LocalTransferSettings) connection;
		// Create connection TO
		Map<String, String> localConnectionSettings = new HashMap<String, String>();
		localConnectionSettings.put("path", localConnection.getPath().getAbsolutePath());

		configTO.setTransferSettings(connection);

		// Create
		Config config = new Config(tempLocalDir, configTO, repoTO);

		config.setConnection(connection);
		config.getAppDir().mkdirs();
		config.getCacheDir().mkdirs();
		config.getDatabaseDir().mkdirs();
		config.getLogDir().mkdirs();
		config.getStateDir().mkdirs();

		// Write to config folder (required for some tests)
		new Persister().write(configTO, new File(config.getAppDir() + "/" + Config.FILE_CONFIG));
		new Persister().write(repoTO, new File(config.getAppDir() + "/" + Config.FILE_REPO));

		return config;
	}

	public static InitOperationOptions createTestInitOperationOptions(String machineName) throws Exception {
		File tempLocalDir = TestFileUtil.createTempDirectoryInSystemTemp(createUniqueName("client-" + machineName, machineName));
		File tempRepoDir = TestFileUtil.createTempDirectoryInSystemTemp(createUniqueName("repo", machineName));
		tempLocalDir.mkdirs();
		tempRepoDir.mkdirs();

		RepoTO repoTO = createRepoTO();

		// Create config TO
		ConfigTO configTO = new ConfigTO();
		configTO.setMachineName(machineName + Math.abs(new Random().nextInt()));

		// Get Masterkey
		SaltedSecretKey masterKey = getMasterKey();
		configTO.setMasterKey(masterKey);

		// Generic connection settings wont work anymore, because they are plugin dependent now.
		LocalTransferSettings transferSettings = Plugins.get("local", TransferPlugin.class).createEmptySettings();
		transferSettings.setPath(tempRepoDir);

		configTO.setTransferSettings(transferSettings);

		InitOperationOptions operationOptions = new InitOperationOptions();

		operationOptions.setLocalDir(tempLocalDir);
		operationOptions.setConfigTO(configTO);
		operationOptions.setRepoTO(repoTO);

		operationOptions.setEncryptionEnabled(cryptoEnabled);
		operationOptions.setCipherSpecs(CipherSpecs.getDefaultCipherSpecs());
		operationOptions.setPassword(cryptoEnabled ? "some password" : null);

		return operationOptions;
	}

	public static InitOperationOptions createTestUnreliableInitOperationOptions(String machineName, List<String> failingOperationPatterns)
			throws Exception {
		InitOperationOptions initOperationOptions = createTestInitOperationOptions(machineName);
		// createTestInitOperationOptions always returns LocalTransferSettings
		File tempRpoDir = ((LocalTransferSettings) initOperationOptions.getConfigTO().getTransferSettings()).getPath();
		UnreliableLocalTransferSettings transferSettings = Plugins.get("unreliable_local", TransferPlugin.class).createEmptySettings();
		transferSettings.setPath(tempRpoDir);
		transferSettings.setFailingOperationPatterns(failingOperationPatterns);

		initOperationOptions.getConfigTO().setTransferSettings(transferSettings);

		return initOperationOptions;
	}

	public static TransferSettings createTestLocalConnection() throws Exception {
		TransferPlugin plugin = Plugins.get("local", TransferPlugin.class);
		LocalTransferSettings conn = plugin.createEmptySettings();

		File tempRepoDir = TestFileUtil.createTempDirectoryInSystemTemp(createUniqueName("repo", conn));
		conn.setPath(tempRepoDir);

		// TODO [medium] : possible problem
		plugin.createTransferManager(conn, null).init(true);

		return conn;
	}

	public static UnreliableLocalTransferSettings createTestUnreliableLocalConnection(List<String> failingOperationPatterns) throws Exception {
		UnreliableLocalTransferPlugin unreliableLocalPlugin = new UnreliableLocalTransferPlugin();
		UnreliableLocalTransferSettings unreliableLocalConnection = createTestUnreliableLocalConnectionWithoutInit(unreliableLocalPlugin,
				failingOperationPatterns);

		unreliableLocalPlugin.createTransferManager(unreliableLocalConnection, null).init(true);

		return unreliableLocalConnection;
	}

	public static UnreliableLocalTransferSettings createTestUnreliableLocalConnectionWithoutInit(UnreliableLocalTransferPlugin unreliableLocalPlugin,
			List<String> failingOperationPatterns) throws Exception {
		UnreliableLocalTransferSettings unreliableLocalConnection = unreliableLocalPlugin.createEmptySettings();

		File tempRepoDir = TestFileUtil.createTempDirectoryInSystemTemp(createUniqueName("repo", new Random().nextFloat()));

		unreliableLocalConnection.setPath(tempRepoDir);
		unreliableLocalConnection.setFailingOperationPatterns(failingOperationPatterns);
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
		LocalTransferSettings connection = (LocalTransferSettings) config.getConnection();
		TestFileUtil.deleteDirectory(connection.getPath());
	}

	public static String createUniqueName(String name, Object uniqueHashObj) {
		return String.format("syncany-%s-%d-%s", RUNDATE, 10000 + uniqueHashObj.hashCode() % 89999, name);
	}

	public static void setCrypto(boolean cryptoEnabled) {
		TestConfigUtil.cryptoEnabled = cryptoEnabled;
	}

	public static boolean getCrypto() {
		return cryptoEnabled;
	}
}
