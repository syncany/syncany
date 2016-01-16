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
package org.syncany.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to find classes, methods and fields with certain properties -
 * typically having an annotation or a certain erasure.
 *
 * @author Christian Roth <christian.roth@port17.de>
 */
public abstract class ReflectionUtil {
	public static Field[] getAllFieldsWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
		List<Field> matchedAnnotations = new ArrayList<>();

		while (clazz != null) {
			for (Field f : clazz.getDeclaredFields()) {
				if (f.isAnnotationPresent(annotation)) {
					matchedAnnotations.add(f);
				}
			}
			
			clazz = clazz.getSuperclass();
		}

		return matchedAnnotations.toArray(new Field[matchedAnnotations.size()]);
	}

	public static Method[] getAllMethodsWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
		List<Method> matchedAnnotations = new ArrayList<>();

		while (clazz != null) {
			for (Method m : clazz.getDeclaredMethods()) {
				if (m.isAnnotationPresent(annotation)) {
					matchedAnnotations.add(m);
				}
			}
			
			clazz = clazz.getSuperclass();
		}

		return matchedAnnotations.toArray(new Method[matchedAnnotations.size()]);
	}

	public static Constructor<?> getMatchingConstructorForClass(Class<?> clazz, Class<?>... parameterTypes) {
		// Try fast matching
		try {
			return clazz.getConstructor(parameterTypes);
		}
		catch (NoSuchMethodException e) {
			// ignore
		}

		// If fast matching fails, check for assignable constructor
		findConstructor: for (Constructor<?> constructor : clazz.getConstructors()) {
			if (constructor.getParameterTypes().length == parameterTypes.length) {
				int i = 0;

				for (Class<?> t : constructor.getParameterTypes()) {
					// TODO [low] Handle type erasure (see test)
					if (!parameterTypes[i].isAssignableFrom(t)) {
						continue findConstructor;
					}

					++i;
				}

				return constructor;
			}
		}

		return null;
	}

	/**
	 * Returns the {@link Class} from a {@link Type}, or
	 * returns null if the class is not found, or the type 
	 * is not a class.
	 */
	public static Class<?> getClassFromType(Type type) {
		// Java 7 does not support getTypeName() :(
		String fullName = type.toString();

		try {
			if (fullName.startsWith("class ")) {
				return Class.forName(fullName.substring("class ".length()));
			}
		}
		catch (ClassNotFoundException e) {
			return null;
		}

		return null;
	}

	public static boolean isAnnotationPresentInHierarchy(Class<?> clazz, Class<? extends Annotation> annotation) {
		while (clazz != null) {
			if (clazz.isAnnotationPresent(annotation)) {
				return true;
			}
			
			clazz = clazz.getSuperclass();
		}

		return false;
	}

	public static <T extends Annotation> T getAnnotationInHierarchy(Class<?> clazz, Class<T> annotation) {
		while (clazz != null) {
			if (clazz.isAnnotationPresent(annotation)) {
				return clazz.getAnnotation(annotation);
			}
			
			clazz = clazz.getSuperclass();
		}

		return null;
	}

	/**
	 * Returns whether or not the value is a valid value
	 * for the enum given.
	 * 
	 * @param checkEnumValue Value to check
	 * @param enumClass Enum class to check the given value against
	 * @return True if given value is valid, false otherwise 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean isValidEnum(String checkEnumValue, Class<?> enumClass) {
		if (!enumClass.isEnum()) {
			return false;
		}

		try {
			Enum.valueOf((Class<? extends Enum>) enumClass, checkEnumValue);
			return true;
		}
		catch (IllegalArgumentException e) {
			return false;
		}
	}
}
