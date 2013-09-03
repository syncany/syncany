package org.syncany.tests.util;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.syncany.database.Branch;
import org.syncany.database.Database;
import org.syncany.database.DatabaseDAO;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.DatabaseXmlDAO;
import org.syncany.database.VectorClock;

public class TestDatabaseUtil {
	private static Pattern databaseVersionHeaderPattern = Pattern.compile("([^/]+)/\\(([^)]+)\\)/T=?(\\d+)(?:/([^/]+))?");
	private static Pattern vectorClockElementPattern = Pattern.compile("([^\\d]+)(\\d+)");
	
	public static DatabaseVersionHeader createFromString(String databaseVersionHeaderString) throws Exception {
		Matcher databaseVersionHeaderMatcher = databaseVersionHeaderPattern.matcher(databaseVersionHeaderString);
		
		if (!databaseVersionHeaderMatcher.matches()) {
			throw new Exception("Invalid database version header string: "+databaseVersionHeaderString);
		}
		
		String client = databaseVersionHeaderMatcher.group(1);
		String vectorClockString = databaseVersionHeaderMatcher.group(2);
		long databaseVersionHeaderTime = Long.parseLong(databaseVersionHeaderMatcher.group(3));
		String previousClient = (databaseVersionHeaderMatcher.groupCount() > 3) ? databaseVersionHeaderMatcher.group(4) : null;
		
		String[] vectorClockElements = vectorClockString.split(",");		
		VectorClock vectorClock = new VectorClock();
		
		for (String vectorClockElement : vectorClockElements) {
			if ("".equals(vectorClockElement.trim())) {
				continue;
			}
			
			Matcher vectorClockElementMatcher = vectorClockElementPattern.matcher(vectorClockElement);
			
			if (!vectorClockElementMatcher.matches()) {
				throw new Exception("Invalid vector clock element string: "+vectorClockElement);
			}
			
			String vectorClockMachineName = vectorClockElementMatcher.group(1);
			long vectorClockTime = Long.parseLong(vectorClockElementMatcher.group(2));
			
			vectorClock.setClock(vectorClockMachineName, vectorClockTime);
		}
		
		DatabaseVersionHeader newDatabaseVersionHeader = new DatabaseVersionHeader();
		
		newDatabaseVersionHeader.setDate(new Date(databaseVersionHeaderTime));
		newDatabaseVersionHeader.setVectorClock(vectorClock);
		newDatabaseVersionHeader.setClient(client);	
		newDatabaseVersionHeader.setPreviousClient(previousClient); // optional!
		
		return newDatabaseVersionHeader;
	}
	
	public static TreeMap<String, DatabaseVersionHeader> createMapWithMachineKey(String[] keysAndDatabaseVersionHeaderStrings) throws Exception {
		TreeMap<String, DatabaseVersionHeader> databaseVersionHeaderMap = new TreeMap<String, DatabaseVersionHeader>();
		
		for (int i=0; i<keysAndDatabaseVersionHeaderStrings.length; i+=2) {			
			String machineName = keysAndDatabaseVersionHeaderStrings[i];
			DatabaseVersionHeader databaseVersionHeader = createFromString(keysAndDatabaseVersionHeaderStrings[i+1]);

			databaseVersionHeaderMap.put(machineName, databaseVersionHeader);
		}

		return databaseVersionHeaderMap;
	}

	public static Branch createBranch(String[] databaseVersionHeaderStrings) throws Exception {
		Branch branch = new Branch();
		
		for (String databaseVersionHeaderString : databaseVersionHeaderStrings) {
			DatabaseVersionHeader databaseVersionHeader = createFromString(databaseVersionHeaderString);
			branch.add(databaseVersionHeader);
		}

		return branch;
	}
	
	public static Database readDatabaseFileFromDisk(File databaseFile) throws IOException {
		Database db = new Database();
		
		DatabaseDAO dao = new DatabaseXmlDAO();
		dao.load(db, databaseFile);
		
		return db;
	}
	
	public static void writeDatabaseFileToDisk(Database db, File writtenDatabaseFile) throws IOException {
		DatabaseDAO dao = new DatabaseXmlDAO();
		dao.save(db, writtenDatabaseFile);
	}
	
}
