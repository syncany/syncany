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
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.operations.ChangeSet;
import org.syncany.operations.CleanupOperation.CleanupOperationOptions;
import org.syncany.operations.StatusOperation.StatusOperationOptions;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.operations.up.UpOperationResult;
import org.syncany.operations.up.UpOperationResult.UpResultCode;

public class UpCommand extends Command {
	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.INITIALIZED_LOCALDIR;
	}

	@Override
	public int execute(String[] operationArgs) throws Exception {
		UpOperationOptions operationOptions = parseOptions(operationArgs);
		UpOperationResult operationResult = client.up(operationOptions);

		printResults(operationResult);

		return 0;
	}

	public UpOperationOptions parseOptions(String[] operationArgs) throws Exception {
		// Sync up options
		UpOperationOptions operationOptions = new UpOperationOptions();

		OptionParser parser = new OptionParser();
		parser.allowsUnrecognizedOptions();

		OptionSpec<Void> optionNoCleanup = parser.acceptsAll(asList("c", "no-cleanup"));
		OptionSpec<Void> optionForceUpload = parser.acceptsAll(asList("F", "force-upload"));

		OptionSet options = parser.parse(operationArgs);

		// status [<args>]
		operationOptions.setStatusOptions(parseStatusOptions(operationArgs));

		// --no-cleanup
		boolean cleanupEnabled = !options.has(optionNoCleanup);
		operationOptions.setCleanupEnabled(cleanupEnabled);
		
		if (cleanupEnabled) {
			operationOptions.setCleanupOptions(parseCleanupOptions(operationArgs));
		}

		// --force
		operationOptions.setForceUploadEnabled(options.has(optionForceUpload));

		return operationOptions;
	}

	private CleanupOperationOptions parseCleanupOptions(String[] operationArgs) throws Exception {
		CleanupCommand cleanupCommand = new CleanupCommand();
		return cleanupCommand.parseOptions(operationArgs);
	}

	private StatusOperationOptions parseStatusOptions(String[] operationArgs) {
		StatusCommand statusCommand = new StatusCommand();
		return statusCommand.parseOptions(operationArgs);
	}

	public void printResults(UpOperationResult operationResult) {
		if (operationResult.getResultCode() == UpResultCode.NOK_UNKNOWN_DATABASES) {
			out.println("Sync up skipped, because there are remote changes.");
		}
		else if (operationResult.getResultCode() == UpResultCode.OK_APPLIED_CHANGES) {
			ChangeSet changeSet = operationResult.getChangeSet();

			for (String newFile : changeSet.getNewFiles()) {
				out.println("A " + newFile);
			}

			for (String changedFile : changeSet.getChangedFiles()) {
				out.println("M " + changedFile);
			}

			for (String deletedFile : changeSet.getDeletedFiles()) {
				out.println("D " + deletedFile);
			}

			out.println("Sync up finished.");
		}
		else {
			out.println("Sync up skipped, no local changes.");
		}
	}
}
