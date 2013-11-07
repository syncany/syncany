package org.syncany.operations;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;
import org.syncany.config.to.RepoTO;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.SaltedSecretKey;

public abstract class AbstractInitOperation extends Operation {
	public AbstractInitOperation(Config config) {
		super(config);
	}	
	
	protected TransferManager createTransferManager(ConnectionTO connectionTO) throws StorageException {
		Plugin plugin = Plugins.get(connectionTO.getType());
		
		Connection connection = plugin.createConnection();
		connection.init(connectionTO.getSettings());
		
		return connection.createTransferManager();
	}
	
	protected File createAppDirs(File localDir) throws Exception {
		if (localDir == null) {
			throw new Exception("Unable to create app dir, local dir is null.");
		}
		
		File appDir = new File(localDir+"/"+Config.DEFAULT_DIR_APPLICATION);
		File logDir = new File(appDir+"/"+Config.DEFAULT_DIR_LOG);
		File cacheDir = new File(appDir+"/"+Config.DEFAULT_DIR_CACHE);
		File databaseDir = new File(appDir+"/"+Config.DEFAULT_DIR_DATABASE);
		
		appDir.mkdir();
		logDir.mkdir();
		cacheDir.mkdir();
		databaseDir.mkdir();		
		
		return appDir;
	}
	
	protected void writeXmlFile(Object source, File file) throws Exception {
   		Serializer serializer = new Persister();
		serializer.write(source, file);	
	}	
	
	protected void writeEncryptedXmlFile(RepoTO repoTO, File file, List<CipherSpec> cipherSuites, SaltedSecretKey masterKey) throws Exception {				
		ByteArrayOutputStream plaintextRepoOutputStream = new ByteArrayOutputStream();
		Serializer serializer = new Persister();
		serializer.write(repoTO, plaintextRepoOutputStream);
		
		CipherUtil.encrypt(new ByteArrayInputStream(plaintextRepoOutputStream.toByteArray()), new FileOutputStream(file), cipherSuites, masterKey);
	}		
}
