package org.syncany.operations;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.VectorClock;
import org.syncany.database.VectorClock.VectorClockComparison;

public class DatabaseVersionUpdateDetector {
	private static final Logger logger = Logger.getLogger(DatabaseVersionUpdateDetector.class.getSimpleName());
	
	public DatabaseVersionHeader findLastCommonDatabaseVersionHeader(TreeMap<Long, DatabaseVersionHeader> localDatabaseVersionHeaders, TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> remoteDatabaseVersionHeaders) {
		//Map<String, Long> currentRemoteKeys = new HashMap<String, Long>();
		Long currentLocalKey = localDatabaseVersionHeaders.lastKey(); 		
		/*
		System.out.println("- Last local: "+localDatabaseVersionHeaders.get(currentLocalKey)+" (index: "+currentLocalKey+")");
		for (String machineName : remoteDatabaseVersionHeaders.keySet()) {
			currentRemoteKeys.put(machineName, remoteDatabaseVersionHeaders.get(machineName).lastKey());
			System.out.println("- Last "+machineName+": "+remoteDatabaseVersionHeaders.get(machineName).lastEntry().getValue()+" (index: "+remoteDatabaseVersionHeaders.get(machineName).lastKey()+")");
		}
		*/
		nextRemoteMachine: for (String remoteMachineName : remoteDatabaseVersionHeaders.keySet()) {
			System.out.println("Comparing local to "+remoteMachineName);
			Long currentRemoteKey = remoteDatabaseVersionHeaders.get(remoteMachineName).lastKey();
			
			nextLowestRemoteMachineKey: while (currentLocalKey != null && currentRemoteKey != null) {
				
				DatabaseVersionHeader currentRemoteDatabaseVersionHeader = remoteDatabaseVersionHeaders.get(remoteMachineName).get(currentRemoteKey);
				DatabaseVersionHeader currentLocalDatabaseVersionHeader = localDatabaseVersionHeaders.get(currentLocalKey);
				
				VectorClockComparison comparisonResult = VectorClock.compare(currentLocalDatabaseVersionHeader.getVectorClock(), currentRemoteDatabaseVersionHeader.getVectorClock());
				System.out.printf("- Comparison: %s <-> %s = %s\n", new Object[] { currentLocalDatabaseVersionHeader, currentRemoteDatabaseVersionHeader, comparisonResult });

				if (comparisonResult == VectorClockComparison.SMALLER) {
					currentRemoteKey = remoteDatabaseVersionHeaders.get(remoteMachineName).lowerKey(currentRemoteKey);
					//currentRemoteKeys.put(remoteMachineName, currentRemoteKey);
				}
				else if (comparisonResult == VectorClockComparison.GREATER) {
					currentLocalKey = localDatabaseVersionHeaders.lowerKey(currentLocalKey);
				}
				else if (comparisonResult == VectorClockComparison.SIMULTANEOUS) {
					Long previousRemoteKey = remoteDatabaseVersionHeaders.get(remoteMachineName).lowerKey(currentRemoteKey);
					Long previousLocalKey = localDatabaseVersionHeaders.lowerKey(currentLocalKey);					
					System.out.println("aa---"+previousLocalKey+"---"+previousRemoteKey);
					
					if (previousLocalKey == null || previousRemoteKey == null) {
						System.out.println("break: "+previousLocalKey+"//"+previousRemoteKey);
						break nextLowestRemoteMachineKey;
					}

					DatabaseVersionHeader previousLocalDatabaseVersionHeader = localDatabaseVersionHeaders.get(previousLocalKey);
					DatabaseVersionHeader previousRemoteDatabaseVersionHeader = remoteDatabaseVersionHeaders.get(remoteMachineName).get(previousRemoteKey);
					
					VectorClockComparison previousLocalToCurrentRemoteComparisonResult = VectorClock.compare(previousLocalDatabaseVersionHeader.getVectorClock(), currentRemoteDatabaseVersionHeader.getVectorClock());
					VectorClockComparison previousRemoteToCurrentLocalComparisonResult = VectorClock.compare(previousRemoteDatabaseVersionHeader.getVectorClock(), currentLocalDatabaseVersionHeader.getVectorClock());
					System.out.printf("  + Cross comparison 1: %s <-> %s = %s\n", new Object[] { previousLocalDatabaseVersionHeader, currentRemoteDatabaseVersionHeader, previousLocalToCurrentRemoteComparisonResult });
					System.out.printf("  + Cross comparison 2: %s <-> %s = %s\n", new Object[] { previousRemoteDatabaseVersionHeader, currentLocalDatabaseVersionHeader, previousRemoteToCurrentLocalComparisonResult });
					
					if (previousLocalToCurrentRemoteComparisonResult != VectorClockComparison.SIMULTANEOUS) {
						if (previousLocalToCurrentRemoteComparisonResult == VectorClockComparison.SMALLER) {
							System.out.println("xx");
							currentLocalKey = localDatabaseVersionHeaders.lowerKey(currentLocalKey);
						}
						else if (previousLocalToCurrentRemoteComparisonResult == VectorClockComparison.GREATER) {
							System.out.println("yy");
							currentRemoteKey = remoteDatabaseVersionHeaders.get(remoteMachineName).lowerKey(currentRemoteKey);							
						}
						else if (previousLocalToCurrentRemoteComparisonResult == VectorClockComparison.EQUAL) {
							System.out.println("zz");
							currentLocalKey = localDatabaseVersionHeaders.lowerKey(currentLocalKey);
							break nextLowestRemoteMachineKey;
						}
						else {
							System.out.println("THIS SHOULD NOT HAPPEN");
						}
					}
					
					else if (previousRemoteToCurrentLocalComparisonResult != VectorClockComparison.SIMULTANEOUS) {
						if (previousRemoteToCurrentLocalComparisonResult == VectorClockComparison.SMALLER) {
							System.out.println("xx2");
							currentRemoteKey = remoteDatabaseVersionHeaders.get(remoteMachineName).lowerKey(currentRemoteKey);
						}
						else if (previousRemoteToCurrentLocalComparisonResult == VectorClockComparison.GREATER) {
							System.out.println("yy2");
							currentLocalKey = localDatabaseVersionHeaders.lowerKey(currentLocalKey);
						}
						else if (previousRemoteToCurrentLocalComparisonResult == VectorClockComparison.EQUAL) {
							System.out.println("zz2");
							currentRemoteKey = remoteDatabaseVersionHeaders.get(remoteMachineName).lowerKey(currentRemoteKey);
							break nextLowestRemoteMachineKey;
						}
						else {
							System.out.println("THIS SHOULD NOT HAPPEN");
						}
					}
					
					else {
						System.out.println("previous versions are all in conflict");
						currentRemoteKey = remoteDatabaseVersionHeaders.get(remoteMachineName).lowerKey(currentRemoteKey);
						currentLocalKey = localDatabaseVersionHeaders.lowerKey(currentLocalKey);
					}
				}
				else if (comparisonResult == VectorClockComparison.EQUAL) {
					System.out.println("equal");
					continue nextRemoteMachine;
				}
			}
		}
		
		if (currentLocalKey == null) {
			return null;
		}
		
		return localDatabaseVersionHeaders.get(currentLocalKey);
	}
	
