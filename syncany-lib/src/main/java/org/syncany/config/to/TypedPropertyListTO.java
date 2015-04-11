/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.config.to;

import java.util.Map;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementMap;

/**
 * The typed property list is a helper data structure that allows storing an
 * object of a certain type with its properties . 
 * 
 * <p>It is used in the {@link RepoTO} for chunker, multichunker and transformer,
 * and in the {@link ConfigTO} for the connection settings.
 * 
 * <p>It uses the Simple framework for XML serialization, and its corresponding
 * annotation-based configuration.  
 *  
 * @see <a href="http://simple.sourceforge.net/">Simple framework</a>
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class TypedPropertyListTO {
	@Attribute(required=true)
	private String type;

	@ElementMap(entry="property", key="name", required=false, attribute=true, inline=true)
	protected Map<String, String> settings;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public Map<String, String> getSettings() {
		return settings;
	}

	public void setSettings(Map<String, String> settings) {
		this.settings = settings;
	}				
}
