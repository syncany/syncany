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
package org.syncany.plugins.transfer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;
import org.simpleframework.xml.core.Validate;
import org.syncany.config.UserConfig;
import org.syncany.config.to.UserConfigTO;
import org.syncany.crypto.CipherSpecs;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.plugins.Encrypted;
import org.syncany.plugins.Plugin;
import org.syncany.plugins.PluginSettings;
import org.syncany.plugins.Plugins;
import org.syncany.plugins.Setup;
import org.syncany.plugins.UserInteractionListener;
import org.syncany.util.ReflectionUtil;
import org.syncany.util.StringUtil;

/**
 * A connection represents the configuration settings of a storage/connection
 * plugin. It is created through the concrete implementation of a {@link Plugin}.
 * <p/>
 * Options for a plugin specific {@link TransferSettings} can be defined using the
 * {@link Element} annotation. Furthermore some Syncany-specific annotations are available.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Christian Roth <christian.roth@port17.de>
 */
public abstract class TransferSettings {
	private static final Logger logger = Logger.getLogger(TransferSettings.class.getName());
	protected UserInteractionListener userInteractionListener;

	@Attribute
	private String type = findPluginId();

	public UserInteractionListener getUserInteractionListener() {
		return userInteractionListener;
	}

	public void setUserInteractionListener(UserInteractionListener userInteractionListener) {
		this.userInteractionListener = userInteractionListener;
	}

	/**
	 * Get the {@link org.syncany.plugins.transfer.TransferPlugin}'s id.
	 *
	 * @return A string with {@link org.syncany.plugins.transfer.TransferPlugin#getId()}
	 */
	public final String getType() {
		return type;
	}

	/**
	 * Get a setting's value.
	 *
	 * @param key The field name as it is used in the {@link TransferSettings}
	 * @return The value converted to a string using {@link Class#toString()}
	 * @throws StorageException Thrown if the field either does not exist or isn't accessible
	 */
	public final String getField(String key) throws StorageException {
		try {
			Object fieldValueAsObject = this.getClass().getDeclaredField(key).get(this);

			if (fieldValueAsObject == null) {
				return null;
			}

			return fieldValueAsObject.toString();
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			throw new StorageException("Unable to getField named " + key + ": " + e.getMessage());
		}
	}

	/**
	 * Set a setting's value
	 *
	 * @param key The field name as it is used in the {@link TransferSettings}
	 * @param value The object which should be the setting's value. The object's type must match the field type.
	 *              {@link Integer}, {@link String}, {@link Boolean}, {@link File} and implementation of
	 *              {@link TransferSettings} are converted.
	 * @throws StorageException Thrown if the field either does not exist or isn't accessible or
	 *            conversion failed due to invalid field types.
	 */
	public final void setField(String key, Object value) throws StorageException {
		try {
			Field[] elementFields = ReflectionUtil.getAllFieldsWithAnnotation(this.getClass(), Element.class);

			for (Field field : elementFields) {
				field.setAccessible(true);

				String fieldName = field.getName();
				Type fieldType = field.getType();

				if (key.equalsIgnoreCase(fieldName)) {
					if (field.getType() == Integer.TYPE && (value instanceof Integer || value instanceof String)) {
						field.setInt(this, Integer.parseInt(String.valueOf(value)));
					}
					else if (fieldType == Boolean.TYPE && (value instanceof Boolean || value instanceof String)) {
						field.setBoolean(this, Boolean.parseBoolean(String.valueOf(value)));
					}
					else if (fieldType == String.class && value instanceof String) {
						field.set(this, value);
					}
					else if (fieldType == File.class && value instanceof String) {
						field.set(this, new File(String.valueOf(value)));
					}
					else if (TransferSettings.class.isAssignableFrom(value.getClass())) {
						field.set(this, ReflectionUtil.getClassFromType(fieldType).cast(value));
					}
					else {
						throw new RuntimeException("Invalid value type: " + value.getClass());
					}
				}
			}
		}
		catch (Exception e) {
			throw new StorageException("Unable to parse value: " + e.getMessage(), e);
		}
	}

