/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.scenarios.framework;

import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

public class UnlockFile extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		Object randomAccessFileObj = state.get(LockFile.STATE_KEY_RANDOM_ACCESS_FILE);
		Object fileLockObj = state.get(LockFile.STATE_KEY_FILE_LOCK);
		
		if (randomAccessFileObj == null || fileLockObj == null) {
			throw new Exception("File must be locked first before trying to unlock");
		}
		
		RandomAccessFile randomAccessFile = (RandomAccessFile) randomAccessFileObj;
		FileLock fileLock = (FileLock) fileLockObj;
		
		//log(this, randomAccessFile.getAbsolutePath());
		fileLock.release();
		fileLock.close();
		randomAccessFile.close();		
	}		
}
