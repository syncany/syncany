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

import java.io.Console;

/**
 * Wrapper class for {@link Console} to enable mocking for tests.
 * 
 * @author Pim Otte
 *
 */
public class InitConsole {
	private Console console;
	
	public InitConsole(Console console) {
		this.console = console;
	}
	
	public String readLine() {
		return console.readLine();
	}
	
	public String readLine(String fmt, Object... args) {
		return console.readLine(fmt, args);
	}
	
	public char[] readPassword() {
		return console.readPassword();
	}
	
	public char[] readPassword(String fmt, Object... args) {
		return console.readPassword(fmt, args);
	}

}
