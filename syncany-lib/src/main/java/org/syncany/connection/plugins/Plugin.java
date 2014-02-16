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
package org.syncany.connection.plugins;

/**
 * A plugin can be used to store Syncany's repository files on any remote location. 
 * Implementations of the <tt>Plugin</tt> class identify a storage/connection plugin.
 * 
 * <p>Using the 'id' attribute, plugins can be loaded by the {@link Plugins} class. 
 * Once a plugin is loaded, a corresponding {@link Connection} object must be created and 
 * initialized. From the connection object, a {@link TransferManager} can then be used to
 * upload/download files to the repository.
 * 
 * <p>Per <b>naming convention</b>, plugins must end by the name <b>Plugin</b> and extend this class. 
 * Furthermore, all plugin classes must reside in a package <b>org.syncany.connection.plugins.<i>plugin-id</i></b>,
 * where <i>plugin-id</i> is the identifier specified by {@link #getId()}. 
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class Plugin {
	/**
	 * Returns a unique plugin identifier.
	 * 
	 * <p>This identifier must correspond to the to the fully qualified package name in
	 * which the plugin classes reside. all plugin classes must reside in a package 
	 * 'org.syncany.connection.plugins.<i>plugin-id</i>'. 
	 */
    public abstract String getId();
    
    /**
     * Returns a short name of the plugin
     */
    public abstract String getName();
    
    /**
     * Returns the version of the plugin
     */
    public abstract Integer[] getVersion();
    
    /**
     * Creates a plugin-specific {@link Connection}
     */
    public abstract Connection createConnection();    
}
