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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import name.pachler.nio.file.ClosedWatchServiceException;
import name.pachler.nio.file.Path;
import name.pachler.nio.file.StandardWatchEventKind;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchEvent.Kind;
import name.pachler.nio.file.WatchEvent.Modifier;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.ext.ExtendedWatchEventKind;
import static name.pachler.nio.file.impl.Windows.*;

/**
 *
 * @author count
 */
public class WindowsPathWatchService extends PathWatchService {
	private static final int DEFAULT_BUFFER_SIZE = 8192;	// 8k by default

	private static native void initNative();

	private static volatile int threadCounter = 0;
	private Map<Path, WatchRecord> pathToWatchRecordMap = new HashMap();
	private List<WindowsPathWatchThread> startedThreads = new LinkedList<WindowsPathWatchThread>();
	private native void translateFILE_NOTIFY_INFORMATION(WatchRecord watchRecord, ByteBuffer byteBuffer, int bufferSize);

	/**
	 * This routine is called from the translateFILE_NOTIFY_INFORMATION native
	 * method and will add events to the given WatchRecord. Note that
	 * the action value recognizes a special value, -1, that indicates that
	 * the queue overflowed.
	 * @param wr
	 * @param action
	 * @param fileName
	 */
	private void FILE_NOTIFY_INFORMATIONhandler(WatchRecord wr, int action, String fileName){
		int flags = wr.getFlags();
		WatchEvent.Kind<Path> kind = null;
		switch(action){
			case FILE_ACTION_RENAMED_NEW_NAME:
				if(0 != (flags & FLAG_FILTER_ENTRY_RENAME_TO))
				{
					kind = ExtendedWatchEventKind.ENTRY_RENAME_TO;
					break;
				}
				// intentional fallthrough
			case FILE_ACTION_ADDED:
				if(0 != (flags & FLAG_FILTER_ENTRY_CREATE))
					kind = StandardWatchEventKind.ENTRY_CREATE;
				break;
			case FILE_ACTION_RENAMED_OLD_NAME:
				if(0 != (flags & FLAG_FILTER_ENTRY_RENAME_FROM))
				{
					kind = ExtendedWatchEventKind.ENTRY_RENAME_FROM;
					break;
				}
				// intentional fallthrough
			case FILE_ACTION_REMOVED:
				if(0 != (flags & FLAG_FILTER_ENTRY_DELETE))
					kind = StandardWatchEventKind.ENTRY_DELETE;
				break;
			case FILE_ACTION_MODIFIED:
				if(0 != (flags & FLAG_FILTER_ENTRY_MODIFY))
					kind = StandardWatchEventKind.ENTRY_MODIFY;
				break;
		}

		if(kind == null)
			return;

		WatchEvent<?> e = new PathWatchEvent(kind, new PathImpl(new File(fileName)), 1);
		addWatchEvent(wr, e);
	}

	void addWatchEvent(WatchRecord wr, WatchEvent<?> e){
		if(wr.addWatchEvent(e)){
			pendingWatchKeys.add(wr);
		}
	}

	static {
		NativeLibLoader.loadLibrary("jpathwatch-native");
		initNative();
	}

	public WindowsPathWatchService(){
	}

	private void logLastError() {
		int lastError = GetLastError();
		String errorMsg = GetLastError_toString(lastError);
		String message = "Thread '" + Thread.currentThread().getName() + "': error while reading from watch key: " + errorMsg;
		Logger.getLogger(getClass().getName()).log(Level.WARNING, message);
	}

	private void cancelImpl(WatchRecord wr) {
		if((wr.getFlags() & FLAG_FILTER_KEY_INVALID)!=0)
			addWatchEvent(wr, new VoidWatchEvent(ExtendedWatchEventKind.KEY_INVALID));
		CancelIo(wr.handle);
		CloseHandle(wr.handle);
		CloseHandle(wr.overlapped.getEventHandle());
		wr.invalidate();
		wr.thread.eventHandleToWatchRecord.remove(wr.overlapped.getEventHandle());
		pathToWatchRecordMap.remove(wr.getPath());
	}

	private static long openDirectoryHandle(PathImpl pathImpl) {
		String file = pathImpl.getFile().getAbsolutePath();
		int shareMode = FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE;
		int flagsAndAttributes = FILE_FLAG_BACKUP_SEMANTICS | FILE_FLAG_OVERLAPPED;
		return CreateFile(file, FILE_LIST_DIRECTORY, shareMode, null, OPEN_EXISTING, flagsAndAttributes, 0);
	}

