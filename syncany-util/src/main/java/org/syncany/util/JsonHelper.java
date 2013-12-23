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
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

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

	/**
	 * Returns a JSon String corresponding the the Map<String, ?> 
	 * parameter
	 * @param params the java object to be serialised to JSon String
	 */
	public static String fromMapToString(Map<String, ?> params){
		String text = gson.toJson(params).toString();
		return text;
	}

	/**
	 *  Returns a valid Map<String, ?> from JSon String
	 */
	public static Map<String, Object> fromStringToMap(String s){
		Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
		Map<String, Object> parameters = gson.fromJson(s, mapType);
		return parameters;
	}
}
