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

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;
import org.syncany.config.to.ConnectionTO;
import org.syncany.plugins.Plugin;
import org.syncany.plugins.PluginOptionSpecs;
import org.syncany.plugins.Plugins;
import org.syncany.plugins.UserInteractionListener;
import org.syncany.plugins.annotations.Encrypted;
import org.syncany.plugins.annotations.PluginSettings;
import org.syncany.util.ReflectionUtil;
import org.syncany.util.StringUtil;

/**
 * A connection represents the configuration settings of a storage/connection
 * plugin. It is created through the concrete implementation of a {@link Plugin}.
 *
 * Options for a plugin specific {@link TransferSettings} can be defined using the
 * {@link Element} annotation. Furthermore some Syncany-specific annotations are available.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Christian Roth <christian.roth@port17.de>
 */
public abstract class TransferSettings implements ConnectionTO {
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

	public abstract PluginOptionSpecs getOptionSpecs();

	public final String getType() {
		return type;
	}

	@Override
	public String getField(String key) throws StorageException {
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

	@Override
	public TransferSettings setField(String key, String value) throws StorageException {
		try {
			Field[] simpleXmlElementFields = ReflectionUtil.getAllFieldsWithAnnotation(this.getClass(), Element.class);
			
			for (Field field : simpleXmlElementFields) {
				field.setAccessible(true);
				
				String fieldName = field.getName();
				Type fieldType = field.getType();
				
				if (key.equalsIgnoreCase(fieldName)) {
					if (field.getType() == Integer.TYPE) {
						field.setInt(this, Integer.parseInt(value));
					}
					else if (fieldType == Boolean.TYPE) {
						field.setBoolean(this, Boolean.parseBoolean(value));
					}
					else if (fieldType == String.class) {
						field.set(this, value);
					}
					else if (fieldType == File.class) {
						field.set(this, new File(value));
					}
				}
			}
		}
		catch (Exception e) {
			throw new StorageException("Unable to parse key value map (1): " + e.getMessage());
		}

		if (!isValid()) {
			throw new StorageException("Unable to parse key value map (2): settings not valid (perhaps missing some mandatory values)");
		}

		return this;
	}

	public final boolean isValid() {
		try {
			Field[] simpleXmlElementFields = ReflectionUtil.getAllFieldsWithAnnotation(this.getClass(), Element.class);

			for (Field field : simpleXmlElementFields) {
				field.setAccessible(true);
				
				if (field.getAnnotation(Element.class).required() && field.get(this) == null) {
					logger.log(Level.WARNING, "Missing mandatory field {0}#{1}", new Object[] { this.getClass().getSimpleName(), field.getName() });
					return false;
				}
			}
		}
		catch (IllegalAccessException e) {
			logger.log(Level.SEVERE, "illegalaccess", e);
			return false;
		}

		return true;
	}

	@Persist
	private void encrypt() throws Exception {
		for (Field field : ReflectionUtil.getAllFieldsWithAnnotation(this.getClass(), Encrypted.class)) {
			if (field.getType() != String.class) {
				throw new StorageException("Invalid use of Encrypted annotation: Only strings can be encrypted");
			}
			
			// TODO [medium] Do actual encryption
			field.set(this, StringUtil.toHex(((String) field.get(this)).getBytes()));
		}

		logger.log(Level.FINE, "Encrypted transfer setting values");
	}

	@Commit
	private void decrypt() throws Exception {
		for (Field field : ReflectionUtil.getAllFieldsWithAnnotation(this.getClass(), Encrypted.class)) {
			if (field.getType() != String.class) {
				throw new StorageException("Invalid use of Encrypted annotation: Only strings can be encrypted");
			}
			
			// TODO [medium] Do actual decryption
			field.set(this, new String(StringUtil.fromHex((String) field.get(this))));
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