	private static class WatchRecord extends PathWatchKey{

		WatchRecord(WindowsPathWatchService pws, Path path, int flags, WindowsPathWatchThread thread){
			super(pws, path, flags);
			this.thread = thread;
		}

		public long handle;
		public OVERLAPPED overlapped;
		public ByteBuffer buffer;
		private WindowsPathWatchThread thread = null;

		private int getNotifyFilter() {
			int flags = getFlags();
			int notifyFilter = 0;
			if(0 != (flags & FLAG_FILTER_ENTRY_CREATE | FLAG_FILTER_ENTRY_DELETE | FLAG_FILTER_ENTRY_RENAME_FROM | FLAG_FILTER_ENTRY_RENAME_TO))
				notifyFilter |= FILE_NOTIFY_CHANGE_FILE_NAME | FILE_NOTIFY_CHANGE_DIR_NAME;
			if(0 != (flags & FLAG_FILTER_ENTRY_MODIFY))
				notifyFilter |= FILE_NOTIFY_CHANGE_LAST_WRITE | FILE_NOTIFY_CHANGE_SIZE;
			return notifyFilter;
		}

		private boolean getWatchSubtree(){
			int flags = getFlags();
			return 0 != (flags & FLAG_WATCH_SUBTREE);
		}

	}

	private static class Command {
		static final int TYPE_ADD_WATCHRECORD = 1;
		static final int TYPE_SHUTDOWN = 2;
		static final int TYPE_REMOVE_WATCHRECORD = 3;
		static final int TYPE_MODIFY_WATCHRECORD = 4;

		private int type;
		private final WatchRecord wr;
		private int flags;

		private Command(int type, WatchRecord wr) {
			this(type, wr, 0);
		}
		private Command(int type, WatchRecord wr, int flags) {
			this.type = type;
			this.wr = wr;
			this.flags = flags;
		}

		private int getType() {
			return type;
		}

		private WatchRecord getWatchRecord() {
			return wr;
		}

		private int getFlags(){
			return flags;
		}

	}

	private CloseableBlockingQueue<PathWatchKey> pendingWatchKeys = new CloseableBlockingQueue<PathWatchKey>();

	@Override
	public synchronized PathWatchKey register(Path path, Kind<?>[] kinds, Modifier[] modifiers) throws IOException {
		PathImpl pathImpl;
		try {
			pathImpl = (PathImpl)path;
		}catch(ClassCastException ccx){
			throw new IllegalArgumentException("the provided Path was not created by the newPath factory method of name.pachler.nio.file.ext.Bootstrapper");
		}
		if(!pathImpl.getFile().isDirectory())
			throw new IOException("path "+pathImpl.toString()+" is not a directory");	// JDK 1.7 throws NotDirectoryException in this case, but for now we'll just throw an IOException

		int flags = makeFlagMask(kinds, modifiers);

		WatchRecord wr = pathToWatchRecordMap.get(path);
		boolean commandResult;
		if(wr == null){
			// no WatchRecord for this path yet
			long directoryHandle = openDirectoryHandle(pathImpl);

			if(directoryHandle == INVALID_HANDLE_VALUE){
				int errorCode = GetLastError();
				throw new IOException(GetLastError_toString(errorCode));
			}

			WindowsPathWatchThread currentThread = null;
			for(WindowsPathWatchThread t : startedThreads){
				if(!t.isFull()){
					currentThread = t;
					break;
				}
			}
			if (currentThread == null)
			{
				currentThread = new WindowsPathWatchThread();
				// make this thread a deamon thread so that if users forget to close
				// this WatchService (which instructs the thread to terminate), it will
				// at least not prevent the JVM from shutting down (the app would
				// appear to 'hang' on shutdown otherwise)
				currentThread.setDaemon(true);
				currentThread.start();
				startedThreads.add(currentThread);
			}

			wr = new WatchRecord(this, pathImpl, flags, currentThread);
			wr.buffer = new ByteBuffer(DEFAULT_BUFFER_SIZE);
			wr.overlapped = new OVERLAPPED();
			wr.overlapped.setEvent(CreateEvent(null, true, false, null));
			wr.handle = directoryHandle;

			commandResult = currentThread.executeCommand(new Command(Command.TYPE_ADD_WATCHRECORD, wr));
		} else {
			// a WatchRecord already exists for this path, so modify it
			commandResult = wr.thread.executeCommand(new Command(Command.TYPE_MODIFY_WATCHRECORD, wr, flags));
		}

		// granted, that's not very inspired, but probably enough for something
		// that should hardly ever happen anyways.
		if(!commandResult)
			throw new IOException("register() failed, details are in log.");

		return wr;
	}

