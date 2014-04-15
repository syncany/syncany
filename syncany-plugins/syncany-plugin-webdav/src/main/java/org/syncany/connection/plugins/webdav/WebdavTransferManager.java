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
import java.security.MessageDigest;
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
import org.syncany.crypto.CipherUtil;
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
		try {
			return sardine.list(repoPath).size() != 0;
		}
		catch (IOException e) {
			throw new StorageException(e);
		}
	}
	
	private void loadTrustStore() {
		if (trustStore == null) {
			try {				
				trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
				
				if (getConnection().getConfig() != null) { // Can be null if uninitialized!					
					File appDir = getConnection().getConfig().getAppDir();
					File certStoreFile = new File(appDir, "truststore.jks"); 
										
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
						File certStoreFile = new File(appDir, "truststore.jks"); 							
						
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
					
					for (int i = 0; i < certificateChain.length; i++) {
						X509Certificate certificate = certificateChain[i];

						logger.log(Level.FINE, "WebDAV: Checking certificate validity: " + certificate.getSubjectDN().toString());
						logger.log(Level.FINEST, "WebDAV:              Full certificate: " + certificate);
						
						// Check validity
						try {
							certificate.checkValidity();	
						}
						catch (CertificateException e) {
							logger.log(Level.FINE, "WebDAV: Certificate is NOT valid.", e);
							return false;
						}
						
						logger.log(Level.FINE, "WebDAV: Checking is VALID.");
						
						// Certificate found; we trust this, okay!
						if (inTrustStore(certificate)) {
							logger.log(Level.FINE, "WebDAV: Certificate found in trust store.");
							return true;
						}
						
						// Certificate is new; continue ...
						else {
							logger.log(Level.FINE, "WebDAV: Certificate NOT found in trust store.");
						}
					}
						
					// We we reach this code, none of the CAs are known in the trust store
					// So we ask the user if he/she wants to add the server certificate to the trust store  

					if (pluginListener == null) {
						throw new RuntimeException("pluginListener cannot be null!");
					}
					
					boolean userTrustsCertificate = pluginListener.onUserConfirm("Unknown SSL/TLS certificate", formatCertificate(serverCertificate), "Do you want to trust this certificate?");
					
					if (!userTrustsCertificate) {
						logger.log(Level.INFO, "WebDAV: User does not trust certificate. ABORTING.");
						return false;
					}
					
					logger.log(Level.INFO, "WebDAV: User trusts certificate. Adding to trust store.");
					addToTrustStore(serverCertificate);
	
					return true;
				}
				catch (Exception e) {
					logger.log(Level.SEVERE, "WebDAV: Key store exception.", e);
					return false;
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
		try {			
			CipherUtil.enableUnlimitedStrength();
			
			String checksumMd5 = formatChecksum(createChecksum(cert.getEncoded(), "MD5"));
			String checksumSha1 = formatChecksum(createChecksum(cert.getEncoded(), "SHA1"));
			String checksumSha256 = formatChecksum(createChecksum(cert.getEncoded(), "SHA256"));
			
			StringBuilder sb = new StringBuilder();
			
			sb.append(String.format("Owner: %s\n", cert.getSubjectDN().getName()));
			sb.append(String.format("Issuer: %s\n", cert.getIssuerDN().getName()));
			sb.append(String.format("Serial number: %d\n", cert.getSerialNumber()));
			sb.append(String.format("Valid from %s until: %s\n", cert.getNotBefore().toString(), cert.getNotAfter().toString()));
			sb.append("Certificate fingerprints:\n");
			sb.append(String.format("	 MD5:  %s\n", checksumMd5));
			sb.append(String.format("	 SHA1: %s\n", checksumSha1));
			sb.append(String.format("	 SHA256: %s", checksumSha256));
						
			return sb.toString();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}
	
	private String formatChecksum(byte[] checksum) {
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<checksum.length; i++) {
			sb.append(StringUtil.toHex(new byte[] { checksum[i] }).toUpperCase());
			
			if (i < checksum.length-1) {
				sb.append(":");
			}
		}
		
		return sb.toString();
	}

	private byte[] createChecksum(byte[] data, String digestAlgorithm) {
		try {
			MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
			digest.update(data, 0, data.length);
			
			return digest.digest();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
