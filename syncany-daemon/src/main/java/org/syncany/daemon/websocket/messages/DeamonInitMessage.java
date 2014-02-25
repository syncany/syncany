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
package org.syncany.daemon.websocket.messages;


/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class DeamonInitMessage extends DaemonAbstractInitMessage {
	private int chunkSize;
	private boolean gzip;
	private boolean encryption;
	private String cipherSpec;
	
	public DeamonInitMessage(DaemonMessage parent) {
		super(parent);
		setAction("create");
	}
	
	public String getCipherSpec() {
		return cipherSpec;
	}
	public void setCipherSpec(String cipherSpec) {
		this.cipherSpec = cipherSpec;
	}
	
	public boolean isGzip() {
		return gzip;
	}
	public void setGzip(boolean gzip) {
		this.gzip = gzip;
	}
	
	public boolean isEncryption() {
		return encryption;
	}
	public void setEncryption(boolean encryption) {
		this.encryption = encryption;
	}
	
	public DeamonInitMessage(DeamonInitMessage buildReturnObject) {
		super(buildReturnObject);
	}
	
	public int getChunkSize() {
		return chunkSize;
	}
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}
}
