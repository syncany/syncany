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
import org.syncany.operations.OperationResult;
import org.syncany.operations.daemon.messages.SyncExternalEvent;
import org.syncany.operations.status.StatusOperationOptions;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.operations.up.UpOperationResult;
import org.syncany.operations.up.UpOperationResult.UpResultCode;

import com.google.common.eventbus.Subscribe;

public class UpCommand extends Command {
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
		UpOperationResult operationResult = client.up(operationOptions);

		printResults(operationResult);

		return 0;
	}

	@Override
	public UpOperationOptions parseOptions(String[] operationArgs) throws Exception {
		UpOperationOptions operationOptions = new UpOperationOptions();

		OptionParser parser = new OptionParser();
		parser.allowsUnrecognizedOptions();

		OptionSpec<Void> optionForceUpload = parser.acceptsAll(asList("F", "force-upload"));

		OptionSet options = parser.parse(operationArgs);

		// status [<args>]
		operationOptions.setStatusOptions(parseStatusOptions(operationArgs));

		// --force
		operationOptions.setForceUploadEnabled(options.has(optionForceUpload));

		return operationOptions;
	}

	private StatusOperationOptions parseStatusOptions(String[] operationArgs) throws Exception {
		StatusCommand statusCommand = new StatusCommand();
		return statusCommand.parseOptions(operationArgs);
	}

	@Override
	public void printResults(OperationResult operationResult) {
		UpOperationResult concreteOperationResult = (UpOperationResult)operationResult;
		
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
	public void onSyncEventReceived(SyncExternalEvent syncEvent) {
		switch (syncEvent.getType()) {
		case UP_START:
			out.printr("Starting indexing and upload ...");			
			break;
			
		case STATUS_START:
			out.printr("Checking file tree ...");
			break;

		case UP_INDEX_START:
			out.printr("Indexing file tree ...");
			break;
					
		case UP_UPLOAD_FILE:
			String uploadFilename = (String) syncEvent.getSubjects()[0];
			out.printr("Uploading " + uploadFilename + " ...");
			break;
		
		default:					
			// Nothing.
		}
	}
}
