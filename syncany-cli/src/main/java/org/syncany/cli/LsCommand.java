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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.ObjectId;
import org.syncany.database.PartialFileHistory;
import org.syncany.operations.ls.LsOperationOptions;
import org.syncany.operations.ls.LsOperationResult;

import com.google.common.base.Function;

public class LsCommand extends AbstractHistoryCommand {
	private static final int CHECKSUM_LENGTH_LONG = 40;
	private static final int CHECKSUM_LENGTH_SHORT = 10;
	
	private int checksumLength;
	private boolean groupedVersions;
	
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
		OptionSpec<Void> optionRecursive = parser.acceptsAll(asList("r", "recursive"));
		OptionSpec<String> optionFileTypes = parser.acceptsAll(asList("t", "types")).withRequiredArg();
		OptionSpec<Void> optionLongChecksums = parser.acceptsAll(asList("f", "full-checksums"));
		OptionSpec<Void> optionWithVersions = parser.acceptsAll(asList("V", "versions"));
		OptionSpec<Void> optionGroupedVersions = parser.acceptsAll(asList("g", "group"));

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
				
		// --versions
		operationOptions.setFetchHistories(options.has(optionWithVersions));
		
		// --long-checksums (display option)
		checksumLength = (options.has(optionLongChecksums)) ? CHECKSUM_LENGTH_LONG : CHECKSUM_LENGTH_SHORT;

		// --group (display option)
		groupedVersions = options.has(optionGroupedVersions);
		
		// <path-expr>
		List<?> nonOptionArgs = options.nonOptionArguments();
		
		if (nonOptionArgs.size() > 0) {
			operationOptions.setPathExpression(nonOptionArgs.get(0).toString());
		}

		return operationOptions;
	}

	private void printResults(LsOperationOptions operationOptions, LsOperationResult operationResult) {		
		int longestSize = calculateLongestSize(operationResult.getFileTree());
		int longestVersion = calculateLongestVersion(operationResult.getFileTree());

		if (operationOptions.isFetchHistories()) {
			printHistories(operationOptions, operationResult, longestSize, longestVersion);
				
		}
		else {
			printTree(operationOptions, operationResult, longestSize, longestVersion);			
		}
	}
	
	private void printTree(LsOperationOptions operationOptions, LsOperationResult operationResult, int longestSize, int longestVersion) {
		for (FileVersion fileVersion : operationResult.getFileTree().values()) {			
			printOneVersion(fileVersion, longestVersion, longestSize);				
		}
	}

	private void printHistories(LsOperationOptions operationOptions, LsOperationResult operationResult, int longestSize, int longestVersion) {
		if (groupedVersions) {
			printGroupedHistories(operationOptions, operationResult, longestSize, longestVersion);			
		}
		else {
			printNonGroupedHistories(operationOptions, operationResult, longestSize, longestVersion);			
		}
	}

	private void printNonGroupedHistories(LsOperationOptions operationOptions, LsOperationResult operationResult, int longestSize, int longestVersion) {
		for (FileVersion fileVersion : operationResult.getFileTree().values()) {
			PartialFileHistory fileHistory = operationResult.getFileVersions().get(fileVersion.getFileHistoryId());
			
			for (FileVersion fileVersionInHistory : fileHistory.getFileVersions().values()) {
				printOneVersion(fileVersionInHistory, longestVersion, longestSize);						
			}					
		}	
	}

	private void printGroupedHistories(LsOperationOptions operationOptions, LsOperationResult operationResult, int longestSize, int longestVersion) {
		Iterator<FileVersion> fileVersionIterator = operationResult.getFileTree().values().iterator();
		
		while (fileVersionIterator.hasNext()) {
			FileVersion fileVersion = fileVersionIterator.next();
			PartialFileHistory fileHistory = operationResult.getFileVersions().get(fileVersion.getFileHistoryId());
			
			out.printf("File %s, %s\n", formatObjectId(fileHistory.getFileHistoryId()), fileVersion.getPath());
			
			for (FileVersion fileVersionInHistory : fileHistory.getFileVersions().values()) {
				if (fileVersionInHistory.equals(fileVersion)) {
					out.print(" * ");
				}
				else {
					out.print("   ");	
				}
				
				printOneVersion(fileVersionInHistory, longestVersion, longestSize);						
			}	
			
			if (fileVersionIterator.hasNext()) {
				out.println();
			}
		}		
	}

	private void printOneVersion(FileVersion fileVersion, int longestVersion, int longestSize) {
		String posixPermissions = (fileVersion.getPosixPermissions() != null) ? fileVersion.getPosixPermissions() : "";
		String dosAttributes = (fileVersion.getDosAttributes() != null) ? fileVersion.getDosAttributes() : "";
		String fileChecksum = formatObjectId(fileVersion.getChecksum());
		String fileHistoryId = formatObjectId(fileVersion.getFileHistoryId());
		String path = (fileVersion.getType() == FileType.SYMLINK) ? fileVersion.getPath() + " -> " + fileVersion.getLinkTarget() : fileVersion.getPath();

		out.printf("%-20s %9s %4s %" + longestSize + "d %8s %" + checksumLength + "s %" + checksumLength + "s %"+longestVersion+"d %s\n", 
				dateFormat.format(fileVersion.getUpdated()), posixPermissions, dosAttributes, fileVersion.getSize(), fileVersion.getType(), 
				fileChecksum, fileHistoryId, fileVersion.getVersion(), path);
	}
	
	private String formatObjectId(ObjectId checksum) {
		if (checksum == null || "".equals(checksum)) {
			return "";
		}
		else {
			return checksum.toString().substring(0, checksumLength);
		}
	}

	private int calculateLongestVersion(Map<String, FileVersion> fileVersions) {
		return calculateLongestValue(fileVersions, new Function<FileVersion, Integer>() {
			public Integer apply(FileVersion fileVersion) {
				return (""+fileVersion.getVersion()).length();
			}
		});	
	}
	
	private int calculateLongestSize(Map<String, FileVersion> fileVersions) {
		return calculateLongestValue(fileVersions, new Function<FileVersion, Integer>() {
			public Integer apply(FileVersion fileVersion) {
				return (""+fileVersion.getSize()).length();
			}
		});	
	}
	
	private int calculateLongestValue(Map<String, FileVersion> fileVersions, Function<FileVersion, Integer> callbackFunction) {
		int result = 0;
		
		for (FileVersion fileVersion : fileVersions.values()) {
			result = Math.max(result, callbackFunction.apply(fileVersion));
		}
		
		return result;	
	}
}
