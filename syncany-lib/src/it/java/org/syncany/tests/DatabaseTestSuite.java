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
package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.syncany.tests.integration.database.DatabaseReconciliatorTest;
import org.syncany.tests.integration.database.FileVersionComparatorTest;
import org.syncany.tests.integration.database.MemoryDatabaseCacheTest;
import org.syncany.tests.integration.database.PartialFileHistoryTest;
import org.syncany.tests.integration.database.dao.ApplicationDaoTest;
import org.syncany.tests.integration.database.dao.ChunkDaoTest;
import org.syncany.tests.integration.database.dao.DatabaseVersionDaoTest;
import org.syncany.tests.integration.database.dao.FileContentDaoTest;
import org.syncany.tests.integration.database.dao.FileVersionDaoTest;
import org.syncany.tests.integration.database.dao.MultiChunkDaoTest;
import org.syncany.tests.integration.database.dao.XmlDatabaseDaoTest;
import org.syncany.database.ObjectIdTest;
import org.syncany.database.VectorClockTest;

@RunWith(Suite.class)
@SuiteClasses({
	ApplicationDaoTest.class,
	ChunkDaoTest.class,
	DatabaseReconciliatorTest.class,
	DatabaseVersionDaoTest.class,
	FileVersionComparatorTest.class,
	FileVersionDaoTest.class,
	FileContentDaoTest.class,
	MultiChunkDaoTest.class,
	MemoryDatabaseCacheTest.class,
	ObjectIdTest.class,
	PartialFileHistoryTest.class,
	VectorClockTest.class,
	XmlDatabaseDaoTest.class
})
public class DatabaseTestSuite {
	// This class executes all tests	
}
