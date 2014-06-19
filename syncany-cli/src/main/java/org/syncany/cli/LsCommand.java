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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileType;
import org.syncany.operations.ls.LsOperationOptions;
import org.syncany.operations.ls.LsOperationResult;

public class LsCommand extends AbstractHistoryCommand {
	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.INITIALIZED_LOCALDIR;
	}

	@Override
	public int execute(String[] operationArgs) throws Exception {
		LsOperationOptions operationOptions = parseOptions(operationArgs);
		LsOperationResult operationResult = client.ls(operationOptions);

		printResults(operationResult);

		return 0;
	}	

	private LsOperationOptions parseOptions(String[] operationArgs) throws Exception {
		LsOperationOptions operationOptions = new LsOperationOptions();

		OptionParser parser = new OptionParser();
		OptionSpec<String> optionDateStr = parser.acceptsAll(asList("D", "date")).withRequiredArg();
		OptionSpec<Void> optionRecursive = parser.acceptsAll(asList("r", "recursive"));
		OptionSpec<String> optionFileTypes = parser.acceptsAll(asList("t", "types")).withRequiredArg();

		OptionSet options = parser.parse(operationArgs);

		// --date=..
		if (options.has(optionDateStr)) {			
			Date logViewDate = parseDateOption(options.valueOf(optionDateStr));
			operationOptions.setDate(logViewDate);
		}
		
		// --recursive
		operationOptions.setRecursive(options.has(optionRecursive));
		
		// --types=[tds]
		if (options.has(optionFileTypes)) {
			String fileTypesStr = options.valueOf(optionFileTypes).toLowerCase();
			List<FileType> fileTypes = new ArrayList<FileType>();
			
			if (fileTypesStr.contains("f")) {
				fileTypes.add(FileType.FILE);
			}
			
			if (fileTypesStr.contains("d")) {
				fileTypes.add(FileType.FOLDER);
			}
			
			if (fileTypesStr.contains("s")) {
				fileTypes.add(FileType.SYMLINK);
			}
			
			operationOptions.setFileTypes(fileTypes);
		}
		
		// <path-expr>
		List<?> nonOptionArgs = options.nonOptionArguments();
		
		if (nonOptionArgs.size() > 0) {
			operationOptions.setPathExpression(nonOptionArgs.get(0).toString());
		}

		return operationOptions;
	}

	private void printResults(LsOperationResult operationResult) {		
		int longestPath = calculateLongestPath(operationResult.getFileTree());

		for (FileVersion fileVersion : operationResult.getFileTree().values()) {			
			out.printf("%-" + longestPath + "s %s", fileVersion.getPath(), fileVersion.getFileHistoryId());
			printOneVersion(fileVersion);
			out.println();
		}
	}
	
	private int calculateLongestPath(Map<String, FileVersion> fileVersions) {
		int result = 0;
		
		for (FileVersion fileVersion : fileVersions.values()) {
			result = Math.max(result, fileVersion.getPath().length());
		}
		
		return result;	
	}
}
