package org.syncany.cli;

import org.syncany.operations.StatusOperation.ChangeSet;
import org.syncany.operations.DownOperation.DownOperationOptions;
import org.syncany.operations.DownOperation.DownOperationResult;

public class DownCommand extends Command {
	@Override
	public boolean initializedLocalDirRequired() {	
		return true;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		DownOperationOptions operationOptions = parseOptions(operationArgs);		
		DownOperationResult operationResult = client.down(operationOptions);		
		
		printResults(operationResult);
		
		return 0;
	}

	public DownOperationOptions parseOptions(String[] operationArguments) {
		return new DownOperationOptions();
	}

	public void printResults(DownOperationResult operationResult) {
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
