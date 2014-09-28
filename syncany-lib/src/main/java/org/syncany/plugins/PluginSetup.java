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

import org.simpleframework.xml.Element;
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
	public static List<Field> getOrderedFields(Class<? extends TransferSettings> transferSettingsClass) {		
		Ordering<Field> byOrderAnnotation = new Ordering<Field>() {
			@Override
			public int compare(Field leftField, Field rightField) {
				int leftOrderValue = (leftField.getAnnotation(Option.class) != null) ? leftField.getAnnotation(Option.class).order() : -1;
				int rightOrderValue = (rightField.getAnnotation(Option.class) != null) ? rightField.getAnnotation(Option.class).order() : -1;
				
				return Ints.compare(leftOrderValue, rightOrderValue);
			}
		};
		
		List<Field> fields = Lists.newArrayList(ReflectionUtil.getAllFieldsWithAnnotation(transferSettingsClass, Element.class));
		return ImmutableList.copyOf(byOrderAnnotation.nullsLast().sortedCopy(fields));		
	}

	public static List<PluginOption> getOrderedOptions(Class<? extends TransferSettings> transferSettingsClass) {		
		List<Field> fields = getOrderedFields(transferSettingsClass);		
		ImmutableList.Builder<PluginOption> options = ImmutableList.builder();

		for (Field field : fields) {
			Element elementAnnotation = field.getAnnotation(Element.class);
			Option optionAnnotation = field.getAnnotation(Option.class);
			
			String fieldName = (!elementAnnotation.name().equalsIgnoreCase("")) ? elementAnnotation.name() : field.getName();
			String fieldDesc = (optionAnnotation != null && !optionAnnotation.description().equalsIgnoreCase("")) ? optionAnnotation.description() : field.getName();
			Class<? extends FieldCallback> callback = (optionAnnotation != null && !optionAnnotation.callback().isInterface()) ? optionAnnotation.callback() : null;
			boolean required = elementAnnotation.required();
			boolean encrypted = optionAnnotation.encrypted();

			options.add(new PluginOption(field, fieldName, fieldDesc, field.getType(), encrypted, required, callback));
		}

		return options.build();
	}
}