	public Map<String, DatabaseVersionHeader> findFirstConflictingDatabaseVersionHeader(DatabaseVersionHeader lastCommonHeader, String localName, TreeMap<Long,DatabaseVersionHeader> localDatabaseVersionHeaders, Map<String, TreeMap<Long, DatabaseVersionHeader>> remoteDatabaseVersionHeaders) {
		Map<String, DatabaseVersionHeader> firstConflictingDatabaseVersionHeaders = new HashMap<String, DatabaseVersionHeader>();
		
		boolean next = false;
		for (DatabaseVersionHeader databaseVersionHeader : localDatabaseVersionHeaders.values()) {
			if (next) {
				firstConflictingDatabaseVersionHeaders.put(localName, databaseVersionHeader);
				break;
			}
			else if (databaseVersionHeader.equals(lastCommonHeader)) {
				next = true;
			}			
		}
		
		for (Map.Entry<String, TreeMap<Long, DatabaseVersionHeader>> entry : remoteDatabaseVersionHeaders.entrySet()) {
			String thisRemoteMachineName = entry.getKey();
			TreeMap<Long, DatabaseVersionHeader> thisRemoteDatabaseVersionHeaders = entry.getValue();
			
			boolean next2 = false;
			for (DatabaseVersionHeader databaseVersionHeader : thisRemoteDatabaseVersionHeaders.values()) {
				if (next2) {
					firstConflictingDatabaseVersionHeaders.put(thisRemoteMachineName, databaseVersionHeader);
					break;
				}
				else if (databaseVersionHeader.equals(lastCommonHeader)) {
					next2 = true;
				}			
			}
			
			if (next2 == false) { // Nothing found. Add first as conflict 
				firstConflictingDatabaseVersionHeaders.put(thisRemoteMachineName, thisRemoteDatabaseVersionHeaders.firstEntry().getValue());
			}
		}
		
		return firstConflictingDatabaseVersionHeaders;
	}
	
	public Map<String, DatabaseVersionHeader> findWinningFirstConflictingDatabaseVersionHeaders(Map<String, DatabaseVersionHeader> databaseVersionHeaders) {
		// TODO this method curently does not catch the scenario in which two first winning conflict headers have the same timestamp, this could be baaad, though very unlikely
		DatabaseVersionHeader winningFirstConflictingDatabaseVersionHeader = null;
		
		for (DatabaseVersionHeader databaseVersionHeader : databaseVersionHeaders.values()) {
			if (winningFirstConflictingDatabaseVersionHeader == null) {
				winningFirstConflictingDatabaseVersionHeader = databaseVersionHeader;
			}
			else if (databaseVersionHeader.getUploadedDate().before(winningFirstConflictingDatabaseVersionHeader.getUploadedDate())) {
				winningFirstConflictingDatabaseVersionHeader = databaseVersionHeader;				
			}
		}
		
		Map<String, DatabaseVersionHeader> winningFirstConflictingDatabaseVersionHeaders = new HashMap<String, DatabaseVersionHeader>();

		for (Map.Entry<String, DatabaseVersionHeader> entry : databaseVersionHeaders.entrySet()) {
			if (winningFirstConflictingDatabaseVersionHeader.equals(entry.getValue())) {
				winningFirstConflictingDatabaseVersionHeaders.put(entry.getKey(), entry.getValue());
			}
		}
		
		return winningFirstConflictingDatabaseVersionHeaders;
	}
	
}
