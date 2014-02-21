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
package org.syncany.connection.plugins.s3;

import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class S3Plugin extends Plugin {
    public static final String ID = "s3";
    
    @Override
    public String getId() {
        return ID;
    }    

    @Override
    public String getName() {
        return "Amazon S3";
    }

    @Override
    public Integer[] getVersion() {
        return new Integer[] { 0, 1 };
    }

    @Override
    public Connection createConnection() {
        return new S3Connection();
    }
}
