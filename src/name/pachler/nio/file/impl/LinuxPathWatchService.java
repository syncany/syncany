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
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import name.pachler.nio.file.ClosedWatchServiceException;
import name.pachler.nio.file.StandardWatchEventKind;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchEvent.Kind;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.ext.ExtendedWatchEventKind;
import static name.pachler.nio.file.impl.Linux.*;

/**
 * <p>This Linux implementation of the WatchService interface works without
 * the use of threads or asynchronous I/O, using Linux' inotify file system
 * event facitily.</p>
 * <p>The implementation hinges around select() to wait for events on the
 * inotify file descriptor that each LinuxPathWatchService. Each time
 * a WatchKey is registered (through Path.register(), which eventually calls
 * register() on the LinuxPathWatchService), inotify_add_watch() is called
 * on the service's inotify file descriptor.</br>
 * To wait for events, the take() and poll() methods use select() to
 * wait for the inotify FD to become readable. </br>
 * However, a lot of things can happen while a thread is waiting inside
 * poll() or take() with select():
 * <ul>
 * <li>the close() method can be called: The expectation is that calls blocked
 * in take() or poll() will throw a ClosedWatchServiceException() more or less
 * immediately after close() returns.</li>
 * <li>Another thread also calls poll() or take(), but maybe with a timeout
 * value that would make it terminate earlier than the first thread. This
 * thread may not be blocked by the first thread</li>
 * <li>poll() and take() are expected to only retreive one event, but read()
 * call on an inotify file descriptor can return multiple events at once; there
 * is no reliable way only retrieve one event. As a consequence, the reading
 * thread needs to ensure that other waiting threads receive the remaining
 * events<li>
 * <ul>
 * </p>
 * <p>With these requirements in mind (and with the desire not to create a separate
 * monitoring thread per WatchService like on Windows), the solution was to
 * perform management on the inotify descriptor with the threads that call
 * into poll()/take() on a first comes first served basis:<br/>
 * The first thread calling into pollImpl() (called from poll()/take()) becomes
 * the master thread. It is the only thread at any given time that services
 * the inotify file descriptor by calling select() and read(). The thread
 * looses it's master thread status when it is done with calling select()
 * and read().<br>
 * All other threads that call into pollImpl() simply call wait(). When the
 * master thread is done, it calls notify() to wake up the next thread, which
 * might then become the master thread.
 * <p>Also note that select() waits on two file descriptors: Because select() does
 * not return when a file descriptor closes while it is waiting for it
 * (for reasons that elude me), a command pipe is used which receives commands
 * from other threads.</p>
 * <p>This is how close() is implemented: Instead of closing the inotify file descriptor
 * directly, it writes a command byte into the command pipe. If there is a
 * master thread, it wakes up, consumes the command byte and executes the
 * command (which is to close the inotify FD). If there is no master thread,
 * the thread calling close() closes the inotify file descriptor.<br>
 * All this is necessary because a thread calling select() can't be interrupted,
 * and select() does not return when one of it's file descriptors is closed
 * while it is waiting for it</p>
 *
 * @author count
 */
public class LinuxPathWatchService extends PathWatchService
{
	static
	{
		NativeLibLoader.loadLibrary("jpathwatch-native");
	}

	// IMPORTANT: None of these members may be accecced outside a synchronized block!
	private int inotifyFd = -1;
	private int commandPipeReadFd;
	private int commandPipeWriteFd;
	private Map<Integer, LinuxPathWatchKey> keys = new HashMap<Integer, LinuxPathWatchKey>();
	private Set<PathWatchKey> signalledWatchKeys = new HashSet<PathWatchKey>();
	private Queue<PathWatchKey> pendingWatchKeys = new LinkedList<PathWatchKey>();

	public static final byte CMD_CLOSE = 1;
	public static final byte CMD_NOTIFY = 2;

	private native int translateInotifyEvents(byte[] buffer, int bufferPos, int bufferSize);

