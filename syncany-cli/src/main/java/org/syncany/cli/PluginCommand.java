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
package org.syncany.cli;

import java.util.List;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.syncany.operations.LogOperation;
import org.syncany.operations.PluginOperation.PluginAction;
import org.syncany.operations.PluginOperation.PluginOperationOptions;
import org.syncany.operations.PluginOperation.PluginOperationResult;

public class PluginCommand extends Command {
	private static final Logger logger = Logger.getLogger(LogOperation.class.getSimpleName());

	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.ANY;
	}

	@Override
	public int execute(String[] operationArgs) throws Exception {
		PluginOperationOptions operationOptions = parseOptions(operationArgs);
		PluginOperationResult operationResult = client.plugin(operationOptions);

		printResults(operationResult);

		return 0;
	}	

	private void printResults(PluginOperationResult operationResult) throws Exception {
		throw new Exception("Not yet implemented.");
	}

	private PluginOperationOptions parseOptions(String[] operationArgs) throws Exception {
		PluginOperationOptions operationOptions = new PluginOperationOptions();

		OptionParser parser = new OptionParser();
		OptionSet options = parser.parse(operationArgs);

		// Files
		List<?> nonOptionArgs = options.nonOptionArguments();
		
		if (nonOptionArgs.size() == 0) {
			throw new Exception("Invalid syntax, please specify an action (list, rlist, get, activate, deactivate).");
		}
		
		// <action>
		String actionStr = nonOptionArgs.get(0).toString();
		PluginAction action = parsePluginAction(actionStr);
		
		operationOptions.setAction(action);
		
		// Additional options per-action
		if (action == PluginAction.GET || action == PluginAction.ACTIVATE || action == PluginAction.DEACTIVATE) {
			if (nonOptionArgs.size() != 2) {
				throw new Exception("Invalid syntax, please specify a plugin ID.");
			}
			
			String pluginId = nonOptionArgs.get(1).toString();
			operationOptions.setPluginId(pluginId);
		}
		else {
			if (nonOptionArgs.size() != 1) {
				throw new Exception("Invalid syntax, no other options expected.");
			}			
		}

		return operationOptions;
	}

	private PluginAction parsePluginAction(String actionStr) throws Exception {
		try {
			return PluginAction.valueOf(actionStr);
		}
		catch (Exception e) {
			throw new Exception("Invalid syntax, unknown action '" + actionStr + "'");
		}
	}
}
