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
package org.syncany.cli;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import org.syncany.cli.util.CliTableUtil;
import org.syncany.operations.OperationResult;
import org.syncany.operations.daemon.messages.PluginConnectToHostExternalEvent;
import org.syncany.operations.daemon.messages.PluginInstallExternalEvent;
import org.syncany.operations.plugin.ExtendedPluginInfo;
import org.syncany.operations.plugin.PluginInfo;
import org.syncany.operations.plugin.PluginOperationAction;
import org.syncany.operations.plugin.PluginOperationOptions;
import org.syncany.operations.plugin.PluginOperationOptions.PluginListMode;
import org.syncany.operations.plugin.PluginOperationResult;
import org.syncany.operations.plugin.PluginOperationResult.PluginResultCode;
import org.syncany.util.StringUtil;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.Subscribe;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class PluginCommand extends Command {
	private boolean minimalOutput = false;

	@Override
	public CommandScope getRequiredCommandScope() {
		return CommandScope.ANY;
	}

	@Override
	public boolean canExecuteInDaemonScope() {
		return false; // TODO [low] Doesn't have an impact if command scope is ANY
	}

	@Override
	public int execute(String[] operationArgs) throws Exception {
		PluginOperationOptions operationOptions = parseOptions(operationArgs);
		PluginOperationResult operationResult = client.plugin(operationOptions);

		printResults(operationResult);

		return 0;
	}

	@Override
	public PluginOperationOptions parseOptions(String[] operationArgs) throws Exception {
		PluginOperationOptions operationOptions = new PluginOperationOptions();

		OptionParser parser = new OptionParser();
		OptionSpec<Void> optionLocal = parser.acceptsAll(asList("L", "local-only"));
		OptionSpec<Void> optionRemote = parser.acceptsAll(asList("R", "remote-only"));
		OptionSpec<Void> optionSnapshots = parser.acceptsAll(asList("s", "snapshot", "snapshots"));
		OptionSpec<Void> optionMinimalOutput = parser.acceptsAll(asList("m", "minimal-output"));
		OptionSpec<String> optionApiEndpoint = parser.acceptsAll(asList("a", "api-endpoint")).withRequiredArg();

		OptionSet options = parser.parse(operationArgs);

		// Files
		List<?> nonOptionArgs = options.nonOptionArguments();

		if (nonOptionArgs.size() == 0) {
			throw new Exception("Invalid syntax, please specify an action (list, install, remove, update).");
		}

		// <action>
		String actionStr = nonOptionArgs.get(0).toString();
		PluginOperationAction action = parsePluginAction(actionStr);

		operationOptions.setAction(action);

		// --minimal-output
		minimalOutput = options.has(optionMinimalOutput);

		// --snapshots
		operationOptions.setSnapshots(options.has(optionSnapshots));
		
		// --api-endpoint
		if (options.has(optionApiEndpoint)) {
			operationOptions.setApiEndpoint(options.valueOf(optionApiEndpoint));
		}

		// install|remove <plugin-id>
		if (action == PluginOperationAction.INSTALL || action == PluginOperationAction.REMOVE) {
			if (nonOptionArgs.size() != 2) {
				throw new Exception("Invalid syntax, please specify a plugin ID.");
			}

			// <plugin-id>
			String pluginId = nonOptionArgs.get(1).toString();
			operationOptions.setPluginId(pluginId);
		}

		// --local-only, --remote-only
		else if (action == PluginOperationAction.LIST) {
			if (options.has(optionLocal)) {
				operationOptions.setListMode(PluginListMode.LOCAL);
			}
			else if (options.has(optionRemote)) {
				operationOptions.setListMode(PluginListMode.REMOTE);
			}
			else {
				operationOptions.setListMode(PluginListMode.ALL);
			}

			// <plugin-id> (optional in 'list' or 'update')
			if (nonOptionArgs.size() == 2) {
				String pluginId = nonOptionArgs.get(1).toString();
				operationOptions.setPluginId(pluginId);
			}
		}

		else if (action == PluginOperationAction.UPDATE && nonOptionArgs.size() == 2) {
			String pluginId = nonOptionArgs.get(1).toString();
			operationOptions.setPluginId(pluginId);
		}

		return operationOptions;
	}

	private PluginOperationAction parsePluginAction(String actionStr) throws Exception {
		try {
			return PluginOperationAction.valueOf(actionStr.toUpperCase());
		}
		catch (Exception e) {
			throw new Exception("Invalid syntax, unknown action '" + actionStr + "'");
		}
	}

	@Override
	public void printResults(OperationResult operationResult) {
		PluginOperationResult concreteOperationResult = (PluginOperationResult) operationResult;

		switch (concreteOperationResult.getAction()) {
			case LIST:
				printResultList(concreteOperationResult);
				return;

			case INSTALL:
				printResultInstall(concreteOperationResult);
				return;

			case REMOVE:
				printResultRemove(concreteOperationResult);
				return;

			case UPDATE:
				printResultUpdate(concreteOperationResult);
				return;

			default:
				out.println("Unknown action: " + concreteOperationResult.getAction());
		}
	}

	private void printResultList(PluginOperationResult operationResult) {
		if (operationResult.getResultCode() == PluginResultCode.OK) {
			List<String[]> tableValues = new ArrayList<String[]>();
			
			tableValues.add(new String[]{"Id", "Name", "Local Version", "Type", "Remote Version", "Updatable", "Provided By"});
			
			int outdatedCount = 0;
			int updatableCount = 0;
			int thirdPartyCount = 0;

			for (ExtendedPluginInfo extPluginInfo : operationResult.getPluginList()) {
				PluginInfo pluginInfo = (extPluginInfo.isInstalled()) ? extPluginInfo.getLocalPluginInfo() : extPluginInfo.getRemotePluginInfo();

				String localVersionStr = (extPluginInfo.isInstalled()) ? extPluginInfo.getLocalPluginInfo().getPluginVersion() : "";
				String installedStr = extPluginInfo.isInstalled() ? (extPluginInfo.canUninstall() ? "User" : "Global") : "";
				String remoteVersionStr = (extPluginInfo.isRemoteAvailable()) ? extPluginInfo.getRemotePluginInfo().getPluginVersion() : "";
				String thirdPartyStr = (pluginInfo.isPluginThirdParty()) ? "Third Party" : "Syncany Team"; 
				String updatableStr = "";

				if (extPluginInfo.isInstalled() && extPluginInfo.isOutdated()) {
					if (extPluginInfo.canUninstall()) {
						updatableStr = "Auto";
						updatableCount++;
					}
					else {
						updatableStr = "Manual";
					}

					outdatedCount++;
				}

				if (pluginInfo.isPluginThirdParty()) {
					thirdPartyCount++;
				}
				
				tableValues.add(new String[]{pluginInfo.getPluginId(), pluginInfo.getPluginName(), localVersionStr, installedStr, remoteVersionStr, updatableStr, thirdPartyStr});
			}

			CliTableUtil.printTable(out, tableValues, "No plugins found.");

			if (outdatedCount > 0) {
				String isAre = (outdatedCount == 1) ? "is" : "are";
				String pluginPlugins = (outdatedCount == 1) ? "plugin" : "plugins";
				
				out.printf("\nUpdates:\nThere %s %d outdated %s, %d of them %s automatically updatable.\n", isAre, outdatedCount, pluginPlugins, updatableCount, isAre);
			}
			
			if (thirdPartyCount > 0) {
				String pluginPlugins = (thirdPartyCount == 1) ? "plugin" : "plugins";				
				out.printf("\nThird party plugins:\nPlease note that the Syncany Team does not take review or maintain the third-party %s\nlisted above. Please report issues to the corresponding plugin site.\n", pluginPlugins);
			}
		}
		else {
			out.printf("Listing plugins failed. No connection? Try -d to get more details.\n");
			out.println();
		}
	}

	private void printResultInstall(PluginOperationResult operationResult) {
		// Print minimal result
		if (minimalOutput) {
			if (operationResult.getResultCode() == PluginResultCode.OK) {
				out.println("OK");
			}
			else {
				out.println("NOK");
			}
		}
		// Print regular result
		else {
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
	}

	private void printResultUpdate(final PluginOperationResult operationResult) {
		// Print regular result
		if (operationResult.getResultCode() == PluginResultCode.OK) {
			if (operationResult.getUpdatedPluginIds().size() == 0) {
				out.println("All plugins are up to date.");
			}
			else {
				Iterables.removeAll(operationResult.getUpdatedPluginIds(), operationResult.getErroneousPluginIds());
				Iterables.removeAll(operationResult.getUpdatedPluginIds(), operationResult.getDelayedPluginIds());

				if (operationResult.getDelayedPluginIds().size() > 0) {
					out.printf("Plugins to be updated: %s\n", StringUtil.join(operationResult.getDelayedPluginIds(), ", "));
				}

				if (operationResult.getUpdatedPluginIds().size() > 0) {
					out.printf("Plugins successfully updated: %s\n", StringUtil.join(operationResult.getUpdatedPluginIds(), ", "));
				}

				if (operationResult.getErroneousPluginIds().size() > 0) {
					out.printf("Failed to update %s. Try -d to get more details\n", StringUtil.join(operationResult.getErroneousPluginIds(), ", "));
				}

				out.println();
			}
		}
		else {
			out.println("Plugin update failed. Try -d to get more details.");
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
		// Print minimal result
		if (minimalOutput) {
			if (operationResult.getResultCode() == PluginResultCode.OK) {
				out.println("OK");
			}
			else {
				out.println("NOK");
			}
		}
		// Print regular result
		else {
			if (operationResult.getResultCode() == PluginResultCode.OK) {
				out.printf("Plugin successfully removed.\n");
				out.printf("Original local was %s\n", operationResult.getSourcePluginPath());
				out.println();
			}
			else {
				out.println("Plugin removal failed.");
				out.println();

				out.println("Note: Plugins shipped with the application or additional packages");
				out.println("      cannot be removed. These plugin are marked 'Global' in the list.");
				out.println();
			}
		}
	}

	private void printPluginDetails(PluginInfo pluginInfo) {
		out.println("Plugin details:");
		out.println("- ID: " + pluginInfo.getPluginId());
		out.println("- Name: " + pluginInfo.getPluginName());
		out.println("- Version: " + pluginInfo.getPluginVersion());
		out.println();
	}

	@Subscribe
	public void onPluginConnectToHostEventReceived(PluginConnectToHostExternalEvent event) {
		if (!minimalOutput) {
			out.printr("Connecting to " + event.getHost() + " ...");
		}
	}

	@Subscribe
	public void onPluginInstallEventReceived(PluginInstallExternalEvent event) {
		if (!minimalOutput) {
			out.printr("Installing plugin from " + event.getSource() + " ...");
		}
	}
}
