/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com>
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

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.Client;
import org.syncany.operations.OperationResult;
import org.syncany.operations.daemon.messages.ConnectToHostExternalEvent;
import org.syncany.operations.update.AppInfo;
import org.syncany.operations.update.UpdateOperation;
import org.syncany.operations.update.UpdateOperationAction;
import org.syncany.operations.update.UpdateOperationOptions;
import org.syncany.operations.update.UpdateOperationResult;
import org.syncany.operations.update.UpdateOperationResult.UpdateResultCode;

import com.google.common.eventbus.Subscribe;

public class UpdateCommand extends Command {
	@Override
	public CommandScope getRequiredCommandScope() {
		return CommandScope.ANY;
	}

	@Override
	public boolean canExecuteInDaemonScope() {
		return false;
	}

	@Override
	public int execute(String[] operationArgs) throws Exception {
		UpdateOperationOptions operationOptions = parseOptions(operationArgs);
		UpdateOperationResult operationResult = new UpdateOperation(config, operationOptions).execute();

		printResults(operationResult);

		return 0;
	}

	@Override
	public UpdateOperationOptions parseOptions(String[] operationArgs) throws Exception {
		UpdateOperationOptions operationOptions = new UpdateOperationOptions();

		OptionParser parser = new OptionParser();
		OptionSpec<Void> optionSnapshots = parser.acceptsAll(asList("s", "snapshot", "snapshots"));
		OptionSpec<String> optionApiEndpoint = parser.acceptsAll(asList("a", "api-endpoint")).withRequiredArg();

		OptionSet options = parser.parse(operationArgs);

		// Action
		List<?> nonOptionArgs = options.nonOptionArguments();

		if (nonOptionArgs.size() == 0) {
			throw new Exception("Invalid syntax, please specify an action (check).");
		}

		// <action>
		String actionStr = nonOptionArgs.get(0).toString();
		UpdateOperationAction action = parseAction(actionStr);

		operationOptions.setAction(action);

		// --snapshots
		operationOptions.setSnapshots(options.has(optionSnapshots));
		
		// --api-endpoint
		if (options.has(optionApiEndpoint)) {
			operationOptions.setApiEndpoint(options.valueOf(optionApiEndpoint));
		}

		return operationOptions;
	}

	private UpdateOperationAction parseAction(String actionStr) throws Exception {
		try {
			return UpdateOperationAction.valueOf(actionStr.toUpperCase());
		}
		catch (Exception e) {
			throw new Exception("Invalid syntax, unknown action '" + actionStr + "'");
		}
	}

	@Override
	public void printResults(OperationResult operationResult) {
		UpdateOperationResult concreteOperationResult = (UpdateOperationResult) operationResult;

		switch (concreteOperationResult.getAction()) {
			case CHECK:
				printResultCheck(concreteOperationResult);
				return;

			default:
				out.println("Unknown action: " + concreteOperationResult.getAction());
		}
	}

	private void printResultCheck(UpdateOperationResult operationResult) {
		if (operationResult.getResultCode() == UpdateResultCode.OK) {
			AppInfo remoteAppInfo = operationResult.getAppInfo();

			String localAppVersion = Client.getApplicationVersionFull();
			String remoteAppVersion = remoteAppInfo.getAppVersion();
			
			if (operationResult.isNewVersionAvailable()) {
				out.println("A new version is available. Local version is " + localAppVersion + ", remote version is " + remoteAppVersion);
				out.println("Download at " + remoteAppInfo.getDownloadUrl());
			}
			else {
				out.println("Up to date, at version " + localAppVersion);	
			}			
		}
		else {
			out.println("Checking for updates failed. No connection? Try -d to get more details.");
			out.println();
		}
	}

	@Subscribe
	public void onConnectToHostEventReceived(ConnectToHostExternalEvent event) {
		out.printr("Connecting to " + event.getHost() + " ...");
	}
}
