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
package org.syncany.config;

import java.io.File;
import java.util.ArrayList;

import org.syncany.chunk.Chunker;
import org.syncany.chunk.CipherTransformer;
import org.syncany.chunk.FixedChunker;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.NoTransformer;
import org.syncany.chunk.Transformer;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.RepoTO;
import org.syncany.config.to.RepoTO.MultiChunkerTO;
import org.syncany.config.to.RepoTO.TransformerTO;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.database.VectorClock;
import org.syncany.plugins.Plugins;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

/**
 * The config class is the central point to configure a Syncany instance. It is mainly
 * used in the operations, but parts of it are also used in other parts of the 
 * application -- especially file locations and names.
 * 
 * <p>An instance of the <tt>Config</tt> class must be created through the transfer 
 * objects {@link ConfigTO} and {@link RepoTO}. 
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Config {
	public static final String DIR_APPLICATION = ".syncany";
	public static final String DIR_CACHE = "cache";
	public static final String DIR_DATABASE = "db";
	public static final String DIR_LOG = "logs";
	public static final String DIR_STATE = "state";

	// File in managed folder root
	public static final String FILE_IGNORE = ".syignore";

	// Files in .syncany
	public static final String FILE_CONFIG = "config.xml";
	public static final String FILE_REPO = "syncany";
	public static final String FILE_MASTER = "master";

	// File in .syncany/db
	public static final String FILE_DATABASE = "local.db";

	// Files in .syncany/state
	public static final String FILE_PORT = "port.xml";
	public static final String FILE_CLEANUP = "cleanup.xml";

	private byte[] repoId;
	private String machineName;
	private String displayName;
	private File localDir;
	private File appDir;
	private File cacheDir;
	private File databaseDir;
	private File logDir;
	private File stateDir;

	private SaltedSecretKey masterKey;

	private Cache cache;
	private TransferPlugin plugin;
	private TransferSettings connection;
	private Chunker chunker;
	private MultiChunker multiChunker;
	private Transformer transformer;
	private IgnoredFiles ignoredFiles;

	static {
		UserConfig.init();
		Logging.init();
	}

	public Config(File aLocalDir, ConfigTO configTO, RepoTO repoTO) throws ConfigException {
		if (aLocalDir == null || configTO == null || repoTO == null) {
			throw new ConfigException("Arguments aLocalDir, configTO and repoTO cannot be null.");
		}

		initNames(configTO);
		initMasterKey(configTO);
		initDirectories(aLocalDir);
		initCache(configTO);
		initIgnoredFile();
		initRepo(repoTO);
		initConnection(configTO);
	}

	private void initNames(ConfigTO configTO) throws ConfigException {
		setMachineName(configTO.getMachineName());
		setDisplayName(configTO.getDisplayName());
	}

	private void initMasterKey(ConfigTO configTO) {
		masterKey = configTO.getMasterKey(); // can be null
	}

	private void initDirectories(File aLocalDir) throws ConfigException {
		localDir = FileUtil.getCanonicalFile(aLocalDir);
		appDir = FileUtil.getCanonicalFile(new File(localDir, DIR_APPLICATION));
		cacheDir = FileUtil.getCanonicalFile(new File(appDir, DIR_CACHE));
		databaseDir = FileUtil.getCanonicalFile(new File(appDir, DIR_DATABASE));
		logDir = FileUtil.getCanonicalFile(new File(appDir, DIR_LOG));
		stateDir = FileUtil.getCanonicalFile(new File(appDir, DIR_STATE));
	}

	private void initCache(ConfigTO configTO) {
		cache = new Cache(cacheDir);

		if (configTO.getCacheKeepBytes() != null && configTO.getCacheKeepBytes() >= 0) {
			cache.setKeepBytes(configTO.getCacheKeepBytes());
		}
	}

	private void initIgnoredFile() throws ConfigException {
		File ignoreFile = new File(localDir, FILE_IGNORE);
		ignoredFiles = new IgnoredFiles(ignoreFile);
	}

	private void initRepo(RepoTO repoTO) throws ConfigException {
		try {
			initRepoId(repoTO);
			initChunker(repoTO);
			initMultiChunker(repoTO);
			initTransformers(repoTO);
		}
		catch (Exception e) {
			throw new ConfigException("Unable to initialize repository information from config.", e);
		}
	}

	private void initRepoId(RepoTO repoTO) {
		repoId = repoTO.getRepoId();
	}

	private void initChunker(RepoTO repoTO) throws Exception {
		// TODO [feature request] make chunking options configurable, something like described in #29
		// See: https://github.com/syncany/syncany/issues/29#issuecomment-43425647

		chunker = new FixedChunker(512 * 1024, "SHA1");
	}

	private void initMultiChunker(RepoTO repoTO) throws ConfigException {
		MultiChunkerTO multiChunkerTO = repoTO.getMultiChunker();

		if (multiChunkerTO == null) {
			throw new ConfigException("No multichunker in repository config.");
		}

		multiChunker = MultiChunker.getInstance(multiChunkerTO.getType());

		if (multiChunker == null) {
			throw new ConfigException("Invalid multichunk type or settings: " + multiChunkerTO.getType());
		}

		multiChunker.init(multiChunkerTO.getSettings());
	}

	private void initTransformers(RepoTO repoTO) throws Exception {
		if (repoTO.getTransformers() == null || repoTO.getTransformers().size() == 0) {
			transformer = new NoTransformer();
		}
		else {
			ArrayList<TransformerTO> transformerTOs = new ArrayList<TransformerTO>(repoTO.getTransformers());
			Transformer lastTransformer = null;

			for (int i = transformerTOs.size() - 1; i >= 0; i--) {
				TransformerTO transformerTO = transformerTOs.get(i);
				Transformer transformer = Transformer.getInstance(transformerTO.getType());

				if (transformer == null) {
					throw new ConfigException("Cannot find transformer '" + transformerTO.getType() + "'");
				}

				if (transformer instanceof CipherTransformer) { // Dirty workaround
					transformerTO.getSettings().put(CipherTransformer.PROPERTY_MASTER_KEY, StringUtil.toHex(getMasterKey().getEncoded()));
					transformerTO.getSettings().put(CipherTransformer.PROPERTY_MASTER_KEY_SALT, StringUtil.toHex(getMasterKey().getSalt()));
				}

				transformer.init(transformerTO.getSettings());

				if (lastTransformer != null) {
					transformer.setNextTransformer(lastTransformer);
				}

				lastTransformer = transformer;
			}

			transformer = lastTransformer;
		}
	}

	private void initConnection(ConfigTO configTO) throws ConfigException {
		if (configTO.getConnectionTO() != null) {
			plugin = Plugins.get(configTO.getConnectionTO().getType(), TransferPlugin.class);

			if (plugin == null) {
				throw new ConfigException("Plugin not supported: " + configTO.getConnectionTO().getType());
			}

			try {
				connection = plugin.createSettings();
				connection.init(configTO.getConnectionTO().getSettings());
			}
			catch (StorageException e) {
				throw new ConfigException("Cannot initialize storage: " + e.getMessage(), e);
			}
		}
	}

	public java.sql.Connection createDatabaseConnection() {
		return DatabaseConnectionFactory.createConnection(getDatabaseFile());
	}

	public File getCacheDir() {
		return cacheDir;
	}

	public File getAppDir() {
		return appDir;
	}

	public String getMachineName() {
		return machineName;
	}

	public void setMachineName(String machineName) throws ConfigException {
		if (machineName == null || !VectorClock.MACHINE_PATTERN.matcher(machineName).matches()) {
			throw new ConfigException("Machine name cannot be empty and must be only characters (A-Z).");
		}

		this.machineName = machineName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public TransferPlugin getTransferPlugin() {
		return plugin;
	}

	public TransferSettings getConnection() {
		return connection;
	}

	public void setConnection(TransferSettings connection) {
		this.connection = connection;
	}

	public byte[] getRepoId() {
		return repoId;
	}

	public Chunker getChunker() {
		return chunker;
	}

	public Cache getCache() {
		return cache;
	}

	public IgnoredFiles getIgnoredFiles() {
		return ignoredFiles;
	}

	public MultiChunker getMultiChunker() {
		return multiChunker;
	}

	public Transformer getTransformer() {
		return transformer;
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}

	public File getLocalDir() {
		return localDir;
	}

	public File getDatabaseDir() {
		return databaseDir;
	}

	public File getLogDir() {
		return logDir;
	}

	public File getStateDir() {
		return stateDir;
	}

	public SaltedSecretKey getMasterKey() {
		return masterKey;
	}

	public File getDatabaseFile() {
		return new File(databaseDir, FILE_DATABASE);
	}

	public File getPortFile() {
		return new File(stateDir, FILE_PORT);
	}

	public File getCleanupFile() {
		return new File(stateDir, FILE_CLEANUP);
	}
}
