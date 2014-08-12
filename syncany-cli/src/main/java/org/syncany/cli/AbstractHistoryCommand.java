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
package org.syncany.cli;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.syncany.cli.util.CliUtil;

public abstract class AbstractHistoryCommand extends Command {
	protected static final Logger logger = Logger.getLogger(AbstractHistoryCommand.class.getSimpleName());
	protected static final String dateFormatPattern = "yy-MM-dd HH:mm:ss";
	protected static final DateFormat dateFormat = new SimpleDateFormat(dateFormatPattern);
	
	protected Date parseDateOption(String dateStr) throws Exception {
		Pattern relativeDatePattern = Pattern.compile("(\\d+(?:[.,]\\d+)?)(mo|[smhdwy])");		
		Matcher relativeDateMatcher = relativeDatePattern.matcher(dateStr);		
		
		if (relativeDateMatcher.find()) {
			long restoreDateMillies = CliUtil.parseTimePeriod(dateStr)*1000;
			
			Date restoreDate = new Date(System.currentTimeMillis()-restoreDateMillies);
			
			logger.log(Level.FINE, "Restore date: "+restoreDate);
			return restoreDate;
		}
		else {
			try {
				Date restoreDate = dateFormat.parse(dateStr);
				
				logger.log(Level.FINE, "Restore date: "+restoreDate);
				return restoreDate;
			}
			catch (Exception e) {
				throw new Exception("Invalid '--date' argument: " + dateStr + ", use relative date or absolute format: " + dateFormatPattern);
			}
		}		
	}
}
