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
package org.syncany.operations.init;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

import org.syncany.config.Config;
import org.syncany.config.DaemonConfigHelper;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.MasterTO;
import org.syncany.config.to.RepoTO;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.operations.init.InitOperationResult.InitResultCode;
import org.syncany.plugins.Plugins;
import org.syncany.plugins.UserInteractionListener;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.StorageTestResult;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.plugins.transfer.files.MasterRemoteFile;
import org.syncany.plugins.transfer.files.SyncanyRemoteFile;

/**
 * The init operation initializes a new repository at a given remote storage
 * location. Its responsibilities include:
 *
 * <ul>
 *   <li>Generating a master key from the user password (if encryption is enabled)
 *       using the {@link CipherUtil#createMasterKey(String) createMasterKey()} method</li>
 *   <li>Creating the local Syncany folder structure in the local directory (.syncany
 *       folder and the sub-structure).</li>
 *   <li>Initializing the remote storage (creating folder-structure, if necessary)
 *       using the transfer manager's {@link TransferManager#init()} method.</li>
 *   <li>Creating a new repo and master file using {@link RepoTO} and {@link MasterTO},
 *       saving them locally and uploading them to the remote repository.</li>
 * </ul>
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class InitOperation extends AbstractInitOperation {
	private InitOperationOptions options;
	private InitOperationResult result;

	private TransferPlugin plugin;
	private TransferManager transferManager;

	public InitOperation(InitOperationOptions options, UserInteractionListener listener) {
		super(null, listener);

		this.options = options;
		this.result = new InitOperationResult();
	}

	@Override
	public InitOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Init'");
		logger.log(Level.INFO, "--------------------------------------------");

		// Init plugin and transfer manager
		plugin = Plugins.get(options.getConfigTO().getTransferSettings().getType(), TransferPlugin.class);

		TransferSettings transferSettings = options.getConfigTO().getTransferSettings();
		transferSettings.setUserInteractionListener(listener);

		transferManager = plugin.createTransferManager(transferSettings, config);

		// Test the repo
		if (!performRepoTest()) {
			logger.log(Level.INFO, "- Connecting to the repo failed, repo already exists or cannot be created: " + result.getResultCode());
			return result;
		}

		logger.log(Level.INFO, "- Connecting to the repo was successful");

		// Ask password (if needed)
		String masterKeyPassword = null;

		if (options.isEncryptionEnabled()) {
			masterKeyPassword = getOrAskPassword();
		}

		// Create local .syncany directory
		File appDir = createAppDirs(options.getLocalDir()); // TODO [medium] create temp dir first, ask password cannot be done after
		File configFile = new File(appDir, Config.FILE_CONFIG);
		File repoFile = new File(appDir, Config.FILE_REPO);
		File masterFile = new File(appDir, Config.FILE_MASTER);

		// Save config.xml and repo file
		if (options.isEncryptionEnabled()) {
			SaltedSecretKey masterKey = createMasterKeyFromPassword(masterKeyPassword); // This takes looong!
			options.getConfigTO().setMasterKey(masterKey);

			writeXmlFile(new MasterTO(masterKey.getSalt()), masterFile);
			writeEncryptedXmlFile(options.getRepoTO(), repoFile, options.getCipherSpecs(), masterKey);
		}
		else {
			writeXmlFile(options.getRepoTO(), repoFile);
		}

		options.getConfigTO().save(configFile);

		// Make remote changes
		logger.log(Level.INFO, "Uploading local repository");

		initRemoteRepository();

		try {
			if (options.isEncryptionEnabled()) {
				uploadMasterFile(masterFile, transferManager);
			}

			uploadRepoFile(repoFile, transferManager);
		}
		catch (StorageException | IOException e) {
			cleanLocalRepository(e);
		}

		// Shutdown plugin
		transferManager.disconnect();

		// Add to daemon (if requested)
		if (options.isDaemon()) {
			try {
				boolean addedToDaemonConfig = DaemonConfigHelper.addToDaemonConfig(options.getLocalDir());
				result.setAddedToDaemon(addedToDaemonConfig);
			}
			catch (Exception e) {
				logger.log(Level.WARNING, "Cannot add folder to daemon config.", e);
				result.setAddedToDaemon(false);
			}
		}

		// Make link
		GenlinkOperationResult genlinkOperationResult = generateLink(options.getConfigTO());

		result.setResultCode(InitResultCode.OK);
		result.setGenLinkResult(genlinkOperationResult);

		return result;
	}

	private boolean performRepoTest() {
		boolean testCreateTarget = options.isCreateTarget();
		StorageTestResult testResult = transferManager.test(testCreateTarget);

		logger.log(Level.INFO, "Storage test result ist " + testResult);

		if (testResult.isTargetExists() && testResult.isTargetCanWrite() && !testResult.isRepoFileExists()) {
			logger.log(Level.INFO, "--> OKAY: Target exists and is writable, but repo doesn't exist. We're good to go!");
			return true;
		}
		else if (testCreateTarget && !testResult.isTargetExists() && testResult.isTargetCanCreate()) {
			logger.log(Level.INFO, "--> OKAY: Target does not exist, but can be created. We're good to go!");
			return true;
		}
		else {
			logger.log(Level.INFO, "--> NOT OKAY: Invalid target/repo state. Operation cannot be continued.");

			result.setResultCode(InitResultCode.NOK_TEST_FAILED);
			result.setTestResult(testResult);

			return false;
		}
	}

	private void initRemoteRepository() throws Exception {
		try {
			transferManager.init(options.isCreateTarget());
		}
		catch (StorageException e) {
			// Storing remotely failed. Remove all the directories and files we just created
			cleanLocalRepository(e);
		}
	}

	private void cleanLocalRepository(Exception e) throws Exception {
		try {
			deleteAppDirs(options.getLocalDir());
		}
		catch (Exception e1) {
			throw new StorageException("Couldn't upload to remote repo. Cleanup failed. There may be local directories left");
		}

		throw new StorageException("Couldn't upload to remote repo. Cleaned local repository.", e);
	}

	private GenlinkOperationResult generateLink(ConfigTO configTO) throws Exception {
		return new GenlinkOperation(options.getConfigTO(), options.getGenlinkOptions()).execute();
	}

	private String getOrAskPassword() throws Exception {
		if (options.getPassword() == null) {
			if (listener == null) {
				throw new RuntimeException("Cannot get password from user interface. No listener.");
			}

			return listener.onUserNewPassword();
		}
		else {
			return options.getPassword();
		}
	}

	private SaltedSecretKey createMasterKeyFromPassword(String masterPassword) throws Exception {
		fireNotifyCreateMaster();

		SaltedSecretKey masterKey = CipherUtil.createMasterKey(masterPassword);
		return masterKey;
	}

	protected boolean repoFileExistsOnRemoteStorage(TransferManager transferManager) throws Exception {
		try {
			Map<String, SyncanyRemoteFile> repoFileList = transferManager.list(SyncanyRemoteFile.class);
			return repoFileList.size() > 0;
		}
		catch (Exception e) {
			throw new Exception("Unable to connect to repository.", e);
		}
	}

	private void uploadMasterFile(File masterFile, TransferManager transferManager) throws Exception {
		transferManager.upload(masterFile, new MasterRemoteFile());
	}

	private void uploadRepoFile(File repoFile, TransferManager transferManager) throws Exception {
		transferManager.upload(repoFile, new SyncanyRemoteFile());
	}
}
