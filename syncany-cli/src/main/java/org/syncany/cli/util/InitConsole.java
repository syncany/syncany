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

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Wrapper class for {@link Console} to enable mocking for tests.
 * 
 * @author Pim Otte
 */
public class InitConsole {
	private static InitConsole instance;

	private Console console;
	private BufferedReader systemIn;
	private PrintWriter systemOut;
	
	protected InitConsole() {
		this.console = System.console();
		this.systemIn = null;
		this.systemOut = null;
	}
	
	public static InitConsole getInstance() {
		if (instance == null) {
			instance = new InitConsole();
		}
		
		return instance;
	}
	
	public static void setInstance(InitConsole initConsole) {
		instance = initConsole;
	}
	
	public String readLine() {
		try {
			if (console == null) {
				return getSystemInReader().readLine();
			}
			else {
				return console.readLine();
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public String readLine(String fmt, Object... args) {
		if (console == null) {
			getSystemOutWriter().write(String.format(fmt, args));
			return readLine();
		}
		else {
			return console.readLine(fmt, args);
		}
	}
	
	public char[] readPassword() {
		if (console == null) {
			return readLine().toCharArray();
		}
		else {
			return console.readPassword();
		}
	}
	
	public char[] readPassword(String fmt, Object... args) {
		if (console == null) {
			getSystemOutWriter().write(String.format(fmt, args));
			return readLine().toCharArray();
		}
		else {
			return console.readPassword(fmt, args);
		}
	}
	
	private BufferedReader getSystemInReader() {
		if (systemIn == null) {
			systemIn = new BufferedReader(new InputStreamReader(System.in));
		}
		
		return systemIn;
	}
	
	private PrintWriter getSystemOutWriter() {
		if (systemOut == null) {
			systemOut = new PrintWriter(new OutputStreamWriter(System.out));
		}
		
		return systemOut;
	}
}
