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
package org.syncany.config;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * The log formatter implements a {@link Formatter}. It is used by the application's
 * logging functionality to pretty-print the log output.
 * 
 * <p>Log format:
 * <ul>
 *   <li>Date/time</li>
 *   <li>Logger name</li>
 *   <li>Thread name</li>
 *   <li>Log level</li>
 *   <li>Log message</li>
 * </ul>
 * 
 * <p><b>Note</b>: This class might not be directly referenced through code
 * Instead, it can be referenced (and instantiated) using a logging.properties 
 * file.
 *  
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class LogFormatter extends Formatter {
  private DateFormat dateFormat;

    public LogFormatter() {
        dateFormat = new SimpleDateFormat("d-M-yy H:mm:ss.SSS");
    }

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        sb.append(dateFormat.format(new Date(record.getMillis())));
        sb.append(" | ").append(formatLoggerName(record.getLoggerName()));
        sb.append(" | ").append(formatThreadName(Thread.currentThread().getName()));
        sb.append(" | ").append(formatShortLogLevel(record.getLevel()));
        sb.append(" : ").append(formatMessage(record));
        sb.append("\n");

        if (record.getThrown() != null) {
            sb.append(formatStackTrace(record.getThrown()));
        }

        return sb.toString();
    }
    
    private String formatLoggerName(String loggerName) {
    	if (loggerName.length() > 15) {
    		loggerName = loggerName.substring(0, 15);
    	}
    	
    	return String.format("%-15s", loggerName);
	}
    
    private String formatThreadName(String threadName) {
    	if (threadName.length() > 10) {
    		threadName = threadName.substring(0, 10);
    	}
    	
    	return String.format("%-10s", threadName);
	}    
    
    private String formatShortLogLevel(Level level) {
    	if (level.getName().length() <= 4) {
    		return level.getName();
    	}
    	else {
    		return level.getName().substring(0, 4);
    	}
    }
    
    private String formatStackTrace(Throwable aThrowable) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        
        return result.toString();
    }         
}
