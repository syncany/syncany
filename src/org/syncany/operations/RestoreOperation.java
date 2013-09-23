package org.syncany.operations;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileVersion;
import org.syncany.operations.LoadDatabaseOperation.LoadDatabaseOperationResult;

public class RestoreOperation extends Operation {
	private static final Logger logger = Logger.getLogger(RestoreOperation.class.getSimpleName());	
	private RestoreOperationOptions options;
	
	public RestoreOperation() {
		super(null);
		this.options = new RestoreOperationOptions();
	}	

	public RestoreOperation(Config config) {
		this(config, null);
	}	
	
	public RestoreOperation(Config config, RestoreOperationOptions options) {
		super(config);		
		this.options = options;
	}	
	
	public void init(String[] operationArgs) throws Exception {
		options = new RestoreOperationOptions();

		OptionParser parser = new OptionParser();	
		OptionSpec<String> optionDateStr = parser.acceptsAll(asList("d", "date")).withRequiredArg().required();
		OptionSpec<Void> optionForce = parser.acceptsAll(asList("f", "force"));
		
		OptionSet optionSet = parser.parse(operationArgs);	
		
		// --date
		String dateStr = optionSet.valueOf(optionDateStr);
		
		Pattern relativeDatePattern = Pattern.compile("^(\\d+)([smhDWMY])$");
		Pattern absoluteDatePattern = Pattern.compile("^(\\d{2})-(\\d{2})-(\\d{4})$");
		
		Matcher relativeDateMatcher = relativeDatePattern.matcher(dateStr);		
		
		if (relativeDateMatcher.matches()) {
			int time = Integer.parseInt(relativeDateMatcher.group(1));
			String unitStr = relativeDateMatcher.group(2);
			int unitMultiplier = 0;
			
			if ("s".equals(unitStr)) { unitMultiplier = 1; }
			else if ("m".equals(unitStr)) { unitMultiplier = 60; }
			else if ("h".equals(unitStr)) { unitMultiplier = 60*60; }
			else if ("D".equals(unitStr)) { unitMultiplier = 24*60*60; }
			else if ("W".equals(unitStr)) { unitMultiplier = 7*24*60*60; }
			else if ("M".equals(unitStr)) { unitMultiplier = 30*24*60*60; }
			else if ("Y".equals(unitStr)) { unitMultiplier = 365*24*60*60; }
			
			long restoreDateMillies = time*unitMultiplier;
			Date restoreDate = new Date(restoreDateMillies);
			
			logger.log(Level.FINE, "Restore date: "+restoreDate);
			options.setRestoreTime(restoreDate);
		}
		else {
			Matcher absoluteDateMatcher = absoluteDatePattern.matcher(dateStr);
			
			if (absoluteDateMatcher.matches()) {
				int date = Integer.parseInt(absoluteDateMatcher.group(1));
				int month = Integer.parseInt(absoluteDateMatcher.group(2));
				int year = Integer.parseInt(absoluteDateMatcher.group(3));
				
				GregorianCalendar calendar = new GregorianCalendar();
				calendar.set(year, month-1, date);
				
				Date restoreDate = calendar.getTime();
				
				logger.log(Level.FINE, "Restore date: "+restoreDate);
				options.setRestoreTime(restoreDate);
			}
			else {
				throw new Exception("Invalid '--date' argument: "+dateStr);
			}
		}
		
		// --force
		if (optionSet.has(optionForce)) {
			options.setForce(true);
		}
		else {
			options.setForce(false);
		}
		
		// Files
		List<?> nonOptionArgs = optionSet.nonOptionArguments();
		List<String> restoreFilePaths = new ArrayList<String>();
		
		for (Object nonOptionArg : nonOptionArgs) {
			restoreFilePaths.add(nonOptionArg.toString());
		}

		options.setRestoreFilePaths(restoreFilePaths);		
	}
		
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Restore' at client "+config.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");
		
		Database database = ((LoadDatabaseOperationResult) new LoadDatabaseOperation(config).execute()).getDatabase();		
		DatabaseVersion currentDatabaseVersion = database.getLastDatabaseVersion();
		
		if (currentDatabaseVersion == null) {
			throw new Exception("No database versions yet locally. Nothing to revert.");
		}

		Date restoreDate = options.getRestoreTime();
		List<DatabaseVersion> restoreDatabaseVersions = findDatabaseVersionBeforeRestoreTime(database, restoreDate);
		
		List<String> restoreFilePaths = options.getRestoreFilePaths();
		List<FileVersion> restoreFileVersions = findRestoreFileVersions(database, restoreDatabaseVersions, restoreFilePaths);
		
		return new RestoreOperationResult();
	}	
	
	
	
	private List<FileVersion> findRestoreFileVersions(Database database, List<DatabaseVersion> restoreDatabaseVersions, List<String> restoreFilePaths) {
		Set<Long> usedRestoreFileHistoryIds = new HashSet<Long>();
		List<String> leftOverRestoreFilePaths = new ArrayList<String>(restoreFilePaths);

		List<FileVersion> restoreFileVersions = new ArrayList<FileVersion>();
		
		for (int i=restoreDatabaseVersions.size()-1; i>=0; i--) {
			DatabaseVersion currentDatabaseVersion = restoreDatabaseVersions.get(i);
			List<String> removeRestoreFilePaths = new ArrayList<String>();
			
			for (String restoreFilePath : leftOverRestoreFilePaths) {
				//currentDatabaseVersion.getF
				throw new RuntimeException("Not yet implemented.");
			}
		}
		
		return restoreFileVersions;
	}

	private List<DatabaseVersion> findDatabaseVersionBeforeRestoreTime(Database database, Date restoreDate) {
		List<DatabaseVersion> earlierOrEqualDatabaseVersions = new ArrayList<DatabaseVersion>();
		
		for (DatabaseVersion compareDatabaseVersion : database.getDatabaseVersions()) {
			if (compareDatabaseVersion.getTimestamp().equals(restoreDate) || compareDatabaseVersion.getTimestamp().before(restoreDate)) {
				earlierOrEqualDatabaseVersions.add(compareDatabaseVersion);
			}
		}
		
		return earlierOrEqualDatabaseVersions;
	}



	public static class RestoreOperationOptions implements OperationOptions {
		private Date restoreTime;
		private List<String> restoreFilePaths;
		private boolean force;
		
		public Date getRestoreTime() {
			return restoreTime;
		}
		
		public void setRestoreTime(Date restoreTime) {
			this.restoreTime = restoreTime;
		}
		
		public List<String> getRestoreFilePaths() {
			return restoreFilePaths;
		}
		
		public void setRestoreFilePaths(List<String> restoreFiles) {
			this.restoreFilePaths = restoreFiles;
		}

		public boolean isForce() {
			return force;
		}

		public void setForce(boolean force) {
			this.force = force;
		}					
	}
	
	public class RestoreOperationResult implements OperationResult {
		// Fressen
	}
}
