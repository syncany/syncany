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



/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public enum CommonParameters {
	COMMAND_ACTION("action", true, "create", "connect", "watch"), 
	ENCRYPTION_ENABLED("encryption", true, "yes", "no"), 
	AVAILABLE_URL("connect_url", false, "yes", "no"),
	URL("url", false),
	ENCRYPTION_ALGORITHM("algorithm", false, "AES", "TwoFish"), 
	ENCRYPTION_KEYLENGTH("keylength", false, "128", "256"), 
	ENCRYPTION_PASSWORD("password", true),
	CHUNCK_SIZE("chunck_size", false),
	LOCAL_FOLDER("localFolder", true),
	PLUGIN_ID("pluginId", true), 
	COMMAND_ID("command_id", false);
	
	private String[] values;
	private String value;
	private boolean mandatory;
	
	CommonParameters(String value, boolean mandatory, String...possibleValues){
		this.values = possibleValues;
		this.value = value;
		this.mandatory = mandatory;
	}

	public boolean containsValue(String value) {
		if (values == null || values.length == 0) return true;
		
		for (String s : values){
			if (value.toUpperCase().equals(s.toUpperCase()))
				return true;
		}
		return false;
	}

	public String value() {
		return value;
	}

	public boolean isParameterMandatory() {
		return mandatory;
	}
}
