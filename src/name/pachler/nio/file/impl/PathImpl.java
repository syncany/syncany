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

package name.pachler.nio.file.impl;

import name.pachler.nio.file.impl.PathImpl;
import java.io.File;
import java.io.IOException;
import name.pachler.nio.file.Path;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchEvent.Kind;
import name.pachler.nio.file.WatchEvent.Modifier;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.WatchService;
import name.pachler.nio.file.WatchService;
import name.pachler.nio.file.impl.PathWatchService;

/**
 * This is a minimal implementation of JDK7's Path class,
 * so all we offer here is what is required for WatchService
 * and friends to work
 * @author count
 */
public class PathImpl extends Path {
    private File file;

    /**
     * This constructor does not comply with JDK7.
     * @param filere
     */
    public PathImpl(File file){
		this.file = file;
    }

    public Path resolve(Path other){
		PathImpl otherImpl = (PathImpl)other;
		if(otherImpl==null)
			return this;
		if(otherImpl.getFile().isAbsolute())
			return otherImpl;
		return new PathImpl(new File(getFile(), otherImpl.toString()));
    }


	@Override
    public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
		return register(watcher, events, new WatchEvent.Modifier[0]);
    }

	@Override
    public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
		PathWatchService pws = (PathWatchService)watcher;
		return pws.register(this, events, modifiers);
    }

    /**
     * This method does not exist in JDK7 and is only supplied for convenience
     */
    public File getFile() {
		return file;
    }

	@Override
	public String toString(){
		return file.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final PathImpl other = (PathImpl) obj;
		if (this.file != other.file && (this.file == null || !this.file.equals(other.file))) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return file.hashCode();
	}


}
