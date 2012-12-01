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
import name.pachler.nio.file.ext.ExtendedWatchEventKind;
import static name.pachler.nio.file.impl.BSD.*;

/**
 *
 * @author count
 */
public class BSDPathWatchService extends PathWatchService{
/*	private static class KeventID{
		int ident;	// kevent's ident member
		int filter;	// kevent's filter member
	}
*/
	// the kqueue() file descriptor
	private int kqueuefd = -1;

	// mapping between a path string and KeventID - used to check if
	// the path is already being watched by this watch service
	private Map<String, Integer> dirs = new HashMap<String, Integer>();

	// maps a KevetnID to a watch key. Used to find the watch key for an event
	// that reports activity on the watched directory
	private Map<Integer, PollingPathWatchKey> keys = new HashMap<Integer, PollingPathWatchKey>();
	private int closePipeReadFd;
	private int closePipeWriteFd;
	final private Object changeLock = new Object();

	private Set<PathWatchKey> signalledWatchKeys = new HashSet<PathWatchKey>();
	private Queue<PathWatchKey> pendingWatchKeys = new LinkedList<PathWatchKey>();
	private static final long DEFAULT_POLLING_INTERVAL_MILLIS = 2000;	// 2 seconds
	private long pollingIntervalMillis = DEFAULT_POLLING_INTERVAL_MILLIS;
	private int numKeysRequiringPolling;

	public BSDPathWatchService(){
		try {
			String propertyValue = System.getProperty("name.pachler.io.file.BSDPathWatchService.pollingIntervalMillis", Long.toString(DEFAULT_POLLING_INTERVAL_MILLIS));
			pollingIntervalMillis = Long.parseLong(propertyValue);
		}catch(Throwable t){
			// ignore, pllingIntervalMillis will still have its default value.
		}
		open();
	}
	
	@Override
	public synchronized PathWatchKey register(Path path, Kind<?>[] kinds, Modifier[] modifiers) throws IOException {

		PathImpl pathImpl = checkAndCastToPathImpl(path);

		int flags = makeFlagMask(kinds, modifiers);

		// check that user only provided supported flags and modifiers
		int supportedFlags = (FLAG_FILTER_ENTRY_CREATE | FLAG_FILTER_ENTRY_DELETE | FLAG_FILTER_ENTRY_MODIFY | FLAG_FILTER_KEY_INVALID);
		if((flags & ~supportedFlags) != 0)
			throw new UnsupportedOperationException("The given watch event kind or modifier is not supported by this WatchService");

		String pathname = pathImpl.getFile().getAbsolutePath();

		PollingPathWatchKey key = null;

		// request changeLock
		BSD.write(closePipeWriteFd, new byte[1], 1);
		synchronized(changeLock){

			if(kqueuefd == -1)
				throw new ClosedWatchServiceException();

			{
				Integer dirfdInteger = dirs.get(pathname);
				if(dirfdInteger != null)
					key = keys.get(dirfdInteger);
			}

			if(key == null){
				// no directory file fd registered - we'll need to open
				// one now

				// create file descriptor and watch event first
				boolean success = false;
				int dirfd = -1;
				try {
					dirfd = BSD.open(pathname, BSD.O_RDONLY, 0);
					if(dirfd == -1)
						throw new IOException("error registering the path with the native OS: " + strerror(errno()));
					kevent e = new kevent();
					e.set_ident(dirfd);
					e.set_filter(EVFILT_VNODE);
					e.set_flags((short)(EV_ADD | EV_CLEAR));
					e.set_fflags(NOTE_WRITE | NOTE_DELETE | NOTE_REVOKE);
					int result = kevent(kqueuefd, new kevent[]{e}, null, null);
					// do we need more specific error handling here?
					if(result != 0)
						throw new IOException("error registering the path with the native OS: " + strerror(errno()));

					// create watch key and add it to the key and dirs maps
					key = new PollingPathWatchKey(this, path, 0);
					keys.put(dirfd, key);
					dirs.put(pathname, dirfd);

				} finally {
					// if something went wrong, close descriptors
					if(key == null){
						if(dirfd != -1){
							// if the descriptor has been added to the kqueue,
							// it will be removed automatically when the descriptor
							// closes.
							BSD.close(dirfd);
						}
					}
				}
			}

			if(key != null && key.getFlags() != flags){
				// check if modification flag has changed; moddiff will
				// be +1 if the ENTRY_MODIFIED flag was added, -1 if
				// it was removed (and 0 if it didn't change)
				int moddiff = 0;
				moddiff += (flags & FLAG_FILTER_ENTRY_MODIFY)!=0 ? +1 : 0;
				moddiff += (key.getFlags() & FLAG_FILTER_ENTRY_MODIFY)!=0 ? -1 : 0;

				numKeysRequiringPolling += moddiff;
				key.setFlags(flags);
			}
			// retract request for change lock
			BSD.read(closePipeReadFd, new byte[1], 1);
		}
		
		// first poll to capture initial state
		key.poll();

		return key;
	}

