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
package org.syncany.database;

import java.io.File;
import java.io.IOException;

/**
 * The database DAO represents the database peristence layer abstraction.
 * It can be used to persist a database from memory to the disk, or load a
 * database from a file.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public interface DatabaseDAO {
	public void save(Database db, File destinationFile) throws IOException;	
	public void save(Database db, DatabaseVersion versionFrom, DatabaseVersion versionTo, File destinationFile) throws IOException;
	
	public void load(Database db, File databaseFile) throws IOException;
	public void load(Database db, File databaseFile, boolean headersOnly) throws IOException;
	public void load(Database db, File databaseFile, VectorClock fromVersion, VectorClock toVersion) throws IOException;
	public void load(Database db, File databaseFile, VectorClock fromVersion, VectorClock toVersion, boolean headersOnly) throws IOException;
}
