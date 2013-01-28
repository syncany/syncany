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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.syncany.util.StringUtil;

/**
 * From: http://publib.boulder.ibm.com/infocenter/wasinfo/v6r0/index.jsp?topic=/com.ibm.websphere.express.doc/info/exp/ae/rtrb_createformatter.html
 * Adapted by pheckel
 */
public class LogFormatter extends Formatter {
    /* private static final Config config = Config.getInstance();
     * 
     * WARNING: Do NOT add 'Config' as a static final here. 
     *          Since this class is created in the Config constructor, 
     *          Config.getInstance() will return NULL.
     */
    
    private DateFormat dateFormat;

    public LogFormatter() {
        super();

        dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
    }

    @Override
    public String format(LogRecord record) {
        // Create a StringBuffer to contain the formatted record
        // start with the date.
        StringBuilder sb = new StringBuilder();

        // Get the date from the LogRecord and add it to the buffer        
        sb.append(dateFormat.format(new Date(record.getMillis())));
        sb.append(" | ");
        sb.append(String.format("%-20s", record.getLoggerName()));
        sb.append(" | ");
        sb.append(String.format("%-18s", Thread.currentThread().getName()));
        sb.append(" | ");
        sb.append(String.format("%-8s", record.getLevel().getName()));
        sb.append(" : ");
        sb.append(formatMessage(record));
        sb.append("\n");

        if (record.getThrown() != null) {
            sb.append(StringUtil.getStackTrace(record.getThrown()));
        }

        return sb.toString();
    }
}