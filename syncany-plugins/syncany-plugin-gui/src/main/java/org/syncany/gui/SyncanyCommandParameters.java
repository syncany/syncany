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
 * @author vincent
 *
 */
public enum SyncanyCommandParameters implements SyncanyParameters {
	COMMAND_ACTION("create", "connect"), 
	ENCRYPTION_ENABLED("yes", "no"), 
	ENCRYPTION_ALGORITHM("AES", "TwoFish"), 
	ENCRYPTION_KEYLENGTH("128", "256"), 
	ENCRYPTION_PASSWORD, 
	LOCAL_FOLDER,
	PLUGIN_ID;
	
	private String[] values;
	
	SyncanyCommandParameters(String...values){
		this.values = values;
	}

	public boolean containsValue(String value) {
		if (values == null || values.length == 0) return true;
		
		for (String s : values){
			if (value.toUpperCase().equals(s.toUpperCase()))
				return true;
		}
		return false;
	}
}
