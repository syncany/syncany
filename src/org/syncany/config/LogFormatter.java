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

public class LogFormatter extends Formatter {
  private DateFormat dateFormat;

    public LogFormatter() {
        dateFormat = new SimpleDateFormat("d-M-yy H:mm:ss");
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
