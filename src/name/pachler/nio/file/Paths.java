/*
 * Copyright 2008-2011 Uwe Pachler
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. This particular file is
 * subject to the "Classpath" exception as provided in the LICENSE file
 * that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package name.pachler.nio.file;

import java.io.File;
import java.net.URI;
import name.pachler.nio.file.ext.Bootstrapper;

/**
 * Factory class for Path instances.
 * @author uwep
 */
public abstract class Paths {
	private Paths(){
	}
	
	/**
	 * Creates a new Path instance from the given file system path string.
	 * @param path	the path string
	 * @return	a new Path instance representing the path string
	 */
	public static Path get(String path){
		return Bootstrapper.newPath(new File(path));
	}

	// JDK7 also provides a method to convert a URI into a path, but
	// we won't support that for now (I can't see this being helpful, and
	// would just require more testing
/*	public static Path get(URI uri){
		return Bootstrapper.newPath(new File(uri));
	}
 */
}
