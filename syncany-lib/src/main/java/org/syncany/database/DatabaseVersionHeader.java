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
package org.syncany.database;

import java.util.Date;

/**
 * The database version header identifies a {@link DatabaseVersion} using
 * a {@link VectorClock}, a local timestamp and the client's machine name.
 *
 * <p>The database version header plays a significant role in the
 * reconciliation process, in particular when trying to find a winning
 * branch.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class DatabaseVersionHeader {
	private Date date;
	private VectorClock vectorClock;
	private String client;

	public DatabaseVersionHeader() {
		this.date = new Date();
		this.vectorClock = new VectorClock();
		this.client = "UnknownMachine";
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date timestamp) {
		date = timestamp;
	}

	public VectorClock getVectorClock() {
		return vectorClock;
	}

	public void setVectorClock(VectorClock vectorClock) {
		this.vectorClock = vectorClock;
	}

	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((client == null) ? 0 : client.hashCode());
		result = prime * result + ((vectorClock == null) ? 0 : vectorClock.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof DatabaseVersionHeader)) {
			return false;
		}
		DatabaseVersionHeader other = (DatabaseVersionHeader) obj;
		if (date == null) {
			if (other.date != null) {
				return false;
			}
		}
		else if (!date.equals(other.date)) {
			return false;
		}
		if (client == null) {
			if (other.client != null) {
				return false;
			}
		}
		else if (!client.equals(other.client)) {
			return false;
		}
		if (vectorClock == null) {
			if (other.vectorClock != null) {
				return false;
			}
		}
		else if (!vectorClock.equals(other.vectorClock)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append(client);
		sb.append('/');
		sb.append(vectorClock.toString());
		sb.append("/T=");
		sb.append(date.getTime());

		return sb.toString();
	}
}
