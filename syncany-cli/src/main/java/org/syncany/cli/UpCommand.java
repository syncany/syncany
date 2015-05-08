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
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.operations.ChangeSet;
import org.syncany.operations.OperationResult;
import org.syncany.operations.daemon.messages.LsRemoteStartSyncExternalEvent;
import org.syncany.operations.daemon.messages.StatusStartSyncExternalEvent;
import org.syncany.operations.daemon.messages.UpIndexMidSyncExternalEvent;
import org.syncany.operations.daemon.messages.UpIndexStartSyncExternalEvent;
import org.syncany.operations.daemon.messages.UpStartSyncExternalEvent;
import org.syncany.operations.status.StatusOperationOptions;
import org.syncany.operations.up.UpOperation;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.operations.up.UpOperationResult;
import org.syncany.operations.up.UpOperationResult.UpResultCode;

import com.google.common.eventbus.Subscribe;

public class UpCommand extends Command {
	private int totalFileCount = -1;
	private int uploadedFileCount = 0;
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
		UpOperationOptions operationOptions = parseOptions(operationArgs);
		UpOperationResult operationResult = new UpOperation(config, operationOptions).execute();

		printResults(operationResult);

		return 0;
	}

	@Override
	public UpOperationOptions parseOptions(String[] operationArgs) throws Exception {
		UpOperationOptions operationOptions = new UpOperationOptions();

		OptionParser parser = new OptionParser();
		parser.allowsUnrecognizedOptions();

		OptionSpec<Void> optionForceUpload = parser.acceptsAll(asList("F", "force-upload"));
		OptionSpec<Void> optionNoResumeUpload = parser.acceptsAll(asList("R", "no-resume"));

		OptionSet options = parser.parse(operationArgs);

		// status [<args>]
		operationOptions.setStatusOptions(parseStatusOptions(operationArgs));

		// -F, --force-upload
		operationOptions.setForceUploadEnabled(options.has(optionForceUpload));

		// -R, --no-resume
		operationOptions.setResume(!options.has(optionNoResumeUpload));

		return operationOptions;
	}

	private StatusOperationOptions parseStatusOptions(String[] operationArgs) throws Exception {
		StatusCommand statusCommand = new StatusCommand();
		statusCommand.setOut(out);

		return statusCommand.parseOptions(operationArgs);
	}

	@Override
	public void printResults(OperationResult operationResult) {
		UpOperationResult concreteOperationResult = (UpOperationResult) operationResult;

		if (concreteOperationResult.getResultCode() == UpResultCode.NOK_UNKNOWN_DATABASES) {
			out.println("Sync up skipped, because there are remote changes.");
		}
		else if (concreteOperationResult.getResultCode() == UpResultCode.OK_CHANGES_UPLOADED) {
			ChangeSet changeSet = concreteOperationResult.getChangeSet();

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

	@Subscribe
	public void onUpStartEventReceived(UpStartSyncExternalEvent syncEvent) {
		out.printr("Starting indexing and upload ...");
	}

	@Subscribe
	public void onStatusStartEventReceived(StatusStartSyncExternalEvent syncEvent) {
		out.printr("Checking for new or altered files ...");
	}

	@Subscribe
	public void onLsRemoteStartEventReceived(LsRemoteStartSyncExternalEvent syncEvent) {
		out.printr("Checking remote changes ...");
	}

	@Subscribe
	public void onIndexStartEventReceived(UpIndexStartSyncExternalEvent syncEvent) {
		totalFileCount = syncEvent.getFileCount();
		out.printr("Indexing " + totalFileCount + " new or altered file(s)...");
	}

	@Subscribe
	public void onIndexMidEventReceived(UpIndexMidSyncExternalEvent syncEvent) {
		out.printr("Indexed and uploaded " + syncEvent.getCurrentIndex() + "/" + syncEvent.getFileCount() + " file(s)...");
	}
}
