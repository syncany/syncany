package org.syncany.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
			
			out.println("Sync down finished.");
		}
		else {
			out.println("Sync down skipped, no remote changes.");
		}
	}
}
