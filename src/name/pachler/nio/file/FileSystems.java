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

import name.pachler.nio.file.ext.Bootstrapper;

/**
 * Provides static method to get the default FileSystem object. It is used
 * to acquire a WatchService via the provided FileSystem instance.
 * Note that this
 * class solely exists in jpathwatch to maintain JDK7 source compatibility,
 * but only offers small subset of the functionality implemented in JDK7.
 * @author count
 */
public abstract class FileSystems {
	private static FileSystem defaultfs = new FileSystem(){
		@Override
		public WatchService newWatchService() {
			return Bootstrapper.newWatchService();
		}

	};

	private FileSystems(){
	}

	/**
	 * Gets the default file system. jpathwatch provides this method
	 * as a means to get a {@link FileSystem} that in turn allows
	 * to create a {@link WatchService} instance.
	 * @return the default file system object.
	 */
	public static FileSystem getDefault(){
		return defaultfs;
	}
}
