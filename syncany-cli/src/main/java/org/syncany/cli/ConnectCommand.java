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

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.syncany.config.to.ConfigTO;
import org.syncany.operations.OperationResult;
import org.syncany.operations.init.ConnectOperationOptions;
import org.syncany.operations.init.ConnectOperationOptions.ConnectOptionsStrategy;
import org.syncany.operations.init.ConnectOperationResult;
import org.syncany.operations.init.ConnectOperationResult.ConnectResultCode;
import org.syncany.plugins.transfer.StorageTestResult;
import org.syncany.plugins.transfer.TransferSettings;

import static java.util.Arrays.asList;

public class ConnectCommand extends AbstractInitCommand {
	public ConnectCommand() {
		super();
	}

	@Override
	public CommandScope getRequiredCommandScope() {
		return CommandScope.UNINITIALIZED_LOCALDIR;
	}

	@Override
	public boolean canExecuteInDaemonScope() {
		return false;
	}

	@Override
	public int execute(String[] operationArgs) throws Exception {
		boolean retryNeeded = true;
		boolean performOperation = true;

		ConnectOperationOptions operationOptions = parseOptions(operationArgs);

		while (retryNeeded && performOperation) {
			ConnectOperationResult operationResult = client.connect(operationOptions, this);
			printResults(operationResult);

			retryNeeded = operationResult.getResultCode() != ConnectResultCode.OK
					&& operationResult.getResultCode() != ConnectResultCode.NOK_DECRYPT_ERROR;

			if (retryNeeded) {
				performOperation = isInteractive && askRetryConnection();

				if (performOperation) {
					updateTransferSettings(operationOptions.getConfigTO().getTransferSettings());
				}
			}
		}

		return 0;
	}

	@Override
	public ConnectOperationOptions parseOptions(String[] operationArgs) throws Exception {
		ConnectOperationOptions operationOptions = new ConnectOperationOptions();

		OptionParser parser = new OptionParser();
		OptionSpec<String> optionPlugin = parser.acceptsAll(asList("P", "plugin")).withRequiredArg();
		OptionSpec<String> optionPluginOpts = parser.acceptsAll(asList("o", "plugin-option")).withRequiredArg();
		OptionSpec<Void> optionAddDaemon = parser.acceptsAll(asList("n", "add-daemon"));

		OptionSet options = parser.parse(operationArgs);
		List<?> nonOptionArgs = options.nonOptionArguments();

		// Plugin
		TransferSettings transferSettings = null;

		if (nonOptionArgs.size() == 1) {
			String connectLink = (String) nonOptionArgs.get(0);

			operationOptions.setStrategy(ConnectOptionsStrategy.CONNECTION_LINK);
			operationOptions.setConnectLink(connectLink);

			transferSettings = null;
		}
		else if (nonOptionArgs.size() == 0) {
			operationOptions.setStrategy(ConnectOptionsStrategy.CONNECTION_TO);
			operationOptions.setConnectLink(null);

			transferSettings = createTransferSettingsFromOptions(options, optionPlugin, optionPluginOpts);
		}
		else {
			throw new Exception("Invalid syntax.");
		}

		ConfigTO configTO = createConfigTO(transferSettings);

		operationOptions.setLocalDir(localDir);
		operationOptions.setConfigTO(configTO);
		operationOptions.setDaemon(options.has(optionAddDaemon));

		return operationOptions;
	}

	@Override
	public void printResults(OperationResult operationResult) {
		ConnectOperationResult concreteOperationResult = (ConnectOperationResult) operationResult;

		if (concreteOperationResult.getResultCode() == ConnectResultCode.OK) {
			out.println();
			out.println("Repository connected, and local folder initialized.");
			out.println("You can now use the 'syncany' command to sync your files.");
			out.println();

			if (concreteOperationResult.isAddedToDaemon()) {
				out.println("To automatically sync this folder, simply restart the daemon with 'sy daemon restart'.");
				out.println();
			}
		}
		else if (concreteOperationResult.getResultCode() == ConnectResultCode.NOK_TEST_FAILED) {
			StorageTestResult testResult = concreteOperationResult.getTestResult();
			out.println();

			if (!testResult.isTargetCanConnect()) {
				out.println("ERROR: Cannot connect to the repository, because the connection to the storage backend failed.");
				out.println("       Possible reasons for this could be connectivity issues (are you connect to the Internet?),");
				out.println("       or invalid user credentials (are username/password valid?).");
			}
			else if (!testResult.isTargetExists()) {
				out.println("ERROR: Cannot connect to the repository, because the target does not exist.");
				out.println("       Please check if it really exists and if you can read from it / write to it.");
			}
			else if (!testResult.isTargetCanWrite()) {
				out.println("ERROR: Cannot connect to the repository, because the target is not writable. This is probably");
				out.println("       a permission issue (does the user have write permissions to the target?).");
			}
			else if (!testResult.isRepoFileExists()) {
				out.println("ERROR: Cannot connect to the repository, because no repo file was found ('syncany' file).");
				out.println("       Are you sure that this is a valid Syncany repository? Use 'sy init' to create a new one.");
			}
			else {
				out.println("ERROR: Cannot connect to the repository.");
			}

			out.println();
			printTestResult(testResult);
		}
		else if (concreteOperationResult.getResultCode() == ConnectResultCode.NOK_DECRYPT_ERROR) {
			out.println();
			out.println("ERROR: Invalid password or corrupt ciphertext.");
			out.println();
			out.println("The reason for this might be an invalid password, or that the");
			out.println("link/files have been tampered with.");
			out.println();
		}
		else {
			out.println();
			out.println("ERROR: Cannot connect to repository. Unknown error code: " + operationResult);
			out.println();
		}
	}
}
