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
package org.syncany.connection.plugins;

import java.util.LinkedHashMap;
import java.util.Map;

import org.syncany.connection.plugins.PluginOptionSpec.OptionValidationResult;

/**
 * @author pheckel
 *
 */
public class PluginOptionSpecs extends LinkedHashMap<String, PluginOptionSpec> {
	private static final long serialVersionUID = 969747673844618006L;
	
	public PluginOptionSpecs(PluginOptionSpec... optionSpecs) {
		for (PluginOptionSpec optionSpec : optionSpecs) {
			add(optionSpec);
		}
	}	
	
	public void add(PluginOptionSpec optionSpec) {
		put(optionSpec.getId(), optionSpec);
	}

	public void validate(Map<String, String> optionValues) throws StorageException {
    	for (PluginOptionSpec optionSpec : values()) {  
    		boolean optionExists = optionValues.containsKey(optionSpec.getId());
    		String optionValue = optionValues.get(optionSpec.getId());
    		
    		if (optionExists) {
    			if (optionSpec.validateInput(optionValue) != OptionValidationResult.VALID) {
    				throw new StorageException("Invalid value for option '" + optionSpec.getId() + "'.");
    			}
			}
    		else {
    			if (optionSpec.isMandatory()) {
    				throw new StorageException("Mandatory setting " + optionSpec.getId() + " is not set.");		
    			}
    		}
    	}
    }
}
