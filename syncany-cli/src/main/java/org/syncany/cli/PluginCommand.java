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

import static java.util.Arrays.asList;

import java.util.List;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.operations.LogOperation;
import org.syncany.operations.plugin.PluginInfo;
import org.syncany.operations.plugin.PluginOperationOptions;
import org.syncany.operations.plugin.PluginOperationOptions.PluginAction;
import org.syncany.operations.plugin.PluginOperationOptions.PluginListMode;
import org.syncany.operations.plugin.PluginOperationResult;

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
		out.println("Id       Name            Version        Status");
		out.println("----------------------------------------------------");
		
		for (PluginInfo pluginInfo : operationResult.getPluginList()) {
			String statusStr = "inst/.."; // TODO [medium] fill this
			out.printf("%-7s  %-15s %-14s %s\n", pluginInfo.getPluginId(), pluginInfo.getPluginName(), pluginInfo.getPluginVersion(), statusStr);
		}
	}

	private PluginOperationOptions parseOptions(String[] operationArgs) throws Exception {
		PluginOperationOptions operationOptions = new PluginOperationOptions();

		OptionParser parser = new OptionParser();	
		OptionSpec<Void> optionLocal = parser.acceptsAll(asList("L", "local-only"));
		OptionSpec<Void> optionRemote = parser.acceptsAll(asList("R", "remote-only"));
		
		OptionSet options = parser.parse(operationArgs);

		// Files
		List<?> nonOptionArgs = options.nonOptionArguments();
		
		if (nonOptionArgs.size() == 0) {
			throw new Exception("Invalid syntax, please specify an action (list, install, activate, deactivate).");
		}
		
		// <action>
		String actionStr = nonOptionArgs.get(0).toString();
		PluginAction action = parsePluginAction(actionStr);
		
		operationOptions.setAction(action);
		
		// Additional options per-action
		if (action == PluginAction.INSTALL || action == PluginAction.UNINSTALL) {
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
		
		// --local-only, --remote-only
		if (action == PluginAction.LIST) {
			if (options.has(optionLocal)) {
				operationOptions.setListMode(PluginListMode.LOCAL);	
			}
			else if (options.has(optionRemote)) {
				operationOptions.setListMode(PluginListMode.REMOTE);
			}
			else {
				operationOptions.setListMode(PluginListMode.ALL);
			}			
		}

		return operationOptions;
	}

	private PluginAction parsePluginAction(String actionStr) throws Exception {
		try {
			return PluginAction.valueOf(actionStr.toUpperCase());
		}
		catch (Exception e) {
			throw new Exception("Invalid syntax, unknown action '" + actionStr + "'");
		}
	}
}
