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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The instantiation utility class provides static instantiation helper methods.
 * 
 * @author Gregor Trefs <Gregor.Trefs@gmail.com>
 */
public class InstantiationUtil {
	
	private static final Logger logger = Logger.getLogger(InstantiationUtil.class.getSimpleName());
	
	/**
	 * Instantiates sub-type of given class by its type/name using the default constructor. After creating
	 * a new instance, it must be initialized using the {@link #init(Map) init()} method.  
	 * 
	 * <p>The given type attribute is mapped to fully qualified class name (FQCN) of the form
	 * <tt>org.syncany.chunk.XClass</tt>, where <tt>X</tt> is the camel-cased type
	 * attribute.  
	 * @param <T>
	 * 
	 * @param type Type/name of the sub-type (corresponds to its camel case class name)
	 * @return Returns a new instance
	 * @throws Exception If the FQCN cannot be found or the class cannot be instantiated
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getInstance(Class<T> clazz, String type) {
		final String thisPackage = clazz.getPackage().getName();
		final String camelCaseName = StringUtil.toCamelCase(type);
		final String fqClassName = thisPackage+"."+camelCaseName+clazz.getSimpleName();
		
		// Try to load!
		try {
			return (T) Class.forName(fqClassName).newInstance();
		} 
		catch (Exception ex) {
			logger.log(Level.INFO, String.format("Could not find subtype %s of class %s with FQCN %s. ", type, clazz.getSimpleName(), fqClassName), ex);
			return null;
		}		
	}

}
