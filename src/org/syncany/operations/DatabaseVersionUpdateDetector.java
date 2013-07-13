package org.syncany.operations;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.VectorClock;
import org.syncany.database.VectorClock.VectorClockComparison;

public class DatabaseVersionUpdateDetector {
	private static final Logger logger = Logger.getLogger(DatabaseVersionUpdateDetector.class.getSimpleName());
	
	public DatabaseVersionHeader findLastCommonDatabaseVersionHeader(TreeMap<Long, DatabaseVersionHeader> localDatabaseVersionHeaders, TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> remoteDatabaseVersionHeaders) {
		DatabaseVersionHeader lastCommonDatabaseVersionHeader = null; 
				
		for (Long currentLocalDatabaseKey = localDatabaseVersionHeaders.lastKey(); currentLocalDatabaseKey != null && lastCommonDatabaseVersionHeader == null; currentLocalDatabaseKey = localDatabaseVersionHeaders.lowerKey(currentLocalDatabaseKey)){
			DatabaseVersionHeader currentLocalDatabaseVersionHeader = localDatabaseVersionHeaders.get(currentLocalDatabaseKey);
			VectorClock currentVectorClock = currentLocalDatabaseVersionHeader.getVectorClock();
			
			if(isKeyInAllRemoteDatabasesGreaterOrEqual(currentVectorClock,remoteDatabaseVersionHeaders)) {
				lastCommonDatabaseVersionHeader = currentLocalDatabaseVersionHeader;
			}
		}
		
		return lastCommonDatabaseVersionHeader;
	}

	private boolean isKeyInAllRemoteDatabasesGreaterOrEqual(VectorClock currentVectorClock,
			TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> remoteDatabaseVersionHeaders) {
		Set<String> clients = remoteDatabaseVersionHeaders.keySet();
		Map<String, Boolean> foundInClientMatrix = initializeFoundInClientMatrix(clients);
		
		for (Map.Entry<String,TreeMap<Long,DatabaseVersionHeader>> remoteClientDatabaseVersionsSet : remoteDatabaseVersionHeaders.entrySet()) {
			String currentRemoteClient = remoteClientDatabaseVersionsSet.getKey();
			TreeMap<Long,DatabaseVersionHeader> remoteClientDatabaseVersions = remoteClientDatabaseVersionsSet.getValue();
			
			for (DatabaseVersionHeader remoteDatabaseVersionHeader : remoteClientDatabaseVersions.values()) {
				VectorClock remoteVectorClock = remoteDatabaseVersionHeader.getVectorClock();
				VectorClockComparison result = VectorClock.compare(remoteVectorClock,currentVectorClock);
				if(result == VectorClockComparison.GREATER || result == VectorClockComparison.EQUAL) {
					foundInClientMatrix.put(currentRemoteClient,true);
					break;
				}
			}
			
			if(foundInClientMatrix.get(currentRemoteClient) == false) {
				return false;
			}
		}
		
		return isFoundInClientMatrixFullyTrue(foundInClientMatrix);
	}

	private Map<String, Boolean> initializeFoundInClientMatrix(Set<String> clients) {
		Map<String, Boolean> foundInClientMatrix = new HashMap<String, Boolean>();
		for (String client : clients) {
			foundInClientMatrix.put(client, false);
		}
		return foundInClientMatrix;
	}

	private boolean isFoundInClientMatrixFullyTrue(Map<String, Boolean> foundInClientMatrix) {
		for (Boolean isFound : foundInClientMatrix.values()) {
			if(isFound == false) {
				return false;
			}
		}
		return true;
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
