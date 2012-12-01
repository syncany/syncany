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

import name.pachler.nio.file.Path;
import java.util.List;
import java.util.Vector;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchKey;

/**
 *
 * @author count
 */
public class PathWatchKey extends WatchKey {
	private int flags;
	private PathWatchService service;
	private Vector<WatchEvent<?>> eventQueue = new Vector<WatchEvent<?>>();
	private final Path path;
	private boolean signalled;

	PathWatchKey(PathWatchService pws, Path path, int flags){
		this.service = pws;
		this.path = path;
		this.flags = flags;
	}

	@Override
	protected void finalize(){
		service.cancel(this);
	}


	@Override
	public void cancel() {
		service.cancel(this);
	}

	@Override
	public final boolean isValid() {
		return service != null;
	}

	void invalidate(){
		service = null;
	}

	@Override
	public synchronized List<WatchEvent<?>> pollEvents() {
		Vector<WatchEvent<?>> polledEvents = new Vector<WatchEvent<?>>(eventQueue);
		eventQueue.clear();
		return polledEvents;
	}

	@Override
	public synchronized boolean reset() {
		if(!isValid())
			return false;
		if(!signalled)
			return true;
		signalled = false;
		return service.reset(this);
	}

	Path getPath() {
		return path;
	}

	synchronized boolean addWatchEvent(WatchEvent<?> watchEvent) {
		eventQueue.add(watchEvent);
		if(!signalled){
			signalled = true;
			return true;
		} else
			return false;
	}

	synchronized boolean hasPendingWatchEvents() {
		return !eventQueue.isEmpty();
	}

	int getFlags(){
		return flags;
	}

	void setFlags(int flags){
		this.flags = flags;
	}

	protected int getNumQueuedEvents(){
		return eventQueue.size();
	}
}
