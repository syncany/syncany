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

import java.lang.reflect.Field;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import org.simpleframework.xml.Element;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.util.ReflectionUtil;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */
public class PluginOptions {

	public static final int MAX_NESTED_LEVELS = 3;

	public static List<Field> getOrderedFields(Class<? extends TransferSettings> transferSettingsClass) {

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

	public static List<PluginOption> getOrderedOptions(Class<? extends TransferSettings> transferSettingsClass) {
		return getOrderedOptions(transferSettingsClass, 0);
	}

	private static List<PluginOption> getOrderedOptions(Class<? extends TransferSettings> transferSettingsClass, int level) {

		List<Field> fields = getOrderedFields(transferSettingsClass);
		ImmutableList.Builder<PluginOption> options = ImmutableList.builder();

		for (Field field : fields) {
			Element elementAnnotation = field.getAnnotation(Element.class);
			Setup setupAnnotation = field.getAnnotation(Setup.class);

			String fieldName = (!elementAnnotation.name().equalsIgnoreCase("")) ? elementAnnotation.name() : field.getName();
			String fieldDesc = (setupAnnotation != null && !setupAnnotation.description().equalsIgnoreCase("")) ? setupAnnotation.description()
					: field.getName();
			Class<? extends OptionCallback> callback = (setupAnnotation != null && !setupAnnotation.callback().isInterface()) ? setupAnnotation
					.callback() : null;
			boolean required = elementAnnotation.required();
			boolean sensitive = setupAnnotation != null && setupAnnotation.sensitive();
			boolean encrypted = field.getAnnotation(Encrypted.class) != null;

			if (TransferSettings.class.isAssignableFrom(field.getType())) {
				if (++level > MAX_NESTED_LEVELS) {
					throw new RuntimeException("Plugin uses too many nested transfer settings (max allowed value: " + MAX_NESTED_LEVELS + ")");
				}

				Class<? extends TransferSettings> fieldClass = (Class<? extends TransferSettings>) field.getType();
				options.add(new NestedPluginOption(field, fieldName, fieldDesc, field.getType(), encrypted, sensitive, required, callback,
						getOrderedOptions(fieldClass)));
			}
			else {
				options.add(new PluginOption(field, fieldName, fieldDesc, field.getType(), encrypted, sensitive, required, callback));
			}
		}

		return options.build();

	}

}
