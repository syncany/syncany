package org.syncany.cli;

import org.syncany.operations.StatusOperation.ChangeSet;
import org.syncany.operations.DownOperation.SyncDownOperationOptions;
import org.syncany.operations.DownOperation.SyncDownOperationResult;

public class DownCommand extends Command {
	@Override
	public int execute(String[] operationArgs) throws Exception {
		SyncDownOperationOptions operationOptions = parseOptions(operationArgs);		
		SyncDownOperationResult operationResult = client.down(operationOptions);		
		
		printResults(operationResult);
		
		return 0;
	}

	public SyncDownOperationOptions parseOptions(String[] operationArguments) {
		return new SyncDownOperationOptions();
	}

	public void printResults(SyncDownOperationResult operationResult) {
		ChangeSet changeSet = operationResult.getChangeSet();
		
		if (changeSet.hasChanges()) {
			for (String newFile : changeSet.getNewFiles()) {
				out.println("A "+newFile);
			}
	
			for (String changedFile : changeSet.getChangedFiles()) {
				out.println("M "+changedFile);
			}
			
			for (String deletedFile : changeSet.getDeletedFiles()) {
				out.println("D "+deletedFile);
			}	
			
			out.println("Sync down finished.");
		}
		else {
			out.println("Sync down skipped, no remote changes.");
		}
	}
}
