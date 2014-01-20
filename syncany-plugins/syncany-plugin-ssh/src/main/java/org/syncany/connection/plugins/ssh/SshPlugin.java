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
package org.syncany.connection.plugins.ssh;

import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;

/**
 * Identifies the SSH-based storage {@link Plugin} for Syncany. 
 * 
 * <p>This class defines the identifier, name and 
 * version of the plugin. It furthermore allows the instantiation 
 * of a plugin-specific {@link SshConnection}. 
 * 
 * @author Vincent Wiencek <vwiencek@gmail.com>
 */
public class SshPlugin extends Plugin {
    public static final String ID = "ssh";
    
    @Override
    public String getId() {
        return ID;
    }    
    
	@Override
	public String getName() {
		return "SSH";
	}

    @Override
    public Integer[] getVersion() {
        return new Integer[] { 0, 1 };
    }

    @Override
    public Connection createConnection() {
        return new SshConnection();
    }
}
