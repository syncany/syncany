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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;
import org.syncany.config.to.ConnectionTO;
import org.syncany.plugins.Plugin;
import org.syncany.plugins.PluginOptionSpecs;
import org.syncany.plugins.StorageException;
import org.syncany.plugins.UserInteractionListener;
import org.syncany.plugins.annotations.Encrypted;
import org.syncany.plugins.annotations.PluginSettings;
import org.syncany.util.StringUtil;

/**
 * A connection represents the configuration settings of a storage/connection
 * plugin. It is created through the concrete implementation of a {@link Plugin}.
 *
 * <p>A connection must be initialized by calling the {@link #init(Map) init()} method,
 * using plugin specific configuration parameters.
 *
 * <p>Once initialized, a {@link TransferManager} can be created through the {@link #createTransferManager()}
 * method. The transfer manager can then be used to upload/download files.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Christian Roth <christian.roth@port17.de>
 */
public abstract class TransferSettings implements ConnectionTO {
	private static final Logger logger = Logger.getLogger(TransferSettings.class.getName());
	protected UserInteractionListener userInteractionListener;

	@Attribute
	private String type;

	{
		Reflections reflections = new Reflections("org.syncany");
		try {
			for (Class<?> annotatedClass : reflections.getTypesAnnotatedWith(PluginSettings.class)) {
				if (annotatedClass.getAnnotationsByType(PluginSettings.class)[0].value().getName().equals(this.getClass().getName())) {
					type = ((TransferPlugin) annotatedClass.newInstance()).getId();
				}
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to read type: No TransferPlugin is defined for these settings", e);
		}
	}

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

	public TransferSettings parseKeyValueMap(Map<String, String> keyValueMap) throws StorageException {

		try {
			for (Field f : ReflectionUtils.getAllFields(this.getClass(), ReflectionUtils.withAnnotation(Element.class))) {

				f.setAccessible(true);
				String fName = f.getName();
				Type fType = f.getType();
				if (keyValueMap.containsKey(fName)) {
					String fValue = keyValueMap.get(fName);

					if (f.getType() == Integer.TYPE) {
						f.setInt(this, Integer.parseInt(fValue));
					}
					else if (fType == Boolean.TYPE) {
						f.setBoolean(this, Boolean.parseBoolean(fValue));
					}
					else if (fType == String.class) {
						f.set(this, fValue);
					}
					else if (fType == File.class) {
						f.set(this, new File(fValue));
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
			for (Field f : ReflectionUtils.getAllFields(this.getClass(), ReflectionUtils.withAnnotation(Element.class))) {
				f.setAccessible(true);
				if (f.getAnnotationsByType(Element.class)[0].required() && f.get(this) == null) {
					logger.log(Level.WARNING, "Missing mandatory field {0}#{1}", new Object[] { this.getClass().getSimpleName(), f.getName() });
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

		for (Field f : ReflectionUtils.getAllFields(this.getClass(), ReflectionUtils.withAnnotation(Encrypted.class))) {
			if (f.getType() != String.class) {
				throw new StorageException("Invalid use of Encrypted annotation: Only strings can be encrypted");
			}
			// TODO dummy encprytion
			f.set(this, StringUtil.toHex(((String) f.get(this)).getBytes()));
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "Encrypted transfer setting values");
		}

	}

	@Commit
	private void decrypt() throws Exception {

		for (Field f : ReflectionUtils.getAllFields(this.getClass(), ReflectionUtils.withAnnotation(Encrypted.class))) {
			if (f.getType() != String.class) {
				throw new StorageException("Invalid use of Encrypted annotation: Only strings can be encrypted");
			}
			// TODO dummy decryption
			f.set(this, new String(StringUtil.fromHex((String) f.get(this))));
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "Decrypted transfer setting values");
		}

	}
}
