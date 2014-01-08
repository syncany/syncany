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
package org.syncany.gui.plugin;

import org.syncany.util.SyncanyParameters;

/**
 * @author vincent
 *
 */
public enum SyncanyWebDAVParameter implements SyncanyParameters {
	URL("url", true), 
	PASSWORD("username", true),
	USERNAME("password", true);

	private String value;
	private boolean mandatory;
	
	private SyncanyWebDAVParameter(String value, boolean mandatory) {
		this.value = value;
		this.mandatory = mandatory;
	}

	public boolean containsValue(String value) {
		return true;
	}

	@Override
	public String value() {
		return value;
	}

	@Override
	public boolean isPluginParameter() {
		return true;
	}

	@Override
	public boolean isParameterMandatory() {
		return mandatory;
	}
}
