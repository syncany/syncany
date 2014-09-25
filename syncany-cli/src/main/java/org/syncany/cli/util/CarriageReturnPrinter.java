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

import java.io.PrintStream;

import com.google.common.base.Strings;

public class CarriageReturnPrinter {
	private PrintStream underlyingPrintStream;	
	private int lineLength;
	private boolean lastWasWithCarriageReturn;
	
	public CarriageReturnPrinter(PrintStream underlyingPrintStream) {
		this.underlyingPrintStream = underlyingPrintStream;
		this.lineLength = 0;
		this.lastWasWithCarriageReturn = false;
	}
	
	public void printf(String format, Object... args) {
		clearLastLine();
		
		String line = String.format(format, args);
		
		underlyingPrintStream.print(line);
		lineLength += line.length();
	}
	
	public void print(String s) {	
		clearLastLine();
		
		underlyingPrintStream.print(s);
		lineLength += s.length();
	}
	
	public void println() {		
		clearLastLine();
		
		underlyingPrintStream.println();
		lineLength = 0;
	}

	public void println(String s) {	
		clearLastLine();
		
		underlyingPrintStream.println(s);
		lineLength = 0;
	}
	
	public void printr() {
		printr("");
	}
	
	public void printr(Object s) {		
		clearLastLine();
		
		underlyingPrintStream.print(s);

		lineLength = s.toString().length();
		lastWasWithCarriageReturn = true;
	}	
	
	private void clearLastLine() {
		if (lastWasWithCarriageReturn) {
			String spacesStr = "\r" + Strings.repeat(" ", lineLength) + "\r";		
			underlyingPrintStream.print(spacesStr);
			
			lastWasWithCarriageReturn = false;
		}
	}
}
