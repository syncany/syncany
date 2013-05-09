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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {
    private DateFormat dateFormat;

    public LogFormatter() {
        dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
    }

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

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
            sb.append(getStackTrace(record.getThrown()));
        }

        return sb.toString();
    }
    
    private static String getStackTrace(Throwable aThrowable) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        
        return result.toString();
    }      
}