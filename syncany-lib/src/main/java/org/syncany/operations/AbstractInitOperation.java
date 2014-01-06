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
package org.syncany.operations;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.config.to.RepoTO;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.util.FileUtil;

/**
 * The abstract init operation implements common functions of the {@link InitOperation}
 * and the {@link ConnectOperation}. Its sole purpose is to avoid duplicate code in these
 * similar operations.
 *   
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
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
		
		File appDir = new File(localDir+"/"+Config.DIR_APPLICATION);
		File logDir = new File(appDir+"/"+Config.DIR_LOG);
		File cacheDir = new File(appDir+"/"+Config.DIR_CACHE);
		File databaseDir = new File(appDir+"/"+Config.DIR_DATABASE);
		
		appDir.mkdir();
		logDir.mkdir();
		cacheDir.mkdir();
		databaseDir.mkdir();
		
		if (FileUtil.isWindows()) {
			Files.setAttribute(Paths.get(appDir.getAbsolutePath()), "dos:hidden", true);
		}
		
		return appDir;
	}
	
	protected void deleteAppDirs(File localDir) throws Exception {
		File appDir = new File(localDir+"/"+Config.DIR_APPLICATION);
		File logDir = new File(appDir+"/"+Config.DIR_LOG);
		File cacheDir = new File(appDir+"/"+Config.DIR_CACHE);
		File databaseDir = new File(appDir+"/"+Config.DIR_DATABASE);
		for (File log : logDir.listFiles()) {
			log.delete();
		}
		logDir.delete();
		for (File cache : cacheDir.listFiles()) {
			cache.delete();
		}
		cacheDir.delete();
		for (File db : databaseDir.listFiles()) {
			db.delete();
		}
		databaseDir.delete();
		for (File file : appDir.listFiles()) {
			file.delete();
		}
		appDir.delete();
		
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
	
	protected String getEncryptedLink(ConnectionTO connectionTO, List<CipherSpec> cipherSuites, SaltedSecretKey masterKey) throws Exception {
		ByteArrayOutputStream plaintextOutputStream = new ByteArrayOutputStream();
		Serializer serializer = new Persister();
		serializer.write(connectionTO, plaintextOutputStream);
		
		byte[] masterKeySalt = masterKey.getSalt();
		String masterKeySaltEncodedStr = new String(Base64.encodeBase64(masterKeySalt, false));
		
		byte[] encryptedConnectionBytes = CipherUtil.encrypt(new ByteArrayInputStream(plaintextOutputStream.toByteArray()), cipherSuites, masterKey);
		String encryptedEncodedStorageXml = new String(Base64.encodeBase64(encryptedConnectionBytes, false));
		
		return "syncany://storage/1/"+masterKeySaltEncodedStr+"-"+encryptedEncodedStorageXml;				
	}
	
	protected String getPlaintextLink(ConnectionTO connectionTO) throws Exception {
		ByteArrayOutputStream plaintextOutputStream = new ByteArrayOutputStream();
		Serializer serializer = new Persister();
		serializer.write(connectionTO, plaintextOutputStream);
		
		byte[] plaintextStorageXml = plaintextOutputStream.toByteArray();
		String plaintextEncodedStorageXml = new String(Base64.encodeBase64(plaintextStorageXml, false));
		
		return "syncany://storage/1/not-encrypted/"+plaintextEncodedStorageXml;			
	}
}
