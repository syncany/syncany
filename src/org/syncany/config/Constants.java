/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.config;

import java.util.regex.Pattern;


/**
 *
 * @author Philipp C. Heckel
 */
public abstract class Constants {
    public static final Pattern PLUGIN_NAME_REGEX_PLUGIN_INFO = Pattern.compile("org\\.syncany\\.connection\\.plugins\\.([^.]+)\\.[\\w\\d]+Plugin");
    public static final String PLUGIN_FQCN_PREFIX = "org.syncany.connection.plugins.";
    public static final String PLUGIN_FQCN_SUFFIX = "Plugin";
    public static final String PLUGIN_FQCN_PATTERN = PLUGIN_FQCN_PREFIX+"%s.%s"+PLUGIN_FQCN_SUFFIX;

    /**
     * File separator in the database; Does not have to be equal to the file 
     * system separator.
     */
    public static final String DATABASE_FILE_SEPARATOR = "/";

    public static final String FILE_IGNORE_PREFIX = ".ignore";
}
