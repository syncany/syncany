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

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.operations.ConnectOperation.ConnectOperationListener;
import org.syncany.operations.ConnectOperation.ConnectOperationOptions;
import org.syncany.operations.ConnectOperation.ConnectOperationResult;
import org.syncany.operations.ConnectOperation.ConnectOptionsStrategy;
import org.syncany.operations.ConnectOperation.ConnectResultCode;

public class ConnectCommand extends AbstractInitCommand implements ConnectOperationListener {
	public ConnectCommand() {
		super();
	}

	@Override
	public CommandScope getRequiredCommandScope() {
		return CommandScope.UNINITIALIZED_LOCALDIR;
	}

	@Override
	public int execute(String[] operationArgs) throws Exception {
		boolean retryNeeded = true;
		boolean performOperation = true;

		ConnectOperationOptions operationOptions = parseConnectOptions(operationArgs);

		while (retryNeeded && performOperation) {
			ConnectOperationResult operationResult = client.connect(operationOptions, this);
			printResults(operationResult);

			retryNeeded = operationResult.getResultCode() != ConnectResultCode.OK;

			if (retryNeeded) {
				performOperation = isInteractive && askRetry();

				if (performOperation) {
					updateConnectionTO(operationOptions.getConfigTO().getConnectionTO());
				}
			}
		}

		return 0;
	}

	private ConnectOperationOptions parseConnectOptions(String[] operationArguments) throws OptionException, Exception {
		ConnectOperationOptions operationOptions = new ConnectOperationOptions();

		OptionParser parser = new OptionParser();
		OptionSpec<String> optionPlugin = parser.acceptsAll(asList("P", "plugin")).withRequiredArg();
		OptionSpec<String> optionPluginOpts = parser.acceptsAll(asList("o", "plugin-option")).withRequiredArg();
		OptionSpec<Void> optionNonInteractive = parser.acceptsAll(asList("I", "no-interaction"));

		OptionSet options = parser.parse(operationArguments);
		List<?> nonOptionArgs = options.nonOptionArguments();

		// --no-interaction
		isInteractive = !options.has(optionNonInteractive);

		// Plugin
		ConnectionTO connectionTO = null;

		if (nonOptionArgs.size() == 1) {
			String connectLink = (String) nonOptionArgs.get(0);

			operationOptions.setStrategy(ConnectOptionsStrategy.CONNECTION_LINK);
			operationOptions.setConnectLink(connectLink);

			connectionTO = null;
		}
		else if (nonOptionArgs.size() == 0) {
			operationOptions.setStrategy(ConnectOptionsStrategy.CONNECTION_TO);
			operationOptions.setConnectLink(null);

			connectionTO = createConnectionTOFromOptions(options, optionPlugin, optionPluginOpts, optionNonInteractive);
		}
		else {
			throw new Exception("Invalid syntax.");
		}

		ConfigTO configTO = createConfigTO(connectionTO);
		
		operationOptions.setLocalDir(localDir);
		operationOptions.setConfigTO(configTO);

		return operationOptions;
	}

	private void printResults(ConnectOperationResult operationResult) {
		if (operationResult.getResultCode() == ConnectResultCode.OK) {
			out.println();
			out.println("Repository connected, and local folder initialized.");
			out.println("You can now use the 'syncany' command to sync your files.");
			out.println();
		}
		else if (operationResult.getResultCode() == ConnectResultCode.NOK_NO_REPO) {
			out.println();
			out.println("ERROR: No repository was found at the given location.");
			out.println();
			out.println("Make sure that the connection details are correct and that");
			out.println("a repository actually exists at this location.");
			out.println();
		}
		else if (operationResult.getResultCode() == ConnectResultCode.NOK_NO_CONNECTION) {
			out.println();
			out.println("ERROR: Cannot connect to repository (broken connection).");
			out.println();
			out.println("Make sure that you have a working Internet connection and that ");
			out.println("the connection details (esp. the hostname/IP) are correct.");
			out.println();
		}
		else if (operationResult.getResultCode() == ConnectResultCode.NOK_INVALID_REPO) {
			out.println();
			out.println("ERROR: Invalid repository found at location.");
			out.println();
			out.println("Make sure that the connection details are correct and that");
			out.println("a repository actually exists at this location.");
			out.println();
		}
		else {
			out.println();
			out.println("ERROR: Cannot connect to repository. Unknown error code: " + operationResult);
			out.println();
		}
	}

	private String askPassword() {
		out.println();

		char[] passwordChars = console.readPassword("Password: ");
		return new String(passwordChars);
	}

	@Override
	public String getPasswordCallback() {
		return askPassword();
	}

	@Override
	public void notifyCreateMasterKey() {
		out.println();
		out.println("Creating master key from password (this might take a while) ...");
	}
}
