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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.operations.LogOperation;
import org.syncany.operations.LogOperation.LogOperationOptions;
import org.syncany.operations.LogOperation.LogOperationResult;

public class LogCommand extends Command {
	private static final Logger logger = Logger.getLogger(LogOperation.class.getSimpleName());
	private static final DateFormat dateFormat = new SimpleDateFormat("dd-MM-yy HH:mm:ss");

	@Override
	public CommandScope getRequiredCommandScope() {	
		return CommandScope.INITIALIZED_LOCALDIR;
	}

	@Override
	public int execute(String[] operationArgs) throws Exception {
		LogOperationOptions operationOptions = parseOptions(operationArgs);
		LogOperationResult operationResult = client.log(operationOptions);

		printResults(operationResult);

		return 0;
	}	

	public static List<String> getSupportedFormats() {
		List<String> localFormats = new ArrayList<String>();

		localFormats.add("full");
		localFormats.add("last");

		return Collections.unmodifiableList(localFormats);
	}

	private LogOperationOptions parseOptions(String[] operationArgs) throws Exception {
		LogOperationOptions operationOptions = new LogOperationOptions();

		OptionParser parser = new OptionParser();
		OptionSpec<String> optionFormat = parser.acceptsAll(asList("f", "format")).withRequiredArg().defaultsTo("full");

		OptionSet options = parser.parse(operationArgs);

		// --format
		String format = options.valueOf(optionFormat);

		if (!getSupportedFormats().contains(format)) {
			throw new Exception("Unrecognized log format " + format);
		}

		// Files
		List<?> nonOptionArgs = options.nonOptionArguments();
		List<String> restoreFilePaths = new ArrayList<String>();

		for (Object nonOptionArg : nonOptionArgs) {
			restoreFilePaths.add(nonOptionArg.toString());
		}

		operationOptions.setPaths(restoreFilePaths);
		operationOptions.setFormat(format);

		return operationOptions;
	}

	private void printResults(LogOperationResult operationResult) {		
		if ("full".equals(operationResult.getFormat())) {
			printFullFormat(operationResult.getFileHistories());				
		}
		else if ("last".equals(operationResult.getFormat())) {	
			printLastFormat(operationResult.getFileHistories());
		}
		else {
			out.println(" unkown format " + operationResult.getFormat());
			logger.log(Level.SEVERE, "Unrecognized lof format, should have been rejected earlier " + operationResult.getFormat());
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

		out.printf("%4d %-20s %9s %4s %8d %7s %8s %40s", fileVersion.getVersion(), dateFormat.format(fileVersion.getLastModified()),
				posixPermissions, dosAttributes, fileVersion.getSize(), fileVersion.getType(), fileVersion.getStatus(),
				fileVersion.getChecksum());
	}
}
