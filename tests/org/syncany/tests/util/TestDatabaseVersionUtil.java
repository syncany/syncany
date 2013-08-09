package org.syncany.tests.util;

import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.VectorClock;

public class TestDatabaseVersionUtil {
	private static Pattern databaseVersionHeaderPattern = Pattern.compile("([^/]+)/\\(([^)]+)\\)/T=(\\d+)");
	private static Pattern vectorClockElementPattern = Pattern.compile("([^\\d]+)(\\d+)");
	
	public static DatabaseVersionHeader createFromString(String databaseVersionHeaderString) throws Exception {
		Matcher databaseVersionHeaderMatcher = databaseVersionHeaderPattern.matcher(databaseVersionHeaderString);
		
		if (!databaseVersionHeaderMatcher.matches()) {
			throw new Exception("Invalid database version header string: "+databaseVersionHeaderString);
		}
		
		String uploadedbyClient = databaseVersionHeaderMatcher.group(1);
		String vectorClockString = databaseVersionHeaderMatcher.group(2);
		long databaseVersionHeaderTime = Long.parseLong(databaseVersionHeaderMatcher.group(3));
		
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
		
		return new DatabaseVersionHeader(new Date(databaseVersionHeaderTime), vectorClock, uploadedbyClient);			
	}
	
	public static TreeMap<Long, DatabaseVersionHeader> createDatabaseVersionHeaderMapWithTimeKey(String[] databaseVersionHeaderStrings) throws Exception {
		TreeMap<Long, DatabaseVersionHeader> databaseVersionHeaderMap = new TreeMap<Long, DatabaseVersionHeader>();
		
		for (String databaseVersionHeaderString : databaseVersionHeaderStrings) {
			DatabaseVersionHeader databaseVersionHeader = createFromString(databaseVersionHeaderString);
			databaseVersionHeaderMap.put(databaseVersionHeader.getDate().getTime(), databaseVersionHeader);
		}

		return databaseVersionHeaderMap;
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
	
}
