package org.syncany.cli;

import java.io.PrintStream;

import org.syncany.Client;
import org.syncany.operations.DownOperation.DownOperationOptions;
import org.syncany.operations.SyncOperation.SyncOperationOptions;
import org.syncany.operations.SyncOperation.SyncOperationResult;
import org.syncany.operations.UpOperation.UpOperationOptions;

public class SyncCommand extends Command {
	private DownCommand downCommand;
	private UpCommand upCommand;
	
	public SyncCommand() {
		downCommand = new DownCommand();
		upCommand = new UpCommand();
	}
	
	@Override
	public void setClient(Client client) {
		super.setClient(client);
		
		downCommand.setClient(client);
		upCommand.setClient(client);
	}
	
	@Override
	public void setOut(PrintStream out) {
		super.setOut(out);
		
		downCommand.setOut(out);
		upCommand.setOut(out);
	}
	
	@Override
	public boolean needConfigFile() {	
		return true;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		SyncOperationOptions operationOptions = parseSyncOptions(operationArgs);		
		SyncOperationResult operationResult = client.sync(operationOptions);
		
		printResults(operationResult);
		
		return 0;
	}
	
	public SyncOperationOptions parseSyncOptions(String[] operationArguments) throws Exception {
		DownOperationOptions syncDownOptions = downCommand.parseOptions(operationArguments);
		UpOperationOptions syncUpOptions = upCommand.parseOptions(operationArguments);

		SyncOperationOptions syncOptions = new SyncOperationOptions();
		syncOptions.setSyncDownOptions(syncDownOptions);
		syncOptions.setSyncUpOptions(syncUpOptions);
		
		return syncOptions;
	}
	
	public void printResults(SyncOperationResult operationResult) {
		downCommand.printResults(operationResult.getSyncDownResult());		
		upCommand.printResults(operationResult.getSyncUpResult());
	}	
}
