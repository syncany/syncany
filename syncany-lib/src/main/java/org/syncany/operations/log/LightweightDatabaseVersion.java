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
package org.syncany.operations.log;

import java.util.Date;

import org.simpleframework.xml.Element;
import org.syncany.operations.ChangeSet;

public class LightweightDatabaseVersion {
	@Element(name = "client", required = true)
	private String client;
	
	@Element(name = "date", required = true)
	private Date date;	
	
	@Element(name = "changeSet", required = true)
	private ChangeSet changeSet;
	
	public LightweightDatabaseVersion() {
		// Nothing.
	}
	
	public String getClient() {
		return client;
	}
	
	public void setClient(String client) {
		this.client = client;
	}
	
	public Date getDate() {
		return date;
	}
	
	public void setDate(Date date) {
		this.date = date;
	}
	
	public ChangeSet getChangeSet() {
		return changeSet;
	}
	
	public void setChangeSet(ChangeSet changeSet) {
		this.changeSet = changeSet;
	}		
}