	/**
	 * Check if a {@link TransferSettings} instance is valid i.e. all required fields are present.
	 * {@link TransferSettings} specific validators can be deposited by annotating a method with {@link Validate}.
	 *
	 * @return True if the {@link TransferSettings} instance is valid.
	 */
	public final boolean isValid() {
		Method[] validationMethods = ReflectionUtil.getAllMethodsWithAnnotation(this.getClass(), Validate.class);

		try {
			for (Method method : validationMethods) {
				method.setAccessible(true);
				method.invoke(this);
			}
		}
		catch (InvocationTargetException | IllegalAccessException e) {
			logger.log(Level.SEVERE, "Unable to check if option(s) are valid.", e);

			if (e.getCause() instanceof StorageException) { // Dirty hack
				return false;
			}

			throw new RuntimeException("Unable to call plugin validator: ", e);
		}

		return true;
	}

	/**
	 * Validate if all required fields are present.
	 *
	 * @throws StorageException Thrown if the validation failed due to missing field values.
	 */
	@Validate
	public final void validateRequiredFields() throws StorageException {
		logger.log(Level.FINE, "Validating required fields");

		try {
			Field[] elementFields = ReflectionUtil.getAllFieldsWithAnnotation(this.getClass(), Element.class);

			for (Field field : elementFields) {
				field.setAccessible(true);

				if (field.getAnnotation(Element.class).required() && field.get(this) == null) {
					logger.log(Level.WARNING, "Missing mandatory field {0}#{1}", new Object[] { this.getClass().getSimpleName(), field.getName() });
					throw new StorageException("Missing mandatory field " + this.getClass().getSimpleName() + "#" + field.getName());
				}
			}
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException("IllegalAccessException when validating required fields: ", e);
		}
	}

	@Persist
	private void encrypt() throws Exception {
		// TODO [high] This should be in the @Option annotation
		Field[] optionFields = ReflectionUtil.getAllFieldsWithAnnotation(this.getClass(), Setup.class);

		for (Field field : optionFields) {
			field.setAccessible(true);

			if (field.getAnnotation(Encrypted.class) != null) {
				if (field.getType() != String.class) {
					throw new StorageException("Invalid use of Encrypted annotation: Only strings can be encrypted/decrypted");
				}

				InputStream plainInputStream = IOUtils.toInputStream((String) field.get(this));
				SaltedSecretKey privateKey = UserConfigTO.load(UserConfig.getUserConfigFile()).getPrivateKey();
				byte[] encryptedBytes = CipherUtil.encrypt(plainInputStream, CipherSpecs.getDefaultCipherSpecs(), privateKey);
				field.set(this, StringUtil.toHex(encryptedBytes)); // the field's now encrypted
			}
		}

		logger.log(Level.FINE, "Encrypted transfer setting values");
	}

	@Commit
	private void decrypt() throws Exception {
		// TODO [high] This should be in the @Option annotation
		Field[] optionFields = ReflectionUtil.getAllFieldsWithAnnotation(this.getClass(), Setup.class);

		for (Field field : optionFields) {
			field.setAccessible(true);

			if (field.getAnnotation(Encrypted.class) != null) {
				if (field.getType() != String.class) {
					throw new StorageException("Invalid use of Encrypted annotation: Only strings can be encrypted/decrypted");
				}

				ByteArrayInputStream encryptedInputStream = new ByteArrayInputStream(StringUtil.fromHex((String) field.get(this)));
				SaltedSecretKey privateKey = UserConfigTO.load(UserConfig.getUserConfigFile()).getPrivateKey();
				byte[] decryptedBytes = CipherUtil.decrypt(encryptedInputStream, privateKey);
				field.set(this, new String(decryptedBytes)); // the field's now decrypted
			}
		}

		logger.log(Level.FINE, "Decrypted transfer setting values");
	}

	private String findPluginId() {
		try {
			for (Plugin plugin : Plugins.list()) {
				PluginSettings pluginSettings = plugin.getClass().getAnnotation(PluginSettings.class);

				if (pluginSettings == null || pluginSettings.value().equals(this.getClass())) {
					return plugin.getClass().newInstance().getId();
				}
			}

			throw new RuntimeException("Unable to read type: No TransferPlugin is defined for these settings");
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to read type: No TransferPlugin is defined for these settings", e);
			throw new RuntimeException("Unable to read type: No TransferPlugin is defined for these settings", e);
		}
	}
}
