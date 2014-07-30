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
package org.syncany.plugins;

/**
 * Container class for metadata of a plugin option. Does not contain the actual value,
 * but has the responsibility for all single-option validation. 
 */
public class PluginOptionSpec {
	public enum ValueType {
		STRING, INT, BOOLEAN
	};

	public enum OptionValidationResult {
		VALID, INVALID_TYPE, INVALID_NOT_SET
	}

	private String id;
	private String description;
	private ValueType type;
	private boolean mandatory;
	private boolean sensitive;
	private String defaultValue;

	public PluginOptionSpec(String id, String description, ValueType type, boolean mandatory, boolean sensitive, String defaultValue) {
		
		this.id = id;
		this.description = description;
		this.type = type;
		this.mandatory = mandatory;
		this.sensitive = sensitive;
		this.defaultValue = defaultValue;
	}

	public OptionValidationResult validateInput(String value) {
		String validateValue = getValue(value);

		if (!validateInputMandatory(validateValue)) {
			return OptionValidationResult.INVALID_NOT_SET;
		}

		if (!validateInputType(validateValue)) {
			return OptionValidationResult.INVALID_TYPE;
		}

		return OptionValidationResult.VALID;
	}

	public String getValue(String value) {
		return (value != null && !"".equals(value)) ? value : defaultValue;
	}

	private boolean validateInputMandatory(String value) {
		if (mandatory) {
			return (value != null && !value.equals(""));
		}
		else {
			return true;
		}
	}

	private boolean validateInputType(String value) {
		if (type == ValueType.STRING) {
			return true;
		}
		else if (type == ValueType.INT) {
			try {
				Integer.toString(Integer.parseInt(value));
				return true;
			}
			catch (NumberFormatException e) {
				return false;
			}
		}
		else if (type == ValueType.BOOLEAN) {
			return true;
		}
		else {
			throw new RuntimeException("Unknown type: " + type);
		}
	}

	public String getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public boolean isSensitive() {
		return sensitive;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public ValueType getType() {
		return type;
	}
}
