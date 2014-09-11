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
package org.syncany.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Christian Roth <christian.roth@port17.de>
 * @version 0.0.1
 */

public abstract class ReflectionUtil {

	public static <T extends Annotation> T[] getAnnotationsForClassByType(Class<?> clazz, final Class<T> annotation) {

		List<T> matchedAnnotations = new ArrayList<>();

		for (Annotation a : clazz.getAnnotations()) {
			if (a.annotationType().equals(annotation)) {
				matchedAnnotations.add((T) a);
			}
		}

		return (T[]) matchedAnnotations.toArray(new Annotation[matchedAnnotations.size()]);

	}

	public static Field[] getAllFieldsWithAnnotation(Class<?> clazz, final Class<? extends Annotation> annotation) {

		List<Field> matchedAnnotations = new ArrayList<>();

		for (Field f : clazz.getDeclaredFields()) {
			if (f.isAnnotationPresent(annotation)) {
				matchedAnnotations.add(f);
			}
		}

		return matchedAnnotations.toArray(new Field[matchedAnnotations.size()]);

	}

}