	@Override
	synchronized void cancel(PathWatchKey pathWatchKey) {
		WatchRecord wr = (WatchRecord)pathWatchKey;
		wr.thread.executeCommand(new Command(Command.TYPE_REMOVE_WATCHRECORD, wr));
		// if the this WatchRecord was the last in the thread, terminate the thread
		if(wr.thread.isEmpty()){
			wr.thread.executeCommand(new Command(Command.TYPE_SHUTDOWN, wr));
			startedThreads.remove(wr.thread);
		}
	}

	@Override
	public synchronized boolean reset(PathWatchKey pathWatchKey) {
		WatchRecord wr = (WatchRecord)pathWatchKey;

		if(wr.hasPendingWatchEvents())
			pendingWatchKeys.add(wr);
		return true;
	}

	@Override
	public synchronized void close() throws IOException {
		for (WindowsPathWatchThread thread : this.startedThreads) {
			thread.close();
		}
		pendingWatchKeys.close();
	}

	@Override
	public WatchKey poll() throws InterruptedException, ClosedWatchServiceException {
		return pendingWatchKeys.poll();
	}

	@Override
	public WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException, ClosedWatchServiceException {
		return pendingWatchKeys.poll(timeout, unit);
	}

	@Override
	public WatchKey take() throws InterruptedException, ClosedWatchServiceException {
		return pendingWatchKeys.take();
	}



	public class WindowsPathWatchThread extends Thread {
		private SynchronousQueue<Boolean> commandResultQueue = new SynchronousQueue<Boolean>();
		private ConcurrentLinkedQueue<Command> commandQueue = new ConcurrentLinkedQueue<Command>();
		private Map<Long, WatchRecord> eventHandleToWatchRecord = new HashMap<Long, WatchRecord>();
		private long signallingEvent = CreateEvent(null, true, false, null);
		WindowsPathWatchThread()
		{
			this.setName(getClass().getSimpleName() + '-' + threadCounter++);
		}

		// request to execute command in watcher thread and wait until it has
		// finished processing it.
		private synchronized boolean executeCommand(Command command) {
			commandQueue.add(command);

			// tell thread that there is a new command
			SetEvent(signallingEvent);

			// wait for thread to finish operation if we need syncing
			boolean success = false;
			boolean result = false;
			while(!success){
				try {
					result = commandResultQueue.take();
					success = true;
				}catch(InterruptedException ix){
				}
			}
			return result;
		}

		public boolean isFull(){
			return this.eventHandleToWatchRecord.size() >= MAXIMUM_WAIT_OBJECTS-1; //Allow room for the signaling event handle
		}

		public boolean isEmpty(){
			return this.eventHandleToWatchRecord.isEmpty();
		}

