/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 * Class handling conversion between Json String and Map<String, Object>
 * POJO
 */
public class JsonHelper {
	private static Gson gson = null;

	static {
		gson = new GsonBuilder().create();
	}

	public static String fromObjectToString(Object object){
		return gson.toJson(object).toString();
	}
	
	public static <T> T fromStringToObject(String string, Class<T> clazz) {
		return gson.fromJson(string,  clazz);
	}
	
	public static <T> T fromStringToObject(String string, Type type) {
		return gson.fromJson(string,  type);
	}
}
