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

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherSpecs;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.plugins.Plugins;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferPluginUtil;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.util.Base58;

import com.google.common.primitives.Ints;

/**
 * The application link class represents a <tt>syncany://</tt> link. It allowed creating
 * and parsing a link. The class has two modes of operation: 
 * 
 * <p>To create a new application link from an existing repository, call the {@link #ApplicationLink(TransferSettings)}
 * constructor and subsequently either call {@link #createPlaintextLink()} or {@link #createEncryptedLink(SaltedSecretKey)}.
 * This method will typically be called during the 'init' or 'genlink' process.
 * 
 * <p>To parse an existing application link and return the relevant {@link TransferSettings}, call the 
 * {@link #ApplicationLink(String)} constructor and subsequently call {@link #createTransferSettings()}
 * or {@link #createTransferSettings(SaltedSecretKey)}. This method will typically be called during the 'connect' process.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Christian Roth <christian.roth@port17.de>
 */
public class ApplicationLink {
	private static final Logger logger = Logger.getLogger(ApplicationLink.class.getSimpleName());
	
	private static final String LINK_FORMAT_NOT_ENCRYPTED = "syncany://2/not-encrypted/%s";
	private static final String LINK_FORMAT_ENCRYPTED = "syncany://2/%s/%s";

	private static final Pattern LINK_PATTERN = Pattern.compile("syncany://?2/(?:(not-encrypted/)(.+)|([^/]+)/([^/]+))$");
	private static final int LINK_PATTERN_GROUP_NOT_ENCRYPTED_FLAG = 1;
	private static final int LINK_PATTERN_GROUP_NOT_ENCRYPTED_PLUGIN_ENCODED = 2;
	private static final int LINK_PATTERN_GROUP_ENCRYPTED_MASTER_KEY_SALT = 3;
	private static final int LINK_PATTERN_GROUP_ENCRYPTED_PLUGIN_ENCODED = 4;

	private static final Pattern LINK_SHORT_URL_PATTERN = Pattern.compile("syncany://?s/(.+)$");
	private static final int LINK_SHORT_URL_PATTERN_GROUP_SHORTLINK = 1;
	private static final String LINK_SHORT_URL_FORMAT = "syncany://s/%s";
	private static final String LINK_SHORT_API_URL_GET_FORMAT = "https://api.syncany.org/v2/links/?l=%s";
	private static final String LINK_SHORT_API_URL_ADD = "https://api.syncany.org/v2/links/add";

	private static final Pattern LINK_HTTP_PATTERN = Pattern.compile("https?://.+");
	private static final int LINK_HTTP_MAX_REDIRECT_COUNT = 5;

	private static final int INTEGER_BYTES = 4;

	private TransferSettings transferSettings;
	private boolean shortUrl;

	private boolean encrypted;
	private byte[] masterKeySalt;
	private byte[] encryptedSettingsBytes;
	private byte[] plaintextSettingsBytes;

	public ApplicationLink(TransferSettings transferSettings, boolean shortUrl) {
		this.transferSettings = transferSettings;
		this.shortUrl = shortUrl;
	}

	public ApplicationLink(String applicationLink) throws StorageException {
		if (LINK_SHORT_URL_PATTERN.matcher(applicationLink).matches()) {
			applicationLink = expandLink(applicationLink);
		}

		if (LINK_HTTP_PATTERN.matcher(applicationLink).matches()) {
			applicationLink = resolveLink(applicationLink, 0);
		}

		parseLink(applicationLink);
	}

	public boolean isEncrypted() {
		return encrypted;
	}

	public byte[] getMasterKeySalt() {
		return masterKeySalt;
	}

	public TransferSettings createTransferSettings(SaltedSecretKey masterKey) throws Exception {
		if (!encrypted || encryptedSettingsBytes == null) {
			throw new IllegalArgumentException("Link is not encrypted. Cannot call this method.");
		}

		byte[] plaintextPluginSettingsBytes = CipherUtil.decrypt(new ByteArrayInputStream(encryptedSettingsBytes), masterKey);
		return createTransferSettings(plaintextPluginSettingsBytes);
	}

	public TransferSettings createTransferSettings() throws Exception {
		if (encrypted || plaintextSettingsBytes == null) {
			throw new IllegalArgumentException("Link is encrypted. Cannot call this method.");
		}

		return createTransferSettings(plaintextSettingsBytes);
	}

