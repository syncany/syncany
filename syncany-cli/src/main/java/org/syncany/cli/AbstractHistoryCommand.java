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

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author pheckel
 *
 */
public abstract class AbstractHistoryCommand extends Command {
	private static final Logger logger = Logger.getLogger(AbstractHistoryCommand.class.getSimpleName());
	
	protected Date parseDateOption(String dateStr) throws Exception {
		Pattern relativeDatePattern = Pattern.compile("^(\\d+)([smhDWMY])$");
		Pattern absoluteDatePattern = Pattern.compile("^(\\d{2})-(\\d{2})-(\\d{4})$");
		
		Matcher relativeDateMatcher = relativeDatePattern.matcher(dateStr);		
		
		if (relativeDateMatcher.matches()) {
			int time = Integer.parseInt(relativeDateMatcher.group(1));
			String unitStr = relativeDateMatcher.group(2);
			int unitMultiplier = 0;
			
			if ("s".equals(unitStr)) { unitMultiplier = 1; }
			else if ("m".equals(unitStr)) { unitMultiplier = 60; }
			else if ("h".equals(unitStr)) { unitMultiplier = 60*60; }
			else if ("D".equals(unitStr)) { unitMultiplier = 24*60*60; }
			else if ("W".equals(unitStr)) { unitMultiplier = 7*24*60*60; }
			else if ("M".equals(unitStr)) { unitMultiplier = 30*24*60*60; }
			else if ("Y".equals(unitStr)) { unitMultiplier = 365*24*60*60; }
			
			long restoreDateMillies = time*unitMultiplier*1000;
			Date restoreDate = new Date(System.currentTimeMillis()-restoreDateMillies);
			
			logger.log(Level.FINE, "Restore date: "+restoreDate);
			return restoreDate;
		}
		else {
			Matcher absoluteDateMatcher = absoluteDatePattern.matcher(dateStr);
			
			if (absoluteDateMatcher.matches()) {
				int date = Integer.parseInt(absoluteDateMatcher.group(1));
				int month = Integer.parseInt(absoluteDateMatcher.group(2));
				int year = Integer.parseInt(absoluteDateMatcher.group(3));
				
				GregorianCalendar calendar = new GregorianCalendar();
				calendar.set(year, month-1, date);
				
				Date restoreDate = calendar.getTime();
				
				logger.log(Level.FINE, "Restore date: "+restoreDate);
				return restoreDate;
			}
			else {
				throw new Exception("Invalid '--date' argument: "+dateStr);
			}
		}		
	}
}
