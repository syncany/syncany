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
package org.syncany.connection.plugins.webdav;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.PluginSetting;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.connection.plugins.PluginSetting.ValueType;

public class WebdavConnection extends Connection {
	private String url;
	private String username;
	private String password;

	private boolean secure;
	private SSLSocketFactory sslSocketFactory;

	@Override
	public TransferManager createTransferManager() {
		return new WebdavTransferManager(this);
	}

	@Override
	public void init(Map<String, String> map) throws StorageException {
		// Mandatory
		String url = map.get("url");
		String username = map.get("username");
		String password = map.get("password");

		if (url == null || username == null || password == null) {
			throw new StorageException("Mandatory fields missing for Webdav configuration: url, username and password.");
		}

		this.url = url;
		this.username = username;
		this.password = password;

		// SSL
		if (url.toLowerCase().startsWith("https")) {
			try {
				initSsl();
			}
			catch (Exception e) {
				throw new StorageException(e);
			}
		}
	}

	private void initSsl() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException, UnrecoverableKeyException {
		this.secure = true;

		/*String keyStoreFilename = "/tmp/mystore";
		File keystoreFile = new File(keyStoreFilename);
		FileInputStream fis = new FileInputStream(keystoreFile);
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType()); // JKS
		keyStore.load(fis, null);*/

		TrustStrategy trustStrategy = new TrustStrategy() {			
			@Override
			public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				for (X509Certificate cert : chain) {
					System.out.println(cert);
				}
				 
				return true; // TODO [high] WebDAV SSL: This should query the CLI/GUI (and store the cert. locally); right now, MITMs are easily possible!
			}
		};
		
		this.sslSocketFactory = new SSLSocketFactory(trustStrategy);
	}

	@Override 
	public List<PluginSetting> getSettings() {
    	return Arrays.asList(new PluginSetting[]{
    			new PluginSetting("url", ValueType.STRING, true, false),
    			new PluginSetting("username", ValueType.STRING, true, false),
    			new PluginSetting("password", ValueType.STRING, true, true),
    			
    	});
	}
	

	@Override
	public String toString() {
		return WebdavConnection.class.getSimpleName() + "[url=" + url + ", username=" + username + "]";
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getURL(String filename) {
		return (url.endsWith("/") ? "" : "/") + filename;
	}

	public boolean isSecure() {
		return secure;
	}

	public SSLSocketFactory getSslSocketFactory() {
		return sslSocketFactory;
	}
}
