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

import java.io.IOException;
import name.pachler.nio.file.WatchEvent.Kind;
import name.pachler.nio.file.WatchEvent.Modifier;

/**
 * This class represents an abstract Path object that a WatchService can
 * operate on.<br/>
 * Note that Path is a new way of representing file system paths in JDK7 and
 * is included here to provide source level compatibility. This implementation
 * only uses it as a wrapper for java.io.File.</br>
 * To create a new Path instance, either use the Bootstrapper.newPath()
 * or Paths.
 * @author count
 */
public abstract class Path implements Watchable {

	protected Path(){
	}

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract int hashCode();

	public abstract WatchKey register(WatchService watcher, Kind<?>... events) throws IOException;

	public abstract WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException;

	public abstract Path resolve(Path other);

	@Override
	public abstract String toString();

}
