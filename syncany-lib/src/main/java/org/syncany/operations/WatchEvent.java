/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.operations;

/**
 * @author Vincent Wiencek
 *
 */
public class WatchEvent {
	public static enum WatchEventType {
		UPLOADING, 
		DOWNLOADING, 
		INDEXING ;
	}
	
	private long timestamp;
	private String fileName;
	private WatchEventType type;
	private int batchSize;
	private int progress;

	public WatchEvent(String fileName, WatchEventType type, int progress, int batchSize) {
		this.fileName = fileName;
		this.type = type;
		this.progress = progress;
		this.batchSize = batchSize;
		
		this.timestamp = System.nanoTime();
	}
	
	public String getFileName() {
		return fileName;
	}
	public WatchEventType getType() {
		return type;
	}
	public int getProgress() {
		return progress;
	}
	public int getBatchSize() {
		return batchSize;
	}
	@Override
	public String toString() {
		return "["+timestamp+", "+type+", "+fileName+", "+progress+"/"+batchSize+"]";
	}
}
