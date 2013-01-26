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
package org.syncany.experimental.db;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 *
 * @author Philipp C. Heckel
 */
public abstract class Constants {
    public static final String APPLICATION_NAME = "Syncany";
    public static final String APPLICATION_DONATE_URL = "http://www.syncany.org/donate.html";
    public static final String APPLICATION_URL = "http://www.syncany.org/";
    public static final String APPLICATION_CRASHREPORT_URL = "http://api.syncany.org/crashreport.php";
    public static final String RESOURCE_BUNDLE = "org.syncany.i18n.I18n";

    /**
     * Syncany divides bigger files in chunks. This value defines the kilobytes (KB) of
     * how big one (unencrypted) chunk might become (1024 = 1 MB).
     */
    public static final int DEFAULT_CHUNK_SIZE = 512;

    /**
     * Minimum size of one chunk in kilobytes (KB).
     */
    public static final int MINIMUM_CHUNK_SIZE = 128;

    /**
     * Default size of the syncany cache in megabytes (MB).
     */
    public static final int DEFAULT_CACHE_SIZE = 1024;

    /**
     * Default cipher to encrypt the chunks.
     */
    public static final String DEFAULT_ENCRYPTION_CIPHER = "AES";

    /**
     * Default keylength for the given cipher in bit.
     */
    public static final int DEFAULT_ENCRYPTION_KEYLENGTH = 128;

    public static final boolean DEFAULT_AUTOSTART_ENABLED = true;

    public static final boolean DEFAULT_NOTIFICATIONS_ENABLED = true;
    
    public static final Locale DEFAULT_LOCALE = new Locale("en", "US");
    

    public static final Pattern PLUGIN_NAME_REGEX_PLUGIN_INFO = Pattern.compile("org\\.syncany\\.connection\\.plugins\\.([^.]+)\\.[\\w\\d]+PluginInfo");

    public static final String PLUGIN_FQCN_PREFIX = "org.syncany.connection.plugins.";
    public static final String PLUGIN_FQCN_SUFFIX = "PluginInfo";
    public static final String PLUGIN_FQCN_PATTERN = PLUGIN_FQCN_PREFIX+"%s.%s"+PLUGIN_FQCN_SUFFIX;

    public static final int NOTIFICATION_TIMEOUT = 3000;

    /**
     * If an indexed file is not found in the DB by its path, the file is looked
     * up by its checksum. If more than one file with the same checksum is found,
     * the one with the smallest Leivenstein distance is used as previous version.
     * This value sets an upper bound. If the Levenshtein distance exceeds this
     * value, the file is assumed to be new.
     *
     * <p>The Levenshtein distance represents the number of edits required to
     * change one string into another. Example: d(aab, ccb) = 2
     *
     * @see http://en.wikipedia.org/wiki/Levenshtein_distance
     */
    public static final int MAXIMUM_FILENAME_LEVENSHTEIN_DISTANCE = 4;

    /**
     * File separator in the database; Does not have to be equal to the file 
     * system separator.
     */
    public static final String DATABASE_FILE_SEPARATOR = "/";
    
    public static final String CONFIG_DATABASE_DIRNAME = "db";
    public static final String CONFIG_DATABASE_FILENAME = "syncany";
    public static final String CONFIG_DATABASE_URL_FORMAT = "jdbc:derby:%s;create=true";

    /**
     * Name of the persistence unit to be used.
     *
     * <p>The final version will use an embedded database. For testing,
     * any RDBMS can be used. Detailed settings in <em>META-INF/persistence.xml</em>
     */
    public static final String CONFIG_DATABASE_PERSISTENCE_UNIT = "Syncany";

    public static final String CONFIG_CACHE_DIRNAME = "cache";
    public static final String CONFIG_FILENAME = "config.xml";
    public static final String CONFIG_DEFAULT_FILENAME = "config-default.xml";
    
    public static final String REPOSITORY_FILE_CONTENTS = "SyncanyRepositoryVersion1";
    
    public static final String PROFILE_IMAGE_DIRNAME = "profiles";
    public static final int PROFILE_IMAGE_MAX_WIDTH = 48;
    public static final int PROFILE_IMAGE_MAX_HEIGHT = 48;

    public static final String ICON_LOGO_48 = "logo48.png";
    public static final String FILE_IGNORE_PREFIX = ".ignore";
    public static final String TRAY_DIRNAME = "tray";
    public static final String TRAY_FILENAME_DEFAULT = "tray.png";
    public static final String TRAY_FILENAME_UPTODATE = "tray-uptodate.png";
    public static final String TRAY_FILENAME_FORMAT_UPDATING = "tray-updating%s.png";
    
    public static final String JAVA_GNOME_NATIVE_LIB_FORMAT = "libgtkjni-4.0.20-dev-appindicator-r806-%s.so";

}
