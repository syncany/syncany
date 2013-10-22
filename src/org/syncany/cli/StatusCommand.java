package org.syncany.cli;

import static java.util.Arrays.asList;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.operations.StatusOperation.ChangeSet;
import org.syncany.operations.StatusOperation.StatusOperationOptions;

public class StatusCommand extends Command {
	@Override
	public boolean needConfigFile() {	
		return true;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		StatusOperationOptions operationOptions = parseOptions(operationArgs);
		ChangeSet changeSet = client.status(operationOptions);
		
		printResults(changeSet);
		
		return 0;
	}

	public StatusOperationOptions parseOptions(String[] operationArgs) {
		StatusOperationOptions operationOptions = new StatusOperationOptions();

		OptionParser parser = new OptionParser();	
		parser.allowsUnrecognizedOptions();
		
		OptionSpec<Void> optionForceChecksum = parser.acceptsAll(asList("f", "force-checksum"));
		
		OptionSet options = parser.parse(operationArgs);	
		
		// --force-checksum
		operationOptions.setForceChecksum(options.has(optionForceChecksum));
		
		return operationOptions;
	}	

	public void printResults(ChangeSet changeSet) {
		if (changeSet.hasChanges()) {
			for (String newFile : changeSet.getNewFiles()) {
				out.println("? "+newFile);
			}

			for (String changedFile : changeSet.getChangedFiles()) {
				out.println("M "+changedFile);
			}
			
			for (String deletedFile : changeSet.getDeletedFiles()) {
				out.println("D "+deletedFile);
			}						
		}
		else {
			out.println("No local changes.");
		}
	}

}
