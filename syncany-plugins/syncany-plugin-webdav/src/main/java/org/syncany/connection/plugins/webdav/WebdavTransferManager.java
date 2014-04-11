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
package org.syncany.connection.plugins.webdav;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.syncany.connection.plugins.AbstractTransferManager;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.PluginListener;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.github.sardine.impl.SardineException;
import com.github.sardine.impl.SardineImpl;

public class WebdavTransferManager extends AbstractTransferManager {
	private static final Logger logger = Logger.getLogger(WebdavTransferManager.class.getSimpleName());

	private static final String APPLICATION_CONTENT_TYPE = "application/x-syncany";
	private static final int HTTP_NOT_FOUND = 404;

	private static KeyStore trustStore;
	private static boolean hasNewCertificates;

	private Sardine sardine;	
	private PluginListener pluginListener;	
	
	private String repoPath;
	private String multichunkPath;
	private String databasePath;

	public WebdavTransferManager(WebdavConnection connection, PluginListener pluginListener) {
		super(connection);

		this.sardine = null;
		this.pluginListener = pluginListener;		

		this.repoPath = connection.getUrl().replaceAll("/$", "") + "/";
		this.multichunkPath = repoPath + "multichunks/";
		this.databasePath = repoPath + "databases/";
		
		loadTrustStore();
	}

	@Override
	public WebdavConnection getConnection() {
		return (WebdavConnection) super.getConnection();
	}

