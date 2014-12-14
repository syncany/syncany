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

import java.util.List;

import org.syncany.util.StringUtil;

public class CliTableUtil {
	public static void printTable(CarriageReturnPrinter out, List<String[]> tableValues, String noRowsMessage) {
		if (tableValues.size() > 0) {
			Integer[] tableColumnWidths = calculateColumnWidths(tableValues);
			String tableRowFormat = "%-" + StringUtil.join(tableColumnWidths, "s | %-") + "s\n";

			printTableHeader(out, tableValues.get(0), tableRowFormat, tableColumnWidths);

			if (tableValues.size() > 1) {
				printTableBody(out, tableValues, tableRowFormat, tableColumnWidths);
			}
			else {
				out.println(noRowsMessage);
			}
		}
	}

	private static void printTableBody(CarriageReturnPrinter out, List<String[]> tableValues, String tableRowFormat, Integer[] tableColumnWidths) {
		for (int i = 1; i < tableValues.size(); i++) {
			out.printf(tableRowFormat, (Object[]) tableValues.get(i));
		}
	}

	private static void printTableHeader(CarriageReturnPrinter out, String[] tableHeader, String tableRowFormat, Integer[] tableColumnWidths) {
		out.printf(tableRowFormat, (Object[]) tableHeader);

		for (int i = 0; i < tableColumnWidths.length; i++) {
			if (i > 0) {
				out.print("-");
			}

			for (int j = 0; j < tableColumnWidths[i]; j++) {
				out.print("-");
			}

			if (i < tableColumnWidths.length - 1) {
				out.print("-");
				out.print("+");
			}
		}

		out.println();
	}

	private static Integer[] calculateColumnWidths(List<String[]> tableValues) {
		Integer[] tableColumnWidths = new Integer[tableValues.get(0).length];

		for (String[] tableRow : tableValues) {
			for (int i = 0; i < tableRow.length; i++) {
				if (tableColumnWidths[i] == null || (tableRow[i] != null && tableColumnWidths[i] < tableRow[i].length())) {
					tableColumnWidths[i] = tableRow[i].length();
				}
			}
		}

		return tableColumnWidths;
	}	
}