	private synchronized void inotifyEventHandler(int wd, int mask, int cookie, String name){		
		LinuxPathWatchKey key = keys.get(wd);

		if(key == null){
			// this is a precautionary check:
			// We shouldn't get events from keys we don't know about, except for
			// specific cases (see below). In case we do get such a
			// notification, a warning is logged so that at least a bug
			// report can be filed for this case.
			//
			// The specific condition under which we know that we'll receive
			// events for events not in keys is when the user calls cancel()
			// on the key: In this case the IN_IGNORED bit is set, because
			// inotify will send this event after we called inotify_rm_watch().
			if(wd == -1 && (mask & IN_Q_OVERFLOW) != 0)
			{
				// On Linux, there is only one queue per watch descriptor,
				// therefore overflow happens for all keys. So we need to deliver
				// an overflow event to all of them.
				for(LinuxPathWatchKey k : keys.values()){
					k.addWatchEvent(new VoidWatchEvent(StandardWatchEventKind.OVERFLOW));
					if(!signalledWatchKeys.contains(k)){
						signalledWatchKeys.add(k);
						pendingWatchKeys.add(k);
					}
				}
			}
			else if((mask & IN_IGNORED) == 0)
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "no WatchKey found for given watch descriptor {0}", wd);
			return;
		}

		// for some events, (file-)name can be null
		Path path = null;
		if(name != null)
			path = new PathImpl(new File(name));
		
		int flags = key.getFlags();

		boolean eventsAdded = false;
		if((mask & IN_CREATE) != 0)
		{
			key.addWatchEvent(new PathWatchEvent(StandardWatchEventKind.ENTRY_CREATE, path, 1));
			eventsAdded = true;
		}
		if((mask & IN_MODIFY) != 0)
		{
			key.addWatchEvent(new PathWatchEvent(StandardWatchEventKind.ENTRY_MODIFY, path, 1));
			eventsAdded = true;
		}
		if((mask & IN_DELETE) != 0)
		{
			key.addWatchEvent(new PathWatchEvent(StandardWatchEventKind.ENTRY_DELETE, path, 1));
			eventsAdded = true;
		}
		if((mask & IN_MOVED_TO) != 0)
		{
			WatchEvent.Kind<Path> kind;
			if((flags & FLAG_FILTER_ENTRY_RENAME_TO) != 0)
				kind = ExtendedWatchEventKind.ENTRY_RENAME_TO;
			else
				kind = StandardWatchEventKind.ENTRY_CREATE;
			key.addWatchEvent(new PathWatchEvent(kind, path, 1));
			eventsAdded = true;
		}
		if((mask & IN_MOVED_FROM) != 0)
		{
			WatchEvent.Kind<Path> kind;
			if((flags & FLAG_FILTER_ENTRY_RENAME_FROM) != 0)
				kind = ExtendedWatchEventKind.ENTRY_RENAME_FROM;
			else
				kind = StandardWatchEventKind.ENTRY_DELETE;
			key.addWatchEvent(new PathWatchEvent(kind, path, 1));
			eventsAdded = true;
		}
		if((mask & IN_IGNORED) != 0)
		{
			key.addWatchEvent(new VoidWatchEvent(ExtendedWatchEventKind.KEY_INVALID));
			key.invalidate();
			eventsAdded = true;
		}

