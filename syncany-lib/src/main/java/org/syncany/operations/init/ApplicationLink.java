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
	
	private static final Pattern LINK_PATTERN = Pattern.compile("^syncany://2/(?:(not-encrypted/)(.+)|([^/]+)/([^/]+))$");
	private static final int LINK_PATTERN_GROUP_NOT_ENCRYPTED_FLAG = 1;
	private static final int LINK_PATTERN_GROUP_NOT_ENCRYPTED_PLUGIN_ENCODED = 2;
	private static final int LINK_PATTERN_GROUP_ENCRYPTED_MASTER_KEY_SALT = 3;
	private static final int LINK_PATTERN_GROUP_ENCRYPTED_PLUGIN_ENCODED = 4;

	private static final int INTEGER_BYTES = 4;

	private TransferSettings transferSettings;
	
	private Matcher linkMatcher;
	private boolean encrypted;	
	private byte[] masterKeySalt;
	private byte[] encryptedSettingsBytes;
	private byte[] plaintextSettingsBytes;
	
	public ApplicationLink(TransferSettings transferSettings) {
		this.transferSettings = transferSettings;
	}
	
	public ApplicationLink(String applicationLink) throws StorageException {
		this.linkMatcher = LINK_PATTERN.matcher(applicationLink);

		if (!linkMatcher.matches()) {
			throw new StorageException("Invalid link provided, must start with syncany:// and match link pattern.");
		}

		this.encrypted = linkMatcher.group(LINK_PATTERN_GROUP_NOT_ENCRYPTED_FLAG) == null;

		if (this.encrypted) {
			String masterKeySaltStr = linkMatcher.group(LINK_PATTERN_GROUP_ENCRYPTED_MASTER_KEY_SALT);
			String encryptedPluginSettingsStr = linkMatcher.group(LINK_PATTERN_GROUP_ENCRYPTED_PLUGIN_ENCODED);

			logger.log(Level.INFO, "- Master salt: " + masterKeySaltStr);
			logger.log(Level.INFO, "- Encrypted plugin settings: " + encryptedPluginSettingsStr);

			this.masterKeySalt = Base58.decode(masterKeySaltStr);
			this.encryptedSettingsBytes = Base58.decode(encryptedPluginSettingsStr);
			this.plaintextSettingsBytes = null;
		}
		else {
			String plaintextEncodedSettingsStr = linkMatcher.group(LINK_PATTERN_GROUP_NOT_ENCRYPTED_PLUGIN_ENCODED);
			
			this.masterKeySalt = null;
			this.encryptedSettingsBytes = null;
			this.plaintextSettingsBytes = Base58.decode(plaintextEncodedSettingsStr);
		}
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

		return String.format(LINK_FORMAT_ENCRYPTED, masterKeySaltEncodedStr, encryptedEncodedPlugin);
	}

	public String createPlaintextLink() throws Exception {
		byte[] plaintextStorageXml = getPlaintextStorageXml();
		String plaintextEncodedStorage = Base58.encode(plaintextStorageXml);

		return String.format(LINK_FORMAT_NOT_ENCRYPTED, plaintextEncodedStorage);
	}	

	private TransferSettings createTransferSettings(byte[] plaintextPluginSettingsBytes) throws StorageException, IOException {
		// Find plugin ID and settings XML
		int pluginIdentifierLength = Ints.fromByteArray(Arrays.copyOfRange(plaintextPluginSettingsBytes, 0, INTEGER_BYTES));
		String pluginId = new String(Arrays.copyOfRange(plaintextPluginSettingsBytes, INTEGER_BYTES, INTEGER_BYTES + pluginIdentifierLength));
		byte[] gzippedPluginSettingsByteArray = Arrays.copyOfRange(plaintextPluginSettingsBytes, INTEGER_BYTES + pluginIdentifierLength, plaintextPluginSettingsBytes.length);
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
