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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.operations.ls.LsOperationOptions;
import org.syncany.operations.ls.LsOperationOptions.LogOutputFormat;
import org.syncany.operations.ls.LsOperationResult;

public class LsCommand extends AbstractHistoryCommand {
	private static final DateFormat dateFormat = new SimpleDateFormat("dd-MM-yy HH:mm:ss");

	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.INITIALIZED_LOCALDIR;
	}

	@Override
	public int execute(String[] operationArgs) throws Exception {
		LsOperationOptions operationOptions = parseOptions(operationArgs);
		LsOperationResult operationResult = client.ls(operationOptions);

		printResults(operationOptions, operationResult);

		return 0;
	}	

	private LsOperationOptions parseOptions(String[] operationArgs) throws Exception {
		LsOperationOptions operationOptions = new LsOperationOptions();

		OptionParser parser = new OptionParser();
		OptionSpec<String> optionDateStr = parser.acceptsAll(asList("D", "date")).withRequiredArg();
		OptionSpec<String> optionFormat = parser.acceptsAll(asList("f", "format")).withRequiredArg().defaultsTo(LogOutputFormat.LAST.toString());

		OptionSet options = parser.parse(operationArgs);

		// --date=..
		if (options.has(optionDateStr)) {			
			Date logViewDate = parseDateOption(options.valueOf(optionDateStr));
			operationOptions.setDate(logViewDate);
		}
		
		// --format=full|last
		LogOutputFormat format = parseLogFormat(options.valueOf(optionFormat));
		operationOptions.setFormat(format);

		// <filter>
		List<?> nonOptionArgs = options.nonOptionArguments();
		
		if (nonOptionArgs.size() > 0) {
			operationOptions.setFilter(nonOptionArgs.get(0).toString());
		}

		return operationOptions;
	}

	private LogOutputFormat parseLogFormat(String formatStr) throws Exception {
		try {
			return LogOutputFormat.valueOf(formatStr.toUpperCase());
		}
		catch (Exception e) {
			throw new Exception("Unknown log format: " + formatStr);
		}
	}

	private void printResults(LsOperationOptions operationOptions, LsOperationResult operationResult) {		
		if (operationOptions.getFormat() == LogOutputFormat.FULL) {
			printFullFormat(operationResult.getFileHistories());				
		}
		else if (operationOptions.getFormat() == LogOutputFormat.LAST) {
			printLastFormat(operationResult.getFileHistories());
		}	
	}

	private void printLastFormat(List<PartialFileHistory> fileHistories) {
		int longestPath = calculateLongestPath(fileHistories, true);

		for (PartialFileHistory fileHistory : fileHistories) {			
			FileVersion lastVersion = fileHistory.getLastVersion();

			out.printf("%-" + longestPath + "s %s", lastVersion.getPath(), fileHistory.getFileHistoryId());
			printOneVersion(lastVersion);
			out.println();
		}
	}

	private void printFullFormat(List<PartialFileHistory> fileHistories) {
		for (PartialFileHistory fileHistory : fileHistories) {			
			FileVersion lastVersion = fileHistory.getLastVersion();
	
			out.printf("%s %s\n", lastVersion.getPath(), fileHistory.getFileHistoryId());
	
			for (FileVersion fileVersion : fileHistory.getFileVersions().values()) {
				out.print('\t');
				printOneVersion(fileVersion);
	
				if (fileVersion.getPath().equals(lastVersion.getPath())) {
					out.println();
				}
				else {
					out.println(" " + fileVersion.getPath());
				}
			}
		}
	}
	
	private int calculateLongestPath(List<PartialFileHistory> fileHistories, boolean lastOnly) {
		int result = 0;
		
		for (PartialFileHistory fileHistory : fileHistories) {
			if (lastOnly) {
				result = Math.max(result, fileHistory.getLastVersion().getPath().length());
			}
			else {
				for (FileVersion fileVersion : fileHistory.getFileVersions().values()) {
					result = Math.max(result, fileVersion.getPath().length());
				}
			}
		}
		
		return result;	
	}

	private void printOneVersion(FileVersion fileVersion) {
		String posixPermissions = (fileVersion.getPosixPermissions() != null) ? fileVersion.getPosixPermissions() : "";
		String dosAttributes = (fileVersion.getDosAttributes() != null) ? fileVersion.getDosAttributes() : "";
		String fileChecksum = (fileVersion.getChecksum() != null) ? fileVersion.getChecksum().toString() : "";
		
		out.printf("%4d %-20s %9s %4s %8d %7s %8s %40s", fileVersion.getVersion(), dateFormat.format(fileVersion.getLastModified()),
				posixPermissions, dosAttributes, fileVersion.getSize(), fileVersion.getType(), fileVersion.getStatus(),
				fileChecksum);
	}
}
