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
import org.syncany.operations.plugin.ExtendedPluginInfo;
import org.syncany.operations.plugin.PluginInfo;
import org.syncany.operations.plugin.PluginOperationOptions;
import org.syncany.operations.plugin.PluginOperationOptions.PluginAction;
import org.syncany.operations.plugin.PluginOperationOptions.PluginListMode;
import org.syncany.operations.plugin.PluginOperationResult;
import org.syncany.operations.plugin.PluginOperationResult.PluginResultCode;

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

		printResults(operationOptions, operationResult);

		return 0;
	}	

	private String shortenStr(int len, String s) {
		if (s.length() > len) {
			return s.substring(0, len-2) + "..";
		}
		else {
			return s;
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
			throw new Exception("Invalid syntax, please specify an action (list, install, remove).");
		}
		
		// <action>
		String actionStr = nonOptionArgs.get(0).toString();
		PluginAction action = parsePluginAction(actionStr);
		
		operationOptions.setAction(action);
		
		// Additional options per-action
		if (action == PluginAction.INSTALL || action == PluginAction.REMOVE) {
			if (nonOptionArgs.size() != 2) {
				throw new Exception("Invalid syntax, please specify a plugin ID.");
			}
			
			String pluginId = nonOptionArgs.get(1).toString();
			operationOptions.setPluginId(pluginId);
		}		
		
		// --local-only, --remote-only
		else if (action == PluginAction.LIST) {
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
	

	private void printResults(PluginOperationOptions operationOptions, PluginOperationResult operationResult) throws Exception {
		switch (operationOptions.getAction()) {
		case LIST:
			printResultList(operationResult);
			return;

		case INSTALL:
			printResultInstall(operationResult);
			return;
			
		case REMOVE:
			printResultRemove(operationResult);
			return;

		default:
			throw new Exception("Unknown action: " + operationOptions.getAction());
		}
	}

	private void printResultList(PluginOperationResult operationResult) {
		if (operationResult.getResultCode() == PluginResultCode.OK) {
			out.println("Id       Name            Local Version  Remote Version  Inst.  Upgr.");
			out.println("---------------------------------------------------------------------------------");
			
			for (ExtendedPluginInfo extPluginInfo : operationResult.getPluginList()) {
				PluginInfo pluginInfo = (extPluginInfo.isInstalled()) ? extPluginInfo.getLocalPluginInfo() : extPluginInfo.getRemotePluginInfo();
	
				String localVersionStr = shortenStr(14, (extPluginInfo.isInstalled()) ? extPluginInfo.getLocalPluginInfo().getPluginVersion() : "");
				String remoteVersionStr = shortenStr(14, (extPluginInfo.isRemoteAvailable()) ? extPluginInfo.getRemotePluginInfo().getPluginVersion() : "");
				String installedStr = extPluginInfo.isInstalled() ? "yes" : "";
				String upgradeAvailableStr = extPluginInfo.isUpgradeAvailable() ? "yes" : "";			
				
				out.printf("%-7s  %-15s %-14s %-14s  %-5s  %-5s\n", pluginInfo.getPluginId(), pluginInfo.getPluginName(), localVersionStr, remoteVersionStr, installedStr, upgradeAvailableStr);
			}
		}
		else {
			out.printf("Listing plugins failed. No connection? Try -d to get more details.\n");
			out.println();
		}
	}
	
	private void printResultInstall(PluginOperationResult operationResult) {
		if (operationResult.getResultCode() == PluginResultCode.OK) {
			out.printf("Plugin successfully installed from %s\n", operationResult.getSourcePluginPath());
			out.printf("Install location: %s\n", operationResult.getTargetPluginPath());
			out.println();
				
			printPluginDetails(operationResult.getAffectedPluginInfo());
		}
		else {
			out.println("Plugin installation failed. Try -d to get more details.");
			out.println();
		}
	}

	private void printResultRemove(PluginOperationResult operationResult) {
		if (operationResult.getResultCode() == PluginResultCode.OK) {
			out.printf("Plugin successfully removed.\n");
			out.printf("Original local was %s\n", operationResult.getSourcePluginPath());
			out.println();
		}
		else {
			out.println("Plugin removal failed.");
			out.println();
			
			out.println("Note: Plugins shipped with the application");
			out.println("      cannot be removed.");
			out.println();
		}
	}
	
	private void printPluginDetails(PluginInfo pluginInfo) {
		out.println("Plugin details:");
		out.println("- ID: "+pluginInfo.getPluginId());
		out.println("- Name: "+pluginInfo.getPluginName());
		out.println("- Version: "+pluginInfo.getPluginVersion());
		out.println();	
	}
}
