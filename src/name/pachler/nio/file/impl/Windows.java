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

public class Windows {
	private static native long getINVALID_HANDLE_VALUE();

	static {
		NativeLibLoader.loadLibrary("jpathwatch-native");
		INVALID_HANDLE_VALUE = getINVALID_HANDLE_VALUE();
	}

	static class OVERLAPPED{
		private static native long allocatePeer();
		private static native void deallocatePeer(long peer);

		private static native void setEventOnPeer(long peer, long eventHandle);
		private static native void setOffsetOnPeer(long peer, long offset);

		static {
			initNative();
		}

		private static native void initNative();

		private long peer;
		private long offset;
		private long eventHandle;


		OVERLAPPED(){
			peer = allocatePeer();
		}

		@Override
		protected void finalize() throws Throwable {
			try {
				deallocatePeer(peer);
				peer = 0;
			} finally {
				super.finalize();
			}
		}


		public long getEventHandle() {
			return eventHandle;
		}

		public void setEvent(long eventHandle) {
			this.eventHandle = eventHandle;
			setEventOnPeer(peer, eventHandle);
		}

		public long getOffset() {
			return offset;
		}

		public void setOffset(long offset) {
			this.offset = offset;
			setOffsetOnPeer(peer, offset);
		}
	}


	static class ByteBuffer{
		private static native long allocate(int size);
		private static native void deallocate(long address);

		private long bufferAddress;
		private int bufferSize;

		static {
			initNative();
		}

		private static native void initNative();

		ByteBuffer(int bufferSize){
			if(bufferSize < 0)
				throw new IllegalArgumentException("bufferSize may not be less than zero");
			this.bufferAddress = allocate(bufferSize);
			this.bufferSize = bufferSize;
		}

		@Override
		protected void finalize() throws Throwable {
			try {
				deallocate(bufferAddress);
			} finally {
				super.finalize();
			}
		}
	}

	// this type is listed here for completeness; currently
	// there are no security descriptor functions implemented.
	static class SECURITY_DESCRIPTOR{
	}

	// this type is listed here for completeness; currently
	// there are no security descriptor functions implemented.
	static class SECURITY_ATTRIBUTES {
		SECURITY_DESCRIPTOR securityDescriptor;
		boolean ineritHandle;
	}

	public static final int GENERIC_READ = 0x80000000;
	public static final int GENERIC_WRITE = 0x40000000;
	public static final int GENERIC_EXECUTE = 0x20000000;
	public static final int GENERIC_ALL = 0x10000000;

	public static final int FILE_LIST_DIRECTORY		= 0x00000001;
	public static final int FILE_READ_DATA			= 0x00000001;
	public static final int FILE_ADD_FILE			= 0x00000002;
	public static final int FILE_WRITE_DATA			= 0x00000002;
	public static final int FILE_ADD_SUBDIRECTORY		= 0x00000004;
	public static final int FILE_APPEND_DATA		= 0x00000004;
	public static final int FILE_CREATE_PIPE_INSTANCE	= 0x00000004;
	public static final int FILE_READ_EA			= 0x00000008;
	public static final int FILE_READ_PROPERTIES		= 0x00000008;
	public static final int FILE_WRITE_EA			= 0x00000010;
	public static final int FILE_WRITE_PROPERTIES		= 0x00000010;
	public static final int FILE_EXECUTE			= 0x00000020;
	public static final int FILE_TRAVERSE			= 0x00000020;
	public static final int FILE_DELETE_CHILD		= 0x00000040;
	public static final int FILE_READ_ATTRIBUTES		= 0x00000080;
	public static final int FILE_WRITE_ATTRIBUTES		= 0x00000100;

	public static final int FILE_SHARE_READ = 0x00000001;
	public static final int FILE_SHARE_WRITE = 0x00000002;
	public static final int FILE_SHARE_DELETE = 0x00000004;

	public static final int CREATE_NEW = 1;
	public static final int CREATE_ALWAYS = 2;
	public static final int OPEN_EXISTING = 3;
	public static final int OPEN_ALWAYS = 4;
	public static final int TRUNCATE_EXISTING = 5;

	public static final int FILE_ATTRIBUTE_READONLY = (0x1);
	public static final int FILE_ATTRIBUTE_TEMPORARY = (0x100);
	public static final int FILE_ATTRIBUTE_SYSTEM = (0x4);
	public static final int FILE_ATTRIBUTE_OFFLINE = (0x1000);
	public static final int FILE_ATTRIBUTE_NORMAL = (0x80);
	public static final int FILE_ATTRIBUTE_HIDDEN = (0x2);
	public static final int FILE_ATTRIBUTE_ENCRYPTED = (0x4000);
	public static final int FILE_ATTRIBUTE_ARCHIVE = (0x20);

	public static final int FILE_FLAG_BACKUP_SEMANTICS = 0x02000000;
	public static final int FILE_FLAG_DELETE_ON_CLOSE = 0x04000000;
	public static final int FILE_FLAG_NO_BUFFERING = 0x20000000;
	public static final int FILE_FLAG_OPEN_NO_RECALL = 0x00100000;
	public static final int FILE_FLAG_OPEN_REPARSE_POINT = 0x00200000;
	public static final int FILE_FLAG_OVERLAPPED = 0x40000000;
	public static final int FILE_FLAG_POSIX_SEMANTICS = 0x0100000;
	public static final int FILE_FLAG_RANDOM_ACCESS = 0x10000000;
	public static final int FILE_FLAG_SEQUENTIAL_SCAN = 0x08000000;
	public static final int FILE_FLAG_WRITE_THROUGH = 0x80000000;

	public static final long INVALID_HANDLE_VALUE;

