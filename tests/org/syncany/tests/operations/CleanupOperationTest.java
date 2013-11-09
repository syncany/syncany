/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.operations;

import java.util.ArrayList;
import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.operations.CleanupOperation;
import org.syncany.operations.CleanupOperation.CleanupOperationOptions;
import org.syncany.operations.CleanupOperation.CleanupOperationStrategy;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestDatabaseUtil;

public class CleanupOperationTest {

	Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT-1"));
	
	@Test
	public void testIdentifyDatabaseVersions() throws Exception { 
		Config config = TestConfigUtil.createTestLocalConfig();

		Database database = new Database();

		List<DatabaseVersion> olderDatabaseVersions = createConsistentDatabaseVersions(5, 5, -40, null);
		List<DatabaseVersion> newerDatabaseVersions = createConsistentDatabaseVersions(5, 5, 40, olderDatabaseVersions.get(olderDatabaseVersions.size()-1));

		database.addDatabaseVersions(olderDatabaseVersions);
		database.addDatabaseVersions(newerDatabaseVersions);
		
		CleanupOperation operation = new CleanupOperation(config, database);
		CleanupOperationOptions options = new CleanupOperationOptions();
		options.setCleanUpOlderThanDays(30);
		options.setStrategy(CleanupOperationStrategy.DAYRANGE);
		List<DatabaseVersion> identifiedDatabaseVersions = operation.identifyDatabaseVersions(options);
		
		assertEquals(identifiedDatabaseVersions, olderDatabaseVersions);
	}
	
	private List<DatabaseVersion> createConsistentDatabaseVersions(int amount, int minuteOffset, int dayOffset, DatabaseVersion basedOn) {
		List<DatabaseVersion> databaseVersions = new ArrayList<DatabaseVersion>();
		DatabaseVersion dbv = basedOn;
		calendar.add(Calendar.DATE, dayOffset);  			
		for(int i=0; i < amount; i++) {
			calendar.add(Calendar.MINUTE, minuteOffset);  
			Date dbvDate = calendar.getTime();
			dbv = TestDatabaseUtil.createDatabaseVersion(dbv, dbvDate);
			databaseVersions.add(dbv);
		}
		return databaseVersions;
	}
}
