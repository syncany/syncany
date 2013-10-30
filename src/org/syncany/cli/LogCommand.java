package org.syncany.cli;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.operations.LogOperation.LogOperationOptions;
import org.syncany.operations.LogOperation.LogOperationResult;
import org.syncany.util.StringUtil;

public class LogCommand extends Command {
	private static final DateFormat dateFormat = new SimpleDateFormat("dd-MM-yy HH:mm:ss"); 
	
	@Override
	public boolean initializedLocalDirRequired() {	
		return true;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		LogOperationOptions operationOptions = parseOptions(operationArgs);
		LogOperationResult operationResult = client.log(operationOptions);
		
		printResults(operationResult);
		
		return 0;		
	}
	
	public LogOperationOptions parseOptions(String[] operationArgs) throws Exception {
		LogOperationOptions operationOptions = new LogOperationOptions();

		OptionParser parser = new OptionParser();	
		
		OptionSet options = parser.parse(operationArgs);	

		// Files
		List<?> nonOptionArgs = options.nonOptionArguments();
		List<String> restoreFilePaths = new ArrayList<String>();
		
		for (Object nonOptionArg : nonOptionArgs) {
			restoreFilePaths.add(nonOptionArg.toString());
		}

		operationOptions.setPaths(restoreFilePaths);	
		
		return operationOptions;
	}
	
	private void printResults(LogOperationResult operationResult) {
		for (PartialFileHistory fileHistory : operationResult.getFileHistories()) {
			for (FileVersion fileVersion : fileHistory.getFileVersions().values()) {
				String posixPermissions = (fileVersion.getPosixPermissions() != null) ? fileVersion.getPosixPermissions() : "";
				String dosAttributes = (fileVersion.getDosAttributes() != null) ? fileVersion.getDosAttributes() : "";
				
				out.printf(
					"%20d %4d %-20s %9s %4s %8d %7s %8s %40s %s\n",
					
					fileHistory.getFileId(),
					fileVersion.getVersion(),
					dateFormat.format(fileVersion.getLastModified()),
					posixPermissions,
					dosAttributes,
					fileVersion.getSize(),
					fileVersion.getType(),
					fileVersion.getStatus(),
					StringUtil.toHex(fileVersion.getChecksum()),
					fileVersion.getPath()				
				);
			}
		}
	}	
}
