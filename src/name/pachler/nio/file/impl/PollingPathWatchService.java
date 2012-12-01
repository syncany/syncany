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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import name.pachler.nio.file.ClosedWatchServiceException;
import name.pachler.nio.file.Path;
import name.pachler.nio.file.WatchEvent.Kind;
import name.pachler.nio.file.WatchEvent.Modifier;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.ext.Bootstrapper;
import name.pachler.nio.file.ext.ExtendedWatchEventKind;

/**
 *
 * @author uwep
 */
public class PollingPathWatchService extends PathWatchService {

	private Map<Path,PollingPathWatchKey> keys = new HashMap<Path, PollingPathWatchKey>();
	private Set<PathWatchKey> signalledKeys = new HashSet<PathWatchKey>();
	private Queue<PathWatchKey> pendingKeys = new LinkedList<PathWatchKey>();
	private final Object internalLock = new Object();
	private boolean closed = false;
	private long pollInterval;

	public PollingPathWatchService() {
		pollInterval = Bootstrapper.getDefaultPollingInterval();
	}


	@Override
	public synchronized PathWatchKey register(Path path, Kind<?>[] kinds, Modifier[] modifiers) throws IOException {
		PathImpl pathImpl = checkAndCastToPathImpl(path);

		int flags = makeFlagMask(kinds, modifiers);

		// check that user only provided supported flags and modifiers
		int supportedFlags = (FLAG_FILTER_ENTRY_CREATE | FLAG_FILTER_ENTRY_DELETE | FLAG_FILTER_ENTRY_MODIFY | FLAG_FILTER_KEY_INVALID );
		if((flags & ~supportedFlags) != 0)
			throw new UnsupportedOperationException("The given watch event kind or modifier is not supported by this WatchService");

		PollingPathWatchKey key = keys.get(pathImpl);

		if(key == null){
			key = new PollingPathWatchKey(this, path, flags);
			keys.put(path, key);
		} else {
			key.setFlags(flags);
		}

		// first poll to capture initial state
		key.poll();

		return key;
	}

	@Override
	synchronized void cancel(PathWatchKey pathWatchKey) {
		PathWatchKey key = keys.get(pathWatchKey.getPath());
		if(key != pathWatchKey)
			return;	// this is not one of our keys

		boolean eventAdded = cancelImpl(pathWatchKey);
		if(eventAdded)
			queueKey(pathWatchKey);
	}

	private boolean cancelImpl(PathWatchKey key){
		keys.remove(key.getPath());
		key.invalidate();

		boolean eventAdded = false;

		if((key.getFlags() & FLAG_FILTER_KEY_INVALID) != 0)
		{
			key.addWatchEvent(new VoidWatchEvent(ExtendedWatchEventKind.KEY_INVALID));
			eventAdded = true;
		}

		return eventAdded;
	}

	@Override
	public synchronized boolean reset(PathWatchKey pathWatchKey) {

		if(!pathWatchKey.isValid() || keys.get(pathWatchKey.getPath()) != pathWatchKey)
			return false;	// this is not one of our keys...

		if(pathWatchKey.hasPendingWatchEvents())
			pendingKeys.add(pathWatchKey);
		else
			signalledKeys.remove(pathWatchKey);

		return true;
	}

	@Override
	public synchronized void close() throws IOException {
		synchronized(internalLock){
			closed = true;
			internalLock.notifyAll();
		}

	}

	@Override
	public synchronized WatchKey poll() throws InterruptedException, ClosedWatchServiceException {
		if(closed)
			throw new ClosedWatchServiceException();

		// if we have pending keys, return them first. This will make the caller
		// take all keys from the queue first
		if(pendingKeys.size() > 0)
			return pendingKeys.remove();

		for(PollingPathWatchKey k : keys.values()){
			try {
				k.poll();
			} catch (FileNotFoundException ex) {
				cancelImpl(k);
			}
			if(k.hasPendingWatchEvents() && !signalledKeys.contains(k.getPath())){
				signalledKeys.add(k);
				pendingKeys.add(k);
			}
		}

		return pendingKeys.poll();
	}

	@Override
	public WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException, ClosedWatchServiceException {
		// check first if there is a pending key - because if there is, there
		// is no need to wait. We'll also start the timer now, because
		// polling takes time, and this way we know how much we've spent
		long startTime = System.currentTimeMillis();

		WatchKey key = poll();
		if(key != null)
			return key;

		synchronized(internalLock){

			long pollDuration = System.currentTimeMillis() - startTime;
			long timeoutMillis = unit.toMillis(timeout);

			long millis = timeoutMillis - pollDuration;

			// if polling took longer than our timeout, we won't wait and
			// poll a second time.
			if(millis <= 0)
				return null;

			internalLock.wait(millis);
		}

		return poll();
	}

	@Override
	public WatchKey take() throws InterruptedException, ClosedWatchServiceException {
		for(;;){
			WatchKey key = poll(pollInterval, TimeUnit.MILLISECONDS);
			if(key != null)
				return key;
		}
	}

	private void queueKey(PathWatchKey key) {
		// if this key has pending events and is not signalled
		// yet, set it to signalled.
		if(key.hasPendingWatchEvents() && !signalledKeys.contains(key))
		{
			signalledKeys.add(key);
			pendingKeys.add(key);
		}
	}

	public long getPollInterval(){
		return pollInterval;
	}

	public void setPollInterval(long pollInterval){
		this.pollInterval = pollInterval;
	}

}