		public synchronized void close() throws IOException {
			if(this.isAlive() == false || signallingEvent == 0)
				throw new ClosedWatchServiceException();
			this.executeCommand(new Command(Command.TYPE_SHUTDOWN, null));
		}
		public void run(){
			for(;;){
				final int result;

				long[] handles = new long[eventHandleToWatchRecord.size()+1];
				handles[0] = signallingEvent;
				int handleIndex = 1;
				for(long h : eventHandleToWatchRecord.keySet())
					handles[handleIndex++] = h;

				try
				{
					result = WaitForMultipleObjects(handles, false, INFINITE);
				}
				catch(RuntimeException e)
				{
					String message = "Thread '" + Thread.currentThread().getName() + "': error while calling WaitForMultipleObjects. Exception: " + e;
					Logger.getLogger(getClass().getName()).log(Level.WARNING, message);
					boolean done = false;
					while(!done){
						try {
							// indicate that we processed the command
							commandResultQueue.put(false);
							done = true;
						} catch (InterruptedException ex) {
							Logger.getLogger(WindowsPathWatchService.class.getName()).log(Level.SEVERE, null, ex);
						}
					}
					throw e;
				}

				if(result == WAIT_OBJECT_0){
					ResetEvent(signallingEvent);

					Command cmd = commandQueue.poll();
					boolean success = false;
					WatchRecord watchRecord = null;
					assert(cmd != null);
					try {
						switch(cmd.getType()){
							case Command.TYPE_SHUTDOWN:
								// using a duplicate of the watch record to prevent
								// modifying the list while iterating over it
								for(WatchRecord wr : new ArrayList<WatchRecord>(eventHandleToWatchRecord.values()))
									cancelImpl(wr);

								eventHandleToWatchRecord.clear();
								CloseHandle(signallingEvent);
								signallingEvent = 0;
								success = true;
								return;
							case Command.TYPE_ADD_WATCHRECORD:{
								watchRecord = cmd.getWatchRecord();
								eventHandleToWatchRecord.put(watchRecord.overlapped.getEventHandle(),watchRecord);
								pathToWatchRecordMap.put(watchRecord.getPath(), watchRecord);

								success = ReadDirectoryChanges(watchRecord.handle, watchRecord.buffer, watchRecord.getWatchSubtree(), watchRecord.getNotifyFilter(), null, watchRecord.overlapped, null);
							}	break;
							case Command.TYPE_MODIFY_WATCHRECORD:{
								// on modification, re-issue read operation for watch record
								watchRecord = cmd.getWatchRecord();

								int newFlags = cmd.getFlags();
								int oldFlags = watchRecord.getFlags();
								boolean watchSubtreeChanged = ((newFlags ^ oldFlags) & FLAG_WATCH_SUBTREE) != 0;
								watchRecord.setFlags(newFlags);

								success = CancelIo(watchRecord.handle);

								if(success && watchSubtreeChanged){
									// close and re-open directory watch if recursion options changed
									success = CloseHandle(watchRecord.handle);
									addWatchEvent(watchRecord, new VoidWatchEvent(StandardWatchEventKind.OVERFLOW));
									if(success){
										watchRecord.handle = openDirectoryHandle((PathImpl)watchRecord.getPath());
										success = watchRecord.handle != INVALID_HANDLE_VALUE;
									}

								}

								if(success)
									success = ReadDirectoryChanges(watchRecord.handle, watchRecord.buffer, watchRecord.getWatchSubtree(), watchRecord.getNotifyFilter(), null, watchRecord.overlapped, null);

							}	break;
							case Command.TYPE_REMOVE_WATCHRECORD:{
								watchRecord = cmd.getWatchRecord();
								cancelImpl(watchRecord);
								success = true;
							}	break;
							default:
								throw new RuntimeException("unhandled command type");
						}
					}finally{
						if(!success)
						{
							logLastError();
							cancelImpl(watchRecord);
						}
						boolean done = false;
						while(!done){
							try {
								// indicate that we processed the command
								commandResultQueue.put(success);
								done = true;
							} catch (InterruptedException ex) {
								Logger.getLogger(WindowsPathWatchService.class.getName()).log(Level.SEVERE, null, ex);
							}
						}
					}

				} else if (WAIT_OBJECT_0 < result && result < handles.length){
					int index = result - WAIT_OBJECT_0;
					long h = handles[index];
					WatchRecord wr = eventHandleToWatchRecord.get(h);
					ResetEvent(wr.overlapped.getEventHandle());

					boolean success = true;

					int[] numberOfBytesTransferred = {0};
					if(success)
						success = GetOverlappedResult(wr.overlapped.getEventHandle(), wr.overlapped, numberOfBytesTransferred, true);

					if(success)
					{
						if(numberOfBytesTransferred[0] != 0)
							translateFILE_NOTIFY_INFORMATION(wr, wr.buffer, numberOfBytesTransferred[0]);
						else
							addWatchEvent(wr, new VoidWatchEvent(StandardWatchEventKind.OVERFLOW));	// handle queue overflow
					}

					// queue another I/O operation
					if(success)
						success = ReadDirectoryChanges(wr.handle, wr.buffer, wr.getWatchSubtree(), wr.getNotifyFilter(), null, wr.overlapped, null);

					// finally, if something went wrong, cancel watch key
					if(!success){
						// ERROR_ACCESS_DENIED indicates that we can no longer
						// read from the watched directory, probably because
						// it has been deleted or unmounted.
						// Other errors might indicate a real problem and are
						// therefore logged.
						if(GetLastError() != ERROR_ACCESS_DENIED)
							logLastError();
						cancelImpl(wr);
					}
				}
			}
		}
	}

	@Override
	public void finalize() throws Throwable{
		try {
			close();
		}finally{
			super.finalize();
		}
	}
}
