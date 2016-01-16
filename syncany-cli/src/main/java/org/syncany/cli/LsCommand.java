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
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.cli.util.CommandLineUtil;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.ObjectId;
import org.syncany.database.PartialFileHistory;
import org.syncany.operations.OperationResult;
import org.syncany.operations.ls.LsOperation;
import org.syncany.operations.ls.LsOperationOptions;
import org.syncany.operations.ls.LsOperationResult;

import com.google.common.base.Function;

public class LsCommand extends Command {	
	protected static final Logger logger = Logger.getLogger(LsCommand.class.getSimpleName());
	
	private static final int CHECKSUM_LENGTH_LONG = 40;
	private static final int CHECKSUM_LENGTH_SHORT = 10;	
	private static final String DATE_FORMAT_PATTERN = "yy-MM-dd HH:mm:ss";
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_PATTERN);
	
	private int checksumLength;
	private boolean groupedVersions;
	private boolean fetchHistories;
	
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
		LsOperationOptions operationOptions = parseOptions(operationArgs);
		LsOperationResult operationResult = new LsOperation(config, operationOptions).execute();

		printResults(operationResult);

		return 0;
	}	

	@Override
	public LsOperationOptions parseOptions(String[] operationArgs) throws Exception {
		LsOperationOptions operationOptions = new LsOperationOptions();

		OptionParser parser = new OptionParser();
		parser.allowsUnrecognizedOptions();
		
		OptionSpec<String> optionDateStr = parser.acceptsAll(asList("D", "date")).withRequiredArg();
		OptionSpec<Void> optionRecursive = parser.acceptsAll(asList("r", "recursive"));
		OptionSpec<String> optionFileTypes = parser.acceptsAll(asList("t", "types")).withRequiredArg();
		OptionSpec<Void> optionLongChecksums = parser.acceptsAll(asList("f", "full-checksums"));
		OptionSpec<Void> optionWithVersions = parser.acceptsAll(asList("V", "versions"));
		OptionSpec<Void> optionGroupedVersions = parser.acceptsAll(asList("g", "group"));
		OptionSpec<Void> optionFileHistoryId = parser.acceptsAll(asList("H", "file-history"));
		OptionSpec<Void> optionDeleted = parser.acceptsAll(asList("q", "deleted"));

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
			HashSet<FileType> fileTypes = new HashSet<>();
			
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
		fetchHistories = options.has(optionWithVersions) || options.has(optionFileHistoryId);
		operationOptions.setFetchHistories(fetchHistories);

		// --file-history
		operationOptions.setFileHistoryId(options.has(optionFileHistoryId));
		
		// --long-checksums (display option)
		checksumLength = (options.has(optionLongChecksums)) ? CHECKSUM_LENGTH_LONG : CHECKSUM_LENGTH_SHORT;

		// --group (display option)
		groupedVersions = options.has(optionGroupedVersions);
		
		// --deleted
		operationOptions.setDeleted(options.has(optionDeleted));
		
		// <path-expr>
		List<?> nonOptionArgs = options.nonOptionArguments();
		
		if (nonOptionArgs.size() > 0) {
			operationOptions.setPathExpression(nonOptionArgs.get(0).toString());
		}

		return operationOptions;
	}

	@Override
	public void printResults(OperationResult operationResult) {
		LsOperationResult concreteOperationResult = (LsOperationResult) operationResult;
		
		int longestSize = calculateLongestSize(concreteOperationResult.getFileList());
		int longestVersion = calculateLongestVersion(concreteOperationResult.getFileList());

		if (fetchHistories) {
			printHistories(concreteOperationResult, longestSize, longestVersion);				
		}
		else {
			printTree(concreteOperationResult, longestSize, longestVersion);			
		}
	}
	
	private void printTree(LsOperationResult operationResult, int longestSize, int longestVersion) {
		for (FileVersion fileVersion : operationResult.getFileList()) {			
			printOneVersion(fileVersion, longestVersion, longestSize);				
		}
	}

	private void printHistories(LsOperationResult operationResult, int longestSize, int longestVersion) {
		if (groupedVersions) {
			printGroupedHistories(operationResult, longestSize, longestVersion);			
		}
		else {
			printNonGroupedHistories(operationResult, longestSize, longestVersion);			
		}
	}

	private void printNonGroupedHistories(LsOperationResult operationResult, int longestSize, int longestVersion) {
		for (FileVersion fileVersion : operationResult.getFileList()) {
			PartialFileHistory fileHistory = operationResult.getFileVersions().get(fileVersion.getFileHistoryId());
			
			for (FileVersion fileVersionInHistory : fileHistory.getFileVersions().values()) {
				printOneVersion(fileVersionInHistory, longestVersion, longestSize);						
			}					
		}	
	}

	private void printGroupedHistories(LsOperationResult operationResult, int longestSize, int longestVersion) {
		Iterator<FileVersion> fileVersionIterator = operationResult.getFileList().iterator();
		
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
		String fileStatus = formatFileStatusShortStr(fileVersion.getStatus());
		String fileType = formatFileTypeShortStr(fileVersion.getType());
		String posixPermissions = (fileVersion.getPosixPermissions() != null) ? fileVersion.getPosixPermissions() : "";
		String dosAttributes = (fileVersion.getDosAttributes() != null) ? fileVersion.getDosAttributes() : "";
		String fileChecksum = formatObjectId(fileVersion.getChecksum());
		String fileHistoryId = formatObjectId(fileVersion.getFileHistoryId());
		String path = (fileVersion.getType() == FileType.SYMLINK) ? fileVersion.getPath() + " -> " + fileVersion.getLinkTarget() : fileVersion.getPath();

		out.printf("%s %s %s %9s %4s %" + longestSize + "d %" + checksumLength + "s %" + checksumLength + "s %"+longestVersion+"d %s\n", 
				DATE_FORMAT.format(fileVersion.getUpdated()), fileStatus, fileType, posixPermissions, dosAttributes, fileVersion.getSize(), 
				fileChecksum, fileHistoryId, fileVersion.getVersion(), path);
	}
	
	private String formatFileStatusShortStr(FileStatus status) {
		switch (status) {
		case NEW:
			return "A";
			
		case CHANGED:
		case RENAMED:
			return "M";
			
		case DELETED:
			return "D";
			
		default:
			return "?";
		}
	}

	private String formatFileTypeShortStr(FileType type) {
		switch (type) {
		case FILE:
			return "-";
			
		case FOLDER: 
			return "d";
			
		case SYMLINK:
			return "s";
			
		default:
			return "?";				
		}
	}

	private String formatObjectId(ObjectId checksum) {
		if (checksum == null || "".equals(checksum)) {
			return "";
		}
		else {
			return checksum.toString().substring(0, checksumLength);
		}
	}

	private int calculateLongestVersion(List<FileVersion> fileVersions) {
		return calculateLongestValue(fileVersions, new Function<FileVersion, Integer>() {
			public Integer apply(FileVersion fileVersion) {
				return (""+fileVersion.getVersion()).length();
			}
		});	
	}
	
	private int calculateLongestSize(List<FileVersion> fileVersions) {
		return calculateLongestValue(fileVersions, new Function<FileVersion, Integer>() {
			public Integer apply(FileVersion fileVersion) {
				return (""+fileVersion.getSize()).length();
			}
		});	
	}
	
	private int calculateLongestValue(List<FileVersion> fileVersions, Function<FileVersion, Integer> callbackFunction) {
		int result = 0;
		
		for (FileVersion fileVersion : fileVersions) {
			result = Math.max(result, callbackFunction.apply(fileVersion));
		}
		
		return result;	
	}
	
	protected Date parseDateOption(String dateStr) throws Exception {
		Pattern relativeDatePattern = Pattern.compile("(\\d+(?:[.,]\\d+)?)(mo|[smhdwy])");		
		Matcher relativeDateMatcher = relativeDatePattern.matcher(dateStr);		
		
		if (relativeDateMatcher.find()) {
			long restoreDateMillies = CommandLineUtil.parseTimePeriod(dateStr)*1000;
			
			Date restoreDate = new Date(System.currentTimeMillis()-restoreDateMillies);
			
			logger.log(Level.FINE, "Restore date: "+restoreDate);
			return restoreDate;
		}
		else {
			try {
				Date restoreDate = DATE_FORMAT.parse(dateStr);
				
				logger.log(Level.FINE, "Restore date: "+restoreDate);
				return restoreDate;
			}
			catch (Exception e) {
				throw new Exception("Invalid '--date' argument: " + dateStr + ", use relative date or absolute format: " + DATE_FORMAT_PATTERN);
			}
		}		
	}
}
