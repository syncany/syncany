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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.operations.OperationResult;
import org.syncany.operations.log.LogOperationOptions;
import org.syncany.operations.log.LogOperationResult;

import com.google.common.collect.Lists;

public class LogCommand extends Command {	
	protected static final Logger logger = Logger.getLogger(LogCommand.class.getSimpleName());
	
	private static final String DATE_FORMAT_PATTERN = "yy-MM-dd HH:mm:ss";
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_PATTERN);
	private static final FileVersionComparator FILE_VERSION_COMPARATOR = new FileVersionComparator();
	
	private boolean excludeEmpty; 
	
	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.INITIALIZED_LOCALDIR;
	}

	@Override
	public boolean canExecuteInDaemonScope() {
		return true;
	}

	@Override
	public int execute(String[] operationArgs) throws Exception {
		LogOperationOptions operationOptions = parseOptions(operationArgs);
		LogOperationResult operationResult = client.log(operationOptions);

		printResults(operationResult);

		return 0;
	}	

	@Override
	public LogOperationOptions parseOptions(String[] operationArgs) throws Exception {
		LogOperationOptions operationOptions = new LogOperationOptions();

		OptionParser parser = new OptionParser();
		parser.allowsUnrecognizedOptions();
		
		OptionSpec<Integer> optionMaxCountStr = parser.acceptsAll(asList("n", "count")).withRequiredArg().ofType(Integer.class);
		OptionSpec<Void> optionExcludeEmpty = parser.acceptsAll(asList("x", "exclude-empty"));

		OptionSet options = parser.parse(operationArgs);
		
		// Disable chunk data retrieval
		operationOptions.setExcludeChunkData(true);
		
		// -x, --exclude-empty
		excludeEmpty = options.has(optionExcludeEmpty);
		
		// -n, --count=..
		if (options.has(optionMaxCountStr)) {			
			operationOptions.setMaxCount(options.valueOf(optionMaxCountStr));
		}
		
		return operationOptions;
	}

	@Override
	public void printResults(OperationResult operationResult) {
		LogOperationResult concreteOperationResult = (LogOperationResult) operationResult;
		List<DatabaseVersion> databaseVersions = concreteOperationResult.getDatabaseVersions();
		
		Collections.reverse(databaseVersions);
		
		for (DatabaseVersion databaseVersion : databaseVersions) {
			boolean hasFileHistories = databaseVersion.getFileHistories().size() > 0;
			boolean printDatabaseVersion = hasFileHistories || !excludeEmpty;
			
			if (printDatabaseVersion) {
				printDatabaseVersion(databaseVersion);
			}
		}
	}

	private void printDatabaseVersion(DatabaseVersion databaseVersion) {
		String dateStr = DATE_FORMAT.format(databaseVersion.getTimestamp());
		String clientStr = databaseVersion.getClient();
				
		out.println(String.format("Database version from %s, by client %s", dateStr, clientStr));
		
		if (!databaseVersion.getFileHistories().isEmpty()) {	
			// Collect all file versions
			List<FileVersion> fileVersions = Lists.newArrayList();

			for (PartialFileHistory fileHistory : databaseVersion.getFileHistories()) {
				fileVersions.addAll(fileHistory.getFileVersions().values());
			}
			
			Collections.sort(fileVersions, FILE_VERSION_COMPARATOR);
			
			// Print them
			for (FileVersion fileVersion : fileVersions) {			
				switch (fileVersion.getStatus()) {
				case NEW:
					out.println("  A " + fileVersion.getPath());
					break;
					
				case CHANGED:
				case RENAMED:
					out.println("  M " + fileVersion.getPath());
					break;

				case DELETED:
					out.println("  D " + fileVersion.getPath());
					break;
				}
			}
		}
		else {
			out.println("  (empty)");
		}
		
		out.println();		
	}	
	
	private static class FileVersionComparator implements Comparator<FileVersion> {
	    @Override
	    public int compare(FileVersion fileVersion1, FileVersion fileVersion2) {	        
	    	return fileVersion1.getPath().compareTo(fileVersion2.getPath());
	    }
	}
}
