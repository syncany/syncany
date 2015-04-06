/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
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

import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simpleframework.xml.Element;
import org.syncany.util.ReflectionUtil;
import org.syncany.util.StringUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;

/**
 * Helper class to read the options of a {@link TransferSettings} using the
 * {@link Setup} and {@link Element} annotations.
 *
 * @author Christian Roth <christian.roth@port17.de>
 */
public class TransferPluginOptions {
	private static final Logger logger = Logger.getLogger(TransferPluginOptions.class.getName());
	private static final int MAX_NESTED_LEVELS = 3;

	/**
	 * Get an ordered list of {@link TransferPluginOption}s, given class a {@link TransferSettings} class.
	 *
	 * <p>This method uses the {@link Setup} and {@link Element} annotation, and their attributes
	 * to sort the options. If no annotation is given or no order attribute is provided, the
	 * option will be listed last.
	 */
	public static List<TransferPluginOption> getOrderedOptions(Class<? extends TransferSettings> transferSettingsClass) {
		return getOrderedOptions(transferSettingsClass, 0);
	}

	private static List<TransferPluginOption> getOrderedOptions(Class<? extends TransferSettings> transferSettingsClass, int level) {
		List<Field> fields = getOrderedFields(transferSettingsClass);
		ImmutableList.Builder<TransferPluginOption> options = ImmutableList.builder();

		for (Field field : fields) {
			TransferPluginOption option = getOptionFromField(field, transferSettingsClass, level);
			options.add(option);
		}

		return options.build();
	}

	private static TransferPluginOption getOptionFromField(Field field, Class<? extends TransferSettings> transferSettingsClass, int level) {		
		Element elementAnnotation = field.getAnnotation(Element.class);
		Setup setupAnnotation = field.getAnnotation(Setup.class);

		boolean hasName = !elementAnnotation.name().equalsIgnoreCase("");
		boolean hasDescription = setupAnnotation != null && !setupAnnotation.description().equals("");
		boolean hasCallback = setupAnnotation != null && !setupAnnotation.callback().isInterface();
		boolean hasConverter = setupAnnotation != null && !setupAnnotation.converter().isInterface();
		boolean hasFileType = setupAnnotation != null && setupAnnotation.fileType() != null;

		String name = (hasName) ? elementAnnotation.name() : field.getName();
		String description = (hasDescription) ? setupAnnotation.description() : field.getName();
		FileType fileType = (hasFileType) ? setupAnnotation.fileType() : null;
		boolean required = elementAnnotation.required();
		boolean sensitive = setupAnnotation != null && setupAnnotation.sensitive();
		boolean singular = setupAnnotation != null && setupAnnotation.singular();
		boolean visible = setupAnnotation != null && setupAnnotation.visible();
		boolean encrypted = field.getAnnotation(Encrypted.class) != null;
		Class<? extends TransferPluginOptionCallback> callback = (hasCallback) ? setupAnnotation.callback() : null;
		Class<? extends TransferPluginOptionConverter> converter = (hasConverter) ? setupAnnotation.converter() : null;

		boolean isNestedOption = TransferSettings.class.isAssignableFrom(field.getType());

		if (isNestedOption) {
			return createNestedOption(field, level, name, description, fileType, encrypted, sensitive, singular, visible, required, callback, converter);			
		}
		else {
			return createNormalOption(field, transferSettingsClass, name, description, fileType, encrypted, sensitive, singular, visible, required, callback, converter);			
		}
	}

	@SuppressWarnings("unchecked")
	private static TransferPluginOption createNestedOption(Field field, int level, String name, String description, FileType fileType,
			boolean encrypted, boolean sensitive, boolean singular, boolean visible, boolean required,
			Class<? extends TransferPluginOptionCallback> callback, Class<? extends TransferPluginOptionConverter> converter) {
		
		if (++level > MAX_NESTED_LEVELS) {
			throw new RuntimeException("Plugin uses too many nested transfer settings (max allowed value: " + MAX_NESTED_LEVELS + ")");
		}

		Class<? extends TransferSettings> fieldClass = (Class<? extends TransferSettings>) field.getType();
		return new NestedTransferPluginOption(field, name, description, fieldClass, fileType, encrypted, sensitive, singular, visible, required, callback, converter,
				getOrderedOptions(fieldClass));
	}

	private static TransferPluginOption createNormalOption(Field field, Class<? extends TransferSettings> transferSettingsClass, String name,
			String description, FileType fileType, boolean encrypted, boolean sensitive, boolean singular, boolean visible, boolean required,
			Class<? extends TransferPluginOptionCallback> callback, Class<? extends TransferPluginOptionConverter> converter) {
		
		if (Enum.class.isAssignableFrom(field.getType())) {
			Object[] enumValues = getEnumValues(field, transferSettingsClass);			
			description = description + " (Valid values are: " + StringUtil.join(enumValues, ", ") + ")";
		}

		return new TransferPluginOption(field, name, description, field.getType(), fileType, encrypted, sensitive, singular, visible, required, callback, converter);
	}

	private static Object[] getEnumValues(Field field, Class<? extends TransferSettings> transferSettingsClass) {
		Object[] enumValues = field.getType().getEnumConstants();		
		logger.log(Level.FINE, "Enum values are: " + StringUtil.join(enumValues, ", "));

		if (enumValues == null) {
			throw new RuntimeException("Invalid TransferSettings class found: Enum at " + transferSettingsClass + " has no values");
		}
		
		return enumValues;
	}

	private static List<Field> getOrderedFields(Class<? extends TransferSettings> transferSettingsClass) {
		Ordering<Field> byOrderAnnotation = new Ordering<Field>() {
			@Override
			public int compare(Field leftField, Field rightField) {
				int leftOrderValue = (leftField.getAnnotation(Setup.class) != null) ? leftField.getAnnotation(Setup.class).order() : -1;
				int rightOrderValue = (rightField.getAnnotation(Setup.class) != null) ? rightField.getAnnotation(Setup.class).order() : -1;

				return Ints.compare(leftOrderValue, rightOrderValue);
			}
		};

		List<Field> fields = Lists.newArrayList(ReflectionUtil.getAllFieldsWithAnnotation(transferSettingsClass, Element.class));
		return ImmutableList.copyOf(byOrderAnnotation.nullsLast().sortedCopy(fields));
	}
}