	public String createEncryptedLink(SaltedSecretKey masterKey) throws Exception {
		byte[] plaintextStorageXml = getPlaintextStorageXml();
		List<CipherSpec> cipherSpecs = CipherSpecs.getDefaultCipherSpecs(); // TODO [low] Shouldn't this be the same as the application?!

		byte[] masterKeySalt = masterKey.getSalt();
		byte[] encryptedPluginBytes = CipherUtil.encrypt(new ByteArrayInputStream(plaintextStorageXml), cipherSpecs, masterKey);

		String masterKeySaltEncodedStr = Base58.encode(masterKeySalt);
		String encryptedEncodedPlugin = Base58.encode(encryptedPluginBytes);

		String applicationLink = String.format(LINK_FORMAT_ENCRYPTED, masterKeySaltEncodedStr, encryptedEncodedPlugin);

		if (shortUrl) {
			return shortenLink(applicationLink);
		}
		else {
			return applicationLink;
		}
	}

	public String createPlaintextLink() throws Exception {
		byte[] plaintextStorageXml = getPlaintextStorageXml();
		String plaintextEncodedStorage = Base58.encode(plaintextStorageXml);

		return String.format(LINK_FORMAT_NOT_ENCRYPTED, plaintextEncodedStorage);
	}

	private String expandLink(String applicationLink) {
		Matcher shortLinkMatcher = LINK_SHORT_URL_PATTERN.matcher(applicationLink);

		if (!shortLinkMatcher.matches()) {
			throw new IllegalArgumentException("Method may only be called with application shortlink.");
		}

		String shortLinkId = shortLinkMatcher.group(LINK_SHORT_URL_PATTERN_GROUP_SHORTLINK);
		return String.format(LINK_SHORT_API_URL_GET_FORMAT, shortLinkId);
	}

	private String resolveLink(String httpApplicationLink, int redirectCount) throws StorageException {
		if (redirectCount >= LINK_HTTP_MAX_REDIRECT_COUNT) {
			throw new StorageException("Max. redirect count of " + LINK_HTTP_MAX_REDIRECT_COUNT + " for URL reached. Canot find syncany:// link.");
		}

		try {
			logger.log(Level.INFO, "- Retrieving HTTP HEAD for " + httpApplicationLink + " ...");

			HttpHead headMethod = new HttpHead(httpApplicationLink);
			HttpResponse httpResponse = createHttpClient().execute(headMethod);

			// Find syncany:// link
			Header locationHeader = httpResponse.getLastHeader("Location");

			if (locationHeader == null) {
				throw new Exception("Link does not redirect to a syncany:// link.");
			}

			String locationHeaderUrl = locationHeader.getValue();
			Matcher locationHeaderMatcher = LINK_PATTERN.matcher(locationHeaderUrl);
			boolean isApplicationLink = locationHeaderMatcher.find();

			if (isApplicationLink) {
				String applicationLink = locationHeaderMatcher.group(0);
				logger.log(Level.INFO, "Resolved application link is: " + applicationLink);

				return applicationLink;
			}
			else {
				return resolveLink(locationHeaderUrl, ++redirectCount);
			}
		}
		catch (Exception e) {
			throw new StorageException(e.getMessage(), e);
		}
	}

	private String shortenLink(String applicationLink) {
		if (!LINK_PATTERN.matcher(applicationLink).matches()) {
			throw new IllegalArgumentException("Invalid link provided, must start with syncany:// and match link pattern.");
		}

		try {
			logger.log(Level.INFO, "Shortining link " + applicationLink + " via " + LINK_SHORT_API_URL_ADD + " ...");

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			nameValuePairs.add(new BasicNameValuePair("l", applicationLink));

			HttpPost postMethod = new HttpPost(LINK_SHORT_API_URL_ADD);
			postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			HttpResponse httpResponse = createHttpClient().execute(postMethod);
			ApplicationLinkShortenerResponse shortenerResponse = new Persister().read(ApplicationLinkShortenerResponse.class, httpResponse
					.getEntity().getContent());
			
			return String.format(LINK_SHORT_URL_FORMAT, shortenerResponse.getShortLinkId());
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Cannot shorten URL. Using long URL.", e);
			return applicationLink;
		}
	}
	
