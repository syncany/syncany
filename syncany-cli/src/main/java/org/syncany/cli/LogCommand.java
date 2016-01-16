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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.operations.OperationResult;
import org.syncany.operations.log.LightweightDatabaseVersion;
import org.syncany.operations.log.LogOperation;
import org.syncany.operations.log.LogOperationOptions;
import org.syncany.operations.log.LogOperationResult;

public class LogCommand extends Command {	
	protected static final Logger logger = Logger.getLogger(LogCommand.class.getSimpleName());
	
	private static final String DATE_FORMAT_PATTERN = "yy-MM-dd HH:mm:ss";
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_PATTERN);
	
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
		LogOperationResult operationResult = new LogOperation(config, operationOptions).execute();

		printResults(operationResult);

		return 0;
	}	

	@Override
	public LogOperationOptions parseOptions(String[] operationArgs) throws Exception {
		LogOperationOptions operationOptions = new LogOperationOptions();

		OptionParser parser = new OptionParser();
		parser.allowsUnrecognizedOptions();
		
		OptionSpec<Integer> optionMaxDatabaseVersionCountStr = parser.acceptsAll(asList("n", "database-count")).withRequiredArg().ofType(Integer.class);
		OptionSpec<Integer> optionStartDatabaseVersionIndexStr = parser.acceptsAll(asList("s", "database-start")).withRequiredArg().ofType(Integer.class);
		OptionSpec<Integer> optionMaxFileHistoryCountStr = parser.acceptsAll(asList("f", "file-count")).withRequiredArg().ofType(Integer.class);
		OptionSpec<Void> optionExcludeEmpty = parser.acceptsAll(asList("x", "exclude-empty"));

		OptionSet options = parser.parse(operationArgs);
		
		// -x, --exclude-empty
		excludeEmpty = options.has(optionExcludeEmpty);
		
		// -n, --database-count=..
		if (options.has(optionMaxDatabaseVersionCountStr)) {			
			operationOptions.setMaxDatabaseVersionCount(options.valueOf(optionMaxDatabaseVersionCountStr));
		}
		
		// -s, --database-start=..
		if (options.has(optionStartDatabaseVersionIndexStr)) {			
			operationOptions.setStartDatabaseVersionIndex(options.valueOf(optionStartDatabaseVersionIndexStr));
		}
		
		// -f, --file-count=..
		if (options.has(optionMaxFileHistoryCountStr)) {			
			operationOptions.setMaxFileHistoryCount(options.valueOf(optionMaxFileHistoryCountStr));
		}
		
		return operationOptions;
	}

	@Override
	public void printResults(OperationResult operationResult) {
		LogOperationResult concreteOperationResult = (LogOperationResult) operationResult;
		List<LightweightDatabaseVersion> databaseVersions = concreteOperationResult.getDatabaseVersions();
		
		Collections.reverse(databaseVersions);
		
		for (LightweightDatabaseVersion databaseVersion : databaseVersions) {
			boolean hasChanges = databaseVersion.getChangeSet().hasChanges();
			boolean printDatabaseVersion = hasChanges || !excludeEmpty;
			
			if (printDatabaseVersion) {
				printDatabaseVersion(databaseVersion);
			}
		}
	}

	private void printDatabaseVersion(LightweightDatabaseVersion databaseVersion) {
		String dateStr = DATE_FORMAT.format(databaseVersion.getDate());
		String clientStr = databaseVersion.getClient();
				
		out.println(String.format("Database version from %s, by client %s", dateStr, clientStr));
		
		if (databaseVersion.getChangeSet().hasChanges()) {	
			for (String newFile : databaseVersion.getChangeSet().getNewFiles()) {
				out.println("  A "+newFile);
			}

			for (String changedFile : databaseVersion.getChangeSet().getChangedFiles()) {
				out.println("  M "+changedFile);
			}
			
			for (String deletedFile : databaseVersion.getChangeSet().getDeletedFiles()) {
				out.println("  D "+deletedFile);
			}	
		}
		else {
			out.println("  (empty)");
		}
		
		out.println();		
	}		
}
