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

import java.util.ArrayList;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.operations.plugin.ExtendedPluginInfo;
import org.syncany.operations.plugin.PluginInfo;
import org.syncany.operations.plugin.PluginOperationOptions;
import org.syncany.operations.plugin.PluginOperationOptions.PluginAction;
import org.syncany.operations.plugin.PluginOperationOptions.PluginListMode;
import org.syncany.operations.plugin.PluginOperationResult;
import org.syncany.operations.plugin.PluginOperationResult.PluginResultCode;
import org.syncany.util.StringUtil;

public class PluginCommand extends Command {
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

	private PluginOperationOptions parseOptions(String[] operationArgs) throws Exception {
		PluginOperationOptions operationOptions = new PluginOperationOptions();

		OptionParser parser = new OptionParser();
		OptionSpec<Void> optionLocal = parser.acceptsAll(asList("L", "local-only"));
		OptionSpec<Void> optionRemote = parser.acceptsAll(asList("R", "remote-only"));
		OptionSpec<Void> optionSnapshots = parser.acceptsAll(asList("s", "snapshot", "snapshots"));

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

		// --snapshots
		operationOptions.setSnapshots(options.has(optionSnapshots));

		// install|remove <plugin-id>
		if (action == PluginAction.INSTALL || action == PluginAction.REMOVE) {
			if (nonOptionArgs.size() != 2) {
				throw new Exception("Invalid syntax, please specify a plugin ID.");
			}

			// <plugin-id>
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

			// <plugin-id> (optional in 'list')
			if (nonOptionArgs.size() == 2) {
				String pluginId = nonOptionArgs.get(1).toString();
				operationOptions.setPluginId(pluginId);
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
			List<String[]> tableValues = new ArrayList<String[]>();
			tableValues.add(new String[] { "Id", "Name", "Local Version", "Remote Version", "Inst.", "Upgr." });

			for (ExtendedPluginInfo extPluginInfo : operationResult.getPluginList()) {
				PluginInfo pluginInfo = (extPluginInfo.isInstalled()) ? extPluginInfo.getLocalPluginInfo() : extPluginInfo.getRemotePluginInfo();

				String localVersionStr = (extPluginInfo.isInstalled()) ? extPluginInfo.getLocalPluginInfo().getPluginVersion() : "";
				String remoteVersionStr = (extPluginInfo.isRemoteAvailable()) ? extPluginInfo.getRemotePluginInfo().getPluginVersion() : "";
				String installedStr = extPluginInfo.isInstalled() ? "yes" : "";
				String upgradeAvailableStr = extPluginInfo.isUpgradeAvailable() ? "yes" : "";

				tableValues.add(new String[] { pluginInfo.getPluginId(), pluginInfo.getPluginName(), localVersionStr, remoteVersionStr, installedStr,
						upgradeAvailableStr });
			}

			printTable(tableValues, "No plugins found.");
		}
		else {
			out.printf("Listing plugins failed. No connection? Try -d to get more details.\n");
			out.println();
		}
	}

	private void printResultInstall(PluginOperationResult operationResult) {
		// Print regular result
		if (operationResult.getResultCode() == PluginResultCode.OK) {
			out.printf("Plugin successfully installed from %s\n", operationResult.getSourcePluginPath());
			out.printf("Install location: %s\n", operationResult.getTargetPluginPath());
			out.println();

			printPluginDetails(operationResult.getAffectedPluginInfo());
			printPluginConflictWarning(operationResult);
		}
		else {
			out.println("Plugin installation failed. Try -d to get more details.");
			out.println();
		}
	}

	private void printPluginConflictWarning(PluginOperationResult operationResult) {
		List<String> conflictingPluginIds = operationResult.getConflictingPluginIds();
		
		if (conflictingPluginIds != null && conflictingPluginIds.size() > 0) {
			out.println("---------------------------------------------------------------------------");
			out.printf(" WARNING: The installed plugin '%s' conflicts with other installed:\n", operationResult.getAffectedPluginInfo().getPluginId());
			out.printf("          plugin(s): %s\n", StringUtil.join(conflictingPluginIds, ", "));
			out.println();
			out.println(" If you'd like to use these plugins in the daemon, it is VERY likely");
			out.println(" that parts of the application WILL CRASH. Data corruption might occur!");
			out.println();
			out.println(" Using the plugins outside of the daemon (sy <command> ...) might also");
			out.println(" be an issue. Details about this in issue #154.");
			out.println("---------------------------------------------------------------------------");
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
		out.println("- ID: " + pluginInfo.getPluginId());
		out.println("- Name: " + pluginInfo.getPluginName());
		out.println("- Version: " + pluginInfo.getPluginVersion());
		out.println();
	}

	private void printTable(List<String[]> tableValues, String noRowsMessage) {
		if (tableValues.size() > 0) {
			Integer[] tableColumnWidths = calculateColumnWidths(tableValues);
			String tableRowFormat = "%-" + StringUtil.join(tableColumnWidths, "s | %-") + "s\n";

			printTableHeader(tableValues.get(0), tableRowFormat, tableColumnWidths);

			if (tableValues.size() > 1) {
				printTableBody(tableValues, tableRowFormat, tableColumnWidths);
			}
			else {
				out.println(noRowsMessage);
			}
		}
	}

	private void printTableBody(List<String[]> tableValues, String tableRowFormat, Integer[] tableColumnWidths) {
		for (int i = 1; i < tableValues.size(); i++) {
			out.printf(tableRowFormat, (Object[]) tableValues.get(i));
		}
	}

	private void printTableHeader(String[] tableHeader, String tableRowFormat, Integer[] tableColumnWidths) {
		out.printf(tableRowFormat, (Object[]) tableHeader);

		for (int i = 0; i < tableColumnWidths.length; i++) {
			if (i > 0) {
				out.print("-");
			}

			for (int j = 0; j < tableColumnWidths[i]; j++) {
				out.print("-");
			}

			if (i < tableColumnWidths.length - 1) {
				out.print("-");
				out.print("+");
			}
		}

		out.println();
	}

	private Integer[] calculateColumnWidths(List<String[]> tableValues) {
		Integer[] tableColumnWidths = new Integer[tableValues.get(0).length];

		for (String[] tableRow : tableValues) {
			for (int i = 0; i < tableRow.length; i++) {
				if (tableColumnWidths[i] == null || (tableRow[i] != null && tableColumnWidths[i] < tableRow[i].length())) {
					tableColumnWidths[i] = tableRow[i].length();
				}
			}
		}

		return tableColumnWidths;
	}
}
