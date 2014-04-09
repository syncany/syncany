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
package org.syncany.tests.util;

import java.io.IOException;
import java.io.OutputStream;

public class SplitOutputStream extends OutputStream {
	private OutputStream out1;
	private OutputStream out2;
	
	public SplitOutputStream(OutputStream out1, OutputStream out2) {
		this.out1 = out1;
		this.out2 = out2;
	}
	
	@Override
	public void write(int b) throws IOException {
		out1.write(b);
		out2.write(b);
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		out1.write(b);
		out2.write(b);
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {	
		out1.write(b, off, len);
		out2.write(b, off, len);
	}
	
	@Override
	public void close() throws IOException {		
		out1.close();
		out2.close();
	}
}
