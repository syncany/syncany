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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.operations.ChangeSet;
import org.syncany.operations.OperationResult;
import org.syncany.operations.daemon.messages.DownDownloadFileSyncExternalEvent;
import org.syncany.operations.daemon.messages.LsRemoteStartSyncExternalEvent;
import org.syncany.operations.down.DownOperation;
import org.syncany.operations.down.DownOperationOptions;
import org.syncany.operations.down.DownOperationOptions.DownConflictStrategy;
import org.syncany.operations.down.DownOperationResult;
import org.syncany.operations.down.DownOperationResult.DownResultCode;

import com.google.common.eventbus.Subscribe;

public class DownCommand extends Command {
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
		DownOperationOptions operationOptions = parseOptions(operationArgs);		
		DownOperationResult operationResult = new DownOperation(config, operationOptions).execute();		
		
		printResults(operationResult);
		
		return 0;
	}

	public DownOperationOptions parseOptions(String[] operationArguments) {
		DownOperationOptions operationOptions = new DownOperationOptions();

		OptionParser parser = new OptionParser();
		parser.allowsUnrecognizedOptions();

		OptionSpec<String> optionConflictStrategy = parser.acceptsAll(asList("C", "conflict-strategy")).withRequiredArg();
		OptionSpec<Void> optionNoApply = parser.acceptsAll(asList("A", "no-apply"));

		OptionSet options = parser.parse(operationArguments);

		// --conflict-strategy=<strategy>
		if (options.has(optionConflictStrategy)) {
			String conflictStrategyStr = options.valueOf(optionConflictStrategy).toUpperCase();
			operationOptions.setConflictStrategy(DownConflictStrategy.valueOf(conflictStrategyStr));
		}
		
		// --no-apply
		if (options.has(optionNoApply)) {
			operationOptions.setApplyChanges(false);
		}

		return operationOptions;
	}

	@Override
	public void printResults(OperationResult operationResult) {
		DownOperationResult concreteOperationResult = (DownOperationResult) operationResult;
		
		if (concreteOperationResult.getResultCode() == DownResultCode.OK_WITH_REMOTE_CHANGES) {
			ChangeSet changeSet = concreteOperationResult.getChangeSet();
			
			if (changeSet.hasChanges()) {
				List<String> newFiles = new ArrayList<String>(changeSet.getNewFiles());
				List<String> changedFiles = new ArrayList<String>(changeSet.getChangedFiles());
				List<String> deletedFiles = new ArrayList<String>(changeSet.getDeletedFiles());
				
				Collections.sort(newFiles);
				Collections.sort(changedFiles);
				Collections.sort(deletedFiles);
				
				for (String newFile : newFiles) {
					out.println("A "+newFile);
				}
		
				for (String changedFile : changedFiles) {
					out.println("M "+changedFile);
				}
				
				for (String deletedFile : deletedFiles) {
					out.println("D "+deletedFile);
				}		
			}
			else {
				out.println(concreteOperationResult.getDownloadedUnknownDatabases().size() + " database file(s) processed.");
			}
			
			out.println("Sync down finished.");
		}
		else {
			out.println("Sync down skipped, no remote changes.");
		}
	}
	
	@Subscribe
	public void onLsRemoteStartEventReceived(LsRemoteStartSyncExternalEvent syncEvent) {
		out.printr("Checking remote changes ...");
	}
	
	@Subscribe
	public void onSyncEventReceived(DownDownloadFileSyncExternalEvent syncEvent) {
		String fileDescription = syncEvent.getFileDescription();
		int currentFileIndex = syncEvent.getCurrentFileIndex();
		int maxFileCount = syncEvent.getMaxFileCount();
		
		out.printr("Downloading " + fileDescription + " "+ currentFileIndex + "/" + maxFileCount + " ...");			
	}
}