	@Override
	synchronized void cancel(PathWatchKey pathWatchKey) {
		// request change lock
		byte[] b = new byte[1];
		write(closePipeWriteFd, b, 1);
		synchronized(changeLock){
			boolean eventsAdded = cancelImpl(pathWatchKey);
			if(eventsAdded)
				queueKey(pathWatchKey);
			
			// clear request
			int nread = read(closePipeReadFd, b, 1);
			assert(nread == 1);
		}
	}

	private boolean cancelImpl(PathWatchKey pathWatchKey){
		PathImpl pathImpl = (PathImpl)pathWatchKey.getPath();
		String pathString = pathImpl.getFile().getPath();
		Integer dirfdInteger = dirs.get(pathString);

		// check if the key that was passed in is ours
		if(dirfdInteger == null)
			return false;	// FIXME: should throw an exception in this case
		PathWatchKey key = keys.get(dirfdInteger);
		if(key != pathWatchKey)
			return false;	// FIXME: should throw an exception in this case

		boolean eventAdded = false;

		if((key.getFlags() & FLAG_FILTER_KEY_INVALID) != 0)
		{
			key.addWatchEvent(new VoidWatchEvent(ExtendedWatchEventKind.KEY_INVALID));
			eventAdded = true;
		}


		// if we get here, the key is ours, so we'll invalidate it and
		// remove it now.
		int dirfd = dirfdInteger.intValue();
		kevent[] changelist = new kevent[]{ new kevent() };
		changelist[0].set_ident(dirfd);
		changelist[0].set_filter(EVFILT_VNODE);
		changelist[0].set_flags(EV_DELETE);
		int result = kevent(kqueuefd, changelist, null, null);
		assert(result == 0);

		key.invalidate();
		if((key.getFlags() & FLAG_FILTER_ENTRY_MODIFY) != 0)
			--numKeysRequiringPolling;
		
		keys.remove(dirfdInteger);
		dirs.remove(pathString);
		
		return eventAdded;
	}

	@Override
	public synchronized boolean reset(PathWatchKey pathWatchKey) {
		if(!pathWatchKey.isValid())
			return false;
		if(pathWatchKey.hasPendingWatchEvents())
			pendingWatchKeys.add(pathWatchKey);
		else
			signalledWatchKeys.remove(pathWatchKey);
		return true;
	}

	private void open(){
		kqueuefd = kqueue();
		int[] pipefd = new int[2];
		int pipeResult = pipe(pipefd);
		closePipeReadFd = pipefd[0];
		closePipeWriteFd = pipefd[1];

	}

	@Override
	public synchronized void close() throws IOException {
		// request change lock
		byte[] b = new byte[1];
		write(closePipeWriteFd, new byte[]{0}, 1);

		synchronized(changeLock){
			// close all file descriptors
			BSD.close(kqueuefd);
			kqueuefd = -1;

			int nread = read(closePipeReadFd, b, 1);
			assert(nread == 1);

			BSD.close(closePipeReadFd);
			BSD.close(closePipeWriteFd);
		}
	}

	@Override
	public WatchKey poll() throws InterruptedException, ClosedWatchServiceException {
		return pollImpl(0);
	}

	@Override
	public WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException, ClosedWatchServiceException {
		long millis = TimeUnit.MILLISECONDS.convert(timeout, unit);
		return pollImpl(millis);
	}

