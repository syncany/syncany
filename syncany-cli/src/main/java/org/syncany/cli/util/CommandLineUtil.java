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
package org.syncany.cli.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Various CLI-utilities.
 * 
 * @author Pim Otte
 */
public class CommandLineUtil {
	/**
	 * Parses a string of type "1y2mo3w4d5h6m7s", where the units represent
	 * years, months, weeks, days, hours, minutes and second respectively.
	 * 
	 * returns: the duration of the period represented by the string in seconds.
	 */
	public static long parseTimePeriod(String period) {
		Pattern relativeDatePattern = Pattern.compile("(\\d+(?:[.,]\\d+)?)(mo|[smhdwy])");		
		Matcher relativeDateMatcher = relativeDatePattern.matcher(period);		
		
		relativeDateMatcher.reset();
		long periodSeconds = 0;
		
		while (relativeDateMatcher.find()) {
			double time = Double.parseDouble(relativeDateMatcher.group(1));
			String unitStr = relativeDateMatcher.group(2).toLowerCase();
			int unitMultiplier = 0;
			
			if (unitStr.startsWith("mo")) { unitMultiplier = 30*24*60*60; } // must be before "m"
			else if (unitStr.startsWith("s")) { unitMultiplier = 1; }
			else if (unitStr.startsWith("m")) { unitMultiplier = 60; }
			else if (unitStr.startsWith("h")) { unitMultiplier = 60*60; }
			else if (unitStr.startsWith("d")) { unitMultiplier = 24*60*60; }
			else if (unitStr.startsWith("w")) { unitMultiplier = 7*24*60*60; }
			else if (unitStr.startsWith("y")) { unitMultiplier = 365*24*60*60; }
			
			periodSeconds += (long) ((double)time*unitMultiplier);
		}
		
		return periodSeconds;
	}
}
