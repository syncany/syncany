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
package org.syncany.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


/**
 *
 * @author pheckel
 */
public class DateUtil {
    public static final DateFormat FORMAT_FULL = new SimpleDateFormat("d MMM yyyy, K:mm a");
    public static final DateFormat FORMAT_DAY = new SimpleDateFormat("EEEE, K:mm a");
    public static final DateFormat FORMAT_HOURS = new SimpleDateFormat("K:mm a");
    
    public static String toNiceFormat(Date date) {
        long diffMinutes = (new Date().getTime() - date.getTime())/1000/60;

        if (removeTime(getToday()).equals(removeTime(date))) {
            return "Today, "+FORMAT_HOURS.format(date);
        }

        else if (removeTime(getYesterday()).equals(removeTime(date))) {
            return "Yesterday, "+FORMAT_HOURS.format(date);
        }

        else if (diffMinutes > 0 && diffMinutes < 7*24*60) {
            return FORMAT_DAY.format(date);
        }

        else {
            return FORMAT_FULL.format(date);
        }
    }
    
    public static Date removeTime(Date date) {
        Calendar cal = Calendar.getInstance();  
        cal.setTime(date);  
        cal.set(Calendar.HOUR_OF_DAY, 0);  
        cal.set(Calendar.MINUTE, 0);  
        cal.set(Calendar.SECOND, 0);  
        cal.set(Calendar.MILLISECOND, 0);  
        return cal.getTime(); 	
    }
        
    public static Date getToday()  {
        return removeTime(new Date());
    }    
    
    public static Date getYesterday()  {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return removeTime(cal.getTime());
    }
    
    public static void main(String[] args) throws ParseException {
        System.out.println(toNiceFormat(getYesterday()));
        System.out.println(toNiceFormat(FORMAT_FULL.parse("14 May 2021, 5:37 PM")));
        System.out.println(toNiceFormat(FORMAT_FULL.parse("5 May 2011, 11:37 PM")));
        System.out.println(toNiceFormat(FORMAT_FULL.parse("4 May 2011, 10:37 PM")));
        System.out.println(toNiceFormat(FORMAT_FULL.parse("3 May 2011, 10:37 PM")));
        System.out.println(toNiceFormat(FORMAT_FULL.parse("3 Apr 2011, 10:37 PM")));
    }
}
