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
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.cli.util.CommandLineUtil;
import org.syncany.operations.OperationResult;
import org.syncany.operations.cleanup.CleanupOperation;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.operations.cleanup.CleanupOperationResult;
import org.syncany.operations.daemon.messages.CleanupStartCleaningSyncExternalEvent;
import org.syncany.operations.daemon.messages.CleanupStartSyncExternalEvent;
import org.syncany.operations.status.StatusOperationOptions;

import com.google.common.eventbus.Subscribe;

public class CleanupCommand extends Command {
	@Override
	public CommandScope getRequiredCommandScope() {
		return CommandScope.INITIALIZED_LOCALDIR;
	}

	@Override
	public boolean canExecuteInDaemonScope() {
		return false;
	}

	@Override
	public int execute(String[] operationArgs) throws Exception {
		CleanupOperationOptions operationOptions = parseOptions(operationArgs);
		CleanupOperationResult operationResult = new CleanupOperation(config, operationOptions).execute();

		printResults(operationResult);

		return 0;
	}

	@Override
	public CleanupOperationOptions parseOptions(String[] operationArgs) throws Exception {
		CleanupOperationOptions operationOptions = new CleanupOperationOptions();

		OptionParser parser = new OptionParser();
		parser.allowsUnrecognizedOptions();

		OptionSpec<Void> optionForce = parser.acceptsAll(asList("f", "force"));
		OptionSpec<Void> optionNoOlderVersionRemoval = parser.acceptsAll(asList("O", "no-delete-older-than"));
		OptionSpec<Void> optionNoVersionRemovalByInterval = parser.acceptsAll(asList("I", "no-delete-interval"));
		OptionSpec<Void> optionNoRemoveTempFiles = parser.acceptsAll(asList("T", "no-temp-removal"));
		OptionSpec<String> optionKeepMinTime = parser.acceptsAll(asList("o", "delete-older-than"))
				.withRequiredArg().ofType(String.class);

		OptionSet options = parser.parse(operationArgs);

		// -F, --force
		operationOptions.setForce(options.has(optionForce));

		// -V, --no-version-removal
		operationOptions.setRemoveOldVersions(!options.has(optionNoOlderVersionRemoval));

		// -T, --no-temp-removal
		operationOptions.setRemoveUnreferencedTemporaryFiles(!options.has(optionNoRemoveTempFiles));
		
		// -I, --no-delete-interval
		operationOptions.setRemoveVersionsByInterval(!options.has(optionNoVersionRemovalByInterval));

		// -o=<time>, --delete-older-than=<time>
		if (options.has(optionKeepMinTime)) {
			long keepDeletedFilesForSeconds = CommandLineUtil.parseTimePeriod(options.valueOf(optionKeepMinTime));

			if (keepDeletedFilesForSeconds < 0) {
				throw new Exception("Invalid value for --delete-older-than==" + keepDeletedFilesForSeconds + "; must be >= 0");
			}

			operationOptions.setMinKeepSeconds(keepDeletedFilesForSeconds);
		}

		// Parse 'status' options
		operationOptions.setStatusOptions(parseStatusOptions(operationArgs));

		return operationOptions;
	}

	private StatusOperationOptions parseStatusOptions(String[] operationArgs) throws Exception {
		StatusCommand statusCommand = new StatusCommand();
		statusCommand.setOut(out);

		return statusCommand.parseOptions(operationArgs);
	}

	@Override
	public void printResults(OperationResult operationResult) {
		CleanupOperationResult concreteOperationResult = (CleanupOperationResult) operationResult;

		switch (concreteOperationResult.getResultCode()) {
		case NOK_DIRTY_LOCAL:
			out.println("Cannot cleanup database if local repository is in a dirty state; Call 'up' first.");
			break;

		case NOK_RECENTLY_CLEANED:
			out.println("Cleanup has been done recently, so it is not necessary.");
			out.println("If you are sure it is necessary, override with --force.");
			break;

		case NOK_LOCAL_CHANGES:
			out.println("Local changes detected. Please call 'up' first'.");
			break;

		case NOK_REMOTE_CHANGES:
			out.println("Remote changes detected or repository is locked by another user. Please call 'down' first.");
			break;

		case NOK_OTHER_OPERATIONS_RUNNING:
			out.println("Cannot run cleanup while other clients are performing up/down/cleanup. Try again later.");
			break;

		case OK:
			if (concreteOperationResult.getMergedDatabaseFilesCount() > 0) {
				out.println(concreteOperationResult.getMergedDatabaseFilesCount() + " database files merged.");
			}

			if (concreteOperationResult.getRemovedMultiChunksCount() > 0) {
				out.printf("%d multichunk(s) deleted on remote storage (freed %.2f MB)\n",
						concreteOperationResult.getRemovedMultiChunksCount(),
						(double) concreteOperationResult.getRemovedMultiChunksSize() / 1024 / 1024);
			}

			if (concreteOperationResult.getRemovedOldVersionsCount() > 0) {
				out.println(concreteOperationResult.getRemovedOldVersionsCount() + " file histories shortened.");
				// TODO [low] This counts only the file histories, not file versions; not very helpful!
			}

			out.println("Cleanup successful.");
			break;

		case OK_NOTHING_DONE:
			out.println("Cleanup not necessary. Nothing done.");
			break;

		default:
			throw new RuntimeException("Invalid result code: " + concreteOperationResult.getResultCode().toString());
		}
	}

	@Subscribe
	public void onCleanupStartEventReceived(CleanupStartSyncExternalEvent syncEvent) {
		out.printr("Checking if cleanup is needed ...");
	}

	@Subscribe
	public void onCleanupStartCleaningEventReceived(CleanupStartCleaningSyncExternalEvent syncEvent) {
		out.printr("Cleanup is needed, starting to clean ...");
	}
}
