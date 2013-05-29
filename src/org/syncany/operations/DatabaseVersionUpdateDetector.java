package org.syncany.operations;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.syncany.database.DatabaseVersion;
import org.syncany.database.VectorClock;

public class DatabaseVersionUpdateDetector {
	public enum UpdateDetectionStatus { POSSIBLE_CONFLICT, NEWER_VERSION, NO_UPDATES };  

	private Set<DatabaseVersionHeaderPair> possibleConflicts;
	private VectorClock newestClock;
				
	public DatabaseVersionUpdateDetector() {
		possibleConflicts = new HashSet<DatabaseVersionHeaderPair>();
		newestClock = null;
	}
	
	public UpdateDetectionStatus detectUpdates(VectorClock... vectorClocks) {
		return detectUpdates2_BROKEN(Arrays.asList(vectorClocks));
	}	
	
	public UpdateDetectionStatus detectUpdates2_BROKEN(List<VectorClock> vectorClocks) {
		return UpdateDetectionStatus.NEWER_VERSION;
		/*
		Set<DatabaseVersion>
		nonConflictingDatabaseVersions = ...;
		conflictingDatabaseVersions = ...;
		conflictingDatabaseVersionPairs = ...;
		*/
		// 1. compare them all and add all conflicting to conflictingDatabaseVersions and conflictingDatabaseVersionPairs
		// 2. add all non-conflicting versions to nonConflictingDatabaseVersions
		// 3. sort nonConflictingDatabaseVersions --> these can be applied without conflict issues (later!)
		// 4. take 
		
		/*
		 A  B  C
		 |  |  |
		 |  |  |
		 x  y  x   // Conflict, A/C wins, choose A
		 |  i  i   // Ignore B and C databases 
		 |  i
		 |
		 
		   |        non conflicting (these can be applied without issues later)
		   |
		   |
		   |
		  _|_____   first conflict
	     |   |   |
		 |   |
		 
		 
		 */
	}
	/*
	public UpdateDetectionStatus detectUpdatesBROKEN(DatabaseVersion localDatabaseVersion, Map<DatabaseVersion, VectorClock> vectorClocks) {
		possibleConflicts.clear();
		newestClock = null;		
		
		for (Map.Entry<DatabaseVersion, VectorClock> outerVectorClockEntry : vectorClocks.entrySet()) {
			for (Map.Entry<DatabaseVersion, VectorClock> innerVectorClockEntry : vectorClocks.entrySet()) {
				VectorClock clock1 = outerVectorClockEntry.getValue();
				VectorClock clock2 = innerVectorClockEntry.getValue();
				
				DatabaseVersionPair databaseVersionPair = new DatabaseVersionPair(firstDatabaseVersion, secondDatabaseVersion)
			}
		}
		
		for (int i=0; i<vectorClocks.size(); i++) { // compare all clocks to each other
			for (int j=i+1; j<vectorClocks.size(); j++) {
				if (j != i) {
					VectorClock clock1 = vectorClocks.get(i);XXX
					VectorClock clock2 = vectorClocks.get(j);XXXXXXXXXX
					
					VectorClockComparison vectorClockComparison = VectorClock.compare(clock1, clock2);
					
					if (vectorClockComparison == VectorClockComparison.SIMULTANEOUS) {
						possibleConflicts.add(new DatabaseVersionConflict(clock1, clock2));
					}
					else if (vectorClockComparison == VectorClockComparison.SMALLER) {
						newestClock = clock2;
					}
					else {
						newestClock = clock1;						
					}
				}
			}
		}
		
		if (possibleConflicts.size() > 0) {
			return UpdateDetectionStatus.POSSIBLE_CONFLICT;
		}
		else if (newestClockIsNewerThanLocalClock) {
			return UpdateDetectionStatus.NEWER_VERSION;
		}
		else {
			return UpdateDetectionStatus.NO_UPDATES;
		}
		/*for (VectorClockConflict vectorClockConflict : possibleConflicts) {
			System.out.println("conflict: "+vectorClockConflict.clock1+" and "+vectorClockConflict.clock2);
		}*/		
	/*}

	public Set<DatabaseVersionConflict> getPossibleConflicts() {
		return possibleConflicts;
	}

	public DatabaseVersion getNewestDatabaseVersion() {
		return newestClock;
	}	*/
	
	/*
	public Object reconcile(Map<Object, VectorClock> elements) {
		VectorClock outerVectorClock = null;
		
		for (Map.Entry<Object, VectorClock> element : elements.entrySet()) {
			VectorClock innerVectorClock = element.getValue();
			
			if (outerVectorClock == null) {
				outerVectorClock = innerVectorClock;
				continue;
			}
			
			
		}
	}*/
}
