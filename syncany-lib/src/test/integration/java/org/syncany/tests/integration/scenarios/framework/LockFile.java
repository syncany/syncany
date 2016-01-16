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
package org.syncany.tests.integration.scenarios.framework;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

public class LockFile extends AbstractClientAction {
	public static final String STATE_KEY_RANDOM_ACCESS_FILE = "FileLock.randomAccessFile";
	public static final String STATE_KEY_FILE_LOCK = "FileLock.fileLock";
	
	@Override
	public void execute() throws Exception {
		if (state.containsKey("lockedFile")) {
			throw new Exception("Currently only one file can be locked at a time.");
		}
		
		File file = pickFile(7878);
		
		log(this, file.getAbsolutePath());
		
		RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
		FileLock fileLock = randomAccessFile.getChannel().lock();
		
		state.put(STATE_KEY_RANDOM_ACCESS_FILE, randomAccessFile);
		state.put(STATE_KEY_FILE_LOCK, fileLock);
		
		// Do not (!) close this file
	}		
}
