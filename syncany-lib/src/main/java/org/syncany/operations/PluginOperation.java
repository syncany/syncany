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
package org.syncany.operations;

import java.util.List;

import org.syncany.config.Config;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;

public class PluginOperation extends Operation {
	private PluginOperationOptions options;
	private PluginOperationResult result;
	
	public PluginOperation(Config config, PluginOperationOptions options) {
		super(config);			
		
		this.options = options;
		this.result = new PluginOperationResult();		
	}

	@Override
	public PluginOperationResult execute() throws Exception {
		switch (options.getAction()) {
		case LIST:
			return executeList();
		
		case RLIST:
		case GET:
		case ACTIVATE:
		case DEACTIVATE:
			throw new Exception("Action not yet implemented: " + options.getAction());

		default:
			throw new Exception("Unknown action: " + options.getAction());
		}
	}
	
	private PluginOperationResult executeList() {
		result.setPluginList(Plugins.list()); // TODO This should include plugins in .syncany/plugins
		return result;
	}

	public enum PluginAction {
		LIST, RLIST, GET, ACTIVATE, DEACTIVATE
	}
	
	public static class PluginOperationOptions implements OperationOptions {
		private PluginAction action;
		private String pluginId;

		public PluginAction getAction() {
			return action;
		}

		public void setAction(PluginAction action) {
			this.action = action;
		}

		public String getPluginId() {
			return pluginId;
		}

		public void setPluginId(String pluginId) {
			this.pluginId = pluginId;
		}	
	}

	public class PluginOperationResult implements OperationResult {
		private List<Plugin> pluginList;

		public List<Plugin> getPluginList() {
			return pluginList;
		}

		public void setPluginList(List<Plugin> pluginList) {
			this.pluginList = pluginList;
		}				
	}
}
