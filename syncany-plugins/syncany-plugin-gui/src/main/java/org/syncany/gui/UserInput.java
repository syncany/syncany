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
package org.syncany.gui;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class UserInput {
	private Map<CommonParameters, String> commonParameters = new HashMap<>();
	private Map<String, String> pluginParameters = new HashMap<>();
	
	public Map<CommonParameters, String> getCommonParameters() {
		return commonParameters;
	}
	
	public Map<String, String> getPluginParameters() {
		return pluginParameters;
	}
	
	public void putCommonParameter(CommonParameters key, String value) {
		if (!key.containsValue(value)) {
			throw new RuntimeException(String.format("Value [%s] not compatible with key [%s]", value, key.toString()));
		}

		commonParameters.put(key, value);
	}
	
	public boolean getCommonParameterAsBoolean(CommonParameters key){
		String parameter = getCommonParameter(key);
		
		switch (parameter.toLowerCase()){
			case "yes":
			case "true":
				return true;
			default:
				return false;
		}
	}

	public String getCommonParameter(CommonParameters key) {
		if (!(key instanceof CommonParameters)) {
			throw new RuntimeException(String.format("Key should be of type SyncanyParameters"));
		}
		
		return commonParameters.get(key);
	}
	
	public void putPluginParameter(String key, String value){
		pluginParameters.put(key, value);
	}
	
//	public String getPluginParameter(String key){
//		return pluginParameters.get(key);
//	}

	public void merge(UserInput userSelection) {
		getCommonParameters().putAll(userSelection.getCommonParameters());
		getPluginParameters().putAll(userSelection.getPluginParameters());
	}
}