	public static final int ERROR_ALREADY_EXISTS = 183;
	public static final int ERROR_FILE_NOT_FOUND = 2;
	public static final int ERROR_ACCESS_DENIED = 5;
	public static final int ERROR_FILE_EXISTS = 80;
	public static final int ERROR_INVALID_FUNCTION = 1;
	public static final int ERROR_OPERATION_ABORTED = 995;
	public static final int ERROR_IO_INCOMPLETE = 996;
	public static final int ERROR_IO_PENDING = 997;

	/**
	 * Calls the Windows GetLongPathName function to get the long form
	 * of the given path name.
	 * @param pathName
	 * @return	the long path name or null in case of error. Call GetLastError()
	 *	in this case.
	 */
	static native String GetLongPathName(String pathName);

	/**
	 * Calls the Windows GetShortPathName function to get the short form
	 * of the given path name.
	 * @param pathName
	 * @return	the long path name or null in case of error. Call GetLastError()
	 *	in this case.
	 */
	static native String GetShortPathName(String pathName);

	/**
	 * The Win32 CreateFile function. For detailed documentation, see
	 * <a href="http://msdn.microsoft.com/en-us/library/aa363858%28VS.85%29.aspx">MSDN</a>
	 * @param fileName	the file name to open
	 * @param desiredAccess
	 * @param shareMode
	 * @param securityAttributes	currently not supported, pass null in here.
	 * @param creationDisposition
	 * @param flagsAndAttributes
	 * @param templateFile	a handle to a template file
	 * @return a handle to the file
	 */
	static native long CreateFile(String fileName, int desiredAccess, int shareMode, SECURITY_ATTRIBUTES securityAttributes, int creationDisposition, int flagsAndAttributes, long templateFileHandle);

	/**
	 * Creates a win32 event kernel object. See
	 * <a href="http://msdn.microsoft.com/en-us/library/ms682396%28VS.85%29.aspx">MSDN</a>
	 * for details.
	 * @param securityAttributes	currently unsupported, pass null in here.
	 * @param manualReset
	 * @param initialState
	 * @param name
	 * @return	returns a handle (which is mapped to int in Java, or
	 * INVALID_HANDLE_VALUE on failure)
	 */
	static native long CreateEvent(SECURITY_ATTRIBUTES securityAttributes, boolean manualReset, boolean initialState, String name);

	static native boolean SetEvent(long eventHandle);

	static native boolean ResetEvent(long eventHandle);

	static final int FILE_NOTIFY_CHANGE_FILE_NAME = 0x00000001;
	static final int FILE_NOTIFY_CHANGE_DIR_NAME = 0x00000002;
	static final int FILE_NOTIFY_CHANGE_ATTRIBUTES = 0x00000004;
	static final int FILE_NOTIFY_CHANGE_SIZE = 0x00000008;
	static final int FILE_NOTIFY_CHANGE_LAST_WRITE = 0x00000010;
	static final int FILE_NOTIFY_CHANGE_LAST_ACCESS = 0x00000020;
	static final int FILE_NOTIFY_CHANGE_CREATION = 0x00000040;
	static final int FILE_NOTIFY_CHANGE_SECURITY = 0x00000100;

	static final int FILE_ACTION_ADDED                   = 0x00000001;
	static final int FILE_ACTION_REMOVED                 = 0x00000002;
	static final int FILE_ACTION_MODIFIED                = 0x00000003;
	static final int FILE_ACTION_RENAMED_OLD_NAME        = 0x00000004;
	static final int FILE_ACTION_RENAMED_NEW_NAME        = 0x00000005;
	static final int FILE_ACTION_ADDED_STREAM            = 0x00000006;
	static final int FILE_ACTION_REMOVED_STREAM          = 0x00000007;
	static final int FILE_ACTION_MODIFIED_STREAM         = 0x00000008;
	static final int FILE_ACTION_REMOVED_BY_DELETE       = 0x00000009;
	static final int FILE_ACTION_ID_NOT_TUNNELLED        = 0x0000000A;
	static final int FILE_ACTION_TUNNELLED_ID_COLLISION  = 0x0000000B;

	/**
	 * See <a href="http://msdn.microsoft.com/en-us/library/aa365465%28VS.85%29.aspx">MSDN</a>
	 * for details.
	 * @param directory	handle to a directory
	 * @param buffer	a native byte buffer
	 * @param watchSubtree
	 * @param notifyFilter
	 * @param bytesReturned	null or an integer array of at least size 1 - the value
	 *	 of the win32
	 * @param overlapped	an OVERLAPPED object (which maps to windows'
	 *	OVERLAPPED struct. Note that
	 * @param completionRoutine
	 * @return
	 */
	static native boolean ReadDirectoryChanges(long directoryHandle, ByteBuffer byteBuffer, boolean watchSubtree, int notifyFilter, int[] bytesReturned, OVERLAPPED overlapped, Runnable completionRoutine);

	static final int WAIT_OBJECT_0 = 0;
	static final int WAIT_ABANDONED_0 = 0x80;
	static final int WAIT_TIMEOUT = 0x102;
	static final int WAIT_FAILED = ~0;
	static final int MAXIMUM_WAIT_OBJECTS = 64;
	static final int INFINITE = 0xffffffff;

	/**
	 *
	 * @param handles
	 * @param waitAll
	 * @param milliseconds
	 * @return
	 */
	static native int WaitForMultipleObjects(long[] handles, boolean waitAll, int milliseconds);

	static native int GetLastError();

	static native String GetLastError_toString(int errorCode);

	static native boolean CloseHandle(long handle);

	static native boolean GetOverlappedResult(long fileHandle, OVERLAPPED overlapped, int[] numberOfBytesTransferred,boolean wait);

	static native boolean CancelIo(long fileHandle);

}
