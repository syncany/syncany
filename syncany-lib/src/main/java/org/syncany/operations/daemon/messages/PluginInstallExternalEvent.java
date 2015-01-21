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
package org.syncany.operations.daemon.messages;

import org.syncany.operations.daemon.messages.api.ExternalEvent;

public class PluginInstallExternalEvent extends ExternalEvent {
	private String source;
	private String pluginId = "unknown plugin";

	public PluginInstallExternalEvent() {
		// Nothing
	}

	public PluginInstallExternalEvent(String source) {
		this.source = source;
	}

	public PluginInstallExternalEvent(String source, String pluginId) {
		this.source = source;
		this.pluginId = pluginId;
	}

	public String getSource() {
		return source;
	}

	public String getPluginId() {
		return pluginId;
	}
}