	private void loadTrustStore() {
		if (trustStore == null) {
			try {				
				trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
				
				if (getConnection().getConfig() != null) { // Can be null if uninitialized!					
					File appDir = getConnection().getConfig().getAppDir();
					File userdataDir = new File(appDir, "userdata");
					File certStoreFile = new File(userdataDir, "truststore.jks"); 
										
					if (certStoreFile.exists()) {
						logger.log(Level.INFO, "WebDAV: Loading trust store from " + certStoreFile + " ...");

						FileInputStream trustStoreInputStream = new FileInputStream(certStoreFile); 		 		
						trustStore.load(trustStoreInputStream, new char[0]);
						
						trustStoreInputStream.close();
					}	
					else {
						logger.log(Level.INFO, "WebDAV: Loading trust store (empty, no trust store in config) ...");
						trustStore.load(null, new char[0]); // Initialize empty store						
					}
				}
				else {
					logger.log(Level.INFO, "WebDAV: Loading trust store (empty, no config) ...");
					trustStore.load(null, new char[0]); // Initialize empty store
				}
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		else {
			logger.log(Level.INFO, "WebDAV: Trust store already loaded.");			
		}
	}
	
	private void storeTrustStore() {
		if (trustStore != null) {
			if (!hasNewCertificates) {
				logger.log(Level.INFO, "WebDAV: No new certificates. Nothing to store.");
			}
			else {
				logger.log(Level.INFO, "WebDAV: New certificates. Storing trust store on disk.");

				try {
					if (getConnection().getConfig() != null) { 											
						File appDir = getConnection().getConfig().getAppDir();
						File userdataDir = new File(appDir, "userdata");
						File certStoreFile = new File(userdataDir, "truststore.jks"); 
							
						if (!userdataDir.exists()) {
							if (!userdataDir.mkdirs()) {
								throw new RuntimeException("WebDAV: Cannot store trust store. Cannot create "+userdataDir);
							}
						}
						
						FileOutputStream trustStoreOutputStream = new FileOutputStream(certStoreFile);
						trustStore.store(trustStoreOutputStream, new char[0]);
						
						trustStoreOutputStream.close();
						
						hasNewCertificates = false;
					}
					else {
						logger.log(Level.INFO, "WebDAV: Cannot store trust store; config not initialized.");
					}
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private SSLSocketFactory initSsl() throws Exception {
		TrustStrategy trustStrategy = new TrustStrategy() {
			@Override
			public boolean isTrusted(X509Certificate[] certificateChain, String authType) throws CertificateException {
				logger.log(Level.INFO, "WebDAV: isTrusted("+certificateChain.toString()+", "+authType+")");
								
				try {
					// First check if already in trust store, if so; okay!
					X509Certificate serverCertificate = certificateChain[0];
					
					if (inTrustStore(serverCertificate)) {
						logger.log(Level.FINE, "WebDAV: Certificate is in trust store. DONE!");

						serverCertificate.checkValidity();
						return true;
					}

					// If not in trust store, check if any of the certificates in the chain is
					if (certificateChain.length > 1) {
						for (int i=1; i<certificateChain.length; i++) {
							X509Certificate issuerCertificate = certificateChain[i];

							logger.log(Level.FINE, "WebDAV: Checking certificate validity: "+issuerCertificate);							
							issuerCertificate.checkValidity();	
							
							logger.log(Level.FINE, "WebDAV: Checking is VALID.");
							
							// Certificate found; we trust this, okay!
							if (inTrustStore(issuerCertificate)) {
								logger.log(Level.FINE, "WebDAV: Certificate found in trust store.");
								return true;
							}
							
							// Certificate is new; continue ...
							else {
								logger.log(Level.FINE, "WebDAV: Certificate NOT found in trust store.");
							}
						}
					}
						
					// We we reach this code, none of the CAs are known in the trust store
					// So we ask the user if he/she wants to add the server certificate to the trust store  
							
					boolean userTrustsCertificate = pluginListener.onPluginUserQuery("Unknown SSL/TLS certificate. Do you want to trust it?", formatCertificate(serverCertificate));
					
					if (!userTrustsCertificate) {
						logger.log(Level.INFO, "WebDAV: User does not trust certificate. ABORTING.");
						return false;
					}
					
					logger.log(Level.INFO, "WebDAV: User trusts certificate. Adding to trust store.");
					addToTrustStore(serverCertificate);
	
					return true;
				}
				catch (KeyStoreException e) {
					throw new CertificateException(e);
				}
			}		
			
			private boolean inTrustStore(X509Certificate certificate) throws KeyStoreException {
				String certAlias = getCertificateAlias(certificate);		
				return trustStore.containsAlias(certAlias);
			}
			
			private void addToTrustStore(X509Certificate certificate) throws KeyStoreException {
				String certAlias = getCertificateAlias(certificate);
				trustStore.setCertificateEntry(certAlias, certificate);
				hasNewCertificates = true;				
			}
			
			private String getCertificateAlias(X509Certificate certificate) {
				return StringUtil.toHex(certificate.getSignature());
			}
		};

		return new SSLSocketFactory(trustStrategy);
	}
	
	private String formatCertificate(X509Certificate cert) {
		
		/*
		 
		 TODO [medium] Pretty print certificate.
		 
		 SHOULD BE:
		 
			Owner: EMAILADDRESS=philipp.heckel@gmail.com, CN=webdav.lan, O=Philipp Heckel, L=Darmstadt, ST=Hessen, C=DE
			Issuer: EMAILADDRESS=philipp.heckel@gmail.com, CN=*, O=Philipp Heckel, L=Darmstadt, ST=Hessen, C=DE
			Serial number: 1
			Valid from: Thu Apr 10 16:31:46 CEST 2014 until: Sat Apr 09 16:31:46 CEST 2016
			Certificate fingerprints:
				 MD5:  2A:66:72:AD:E1:D6:40:05:2E:1C:78:D2:3B:49:C5:94
				 SHA1: 49:DC:1D:62:78:A3:C1:F0:D4:AA:4D:72:4A:60:80:68:BD:FF:6A:1C
				 SHA256: 04:42:4E:BD:18:F0:DE:24:29:13:87:26:77:63:56:7B:AA:60:32:79:6B:C0:49:02:94:17:AB:00:DB:B9:A1:C3
				 Signature algorithm name: SHA1withRSA
				 Version: 1
 
		 */
		
		return cert.toString();
	}

	@Override
	public void connect() throws StorageException {
		if (sardine == null) {
			if (getConnection().isSecure()) {
				logger.log(Level.INFO, "WebDAV: Connect called. Creating Sardine (SSL!) ...");

				try {									
					final SSLSocketFactory sslSocketFactory = initSsl();
	
					sardine = new SardineImpl() {
						@Override
						protected SSLSocketFactory createDefaultSecureSocketFactory() {
							return sslSocketFactory;
						}
					};
	
					sardine.setCredentials(getConnection().getUsername(), getConnection().getPassword());
				}
				catch (Exception e) {
					throw new StorageException(e);
				}
			}
			else {
				logger.log(Level.INFO, "WebDAV: Connect called. Creating Sardine (non-SSL) ...");
				sardine = SardineFactory.begin(getConnection().getUsername(), getConnection().getPassword());
			}
		}
	}

	@Override
	public void disconnect() {
		storeTrustStore();
		sardine = null;
	}

	@Override
	public void init(boolean createIfRequired) throws StorageException {
		connect();

		try {
			if (!repoExists() && createIfRequired) {
				logger.log(Level.INFO, "WebDAV: Init called; creating repo directories ... ");
				
				sardine.createDirectory(repoPath);
				sardine.createDirectory(multichunkPath);
				sardine.createDirectory(databasePath);
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Cannot initialize WebDAV folder.", e);
			throw new StorageException(e);
		}
	}

	@Override
	public void download(RemoteFile remoteFile, File localFile) throws StorageException {
		connect();
		String remoteURL = getRemoteFileUrl(remoteFile);

		try {
			logger.log(Level.INFO, "WebDAV: Downloading " + remoteURL + " to temp file " + localFile + " ...");
			
			InputStream webdavFileInputStream = sardine.get(remoteURL);
			FileOutputStream localFileOutputStream = new FileOutputStream(localFile);

			FileUtil.appendToOutputStream(webdavFileInputStream, localFileOutputStream);

			localFileOutputStream.close();
			webdavFileInputStream.close();
		}
		catch (IOException ex) {
			logger.log(Level.SEVERE, "Error while downloading file from WebDAV: " + remoteURL, ex);
			throw new StorageException(ex);
		}
	}

	@Override
	public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
		connect();
		String remoteURL = getRemoteFileUrl(remoteFile);

		try {
			logger.log(Level.INFO, "WebDAV: Uploading local file " + localFile + " to " + remoteURL + " ...");
			InputStream localFileInputStream = new FileInputStream(localFile);

			sardine.put(remoteURL, localFileInputStream, APPLICATION_CONTENT_TYPE);
			localFileInputStream.close();
		}
		catch (Exception ex) {
			logger.log(Level.SEVERE, "Error while uploading file to WebDAV: " + remoteURL, ex);
			throw new StorageException(ex);
		}
	}

	@Override
	public <T extends RemoteFile> Map<String, T> list(Class<T> remoteFileClass) throws StorageException {
		connect();

		try {
			// List folder
			String remoteFileUrl = getRemoteFilePath(remoteFileClass);			
			logger.log(Level.INFO, "WebDAV: Listing objects in " + remoteFileUrl + " ...");
			
			List<DavResource> resources = sardine.list(remoteFileUrl);

			// Create RemoteFile objects
			String rootPath = repoPath.substring(0, repoPath.length() - new URI(repoPath).getRawPath().length());
			Map<String, T> remoteFiles = new HashMap<String, T>();

			for (DavResource res : resources) {
				// WebDAV returns the parent resource itself; ignore it
				String fullResourceUrl = rootPath + res.getPath().replaceAll("/$", "") + "/";				
				boolean isParentResource = remoteFileUrl.equals(fullResourceUrl.toString());

				if (!isParentResource) {
					try {
						T remoteFile = RemoteFile.createRemoteFile(res.getName(), remoteFileClass);
						remoteFiles.put(res.getName(), remoteFile);

						logger.log(Level.FINE, "WebDAV: Matching WebDAV resource: " + res);
					}
					catch (Exception e) {
						logger.log(Level.FINEST, "Cannot create instance of " + remoteFileClass.getSimpleName() + " for object " + res.getName()
								+ "; maybe invalid file name pattern. Ignoring file.");
					}
				}
			}

			return remoteFiles;
		}
		catch (Exception ex) {
			logger.log(Level.SEVERE, "WebDAV: Unable to list WebDAV directory.", ex);
			throw new StorageException(ex);
		}
	}

	@Override
	public boolean delete(RemoteFile remoteFile) throws StorageException {
		connect();
		String remoteURL = getRemoteFileUrl(remoteFile);

		try {
			logger.log(Level.FINE, "WebDAV: Deleting " + remoteURL);
			sardine.delete(remoteURL);
			
			return true;
		}
		catch (SardineException e) {
			if (e.getStatusCode() == HTTP_NOT_FOUND) {
				return true;
			}
			else {
				return false;
			}
		}
		catch (IOException ex) {
			logger.log(Level.SEVERE, "Error while deleting file from WebDAV: " + remoteURL, ex);
			throw new StorageException(ex);
		}
	}

	private String getRemoteFileUrl(RemoteFile remoteFile) {
		return getRemoteFilePath(remoteFile.getClass()) + remoteFile.getName();
	}

	private String getRemoteFilePath(Class<? extends RemoteFile> remoteFile) {
		if (remoteFile.equals(MultiChunkRemoteFile.class)) {
			return multichunkPath;
		}
		else if (remoteFile.equals(DatabaseRemoteFile.class)) {
			return databasePath;
		}
		else {
			return repoPath;
		}
	}

	@Override
	public boolean repoHasWriteAccess() throws StorageException {
		// TODO not tested
		try {
			sardine.createDirectory(repoPath);
			sardine.delete(repoPath);
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}

	/**
	 * Checks if the repo exists at the repo URL.
	 * 
	 * <p><b>Note:</b> This uses list() instead of exists() because Sardine implements
	 * the exists() method with a HTTP HEAD only. Some WebDAV servers respond with "Forbidden" 
	 * if for directories.
	 */
	@Override
	public boolean repoExists() throws StorageException {
		try {
			sardine.list(repoPath);
			return true;
		}
		catch (SardineException e) {
			return false;
		}
		catch (IOException e) {
			throw new StorageException(e);
		}
	}

	@Override
	public boolean repoIsValid() throws StorageException {
		// TODO not tested
		try {
			return sardine.list(repoPath).size() == 0;
		}
		catch (IOException e) {
			throw new StorageException(e);
		}
	}
}
