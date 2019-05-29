/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com>
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

import org.syncany.util.ReflectionUtil;

/**
 * A plugin option represents a single setting of a transfer plugin
 * within the corresponding {@link TransferSettings} class. A plugin option
 * is created during the initialization from the {@link Setup} annotation
 * to aid the guided repository setup (init and connect).
 *
 * @author Christian Roth (christian.roth@port17.de)
 */
public class TransferPluginOption {
	public enum ValidationResult {
		VALID, INVALID_TYPE, INVALID_NOT_SET
	}

	private final Field field;
	private final String name;
	private final String description;
	private final Type type;
	private final FileType fileType;
	private final boolean encrypted;
	private final boolean sensitive;
	private final boolean singular;
	private final boolean visible;
	private final boolean required;
	private final Class<? extends TransferPluginOptionCallback> callback;
	private final Class<? extends TransferPluginOptionConverter> converter;

	public TransferPluginOption(Field field, String name, String description, Type type, FileType fileType, boolean encrypted, boolean sensitive,
			boolean singular, boolean visible, boolean required, Class<? extends TransferPluginOptionCallback> callback,
			Class<? extends TransferPluginOptionConverter> converter) {

		this.field = field;
		this.name = name;
		this.description = description;
		this.type = type;
		this.fileType = fileType;
		this.encrypted = encrypted;
		this.sensitive = sensitive;
		this.singular = singular;
		this.visible = visible;
		this.required = required;
		this.callback = callback;
		this.converter = converter;
	}

	public Field getField() {
		return field;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Type getType() {
		return type;
	}

	public FileType getFileType() {
		return fileType;
	}

	public boolean isEncrypted() {
		return encrypted;
	}

	public boolean isSensitive() {
		return sensitive;
	}

	public boolean isSingular() {
		return singular;
	}

	public boolean isVisible() {
		return visible;
	}

	public boolean isRequired() {
		return required;
	}

	public Class<? extends TransferPluginOptionCallback> getCallback() {
		return callback;
	}

	public Class<? extends TransferPluginOptionConverter> getConverter() {
		return converter;
	}

	public ValidationResult isValid(String value) {
		if (!validateInputMandatory(value)) {
			return ValidationResult.INVALID_NOT_SET;
		}

		if (!validateInputType(value)) {
			return ValidationResult.INVALID_TYPE;
		}

		return ValidationResult.VALID;
	}

	private boolean validateInputMandatory(String value) {
		return !isRequired() || (value != null && !value.equals(""));
	}

	private boolean validateInputType(String value) {
		if (type == String.class) {
			return true;
		}
		else if (type == Integer.TYPE) {
			try {
				Integer.toString(Integer.parseInt(value));
				return true;
			}
			catch (NumberFormatException e) {
				return false;
			}
		}
		else if (type == Boolean.TYPE) {
			return true;
		}
		else if (ReflectionUtil.getClassFromType(type).isEnum()) {
			return ReflectionUtil.isValidEnum(value.toUpperCase(), ReflectionUtil.getClassFromType(type));
		}
		else if (type == File.class) {
			if (isRequired()) {
				if (value != null) {
					return true;
				}
				return false;
			}
			else {
				return true;
			}
		}
		else {
			throw new RuntimeException("Unknown type: " + type);
		}
	}
}