	private CloseableHttpClient createHttpClient() {
		RequestConfig requestConfig = RequestConfig.custom()
				.setSocketTimeout(2000)
				.setConnectTimeout(2000)
				.setRedirectsEnabled(false)
				.build();

		CloseableHttpClient httpClient = HttpClientBuilder
				.create()
				.setDefaultRequestConfig(requestConfig)
				.build();
		
		return httpClient;
	}

	private void parseLink(String applicationLink) throws StorageException {
		Matcher linkMatcher = LINK_PATTERN.matcher(applicationLink);

		if (!linkMatcher.matches()) {
			throw new StorageException("Invalid link provided, must start with syncany:// and match link pattern.");
		}

		encrypted = linkMatcher.group(LINK_PATTERN_GROUP_NOT_ENCRYPTED_FLAG) == null;

		if (encrypted) {
			String masterKeySaltStr = linkMatcher.group(LINK_PATTERN_GROUP_ENCRYPTED_MASTER_KEY_SALT);
			String encryptedPluginSettingsStr = linkMatcher.group(LINK_PATTERN_GROUP_ENCRYPTED_PLUGIN_ENCODED);

			logger.log(Level.INFO, "- Master salt: " + masterKeySaltStr);
			logger.log(Level.INFO, "- Encrypted plugin settings: " + encryptedPluginSettingsStr);

			masterKeySalt = Base58.decode(masterKeySaltStr);
			encryptedSettingsBytes = Base58.decode(encryptedPluginSettingsStr);
			plaintextSettingsBytes = null;
		}
		else {
			String plaintextEncodedSettingsStr = linkMatcher.group(LINK_PATTERN_GROUP_NOT_ENCRYPTED_PLUGIN_ENCODED);

			masterKeySalt = null;
			encryptedSettingsBytes = null;
			plaintextSettingsBytes = Base58.decode(plaintextEncodedSettingsStr);
		}
	}

	private TransferSettings createTransferSettings(byte[] plaintextPluginSettingsBytes) throws StorageException, IOException {
		// Find plugin ID and settings XML
		int pluginIdentifierLength = Ints.fromByteArray(Arrays.copyOfRange(plaintextPluginSettingsBytes, 0, INTEGER_BYTES));
		String pluginId = new String(Arrays.copyOfRange(plaintextPluginSettingsBytes, INTEGER_BYTES, INTEGER_BYTES + pluginIdentifierLength));
		byte[] gzippedPluginSettingsByteArray = Arrays.copyOfRange(plaintextPluginSettingsBytes, INTEGER_BYTES + pluginIdentifierLength,
				plaintextPluginSettingsBytes.length);
		String pluginSettings = IOUtils.toString(new GZIPInputStream(new ByteArrayInputStream(gzippedPluginSettingsByteArray)));

		// Create transfer settings object 
		try {
			TransferPlugin plugin = Plugins.get(pluginId, TransferPlugin.class);

			if (plugin == null) {
				throw new StorageException("Link contains unknown connection type '" + pluginId + "'. Corresponding plugin not found.");
			}

			Class<? extends TransferSettings> pluginTransferSettingsClass = TransferPluginUtil.getTransferSettingsClass(plugin.getClass());
			TransferSettings transferSettings = new Persister().read(pluginTransferSettingsClass, pluginSettings);

			logger.log(Level.INFO, "(Decrypted) link contains: " + pluginId + " -- " + pluginSettings);

			return transferSettings;
		}
		catch (Exception e) {
			throw new StorageException(e);
		}
	}

	private byte[] getPlaintextStorageXml() throws Exception {
		ByteArrayOutputStream plaintextByteArrayOutputStream = new ByteArrayOutputStream();
		DataOutputStream plaintextOutputStream = new DataOutputStream(plaintextByteArrayOutputStream);
		plaintextOutputStream.writeInt(transferSettings.getType().getBytes().length);
		plaintextOutputStream.write(transferSettings.getType().getBytes());

		GZIPOutputStream plaintextGzipOutputStream = new GZIPOutputStream(plaintextOutputStream);
		new Persister(new Format(0)).write(transferSettings, plaintextGzipOutputStream);
		plaintextGzipOutputStream.close();

		return plaintextByteArrayOutputStream.toByteArray();
	}
}