	@Override
	public WatchKey take() throws InterruptedException, ClosedWatchServiceException {
		return pollImpl(-1);
	}

	private WatchKey pollImpl(long timeout) throws InterruptedException, ClosedWatchServiceException {
		long lastStart = System.currentTimeMillis();
		do{
			// if there is a timeout specified, count down timeout
			if(timeout != -1){
				long currentTime = System.currentTimeMillis();
				long lastDuration = currentTime - lastStart;
				timeout -= lastDuration;
				if(timeout < 0)
					timeout = 0;
				lastStart = currentTime;
			}

			kevent[] eventlist = new kevent[32];

			// FIXME: this should be chosen depending on whether we need to
			// poll or not.
			long selectTimeout = timeout;
			if((timeout == -1 || timeout > pollingIntervalMillis) && numKeysRequiringPolling > 0)
				selectTimeout = pollingIntervalMillis;
			int nread = 0;

			synchronized(changeLock){
				// if we have pending watches, we're done and return the first one.
				if(pendingWatchKeys.size() > 0)
					return pendingWatchKeys.remove();

				// check if watch key has been closed
				if(kqueuefd == -1)
					throw new ClosedWatchServiceException();

				int[] readfds = {closePipeReadFd, kqueuefd };
				int selectResult = select(readfds, null, null, selectTimeout);
				if(selectResult == -1){
					// check for interruption
					if(BSD.errno() == BSD.EINTR)
						throw new InterruptedException();
					// otherwise, this is another error that shouldn't occur
					// here.
					String message = BSD.strerror(BSD.errno());
					try {
						close();
					} finally {
						// the message string here is just for debugging
						throw new ClosedWatchServiceException();
					}
				}
				if(readfds[0] == closePipeReadFd){
					// we have been requested to release the changeLock
					continue;
				}

				// we know now that kevent() will not block, because select() told us so...
				if(readfds[1] == kqueuefd)
					nread = kevent(kqueuefd, null, eventlist, null);

				if(nread == -1){
					if(nread == EINTR)
						throw new InterruptedException();

					try {
						close();
					} finally {
						// catch exception and throw ClosedWatchServiceException instead
						throw new ClosedWatchServiceException();
					}
				}

				if(nread > 0)
				{
					// go through all kevent structures and update keys
					for(int i=0; i<nread; ++i){
						kevent e = eventlist[i];
						int dirfd = (int)e.get_ident();
						int fflags = e.get_fflags();

						PollingPathWatchKey key = keys.get(dirfd);

						// in some cases, the key might not be there any more because
						// it was invalidated (and therefore cancelled) in response to
						// a previous kevent
						if(key == null)
							continue;

						boolean eventsAdded;

						// check if watch key has become invalid
						if( (fflags & NOTE_DELETE)!=0 || (fflags & NOTE_REVOKE)!=0)
							eventsAdded = cancelImpl(key);
						else{
							try {
								// poll key's directory
								eventsAdded = key.poll();
							} catch (FileNotFoundException ex) {
								eventsAdded = cancelImpl(key);
							}
						}
						if(eventsAdded)
							queueKey(key);
					}
				} else if(numKeysRequiringPolling > 0){
					// if we timed out and we have keys that need to be polled
					for(PollingPathWatchKey key : keys.values()){
						// only poll keys that have the modification flag set -
						// CREATE/DELETE are flagged in kevent
						if((key.getFlags() & FLAG_FILTER_ENTRY_MODIFY) == 0)
							continue;
						boolean eventsAdded;
						try {
							eventsAdded = key.poll();
						} catch (FileNotFoundException ex) {
							eventsAdded = cancelImpl(key);
						}
						if(eventsAdded)
							queueKey(key);
					}
				}

				// now check for pending watch keys again
				if(pendingWatchKeys.size() > 0)
					return pendingWatchKeys.remove();

			}	// synchronized(changeLock)
		} while(timeout > 0 || timeout == -1);

		return null;
	}

	private void queueKey(PathWatchKey key) {
		// if this key has pending events and is not signalled
		// yet, set it to signalled.
		if(!signalledWatchKeys.contains(key))
		{
			signalledWatchKeys.add(key);
			pendingWatchKeys.add(key);
		}
	}


}