		if(eventsAdded && !signalledWatchKeys.contains(key))
		{
			signalledWatchKeys.add(key);
			pendingWatchKeys.add(key);
		}
	}

	public LinuxPathWatchService() {
		inotifyFd = inotify_init();
		int[] pipefd = new int[2];
		int pipeResult = pipe(pipefd);
		commandPipeReadFd = pipefd[0];
		commandPipeWriteFd = pipefd[1];
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			close();
		} finally {
			super.finalize();
		}
	}




	@Override
	public WatchKey take() throws InterruptedException {
		return pollImpl(-1);
	}

	@Override
	public WatchKey poll() throws InterruptedException {
		return pollImpl(0);
	}

	@Override
	public WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException, ClosedWatchServiceException {
		long millis = TimeUnit.MILLISECONDS.convert(timeout, unit);
		return pollImpl(millis);
	}

	boolean hasMasterThread = false;

	public synchronized void close() throws IOException{
		int nwritten = write(commandPipeWriteFd, new byte[]{CMD_CLOSE}, 1);
		if(nwritten == -1)
			throw new IOException();
		if(!hasMasterThread)
			handleCommand();
	}

	/**
	 * polls for a change on a watched directory to return the respective
	 * WatchKey. This function can be called from any thread, however, if
	 * called concurrently the first entering thread becomes the 'master thread'.
	 * This thread performs the actual work of calling select() on the
	 * inotify file descriptor; other threads simply call wait() to wait until
	 * the master thread exits the function (and gives up it's status as
	 * master thread).
	 * @param timeout	The timeout in milliseconds, which is the maximum time
	 * that the function will wait (block) for an event to occur. 0 means
	 * a pure poll; the function will not wait at all. -1 will make pollImpl()
	 * wait indefinitely.
	 * @return	A WatchKey for the registered Path that changed, or null if
	 *	there was no change detected within the timeout.
	 * @throws InterruptedException
	 * @throws ClosedWatchServiceException	if called on a closed WatchService,
	 *	or if the service was closed from another thread while this thread
	 *	was blocked in the pollImpl() call to wait for an event.
	 */
	private WatchKey pollImpl(long timeout) throws InterruptedException, ClosedWatchServiceException {
		WatchKey key = null;
		do {
			boolean isMasterThread = false;

			// measure start time for timeout
			long startTime = System.currentTimeMillis();
			try {
				synchronized(this){
					// check if there are keys with events pending or if
					// the service is closed
					key = pendingWatchKeys.poll();
					if(key != null)
						continue;
					if(inotifyFd == -1)
						throw new ClosedWatchServiceException();

					// try to become master thread - if this fails, just wait
					// for the current master thread to wake us up (or the
					// timeout, whichever comes earlier)
					isMasterThread = !hasMasterThread;
					if(isMasterThread)
						hasMasterThread = true;
					else if (timeout > 0)
						wait(timeout);
				}

				// if this thread is the master thread, it is its responsibility
				// to wait for events on this WatchService's inotify file
				// descriptor, or for commands on the command pipe.
				// It does so unsynchronized because
				// select() won't release the synchronization lock while it's
				// waiting, which would cause deadlocks with other threads.
				if(isMasterThread){
					int[] fds = {inotifyFd, commandPipeReadFd};
					int n = Unix.select(fds, null, null, timeout);

					// select() error handling
					if(n == -1){
						if(errno()==EINTR)
							throw new InterruptedException();
						else {
							closeImpl();
							throw new ClosedWatchServiceException();
						}
					}

					byte[] buffer = new byte[4096];
					int bufferSize = 0;
					int bufferPos = 0;

					// perform read(). read should not block because we know there
					// is data available, and no other thread is allowed to read
					// from that file descriptor
					if(fds[0] == inotifyFd){
						int nread = read(inotifyFd, buffer, buffer.length);
						if(nread == -1){
							int err = errno();
							if(err == EINTR)
								throw new InterruptedException();
							else{
								closeImpl();
								throw new ClosedWatchServiceException();
							}
						}
						else
							bufferSize = nread;

						synchronized(this){
							// translating inotify events to WatchEvent
							// objects needs to be synchronized
							translateInotifyEvents(buffer, bufferPos, bufferSize);
						}
					}
				}

			} finally {
				synchronized(this){
					if(isMasterThread) {
						// consume bytes in notification pipe
						int n = Unix.ioctl_FIONREAD(commandPipeReadFd);
						if(n>0)
							handleCommand();
						// abandon master thread status and notify next waiting
						// thread, if any
						hasMasterThread = false;
						notify();
					}
					if(key == null)
						key = pendingWatchKeys.poll();
				}
				if(timeout != -1){
					long endTime = System.currentTimeMillis();
					long timeDifference = endTime-startTime;
					timeout = Math.max(0, timeout-timeDifference);
				}
			}
		} while(timeout != 0 && key == null);

		return key;
	}


	@Override
	public synchronized PathWatchKey register(Path path, WatchEvent.Kind<?>[] kinds, WatchEvent.Modifier[] modifiers) throws IOException {
		if(inotifyFd == -1)
			throw new ClosedWatchServiceException();

		PathImpl pathImpl = checkAndCastToPathImpl(path);

		int flags = makeFlagMask(kinds, modifiers);

		// check that user only provided supported flags and modifiers
		int supportedFlags = (
			FLAG_FILTER_ENTRY_CREATE
		|	FLAG_FILTER_ENTRY_DELETE
		|	FLAG_FILTER_ENTRY_MODIFY
		|	FLAG_FILTER_ENTRY_RENAME_FROM
		|	FLAG_FILTER_ENTRY_RENAME_TO
		|	FLAG_FILTER_KEY_INVALID
		|	FLAG_ACCURATE);
		if((flags & ~supportedFlags) != 0)
			throw new UnsupportedOperationException("The given watch event kind or modifier is not supported by this WatchService");

		int mask = 0;
		if(0 != (flags & FLAG_FILTER_ENTRY_CREATE))
		{
			mask |= IN_CREATE;
			if(0 == (flags & FLAG_FILTER_ENTRY_RENAME_TO))
				mask |= IN_MOVED_TO;
		}
		if(0 != (flags & FLAG_FILTER_ENTRY_DELETE))
		{
			mask |= IN_DELETE;
			if(0 == (flags & FLAG_FILTER_ENTRY_RENAME_FROM))
				mask |= IN_MOVED_FROM;
		}
		if(0 != (flags & FLAG_FILTER_ENTRY_MODIFY))
			mask |= IN_MODIFY;
		if(0 != (flags & FLAG_FILTER_ENTRY_RENAME_FROM))
			mask |= IN_MOVED_FROM;
		if(0 != (flags & FLAG_FILTER_ENTRY_RENAME_TO))
			mask |= IN_MOVED_TO;

		String pathname = pathImpl.getFile().getAbsolutePath();
		int watchDescriptor = inotify_add_watch(inotifyFd, pathname, mask);

		if(watchDescriptor == -1){
			// do we need more specific error handling here?
                        String msg = strerror(errno());
			throw new IOException("error registering the path with the native OS: " + msg);
		}
		LinuxPathWatchKey key = keys.get(watchDescriptor);
		if(key == null)
		{
			key = new LinuxPathWatchKey(this, pathImpl, flags);
			keys.put(watchDescriptor, key);
		}
		else
			key.setFlags(flags);
		return key;
	}

	@Override
	synchronized void cancel(PathWatchKey pathWatchKey) {
		if(inotifyFd == -1)
			throw new ClosedWatchServiceException();
		int wd = -1;

		for(Entry<Integer,LinuxPathWatchKey> entry : keys.entrySet())
		{
			if(entry.getValue() == pathWatchKey)
			{
				wd = entry.getKey().intValue();
				if((pathWatchKey.getFlags() & FLAG_FILTER_KEY_INVALID) != 0)
					inotifyEventHandler(wd, IN_IGNORED, 0, null);
				pathWatchKey.invalidate();
				keys.remove(entry.getKey());
				break;
			}
		}

		if(pathWatchKey == null)
			return;

		int result = inotify_rm_watch(inotifyFd, wd);
		if(result == -1)
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "inotify_rm_watch() failed: {0}", strerror(errno()));
	}

	@Override
	public synchronized boolean reset(PathWatchKey pathWatchKey) {
		if(!pathWatchKey.isValid())
			return false;
		if(pathWatchKey.hasPendingWatchEvents()) {
			if(!hasMasterThread)
			pendingWatchKeys.add(pathWatchKey);

		} else
			signalledWatchKeys.remove(pathWatchKey);
		return true;
	}

	private synchronized void handleCommand() {
		// consume notification byte
		byte[] b = new byte[1];
		Unix.read(commandPipeReadFd, b, 1);
		byte command = b[0];
		switch(command){
			case CMD_CLOSE:
				closeImpl();
			case CMD_NOTIFY:
				;
		}
	}

	private synchronized void closeImpl(){
		for(PathWatchKey pwk : keys.values())
			pwk.invalidate();
		keys.clear();
		signalledWatchKeys.clear();
		pendingWatchKeys.clear();
		Unix.close(inotifyFd);
		inotifyFd = -1;
		Unix.close(commandPipeReadFd);
		commandPipeReadFd = -1;
		Unix.close(commandPipeWriteFd);
		commandPipeWriteFd = -1;
	}
}
