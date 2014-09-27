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
package org.syncany.plugins.setup;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

import org.simpleframework.xml.Element;
import org.syncany.plugins.annotations.Encrypted;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.util.ReflectionUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public class PluginSetup {

	public static class Builder {

		private final List<Field> fields;

		private Builder(List<Field> fields) {
			this.fields = fields;
		}

		public List<Item> asQueriableList() {
			return createQueriableList(fields);
		}

		public List<Field> toFields() {
			return fields;
		}

	}

	public static Builder forClass(Class<? extends TransferSettings> transferSettingsClass) {

		List<Field> fields = Lists.newArrayList(ReflectionUtil.getAllFieldsWithAnnotation(transferSettingsClass, Element.class));

		Ordering<Field> byOrderAnnotation = new Ordering<Field>() {
			@Override
			public int compare(Field left, Field right) {
				int leftV = left.getAnnotation(Setup.class) != null ? left.getAnnotation(Setup.class).order() : -1;
				int rightV = right.getAnnotation(Setup.class) != null ? right.getAnnotation(Setup.class).order() : -1;
				return Ints.compare(leftV, rightV);
			}
		};

		return new Builder(ImmutableList.copyOf(byOrderAnnotation.nullsLast().sortedCopy(fields)));

	}

	private static List<Item> createQueriableList(List<Field> fields) {

		ImmutableList.Builder<Item> items = ImmutableList.builder();

		for (Field field : fields) {
			Element elementA = field.getAnnotation(Element.class);
			Setup setupA = field.getAnnotation(Setup.class);
			String fieldName = !elementA.name().equalsIgnoreCase("") ? elementA.name() : field.getName();
			String fieldDesc = setupA != null && !setupA.description().equalsIgnoreCase("") ? setupA.description() : field.getName();
			boolean required = elementA.required();
			boolean encrypted = field.getAnnotation(Encrypted.class) != null;

			items.add(new Item(field, fieldName, fieldDesc, field.getType(), encrypted, required));
		}

		return items.build();

	}

	public static class Item {

		public enum ValidationResult {
			VALID, INVALID_TYPE, validationResult, INVALID_NOT_SET
		}

		private final Field field;
		private final String name;
		private final String description;
		private final Type type;
		private final boolean encrypted;
		private final boolean required;

		public Item(Field field, String name, String description, Type type, boolean encrypted, boolean required) {
			this.field = field;
			this.name = name;
			this.description = description;
			this.type = type;
			this.encrypted = encrypted;
			this.required = required;
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

		public boolean isEncrypted() {
			return encrypted;
		}

		public boolean isRequired() {
			return required;
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
			else if (type == File.class) {
				try {
					new File(value);
					return true;
				}
				catch (NullPointerException e) {
					return false;
				}
			}
			else {
				throw new RuntimeException("Unknown type: " + type);
			}

		}

	}

}
